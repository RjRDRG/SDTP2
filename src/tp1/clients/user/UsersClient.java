package tp1.clients.user;

import tp1.api.User;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;

import java.util.List;

public interface UsersClient {

    String SERVICE = "users";

    Result<User> getUser(String userId, String password);
}
