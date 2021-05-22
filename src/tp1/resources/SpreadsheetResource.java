package tp1.resources;

import jakarta.inject.Singleton;
import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.util.Result;
import tp1.clients.sheet.SpreadsheetClient;
import tp1.clients.sheet.SpreadsheetRetryClient;
import tp1.clients.user.UsersClient;
import tp1.clients.user.UsersRetryClient;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.WebServiceType;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static tp1.server.WebServiceType.SOAP;

@WebService(
		serviceName = SoapSpreadsheets.NAME,
		targetNamespace = SoapSpreadsheets.NAMESPACE,
		endpointInterface = SoapSpreadsheets.INTERFACE
)
@Singleton
public class SpreadsheetResource implements RestSpreadsheets, SoapSpreadsheets {

	private final String domainId;

	private final Map<String, Spreadsheet> spreadsheets;
	private final Map<String, Set<String>> spreadsheetOwners;

	private final SpreadsheetEngine engine;

	private final WebServiceType type;

	public static Discovery discovery;

	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	public SpreadsheetResource(String domainId, WebServiceType type) {
		this.domainId = domainId;
		this.type = type;
		this.spreadsheets = new HashMap<>();
		this.spreadsheetOwners = new HashMap<>();
		this.engine = SpreadsheetEngineImpl.getInstance();
	}




	public static void setDiscovery(Discovery discovery) {
		SpreadsheetResource.discovery = discovery;
	}

	private final static Map<String, SpreadsheetClient> cachedSpreadSheetClients = new ConcurrentHashMap<>();
	public static SpreadsheetClient getRemoteSpreadsheetClient(String domainId) {
		if(cachedSpreadSheetClients.containsKey(domainId))
			return cachedSpreadSheetClients.get(domainId);

		String serverUrl = discovery.knownUrisOf(domainId, SpreadsheetClient.SERVICE).stream()
				.findAny()
				.map(URI::toString)
				.orElse(null);

		SpreadsheetClient client = null;
		if(serverUrl != null) {
			try {
				client = new SpreadsheetRetryClient(serverUrl);
				cachedSpreadSheetClients.put(domainId,client);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return client;
	}


	private UsersClient cachedUserClient;
	private UsersClient getLocalUsersClient() {

		if(cachedUserClient == null) {
			String serverUrl = discovery.knownUrisOf(domainId, UsersClient.SERVICE).stream()
				.findAny()
				.map(URI::toString)
				.orElse(null);

			if(serverUrl != null) {
				try {
					cachedUserClient = new UsersRetryClient(serverUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return cachedUserClient;
	}

	public static void throwWebAppException(WebServiceType type, Response.Status status) throws SheetsException {
		if(type == SOAP)
			throw new SheetsException(status.name());
		else
			throw new WebApplicationException(status);
	}



	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {

		if( sheet == null || password == null)
			throwWebAppException(type, Response.Status.BAD_REQUEST);

		if (sheet.getColumns() <= 0 || sheet.getRows() <= 0)
			throwWebAppException(type, Response.Status.BAD_REQUEST);

		synchronized(this) {

			String spreadsheetOwner = sheet.getOwner();

			Result<User> result = getLocalUsersClient().getUser(spreadsheetOwner, password);
			if(!result.isOK())
				throwWebAppException(type, Response.Status.BAD_REQUEST);

			String sheetId;
			do {
				sheetId = UUID.randomUUID().toString();
			} while (spreadsheets.containsKey(sheetId));

			Spreadsheet spreadsheet = new Spreadsheet(sheet,sheetId,domainId);

			spreadsheets.put(sheetId, spreadsheet);

			if (!spreadsheetOwners.containsKey(spreadsheetOwner))
				spreadsheetOwners.put(spreadsheetOwner, new TreeSet<>());

			spreadsheetOwners.get(spreadsheetOwner).add(sheetId);

			return sheetId;
		}
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {

		if( sheetId == null || password == null ) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(type, Response.Status.NOT_FOUND);
			}

			Result<User> result = getLocalUsersClient().getUser(sheet.getOwner(), password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throwWebAppException(type, Response.Status.FORBIDDEN);
			else if(!result.isOK())
				throwWebAppException(type, Response.Status.BAD_REQUEST);

			spreadsheetOwners.get(sheet.getOwner()).remove(sheetId);
			spreadsheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {

		if( sheetId == null || userId == null ) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		Spreadsheet sheet = spreadsheets.get(sheetId);

		if( sheet == null ) {
			throwWebAppException(type, Response.Status.NOT_FOUND);
		}

		Result<User> result = getLocalUsersClient().getUser(userId, password);
		if(result.error() == Result.ErrorCode.FORBIDDEN)
			throwWebAppException(type, Response.Status.FORBIDDEN);
		else if(!result.isOK())
			throwWebAppException(type, Response.Status.BAD_REQUEST);

		if (!userId.equals(sheet.getOwner())) {

			if (!sheet.getSharedWith().stream().anyMatch(user -> user.contains(userId)))
				throwWebAppException(type, Response.Status.FORBIDDEN);

		}

		return sheet;
	}

	@Override
	public String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range) throws SheetsException {

		if( sheetId == null || userId == null || range == null) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		Spreadsheet spreadsheet = spreadsheets.get(sheetId);

		if( spreadsheet == null ) {
			throwWebAppException(type, Response.Status.NOT_FOUND);
		}

		if (!userId.equals(spreadsheet.getOwner()) && !spreadsheet.getSharedWith().contains(userId)) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		return new CellRange(range).extractRangeValuesFrom(result);
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {

		Spreadsheet spreadsheet = getSpreadsheet(sheetId, userId, password);

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		return result;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException {

		if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		synchronized(this) {

			Spreadsheet spreadsheet = getSpreadsheet(sheetId, userId, password);

			try {
				Pair<Integer,Integer> coordinates =  Cell.CellId2Indexes(cell);

				spreadsheet.placeCellRawValue(coordinates.getLeft(),coordinates.getRight(), rawValue);
			} catch (InvalidCellIdException e) {
				throwWebAppException(type, Response.Status.BAD_REQUEST);
			}

		}

	}


	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {

		if( sheetId == null || userId == null || password == null ) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(type, Response.Status.NOT_FOUND);
			}

			Result<User> result = getLocalUsersClient().getUser(sheet.getOwner(), password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throwWebAppException(type, Response.Status.FORBIDDEN);
			else if(!result.isOK())
				throwWebAppException(type, Response.Status.BAD_REQUEST);

			Set<String> sharedWith = sheet.getSharedWith();

			if (sharedWith.contains(userId))
				throwWebAppException(type, Response.Status.CONFLICT);

			sharedWith.add(userId);
		}
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {

		if( sheetId == null || userId == null || password == null ) {
			throwWebAppException(type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			Result<User> result = getLocalUsersClient().getUser(sheet.getOwner(), password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throwWebAppException(type, Response.Status.FORBIDDEN);
			else if(!result.isOK())
				throwWebAppException(type, Response.Status.BAD_REQUEST);

			Set<String> sharedWith = sheet.getSharedWith();

			if (!sharedWith.contains(userId))
				throwWebAppException(type, Response.Status.NOT_FOUND);

			sharedWith.remove(userId);
		}
	}

	@Override
	public void deleteUserSpreadsheets(String userId, String password) throws SheetsException {
		synchronized (this) {

			Result<User> result = getLocalUsersClient().getUser(userId, password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throwWebAppException(type, Response.Status.FORBIDDEN);
			else if(!result.isOK())
				throwWebAppException(type, Response.Status.BAD_REQUEST);

			Set<String> sheets = spreadsheetOwners.get(userId);

			sheets.forEach(id -> spreadsheets.remove(id));
			spreadsheetOwners.remove(userId);
		}
	}
}

