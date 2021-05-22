package tp1.clients.sheet;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;

public class SpreadsheetRestClient implements SpreadsheetClient {

    private final WebTarget target;

    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    public SpreadsheetRestClient(String serverUrl) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        client.property(ClientProperties.READ_TIMEOUT,    REPLY_TIMEOUT);
        target = client.target(serverUrl).path( RestSpreadsheets.PATH );
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password)   {
        try {
            Response r = target.queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(sheet, MediaType.APPLICATION_JSON));

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(String.class));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password)  {
        try {
            Response r = target.path(sheetId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();

            if (r.getStatus() != Response.Status.OK.getStatusCode())
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
            else
                return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password)  {
        try {
            Response r = target.path(sheetId).queryParam("userId", userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(Spreadsheet.class));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password)  {
        try {
            Response r = target.path(sheetId).path("values").queryParam("userId", userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(new GenericType<String[][]>() {}));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range)   {
        try {
            Response r = target.path("reference").path(sheetId).queryParam("userId", userId).queryParam("range", range).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(new GenericType<String[][]>() {}));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password)   {
        try {
            Response r = target.path(sheetId).path(cell).queryParam("").queryParam("userId", userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .put(Entity.entity(rawValue, MediaType.APPLICATION_JSON));

            if (r.getStatus() != Response.Status.OK.getStatusCode())
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
            else
                return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password)   {
        try {
            Response r = target.path(sheetId).path("share").path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(userId, MediaType.APPLICATION_JSON));

            if (r.getStatus() != Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
            else
                return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        try {
            Response r = target.path(sheetId).path("share").path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();

            if (r.getStatus() != Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
            else
                return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        try {
            Response r = target.path("spreadsheets").path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();

            if (r.getStatus() != Response.Status.OK.getStatusCode())
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
            else
                return Result.ok();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }
}
