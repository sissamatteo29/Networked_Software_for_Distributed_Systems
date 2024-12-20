package it.polimi.middleware.kafka.transactional;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class TransactionalConsumer {
    private static final String defaultGroupId = "groupA";
    private static final String defaultTopic = "topicA";

    private static final String serverAddr = "localhost:9092";

    // Default is "latest": try "earliest" instead
    private static final String offsetResetStrategy = "latest";

    // If this is set to true, the consumer might also read records
    // that come from aborted transactions
    private static final boolean readUncommitted = false;

    public static void main(String[] args) {
        // If there are arguments, use the first as group and the second as topic.
        // Otherwise, use default group and topic.
        String groupId = args.length >= 1 ? args[0] : defaultGroupId;
        String topic = args.length >= 2 ? args[1] : defaultTopic;

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // The consumer must specify the isolation level
        // The producer in this example commits only half of the transactions, but if the 
        // isolation level is set to read_uncommitted, then also uncommitted changes (before aborted) will be seen by this consumer
        if (readUncommitted) {
            props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_uncommitted");
        } else {
            props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetStrategy);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        while (true) {
            final ConsumerRecords<String, String> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
            for (final ConsumerRecord<String, String> record : records) {
                System.out.println("Partition: " + record.partition() +
                        "\tOffset: " + record.offset() +
                        "\tKey: " + record.key() +
                        "\tValue: " + record.value()
                );
            }
        }
    }
}