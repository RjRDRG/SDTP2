package tp1.clients.sheet;

import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.util.Result;

import java.util.Set;

public interface SpreadsheetClient {

    String SERVICE = "sheets";

    Result<String> createSpreadsheet(Spreadsheet sheet, String password );

    Result<Void> deleteSpreadsheet(String sheetId, String password);

    Result<Spreadsheet> getSpreadsheet(String sheetId , String userId, String password);

    Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password);

    Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range);

    Result<Void> updateCell( String sheetId, String cell, String rawValue, String userId, String password);

    Result<Void> shareSpreadsheet(String sheetId, String userId, String password);

    Result<Void> unshareSpreadsheet( String sheetId, String userId,  String password);

    Result<Void> deleteUserSpreadsheets(String userId, String password);
}