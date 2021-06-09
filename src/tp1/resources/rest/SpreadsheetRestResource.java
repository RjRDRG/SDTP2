package tp1.resources.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.util.Result;
import tp1.impl.SpreadsheetsImpl;

import java.util.Map;
import java.util.stream.Collectors;

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
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Result<String> result = impl.createSpreadsheet(sheet, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
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
	public String[][] getReferencedSpreadsheetValues(HttpHeaders headers, String sheetId, String userId, String range) {
		Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> Long.parseLong(e.getValue().get(0))
				));

		Result<String[][]> result = impl.getReferencedSpreadsheetValues(versions, sheetId, userId, range);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public String[][] getSpreadsheetValues(HttpHeaders headers, String sheetId, String userId, String password) {
		Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> Long.parseLong(e.getValue().get(0))
				));

		Result<String[][]> result = impl.getSpreadsheetValues(versions, sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		Result<Void> result = impl.updateCell(sheetId, cell, rawValue, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}


	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		Result<Void> result = impl.shareSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		Result<Void> result = impl.unshareSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}

	@Override
	public void deleteUserSpreadsheets(String userId, String password) {
		Result<Void> result = impl.deleteUserSpreadsheets(userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
	}
}

