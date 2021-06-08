package tp1.clients.sheet;

import tp1.api.Spreadsheet;
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

    private <T> Result<T> retry(Function<SpreadsheetClient, Result<T>> supplier) {
        Result<T> result = Result.error(NOT_AVAILABLE, new Exception("Service Not Available: " + domainId));

        for(Map.Entry<String, SpreadsheetClient> entry : clients.entrySet()) {
            result = supplier.apply(entry.getValue());
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
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        return retry( client -> client.createSpreadsheet(sheet,password));
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        return retry( client -> client.deleteSpreadsheet(sheetId,password));
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        return retry( client -> client.getSpreadsheet(sheetId,userId,password));
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        return retry( client -> client.getSpreadsheetValues(sheetId,userId,password));
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
        return retry( client -> client.getReferencedSpreadsheetValues(sheetId,userId,range));
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        return retry( client -> client.updateCell(sheetId, cell, rawValue, userId, password));
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        return retry( client -> client.shareSpreadsheet(sheetId,userId,password));
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        return retry( client -> client.unshareSpreadsheet(sheetId,userId,password));
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        return retry( client -> client.deleteUserSpreadsheets(userId,password));
    }
}
