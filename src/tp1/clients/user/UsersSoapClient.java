package tp1.clients.user;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class UsersSoapClient implements UsersClient {

    public final static String USERS_WSDL = "/users/?wsdl";

    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    public final SoapUsers target;

    public UsersSoapClient(String serverUrl) throws MalformedURLException {
        QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
        Service service = Service.create( new URL(serverUrl + USERS_WSDL), QNAME );
        target = service.getPort( SoapUsers.class );

        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
    }

    @Override
    public Result<String> createUser(User user) {
        try {
            return Result.ok(target.createUser(user));
        } catch (UsersException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        try {
            return Result.ok(target.getUser(userId, password));
        } catch (UsersException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        try {
            return Result.ok(target.updateUser(userId, password, user));
        } catch (UsersException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        try {
            return Result.ok(target.deleteUser(userId, password));
        } catch (UsersException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        try {
            return Result.ok(target.searchUsers(pattern));
        } catch (UsersException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }
}
