package io.conduktor.demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoCooperative {
    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I am a kafka Consumer");

        String groupId = "my-java-application";
        String topic = "demo_java";

        // create Producer Producer
        Properties properties = new Properties();
        // connect to localhost
//        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");

        // connect to secure cluster of Conduktor playground
        properties.setProperty("bootstrap.servers", "cluster.playground.cdkt.io:9092");
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"1KTbxwfzfRj66qc9aCKTh3\" password=\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiIxS1RieHdmemZSajY2cWM5YUNLVGgzIiwib3JnYW5pemF0aW9uSWQiOjc2OTUyLCJ1c2VySWQiOjg5NTMyLCJmb3JFeHBpcmF0aW9uQ2hlY2siOiIyZDczMDM4Yi1hY2MxLTRjYTUtOTQ4ZS1hNTI2NDQ4ZmUyYmEifX0.LYEjXd-3ZvEC1FBf-qQJc-UUdX1CD4ggcQmCG76VzTA\";");
        properties.setProperty("sasl.mechanism", "PLAIN");

        // create consumer configs
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        properties.setProperty("group.id", groupId);

        // none/earliest/latest
        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());
        //properties.setProperty("group.instance.id", ".....");  // strategy for static assignments

        //create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // get a reference to the main thread
        final Thread mainThread = Thread.currentThread();

        // adding shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Detected shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            // subscribe the topic
//        consumer.subscribe(Arrays.asList("topic1", "topic2"));
            consumer.subscribe(Arrays.asList(topic));

            //poll the topic
            while (true) {
//                log.info("Polling");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    log.info("Key: " + record.key() + ", Value: " + record.value());
                    log.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }
            }
        } catch (WakeupException e) {
            log.info("Consumer is starting to shut down");
        } catch (Exception e){
            log.info("Unexpected exception in the consumer", e);
        } finally {
            consumer.close(); // close the consumer, this will also commit the offsets
            log.info("The consumer is now gracefully shut down");
        }
    }
}
