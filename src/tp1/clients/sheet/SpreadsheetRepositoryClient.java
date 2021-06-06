package tp1.clients.sheet;

import tp1.api.Spreadsheet;
import tp1.api.service.util.Result;

public interface SpreadsheetRepositoryClient {

    Result<Void> createDirectory(String directoryPath);

    Result<String> uploadSpreadsheet(String path, Spreadsheet spreadsheet);

    Result<Void> delete(String path);

    Result<Spreadsheet> getSpreadsheet(String path);

}