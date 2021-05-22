package tp1.clients.user;

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
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import java.util.List;

public class UsersRestClient implements UsersClient {

    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    private final WebTarget target;

    public UsersRestClient(String serverUrl) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        client.property(ClientProperties.READ_TIMEOUT,    REPLY_TIMEOUT);
        target = client.target(serverUrl).path( RestUsers.PATH );
    }

    @Override
    public Result<String> createUser(User user)  {
        try {
            Response r = target.request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(user, MediaType.APPLICATION_JSON));

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(String.class));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<User> getUser(String userId, String password)  {
        try {
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(User.class));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user)  {
        try{
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .put(Entity.entity(user, MediaType.APPLICATION_JSON));

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(User.class));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String password)  {
        try {
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return Result.ok(r.readEntity(User.class));
            else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern)  {
        try {
            Response r = target.path("/").queryParam("query", pattern).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
                return Result.ok(r.readEntity(new GenericType<List<User>>() {}));
            } else
                return Result.error(Response.Status.fromStatusCode(r.getStatus()), new WebApplicationException(r.getStatus()));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE, e);
        }
    }
}
