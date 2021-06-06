package tp1.kafka.event;

public class KafkaEvent {

    private String domainId, publisherURI, jsonPayload;

    public KafkaEvent(String domainId, String publisherURI, String jsonPayload) {
        this.domainId = domainId;
        this.publisherURI = publisherURI;
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

    public String getJsonPayload() {
        return jsonPayload;
    }

    public void setJsonPayload(String jsonPayload) {
        this.jsonPayload = jsonPayload;
    }
}
