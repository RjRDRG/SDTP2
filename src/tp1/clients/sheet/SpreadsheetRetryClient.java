package tp1.clients.sheet;

import tp1.api.service.util.Result;

import java.util.function.Supplier;

import static tp1.api.service.util.Result.ErrorCode.NOT_AVAILABLE;

public class SpreadsheetRetryClient implements SpreadsheetClient{

    public final static int MAX_RETRIES = 10;
    public final static long RETRY_PERIOD = 1000;

    private final SpreadsheetClient client;

    public SpreadsheetRetryClient(String serverUrl) throws Exception{
        if (serverUrl.contains("/rest"))
            this.client = new SpreadsheetRestClient(serverUrl);
        else
            this.client = new SpreadsheetSoapClient(serverUrl);
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
        return retry( () -> client.getReferencedSpreadsheetValues(sheetId,userId,range));
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        return retry( () -> client.deleteUserSpreadsheets(userId,password));
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
