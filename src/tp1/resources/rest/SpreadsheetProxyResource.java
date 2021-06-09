package tp1.resources.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.clients.sheet.SpreadsheetRepositoryClient;
import tp1.discovery.Discovery;
import tp1.impl.SpreadsheetEngineImpl;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class SpreadsheetProxyResource implements RestSpreadsheets {

    private final String domainId;

    private final SpreadsheetEngine engine;

    private final SpreadsheetRepositoryClient repositoryClient;

    private static Logger Log = Logger.getLogger(SpreadsheetRestResource.class.getName());

    public SpreadsheetProxyResource(String domainId, SpreadsheetRepositoryClient repositoryClient) {
        this.domainId = domainId;
        this.repositoryClient = repositoryClient;
        this.engine = SpreadsheetEngineImpl.getInstance();
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {

        if( sheet == null || password == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (sheet.getColumns() <= 0 || sheet.getRows() <= 0)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        synchronized(this) {

            String spreadsheetOwner = sheet.getOwner();

            Result<User> result = Discovery.getLocalUsersClient().getUser(spreadsheetOwner, password);
            if(!result.isOK())
                throw new WebApplicationException(Response.Status.BAD_REQUEST);

            String sheetId = sheet.getOwner()+"~"+UUID.randomUUID();

            Spreadsheet spreadsheet = new Spreadsheet(sheet,sheetId,domainId);

            String path = "/"+domainId+"/"+sheetId.replace('~','/');

            Result<String> reply = repositoryClient.uploadSpreadsheet(path, spreadsheet);

            if(!reply.isOK()) {
                try {
                    reply.value();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }

            return sheetId;
        }
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {

        if( sheetId == null || password == null ) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        synchronized (this) {

            String path = "/"+domainId+"/"+sheetId.replace('~','/');

            Spreadsheet sheet = null;
            try {
                sheet = repositoryClient.getSpreadsheet(path).value();
            } catch (Exception e) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                throw new WebApplicationException(Response.Status.BAD_REQUEST);

            try {
                repositoryClient.delete(path).value();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public Spreadsheet getSpreadsheet(Long version, String sheetId, String userId, String password) {

        if( sheetId == null || userId == null ) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        String path = "/"+domainId+"/"+sheetId.replace('~','/');

        Spreadsheet sheet = null;
        try {
            sheet = repositoryClient.getSpreadsheet(path).value();
        } catch (Exception e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
        if (result.error() == Result.ErrorCode.FORBIDDEN)
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        else if (!result.isOK())
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (!userId.equals(sheet.getOwner())) {
            if (!sheet.getSharedWith().stream().anyMatch(user -> user.contains(userId)))
                throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        return sheet;
    }

    @Override
    public String[][] getReferencedSpreadsheetValues(HttpHeaders headers, String sheetId, String userId, String range) {

        if( sheetId == null || userId == null || range == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        String path = "/"+domainId+"/"+sheetId.replace('~','/');

        Spreadsheet sheet = null;
        try {
            sheet = repositoryClient.getSpreadsheet(path).value();
        } catch (Exception e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (!userId.equals(sheet.getOwner()) && !sheet.getSharedWith().contains(userId)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Long.parseLong(e.getValue().get(0))
                ));

        String[][] result = null;
        try {
            result = engine.computeSpreadsheetValues(versions, sheet);
        } catch (Exception exception) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return new CellRange(range).extractRangeValuesFrom(result);
    }

    @Override
    public String[][] getSpreadsheetValues(HttpHeaders headers, String sheetId, String userId, String password) {

        Spreadsheet spreadsheet = getSpreadsheet(null, sheetId, userId, password);

        Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Long.parseLong(e.getValue().get(0))
                ));

        String[][] result = null;
        try {
            result = engine.computeSpreadsheetValues(versions, spreadsheet);
        } catch (Exception exception) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return result;
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

        if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        synchronized(this) {
            Spreadsheet spreadsheet = getSpreadsheet(null, sheetId, userId, password);

            System.out.println(sheetId + " " + spreadsheet.toString());

            try {
                Pair<Integer,Integer> coordinates =  Cell.CellId2Indexes(cell);

                spreadsheet.placeCellRawValue(coordinates.getLeft(),coordinates.getRight(), rawValue);

                String path = "/"+domainId+"/"+sheetId.replace('~','/');

                if (!repositoryClient.uploadSpreadsheet(path, spreadsheet).isOK())
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);

            } catch (InvalidCellIdException e) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
    }


    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) {

        if( sheetId == null || userId == null || password == null ) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        synchronized (this) {

            String path = "/"+domainId+"/"+sheetId.replace('~','/');

            Spreadsheet sheet = null;
            try {
                sheet = repositoryClient.getSpreadsheet(path).value();
            } catch (Exception e) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                throw new WebApplicationException(Response.Status.BAD_REQUEST);

            Set<String> sharedWith = sheet.getSharedWith();

            if (sharedWith.contains(userId))
                throw new WebApplicationException(Response.Status.CONFLICT);

            sharedWith.add(userId);

            if (!repositoryClient.uploadSpreadsheet(path, sheet).isOK())
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) {

        if( sheetId == null || userId == null || password == null ) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        synchronized (this) {

            String path = "/"+domainId+"/"+sheetId.replace('~','/');

            Spreadsheet sheet = null;
            try {
                sheet = repositoryClient.getSpreadsheet(path).value();
            } catch (Exception e) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                throw new WebApplicationException(Response.Status.BAD_REQUEST);

            Set<String> sharedWith = sheet.getSharedWith();

            if (!sharedWith.contains(userId))
                throw new WebApplicationException(Response.Status.NOT_FOUND);

            sharedWith.remove(userId);

            if (!repositoryClient.uploadSpreadsheet(path, sheet).isOK())
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) {
        synchronized (this) {

            Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                throw new WebApplicationException(Response.Status.BAD_REQUEST);

            if(!repositoryClient.delete("/"+domainId+"/"+userId).isOK())
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}


