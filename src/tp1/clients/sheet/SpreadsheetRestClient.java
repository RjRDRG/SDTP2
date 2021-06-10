package tp1.clients.sheet;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;

import java.util.Map;
import java.util.stream.Collectors;

import static tp1.api.service.rest.RestSpreadsheets.HEADER_VERSION;

public class SpreadsheetRestClient implements SpreadsheetClient {

    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    private final WebTarget target;
    private final String domainId;

    public SpreadsheetRestClient(String serverUrl, String domainId) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        client.property(ClientProperties.READ_TIMEOUT,    REPLY_TIMEOUT);
        target = client.target(serverUrl).path( RestSpreadsheets.PATH );
        this.domainId = domainId;
    }

    private <T> Result<T> addHeaders(Result<T> result, Response response) {
        Map<String, String> headers = response.getStringHeaders().entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(HEADER_VERSION))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(0)
                ));

        result.setOthers(headers);
        return result;
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(Map<String,Long> versions, String sheetId, String userId, String range) {
        try {
            Invocation.Builder builder = target.path("reference").path(sheetId).queryParam("userId", userId).queryParam("range", range).request()
                    .accept(MediaType.APPLICATION_JSON);

            for (Map.Entry<String,Long> entry : versions.entrySet()) {
                builder.header(entry.getKey(), entry.getValue().toString());
            }

            Response r = builder.get();

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

            if (r.getStatus() != Response.Status.NO_CONTENT.getStatusCode())
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
