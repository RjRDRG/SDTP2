package tp1.resources.soap;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.SpreadsheetsImpl;

import java.util.HashMap;
import java.util.UUID;

import static tp1.api.service.util.Result.mapError;

@WebService(
		serviceName = SoapSpreadsheets.NAME,
		targetNamespace = SoapSpreadsheets.NAMESPACE,
		endpointInterface = SoapSpreadsheets.INTERFACE
)
public class SpreadsheetSoapResource implements SoapSpreadsheets {

	private final String domainId;
	private final SpreadsheetsImpl impl;

	public SpreadsheetSoapResource(String domainId) {
		this.domainId = domainId;
		this.impl = new SpreadsheetsImpl(domainId);
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
		sheet.setSheetId(UUID.randomUUID().toString());
		Result<String> result = impl.createSpreadsheet(sheet, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
		else
			return result.value();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
		Result<Void> result = impl.deleteSpreadsheet(sheetId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Result<Spreadsheet> result = impl.getSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
		else
			return result.value();
	}

	@Override
	public String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range) throws SheetsException {
		Result<String[][]> result = impl.getReferencedSpreadsheetValues(new HashMap<>(), sheetId, userId, range);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
		else
			return result.value();
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
		Result<String[][]> result = impl.getSpreadsheetValues(new HashMap<>(), sheetId, userId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
		else
			return result.value();
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException {
		Result<Void> result = impl.updateCell(sheetId, cell, rawValue, userId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
	}


	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Result<Void> result = impl.shareSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Result<Void> result = impl.unshareSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
	}

	@Override
	public void deleteUserSpreadsheets(String userId, String password) throws SheetsException {
		Result<Void> result = impl.deleteUserSpreadsheets(userId, password);
		if(!result.isOK())
			throw new SheetsException(result.error().name());
	}
}

