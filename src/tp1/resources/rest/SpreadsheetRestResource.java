package tp1.resources.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.SpreadsheetsImpl;

import static tp1.api.service.util.Result.mapError;

@Singleton
public class SpreadsheetRestResource implements RestSpreadsheets {

	private final String domainId;
	private final SpreadsheetsImpl impl;

	public SpreadsheetRestResource(String domainId) {
		this.domainId = domainId;
		this.impl = new SpreadsheetsImpl(domainId);
	}

	@Override
	public String createSpreadsheet(Long version, Spreadsheet sheet, String password) {
		Result<String> result = impl.createSpreadsheet(sheet, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public void deleteSpreadsheet(Long version, String sheetId, String password) {
		Result<Void> result = impl.deleteSpreadsheet(sheetId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}

	@Override
	public Spreadsheet getSpreadsheet(Long version, String sheetId, String userId, String password) {
		Result<Spreadsheet> result = impl.getSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public String[][] getReferencedSpreadsheetValues(Long version, String sheetId, String userId, String range) {
		Result<String[][]> result = impl.getReferencedSpreadsheetValues(sheetId, userId, range);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public String[][] getSpreadsheetValues(Long version, String sheetId, String userId, String password) {
		Result<String[][]> result = impl.getSpreadsheetValues(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public void updateCell(Long version, String sheetId, String cell, String rawValue, String userId, String password) {
		Result<Void> result = impl.updateCell(sheetId, cell, rawValue, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}


	@Override
	public void shareSpreadsheet(Long version, String sheetId, String userId, String password) {
		Result<Void> result = impl.shareSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}

	@Override
	public void unshareSpreadsheet(Long version, String sheetId, String userId, String password) {
		Result<Void> result = impl.unshareSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}

	@Override
	public void deleteUserSpreadsheets(Long version, String userId, String password) {
		Result<Void> result = impl.deleteUserSpreadsheets(userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}
}

