package tp1.clients.user;

import tp1.api.User;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;

import java.util.List;

public interface UsersClient {

    String SERVICE = "users";

    Result<String> createUser(User user);

    Result<User> getUser(String userId, String password);

    Result<User> updateUser(String userId, String password, User user);

    Result<User> deleteUser(String userId, String password);

    Result<List<User>> searchUsers(String pattern);
}
