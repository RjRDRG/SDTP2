package tp1.resources.rest;

import com.google.gson.Gson;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.util.Result;
import tp1.discovery.Discovery;
import tp1.impl.SpreadsheetsImpl;
import tp1.kafka.KafkaPublisher;
import tp1.kafka.KafkaSubscriber;
import tp1.kafka.RecordProcessor;
import tp1.kafka.event.*;
import tp1.kafka.sync.SyncPoint;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static tp1.api.service.util.Result.mapError;
import static tp1.discovery.Discovery.DISCOVERY_PERIOD;
import static tp1.kafka.KafkaUtils.KAFKA_ADDRESS;

@Singleton
public class SpreadsheetReplicatedResource implements RestSpreadsheets {

	private final String domainId;

	private KafkaPublisher publisher;
	private final Gson json;
	private final SyncPoint sp;

	private final SpreadsheetsImpl impl;

	public SpreadsheetReplicatedResource(String domainId, SyncPoint sp) {
		this.domainId = domainId;

		this.impl = new SpreadsheetsImpl(domainId);

		this.json = new Gson();
		this.sp = sp;
		this.publisher = null;
	}

	public void registerInKafka() {

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

				try {
					switch (event.getPayloadType()) {
						case CreateSpreadsheetEvent -> {
							CreateSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), CreateSpreadsheetEvent.class);
							Result<String> result = impl.createSpreadsheet(sheetEvent.getSheet(), sheetEvent.getPassword());
							sp.setResult(r.offset(), result);
						}
						case DeleteSpreadsheetEvent -> {
							DeleteSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), DeleteSpreadsheetEvent.class);
							Result<Void> result = impl.deleteSpreadsheet(sheetEvent.getSheetId(), sheetEvent.getPassword());
							sp.setResult(r.offset(), result);
						}
						case UpdateCellEvent -> {
							UpdateCellEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), UpdateCellEvent.class);
							Result<Void> result = impl.updateCell(sheetEvent.getSheetId(), sheetEvent.getCell(), sheetEvent.getRawValue(), sheetEvent.getUserId(), sheetEvent.getPassword());
							sp.setResult(r.offset(), result);
						}
						case ShareSpreadsheetEvent -> {
							ShareSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), ShareSpreadsheetEvent.class);
							Result<Void> result = impl.shareSpreadsheet(sheetEvent.getSheetId(), sheetEvent.getUserId(), sheetEvent.getPassword());
							sp.setResult(r.offset(), result);
						}
						case UnshareSpreadsheetEvent -> {
							UnshareSpreadsheetEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), UnshareSpreadsheetEvent.class);
							Result<Void> result = impl.unshareSpreadsheet(sheetEvent.getSheetId(), sheetEvent.getUserId(), sheetEvent.getPassword());
							sp.setResult(r.offset(), result);
						}
						case DeleteUserSpreadsheetsEvent -> {
							DeleteUserSpreadsheetsEvent sheetEvent = json.fromJson(new String(event.getJsonPayload(), StandardCharsets.ISO_8859_1), DeleteUserSpreadsheetsEvent.class);
							Result<Void> result = impl.deleteUserSpreadsheets(sheetEvent.getUserId(), sheetEvent.getPassword());
							sp.setResult(r.offset(), result);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		if( sheet == null || password == null)
			throw new WebApplicationException(Status.BAD_REQUEST);

		if (sheet.getColumns() <= 0 || sheet.getRows() <= 0)
			throw new WebApplicationException(Status.BAD_REQUEST);

		sheet.setSheetId(UUID.randomUUID().toString());

		byte[] payload = json.toJson(new CreateSpreadsheetEvent(sheet, password)).getBytes(StandardCharsets.ISO_8859_1);
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.CreateSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		Result<String> result = sp.waitForResult(sequenceNumber);

		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			throw new WebApplicationException(
					Response.status(200).header(RestSpreadsheets.HEADER_VERSION+domainId, SyncPoint.getVersion()).entity(result.value()).build()
			);
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {

		if( sheetId == null || password == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new DeleteSpreadsheetEvent(sheetId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.DeleteSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		Result<Void> result = sp.waitForResult(sequenceNumber);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			throw new WebApplicationException(
					Response.status(204).header(RestSpreadsheets.HEADER_VERSION+domainId, SyncPoint.getVersion()).build()
			);
	}

	@Override
	public Spreadsheet getSpreadsheet(HttpHeaders headers, String sheetId, String userId, String password) {

		Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
				.filter(e -> e.getKey().startsWith(HEADER_VERSION))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> Long.parseLong(e.getValue().get(0))
				));

		long version = Optional.ofNullable(versions.get(HEADER_VERSION+domainId)).orElse(-1L);

		if( sheetId == null || userId == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		sp.waitForResult(version);

		Result<Spreadsheet> result = impl.getSpreadsheet(sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			throw new WebApplicationException(
					Response.status(200).header(RestSpreadsheets.HEADER_VERSION+domainId, SyncPoint.getVersion()).entity(result.value()).build()
			);
	}

	@Override
	public String[][] getReferencedSpreadsheetValues(HttpHeaders headers, String sheetId, String userId, String range) {

		if( sheetId == null || userId == null || range == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
				.filter(e -> e.getKey().startsWith(HEADER_VERSION))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> Long.parseLong(e.getValue().get(0))
				));

		long version = Optional.ofNullable(versions.get(HEADER_VERSION+domainId)).orElse(-1L);

		sp.waitForResult(version);

		Result<String[][]> result = impl.getReferencedSpreadsheetValues(versions, sheetId, userId, range);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else {
			Response.ResponseBuilder builder = Response.status(200).entity(result.value());

			for (Map.Entry<String,String> entry : result.getOthers().entrySet()) {
				builder.header(entry.getKey(), entry.getValue());
			}

			builder.header(RestSpreadsheets.HEADER_VERSION + domainId, SyncPoint.getVersion());

			throw new WebApplicationException(builder.build());
		}
	}

	@Override
	public String[][] getSpreadsheetValues(HttpHeaders headers, String sheetId, String userId, String password) {

		if( sheetId == null || userId == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Map<String, Long> versions = headers.getRequestHeaders().entrySet().stream()
				.filter(e -> e.getKey().startsWith(HEADER_VERSION))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> Long.parseLong(e.getValue().get(0))
				));

		long version = Optional.ofNullable(versions.get(HEADER_VERSION+domainId)).orElse(-1L);

		sp.waitForResult(version);

		Result<String[][]> result = impl.getSpreadsheetValues(versions, sheetId, userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else {
			Response.ResponseBuilder builder = Response.status(200).entity(result.value());

			for (Map.Entry<String,String> entry : result.getOthers().entrySet()) {
				builder.header(entry.getKey(), entry.getValue());
			}

			builder.header(RestSpreadsheets.HEADER_VERSION + domainId, SyncPoint.getVersion());

			throw new WebApplicationException(builder.build());
		}
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

		if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new UpdateCellEvent(sheetId, cell, rawValue, userId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.UpdateCellEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		Result<Void> result = sp.waitForResult(sequenceNumber);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			throw new WebApplicationException(
					Response.status(204).header(RestSpreadsheets.HEADER_VERSION+domainId, SyncPoint.getVersion()).build()
			);
	}


	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null || password == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new ShareSpreadsheetEvent(sheetId, userId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.ShareSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		Result<Void> result = sp.waitForResult(sequenceNumber);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			throw new WebApplicationException(
					Response.status(204).header(RestSpreadsheets.HEADER_VERSION+domainId, SyncPoint.getVersion()).build()
			);
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null || password == null ) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		byte[] payload = json.toJson(new UnshareSpreadsheetEvent(sheetId, userId, password)).getBytes(StandardCharsets.ISO_8859_1);;
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.UnshareSpreadsheetEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		Result<Void> result = sp.waitForResult(sequenceNumber);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			throw new WebApplicationException(
					Response.status(204).header(RestSpreadsheets.HEADER_VERSION+domainId, SyncPoint.getVersion()).build()
			);
	}

	@Override
	public void deleteUserSpreadsheets(String userId, String password) {

		byte[] payload = json.toJson(new DeleteUserSpreadsheetsEvent(userId, password)).getBytes(StandardCharsets.ISO_8859_1);
		KafkaEvent kafkaEvent = new KafkaEvent(domainId, Discovery.getServiceURI(), KafkaEvent.Type.DeleteUserSpreadsheetsEvent, payload);

		long sequenceNumber = publisher.publish(domainId, json.toJson(kafkaEvent));

		Result<Void> result = sp.waitForResult(sequenceNumber);
		if(!result.isOK()) {
			throw new WebApplicationException(mapError(result.error()));
		}
		else {
			throw new WebApplicationException(
					Response.status(204).header(RestSpreadsheets.HEADER_VERSION + domainId, SyncPoint.getVersion()).build()
			);
		}
	}
}

