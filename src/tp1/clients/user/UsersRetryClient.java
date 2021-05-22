package tp1.clients.user;

import tp1.api.User;
import tp1.api.service.util.Result;

import java.util.List;
import java.util.function.Supplier;

import static tp1.api.service.util.Result.ErrorCode.NOT_AVAILABLE;

public class UsersRetryClient implements UsersClient{

    public final static int MAX_RETRIES = 10;
    public final static long RETRY_PERIOD = 1000;

    private final UsersClient client;

    public UsersRetryClient(String serverUrl) throws Exception {
        if (serverUrl.contains("/rest"))
            client = new UsersRestClient(serverUrl);
        else
            client = new UsersSoapClient(serverUrl);
    }

    private <T> Result<T> retry(Supplier<Result<T>> supplier) {
        Result<T> result;

        int retries=0;
        do {
            retries++;

            result = supplier.get();
            if(result.error() != NOT_AVAILABLE)
                return result;;

            try { Thread.sleep(RETRY_PERIOD); } catch (InterruptedException ignored) {}

        } while (retries < MAX_RETRIES);

        return result;
    }

    @Override
    public Result<String> createUser(User user) {
        return retry(() -> client.createUser(user));
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        return retry( () -> client.getUser(userId,password));
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        return retry(() -> client.updateUser(userId,password,user));
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        return retry(() -> client.deleteUser(userId,password));
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        return retry( () -> client.searchUsers(pattern));
    }


}
