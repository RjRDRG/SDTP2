package tp1.resources;

import com.google.gson.Gson;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.rest.RestSpreadsheetsReplicated;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.util.Result;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.kafka.KafkaPublisher;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.RecordProcessor;
import tp1.kafka.event.*;
import tp1.kafka.sync.SyncPoint;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static tp1.kafka.KafkaUtils.KAFKA_ADDRESS;

@Singleton
public class SpreadsheetReplicatedResource implements RestSpreadsheetsReplicated {

	private final String domainId;

	private final Map<String, Spreadsheet> spreadsheets;
	private final Map<String, Set<String>> spreadsheetOwners;

	private final SpreadsheetEngine engine;

	private KafkaPublisher publisher;
	private final Gson json;
	private final SyncPoint sp;

	private final String uri;

	public SpreadsheetReplicatedResource(String domainId, SyncPoint sp, String uri) {
		this.domainId = domainId;
		this.uri = uri;
		this.spreadsheets = new HashMap<>();
		this.spreadsheetOwners = new HashMap<>();
		this.engine = SpreadsheetEngineImpl.getInstance();

		this.json = new Gson();
		this.sp = sp;
		this.publisher = null;

		registerInKafka();
	}

	private void registerInKafka() {
		try {
			this.publisher = KafkaPublisher.createPublisher(KAFKA_ADDRESS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			kafkaSubscriber(this.domainId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void kafkaSubscriber(String domainId) {
		List<String> topicList = new ArrayList<>();
		topicList.add(domainId);
		KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber(KAFKA_ADDRESS , topicList);

		subscriber.start (new RecordProcessor() {
			@Override
			public void onReceive(ConsumerRecord<String, String> r) {
				KafkaEvent event = json.fromJson(r.value(), KafkaEvent.class);

				System.out.println(uri + " -> Received " + r.offset());

				try {
					if (!event.getPublisherURI().equals(Discovery.getServiceURI())) {
						switch (event.getPayloadType()) {
							case CreateSpreadsheetEvent -> {
								CreateSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), CreateSpreadsheetEvent.class);
								createSpreadsheetEventConsumer(sheetEvent);
							}
							case DeleteSpreadsheetEvent -> {
								DeleteSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), DeleteSpreadsheetEvent.class);
								deleteSpreadsheetEventConsumer(sheetEvent);
							}
							case UpdateCellEvent -> {
								UpdateCellEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), UpdateCellEvent.class);
								updateCellEventConsumer(sheetEvent);
							}
							case ShareSpreadsheetEvent -> {
								ShareSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), ShareSpreadsheetEvent.class);
								shareSpreadsheetEventConsumer(sheetEvent);
							}
							case UnshareSpreadsheetEvent -> {
								UnshareSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), UnshareSpreadsheetEvent.class);
								unshareSpreadsheetEventConsumer(sheetEvent);
							}
							case DeleteUserSpreadsheetsEvent -> {
								DeleteUserSpreadsheetsEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), DeleteUserSpreadsheetsEvent.class);
								deleteUserSpreadsheetsEventConsumer(sheetEvent);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				sp.setResult(r.offset(), r.value());
			}
		});
	}

	@Override
	public String createSpreadsheet(Long version, Spreadsheet sheet, String password) throws SheetsException {

		if( sheet == null || password == null)
			throw new WebApplicationException(Status.BAD_REQUEST);

		if (sheet.getColumns() <= 0 || sheet.getRows() <= 0)
			throw new WebApplicationException(Status.BAD_REQUEST);

		sheet.setSheetId(UUID.randomUUID().toString());

		byte[] payload = json.toJson(new CreateSpreadsheetEvent(sheet, password)).getBytes(StandardCharsets.ISO_8859_1);
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.CreateSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		CreateSpreadsheetEvent sheetEvent =
				json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), CreateSpreadsheetEvent.class);

		return createSpreadsheetEventConsumer(sheetEvent);
	}

	private String createSpreadsheetEventConsumer(CreateSpreadsheetEvent event) throws SheetsException {
		Spreadsheet sheet = event.getSheet();
		String password = event.getPassword();

		synchronized(this) {

			String spreadsheetOwner = sheet.getOwner();

			Result<User> result = Discovery.getLocalUsersClient().getUser(spreadsheetOwner, password);
			if(!result.isOK())
				throw new WebApplicationException(Status.BAD_REQUEST);

			Spreadsheet spreadsheet = new Spreadsheet(sheet,domainId);

			spreadsheets.put(spreadsheet.getSheetId(), spreadsheet);

			if (!spreadsheetOwners.containsKey(spreadsheetOwner))
				spreadsheetOwners.put(spreadsheetOwner, new TreeSet<>());

			spreadsheetOwners.get(spreadsheetOwner).add(spreadsheet.getSheetId());

			return spreadsheet.getSheetId();
		}
	}

	@Override
	public void deleteSpreadsheet(Long version, String sheetId, String password) throws SheetsException {

		if( sheetId == null || password == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new DeleteSpreadsheetEvent(sheetId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.DeleteSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		DeleteSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), DeleteSpreadsheetEvent.class);
		deleteSpreadsheetEventConsumer(sheetEvent);
	}

	private void deleteSpreadsheetEventConsumer(DeleteSpreadsheetEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				password = event.getPassword();

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throw new WebApplicationException(Status.FORBIDDEN);
			else if(!result.isOK())
				throw new WebApplicationException(Status.BAD_REQUEST);

			spreadsheetOwners.get(sheet.getOwner()).remove(sheetId);
			spreadsheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(Long version, String sheetId, String userId, String password) throws SheetsException {

		if( sheetId == null || userId == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		System.out.println(uri + " -> Waiting for " + version + " current version is " + SyncPoint.getVersion());

		sp.waitForResult(version);

		Spreadsheet sheet = spreadsheets.get(sheetId);

		if( sheet == null ) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
		if(result.error() == Result.ErrorCode.FORBIDDEN) {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		else if(result.error() == Result.ErrorCode.NOT_FOUND) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		else if(!result.isOK()) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		if (!userId.equals(sheet.getOwner())) {
			if (!sheet.getSharedWith().stream().anyMatch(user -> user.contains(userId)))
				throw new WebApplicationException(Status.FORBIDDEN);
		}

		return sheet;
	}

	@Override
	public String[][] getReferencedSpreadsheetValues(Long version, String sheetId, String userId, String range) throws SheetsException {

		if( sheetId == null || userId == null || range == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		sp.waitForResult(version);

		Spreadsheet spreadsheet = spreadsheets.get(sheetId);

		if( spreadsheet == null ) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (!userId.equals(spreadsheet.getOwner()) && !spreadsheet.getSharedWith().contains(userId)) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		return new CellRange(range).extractRangeValuesFrom(result);
	}

	@Override
	public String[][] getSpreadsheetValues(Long version, String sheetId, String userId, String password) throws SheetsException {

		Spreadsheet spreadsheet = getSpreadsheet(version, sheetId, userId, password);

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		return result;
	}

	@Override
	public void updateCell(Long version, String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException {

		if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new UpdateCellEvent(sheetId, cell, rawValue, userId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.UpdateCellEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		UpdateCellEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), UpdateCellEvent.class);
		updateCellEventConsumer(sheetEvent);
	}

	private void updateCellEventConsumer(UpdateCellEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				cell = event.getCell(),
				rawValue = event.getRawValue(),
				userId = event.getUserId(),
				password = event.getPassword();

		synchronized(this) {

			Spreadsheet spreadsheet = getSpreadsheet(SyncPoint.getVersion(), sheetId, userId, password);

			try {
				Pair<Integer,Integer> coordinates =  Cell.CellId2Indexes(cell);

				spreadsheet.placeCellRawValue(coordinates.getLeft(),coordinates.getRight(), rawValue);
			} catch (InvalidCellIdException e) {
				throw new WebApplicationException(Status.BAD_REQUEST);
			}

		}

	}


	@Override
	public void shareSpreadsheet(Long version, String sheetId, String userId, String password) throws SheetsException {

		if( sheetId == null || userId == null || password == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new ShareSpreadsheetEvent(sheetId, userId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.ShareSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		ShareSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), ShareSpreadsheetEvent.class);
		shareSpreadsheetEventConsumer(sheetEvent);
	}

	private void shareSpreadsheetEventConsumer(ShareSpreadsheetEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				userId = event.getUserId(),
				password = event.getPassword();

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throw new WebApplicationException(Status.FORBIDDEN);
			else if(!result.isOK())
				throw new WebApplicationException(Status.BAD_REQUEST);

			Set<String> sharedWith = sheet.getSharedWith();

			if (sharedWith.contains(userId))
				throw new WebApplicationException(Status.CONFLICT);

			sharedWith.add(userId);
		}
	}

	@Override
	public void unshareSpreadsheet(Long version, String sheetId, String userId, String password) throws SheetsException {

		if( sheetId == null || userId == null || password == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new UnshareSpreadsheetEvent(sheetId, userId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.UnshareSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		UnshareSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), UnshareSpreadsheetEvent.class);
		unshareSpreadsheetEventConsumer(sheetEvent);
	}

	private void unshareSpreadsheetEventConsumer(UnshareSpreadsheetEvent event) throws SheetsException {
		String sheetId = event.getSheetId(),
				userId = event.getUserId(),
				password = event.getPassword();

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			Result<User> result = Discovery.getLocalUsersClient().getUser(sheet.getOwner(), password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throw new WebApplicationException(Status.FORBIDDEN);
			else if(!result.isOK())
				throw new WebApplicationException(Status.BAD_REQUEST);

			Set<String> sharedWith = sheet.getSharedWith();

			if (!sharedWith.contains(userId))
				throw new WebApplicationException(Status.NOT_FOUND);

			sharedWith.remove(userId);
		}
	}

	@Override
	public void deleteUserSpreadsheets(Long version, String userId, String password) throws SheetsException {

		byte[] payload = json.toJson(new DeleteUserSpreadsheetsEvent(userId, password)).getBytes(StandardCharsets.ISO_8859_1);
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.DeleteUserSpreadsheetsEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		KafkaEvent event = json.fromJson(sp.waitForResult(sequenceNumber), KafkaEvent.class);
		DeleteUserSpreadsheetsEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), DeleteUserSpreadsheetsEvent.class);
		deleteUserSpreadsheetsEventConsumer(sheetEvent);
	}

	private void deleteUserSpreadsheetsEventConsumer(DeleteUserSpreadsheetsEvent event) throws SheetsException {
		String userId = event.getUserId(),
				password = event.getPassword();

		synchronized (this) {

			Result<User> result = Discovery.getLocalUsersClient().getUser(userId, password);
			if(result.error() == Result.ErrorCode.FORBIDDEN)
				throw new WebApplicationException(Status.FORBIDDEN);
			else if(!result.isOK())
				throw new WebApplicationException(Status.BAD_REQUEST);

			Set<String> sheets = spreadsheetOwners.get(userId);

			sheets.forEach(spreadsheets::remove);
			spreadsheetOwners.remove(userId);
		}
	}
}

