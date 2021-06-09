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

import java.util.Map;
import java.util.stream.Collectors;

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

    private <T> Result<T> addHeaders(Result<T> result, Response response) {
        Map<String, String> headers = response.getStringHeaders().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(0)
                ));

        result.setOthers(headers);
        return result;
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
        try {
            Response r = target.path("reference").path(sheetId).queryParam("userId", userId).queryParam("range", range).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return addHeaders(
                    Result.ok(r.readEntity(new GenericType<String[][]>() {})), r
                );
            else
                return addHeaders(
                    Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus())), r
                );
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        try {
            Response r = target.path("all").path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();

            if (r.getStatus() != Response.Status.OK.getStatusCode())
                return addHeaders(
                        Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus())), r
                );
            else
                return addHeaders(Result.ok(), r);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }
}
