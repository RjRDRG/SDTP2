package tp1.clients.sheet;

import tp1.api.service.util.Result;
import tp1.discovery.Discovery;

import java.util.Map;
import java.util.function.Function;

import static tp1.api.service.util.Result.ErrorCode.NOT_AVAILABLE;

public class SpreadsheetMultiClient implements SpreadsheetClient{

    private final String domainId;
    private final Map<String, SpreadsheetClient> clients;

    public SpreadsheetMultiClient(String domainId, Map<String, SpreadsheetClient> clients) {
        this.domainId = domainId;
        this.clients = clients;
    }

    private <T> Result<T> multi(Function<SpreadsheetClient, Result<T>> function) {
        Result<T> result = Result.error(NOT_AVAILABLE, new Exception("Service Not Available: " + domainId));

        for(Map.Entry<String, SpreadsheetClient> entry : clients.entrySet()) {
            result = function.apply(entry.getValue());
            if(result.error() != NOT_AVAILABLE) {
                return result;
            }
            else {
                Discovery.removeSpreadsheetClient(domainId, entry.getKey());
            }
        }

        return result;
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
        return multi(client -> client.getReferencedSpreadsheetValues(sheetId,userId,range));
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        return multi(client -> client.deleteUserSpreadsheets(userId,password));
    }
}
