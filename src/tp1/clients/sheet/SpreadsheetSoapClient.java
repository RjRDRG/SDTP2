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
import java.util.Map;

public class SpreadsheetSoapClient implements SpreadsheetClient {

    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    public final SoapSpreadsheets target;

    public final String domainId;

    public SpreadsheetSoapClient (String serverUrl, String domainId) throws MalformedURLException {
        QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
        Service service = Service.create( new URL(serverUrl + SPREADSHEETS_WSDL), QNAME );
        target = service.getPort( SoapSpreadsheets.class );

        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

        this.domainId =  domainId;
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(Map<String,Long> versions, String sheetId, String userId, String range) {
        try {
            return Result.ok(target.getReferencedSpreadsheetValues(sheetId, userId, range));
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
