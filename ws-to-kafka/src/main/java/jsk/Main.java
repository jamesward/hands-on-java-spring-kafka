package jsk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

@SpringBootApplication
public class Main implements CommandLineRunner {

    private final URI soToWsUrl;

    private final KafkaTopicConfig kafkaTopicConfig;

    private final KafkaTemplate<String, Question.Data> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final WebSocketClient wsClient = new ReactorNettyWebSocketClient();

    public Main(
            @Value("${serverless.kotlin.kafka.so-to-ws.url}") URI soToWsUrl,
            KafkaTopicConfig kafkaTopicConfig,
            KafkaTemplate<String, Question.Data> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.soToWsUrl = soToWsUrl;
        this.kafkaTopicConfig = kafkaTopicConfig;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) {
        wsClient.execute(soToWsUrl, session -> session.receive().doOnNext(message -> {
            try {
                Question.Data question = objectMapper.readValue(message.getPayloadAsText(), Question.Data.class);
                System.out.println(question.url);

                //sending to Kafka topic
                kafkaTemplate.send(kafkaTopicConfig.getName(), question.url, question);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }).then()).block();
    }

    @Configuration
    static class KafkaSetup {

        @Bean
        NewTopic newTopic(KafkaTopicConfig kafkaTopicConfig) {
            return TopicBuilder.name(kafkaTopicConfig.getName()).partitions(kafkaTopicConfig.getPartitions()).replicas(kafkaTopicConfig.getReplicas()).build();
        }

        @Bean
        KafkaAdmin kafkaAdmin(KafkaConfig kafkaConfig) {
            return new KafkaAdmin(Collections.unmodifiableMap(kafkaConfig.getConfig()));
        }

        @Bean
        ProducerFactory<String, Question.Data> producerFactory(KafkaProperties kafkaProperties, KafkaConfig kafkaConfig, SchemaRegistryConfig schemaRegistryConfig) {
            final Map<String, Object> config = kafkaProperties.buildProducerProperties();
            config.putAll(kafkaConfig.getConfig());
            config.putAll(schemaRegistryConfig.getConfig());
            return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        KafkaTemplate<String, Question.Data> kafkaTemplate(ProducerFactory<String, Question.Data> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

    }
}
