package tp1.kafka.event;

public class KafkaEvent {

    public enum Type {
        CreateSpreadsheetEvent,
        DeleteSpreadsheetEvent,
        DeleteUserSpreadsheetsEvent,
        ShareSpreadsheetEvent,
        UnshareSpreadsheetEvent,
        UpdateCellEvent
    }

    private String domainId, publisherURI;
    private Type payloadType;
    private byte[] jsonPayload;

    public KafkaEvent(String domainId, String publisherURI, Type payloadType, byte[] jsonPayload) {
        this.domainId = domainId;
        this.publisherURI = publisherURI;
        this.payloadType = payloadType;
        this.jsonPayload = jsonPayload;
    }

    public KafkaEvent() {
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getPublisherURI() {
        return publisherURI;
    }

    public void setPublisherURI(String publisherURI) {
        this.publisherURI = publisherURI;
    }

    public Type getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(Type payloadType) {
        this.payloadType = payloadType;
    }

    public byte[] getJsonPayload() {
        return jsonPayload;
    }

    public void setJsonPayload(byte[] jsonPayload) {
        this.jsonPayload = jsonPayload;
    }
}
