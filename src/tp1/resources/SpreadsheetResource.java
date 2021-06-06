package tp1.resources;

import com.google.gson.Gson;
import jakarta.inject.Singleton;
import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import tp1.kafka.KafkaPublisher;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.RecordProcessor;
import tp1.kafka.event.*;
import tp1.kafka.sync.SyncPoint;
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

	private static final String KAFKA_ADDRESS = "localhost:9092";

	private final String domainId;

	private final Map<String, Spreadsheet> spreadsheets;
	private final Map<String, Set<String>> spreadsheetOwners;

	private final SpreadsheetEngine engine;

	private final WebServiceType type;

	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	private KafkaPublisher publisher;
	private Gson json;
	private List<String> topicList;
	private SyncPoint sp;

	public SpreadsheetResource(String domainId, WebServiceType type, SyncPoint sp) {
		this.domainId = domainId;
		this.type = type;
		this.spreadsheets = new HashMap<>();
		this.spreadsheetOwners = new HashMap<>();
		this.engine = SpreadsheetEngineImpl.getInstance();

		publisher = KafkaPublisher.createPublisher(KAFKA_ADDRESS);
		json = new Gson();
		topicList = new LinkedList<String>();
		this.sp = sp;
		kafkaSubscriber(domainId);
	}

	private void kafkaSubscriber(String domainId) {
		topicList.add(domainId);

		KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber(KAFKA_ADDRESS , topicList);

		subscriber.start (new RecordProcessor() {
			@Override
			public void onReceive(ConsumerRecord<String, String> r) {
				System.out.println("Sequence Number: " + r.topic() + ", " + r.offset() + " -> " + r.value());

				KafkaEvent event = json.fromJson(r.value(), KafkaEvent.class);

				try {
					if (!event.getPublisherURI().equals(Discovery.getServiceURI())) //this is replica
						switch (r.key()) {
							case "createSpreadsheet": {
								CreateSpreadsheetEvent sheetEvent = json.fromJson(event.getJsonPayload(), CreateSpreadsheetEvent.class);
								createSpreadsheetEventConsumer(sheetEvent);
								break; }
							case "deleteSpreadsheet": {
								DeleteSpreadsheetEvent sheetEvent= json.fromJson(event.getJsonPayload(), DeleteSpreadsheetEvent.class);
								deleteSpreadsheetEventConsumer(sheetEvent);
								break; }
							case "updateCell": {
								UpdateCellEvent sheetEvent= json.fromJson(event.getJsonPayload(), UpdateCellEvent.class);
								updateCellEventConsumer(sheetEvent);
								break; }
							case "shareSpreadsheet": {
								ShareSpreadsheetEvent sheetEvent= json.fromJson(event.getJsonPayload(), ShareSpreadsheetEvent.class);
								shareSpreadsheetEventConsumer(sheetEvent);
								break; }
							case "unshareSpreadsheet": {
								UnshareSpreadsheetEvent sheetEvent= json.fromJson(event.getJsonPayload(), UnshareSpreadsheetEvent.class);
								unshareSpreadsheetEventConsumer(sheetEvent);
								break; }
							case "deleteUserSpreadsheets": {
								DeleteUserSpreadsheetsEvent sheetEvent= json.fromJson(event.getJsonPayload(), DeleteUserSpreadsheetsEvent.class);
								deleteUserSpreadsheetsEventConsumer(sheetEvent);
								break; }
						}
					else {
						sp.setResult(r.offset(), r.value());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
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

		String payload = json.toJson(new CreateSpreadsheetEvent(sheet, password));
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), payload);

		System.out.println("2222222DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		System.out.println("3333333DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");

		if(sequenceNumber >= 0)
			System.out.println("Message published with sequence number: " + sequenceNumber);
		else
			System.out.println("Failed to publish message");

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		CreateSpreadsheetEvent sheetEvent = json.fromJson(event.getJsonPayload(), CreateSpreadsheetEvent.class);

		System.out.println("44444444444DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");

		return createSpreadsheetEventConsumer(sheetEvent);
	}

	private String createSpreadsheetEventConsumer(CreateSpreadsheetEvent event) throws SheetsException {
		Spreadsheet sheet = event.getSheet();
		String password = event.getPassword();

		synchronized(this) {

			String spreadsheetOwner = sheet.getOwner();

			Result<User> result = Discovery.getLocalUsersClient().getUser(spreadsheetOwner, password);
			if(!result.isOK())
				throwWebAppException(type, Response.Status.BAD_REQUEST);

			String sheetId = UUID.randomUUID().toString();

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

		String payload = json.toJson(new DeleteSpreadsheetEvent(sheetId, password));
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		if(sequenceNumber >= 0)
			System.out.println("Message published with sequence number: " + sequenceNumber);
		else
			System.out.println("Failed to publish message");

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		DeleteSpreadsheetEvent sheetEvent = json.fromJson(event.getJsonPayload(), DeleteSpreadsheetEvent.class);
	}

	private void deleteSpreadsheetEventConsumer(DeleteSpreadsheetEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				password = event.getPassword();

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(type, Response.Status.NOT_FOUND);
			}

			Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
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

		Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
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

		String payload = json.toJson(new UpdateCellEvent(sheetId, cell, rawValue, userId, password));
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		if(sequenceNumber >= 0)
			System.out.println("Message published with sequence number: " + sequenceNumber);
		else
			System.out.println("Failed to publish message");

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		UpdateCellEvent sheetEvent = json.fromJson(event.getJsonPayload(), UpdateCellEvent.class);
	}

	private void updateCellEventConsumer(UpdateCellEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				cell = event.getCell(),
				rawValue = event.getRawValue(),
				userId = event.getUserId(),
				password = event.getPassword();

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

		String payload = json.toJson(new ShareSpreadsheetEvent(sheetId, userId, password));
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		if(sequenceNumber >= 0)
			System.out.println("Message published with sequence number: " + sequenceNumber);
		else
			System.out.println("Failed to publish message");

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		ShareSpreadsheetEvent sheetEvent = json.fromJson(event.getJsonPayload(), ShareSpreadsheetEvent.class);

	}

	private void shareSpreadsheetEventConsumer(ShareSpreadsheetEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				userId = event.getUserId(),
				password = event.getPassword();

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(type, Response.Status.NOT_FOUND);
			}

			Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
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

		String payload = json.toJson(new UnshareSpreadsheetEvent(sheetId, userId, password));
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		if(sequenceNumber >= 0)
			System.out.println("Message published with sequence number: " + sequenceNumber);
		else
			System.out.println("Failed to publish message");

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		UnshareSpreadsheetEvent sheetEvent = json.fromJson(event.getJsonPayload(), UnshareSpreadsheetEvent.class);
	}

	private void unshareSpreadsheetEventConsumer(UnshareSpreadsheetEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				userId = event.getUserId(),
				password = event.getPassword();

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
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
	public void deleteUserSpreadsheets(String userId, String password) {

		String payload = json.toJson(new DeleteUserSpreadsheetsEvent(userId, password));
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		if(sequenceNumber >= 0)
			System.out.println("Message published with sequence number: " + sequenceNumber);
		else
			System.out.println("Failed to publish message");

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		DeleteUserSpreadsheetsEvent sheetEvent = json.fromJson(event.getJsonPayload(), DeleteUserSpreadsheetsEvent.class);
	}

	private void deleteUserSpreadsheetsEventConsumer(DeleteUserSpreadsheetsEvent event) throws SheetsException {
		String userId = event.getUserId(),
				password = event.getPassword();

		synchronized (this) {

			Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
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

