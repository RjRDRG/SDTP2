package tp1.impl;


import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.util.Result;
import tp1.discovery.Discovery;
import tp1.util.Cell;
import tp1.util.CellRange;

import java.util.*;

public class SpreadsheetsImpl {

    private final String domainId;

    private final Map<String, Spreadsheet> spreadsheets;
    private final Map<String, Set<String>> spreadsheetOwners;

    private final SpreadsheetEngine engine;

    public SpreadsheetsImpl(String domainId) {
        this.domainId = domainId;
        this.spreadsheets = new HashMap<>();
        this.spreadsheetOwners = new HashMap<>();
        this.engine = SpreadsheetEngineImpl.getInstance();
    }

    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {

        if( sheet == null || password == null)
            return Result.error(Response.Status.BAD_REQUEST);

        if (sheet.getColumns() <= 0 || sheet.getRows() <= 0)
            return Result.error(Response.Status.BAD_REQUEST);

        synchronized(this) {

            String spreadsheetOwner = sheet.getOwner();

            Result<User> result = Discovery.getLocalUsersClient().getUser(spreadsheetOwner, password);
            if(!result.isOK())
                return Result.error(Response.Status.BAD_REQUEST);

            String sheetId;
            do {
                sheetId = UUID.randomUUID().toString();
            } while (spreadsheets.containsKey(sheetId));

            Spreadsheet spreadsheet = new Spreadsheet(sheet,sheetId,domainId);

            spreadsheets.put(sheetId, spreadsheet);

            if (!spreadsheetOwners.containsKey(spreadsheetOwner))
                spreadsheetOwners.put(spreadsheetOwner, new TreeSet<>());

            spreadsheetOwners.get(spreadsheetOwner).add(sheetId);

            return Result.ok(sheetId);
        }
    }

    public Result<Void> deleteSpreadsheet(String sheetId, String password) {

        if( sheetId == null || password == null ) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        synchronized (this) {

            Spreadsheet sheet = spreadsheets.get(sheetId);

            if( sheet == null ) {
                return Result.error(Response.Status.NOT_FOUND);
            }

            Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                return Result.error(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                return Result.error(Response.Status.BAD_REQUEST);

            spreadsheetOwners.get(sheet.getOwner()).remove(sheetId);
            spreadsheets.remove(sheetId);

            return Result.ok();
        }
    }

    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {

        if( sheetId == null || userId == null ) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        Spreadsheet sheet = spreadsheets.get(sheetId);

        if( sheet == null ) {
            return Result.error(Response.Status.NOT_FOUND);
        }

        Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
        if(result.error() == Result.ErrorCode.FORBIDDEN) {
            return Result.error(Response.Status.FORBIDDEN);
        }
        else if(result.error() == Result.ErrorCode.NOT_FOUND) {
            return Result.error(Response.Status.NOT_FOUND);
        }
        else if(!result.isOK()) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        if (!userId.equals(sheet.getOwner())) {
            if (!sheet.getSharedWith().stream().anyMatch(user -> user.contains(userId)))
                return Result.error(Response.Status.FORBIDDEN);
        }

        return Result.ok(sheet);
    }

    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {

        if( sheetId == null || userId == null || range == null) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        Spreadsheet spreadsheet = spreadsheets.get(sheetId);

        if( spreadsheet == null ) {
            return Result.error(Response.Status.NOT_FOUND);
        }

        if (!userId.equals(spreadsheet.getOwner()) && !spreadsheet.getSharedWith().contains(userId)) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        String[][] result = null;
        try {
            result = engine.computeSpreadsheetValues(spreadsheet);
        } catch (Exception exception) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        result = new CellRange(range).extractRangeValuesFrom(result);

        return Result.ok(result);
    }

    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {

        Result<Spreadsheet> spreadsheet = getSpreadsheet(sheetId, userId, password);

        if (!spreadsheet.isOK())
            return Result.error(spreadsheet.error());

        String[][] result = null;
        try {
            result = engine.computeSpreadsheetValues(spreadsheet.value());
        } catch (Exception exception) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        return Result.ok(result);
    }

    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

        if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        synchronized(this) {

            Result<Spreadsheet> spreadsheet = getSpreadsheet(sheetId, userId, password);

            if (!spreadsheet.isOK())
                return Result.error(spreadsheet.error());

            try {
                Pair<Integer,Integer> coordinates =  Cell.CellId2Indexes(cell);

                spreadsheet.value().placeCellRawValue(coordinates.getLeft(),coordinates.getRight(), rawValue);
            } catch (Exception e) {
                return Result.error(Response.Status.BAD_REQUEST);
            }

            return Result.ok();
        }
    }


    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {

        if( sheetId == null || userId == null || password == null ) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        synchronized (this) {

            Spreadsheet sheet = spreadsheets.get(sheetId);

            if( sheet == null ) {
                return Result.error(Response.Status.NOT_FOUND);
            }

            Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                return Result.error(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                return Result.error(Response.Status.BAD_REQUEST);

            Set<String> sharedWith = sheet.getSharedWith();

            if (sharedWith.contains(userId))
                return Result.error(Response.Status.CONFLICT);

            sharedWith.add(userId);

            return Result.ok();
        }
    }

    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {

        if( sheetId == null || userId == null || password == null ) {
            return Result.error(Response.Status.BAD_REQUEST);
        }

        synchronized (this) {

            Spreadsheet sheet = spreadsheets.get(sheetId);

            Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                return Result.error(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                return Result.error(Response.Status.BAD_REQUEST);

            Set<String> sharedWith = sheet.getSharedWith();

            if (!sharedWith.contains(userId))
                return Result.error(Response.Status.NOT_FOUND);

            sharedWith.remove(userId);

            return Result.ok();
        }
    }

    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        synchronized (this) {

            Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
            if(result.error() == Result.ErrorCode.FORBIDDEN)
                return Result.error(Response.Status.FORBIDDEN);
            else if(!result.isOK())
                return Result.error(Response.Status.BAD_REQUEST);

            Set<String> sheets = spreadsheetOwners.get(userId);

            sheets.forEach(spreadsheets::remove);
            spreadsheetOwners.remove(userId);

            return Result.ok();
        }
    }
}
