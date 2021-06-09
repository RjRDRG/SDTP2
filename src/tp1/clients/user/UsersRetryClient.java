package tp1.clients.user;

import tp1.api.User;
import tp1.api.service.util.Result;
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

    @Override
    public Result<User> getUser(String userId, String password) {
        return retry( () -> client.getUser(userId,password));
    }

    private static <T> Result<T> retry(Supplier<Result<T>> supplier) {
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
}
