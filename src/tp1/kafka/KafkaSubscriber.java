package tp1.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaSubscriber {
	static public KafkaSubscriber createSubscriber( String addr, List<String> topics) {

		Properties props = new Properties();

		// Localização dos servidores kafka (lista de máquinas + porto)
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, addr);

		// Configura o modo de subscrição (ver documentação em kafka.apache.org)
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		//props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

		props.put(ConsumerConfig.GROUP_ID_CONFIG, "grp" + System.nanoTime());

		// Classe para serializar as chaves dos eventos (string)
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		// Classe para serializar os valores dos eventos (string)
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		// Cria um consumidor (assinante/subscriber)
		return new KafkaSubscriber( new KafkaConsumer<String, String>(props), topics);
	}

	private static final long POLL_TIMEOUT = 1L;

	final KafkaConsumer<String, String> consumer;

	public KafkaSubscriber(KafkaConsumer<String, String> consumer, List<String> topics) {
		this.consumer = consumer;
		this.consumer.subscribe(topics);
	}
	
	public void start(RecordProcessor recordProcessor) {
		new Thread( () -> {
			for (;;) {
				consumer.poll(Duration.ofSeconds(POLL_TIMEOUT)).forEach(r -> {
					recordProcessor.onReceive(r);
				});
			}
		}).start();
		
	}

	public void consume(SubscriberListener listener) {
		for (;;) {
			consumer.poll(Duration.ofSeconds(POLL_TIMEOUT)).forEach(r -> {
				listener.onReceive(r.topic(), r.key(), r.value());
			});
		}
	}
	
	public static interface SubscriberListener {
		void onReceive(String topic, String key, String value);
	}


}
