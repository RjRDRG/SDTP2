package tp1.clients.sheet;

import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.util.Result;

import java.util.Set;

public interface SpreadsheetClient {

    String SERVICE = "sheets";

    Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range);

    Result<Void> deleteUserSpreadsheets(String userId, String password);
}