package tp1.clients.sheet;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class SpreadsheetSoapClient implements SpreadsheetClient {

    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    public final SoapSpreadsheets target;

    public SpreadsheetSoapClient (String serverUrl) throws MalformedURLException {
        QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
        Service service = Service.create( new URL(serverUrl + SPREADSHEETS_WSDL), QNAME );
        target = service.getPort( SoapSpreadsheets.class );

        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        try {
            return Result.ok(target.createSpreadsheet(sheet, password));
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        try {
            target.deleteSpreadsheet(sheetId, password);
            return Result.ok();
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password)  {
        try {
            return Result.ok(target.getSpreadsheet(sheetId, userId, password));
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        try {
            return Result.ok(target.getSpreadsheetValues(sheetId, userId, password));
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
        try {
            return Result.ok(target.getReferencedSpreadsheetValues(sheetId, userId, range));
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        try {
            target.updateCell(sheetId, cell, rawValue, userId, password);
            return Result.ok();
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        try {
            target.shareSpreadsheet(sheetId, userId, password);
            return Result.ok();
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        try {
            target.unshareSpreadsheet(sheetId, userId, password);
            return Result.ok();
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        try {
            target.deleteUserSpreadsheets(userId, password);
            return Result.ok();
        } catch (SheetsException e) {
            return Result.error(e.getMessage(),e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.NOT_AVAILABLE,e);
        }
    }
}
