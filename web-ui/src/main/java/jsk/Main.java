package jsk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Configuration
    static class WebSocketConfig {

        @Bean
        SimpleUrlHandlerMapping simpleUrlHandlerMapping(
                KafkaConfig kafkaConfig,
                @Value("${serverless.kotlin.kafka.mytopic.name}") String topicName,
                SchemaRegistryConfig schemaRegistryConfig,
                ObjectMapper objectMapper) {

            System.out.println(schemaRegistryConfig.getConfig());

            final Map<String, Object> config = new HashMap<>();
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonSchemaDeserializer.class);
            config.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
            config.putAll(kafkaConfig.getConfig());
            config.putAll(schemaRegistryConfig.getConfig());

            WebSocketHandler webSocketHandler = (session) -> {
                ReceiverOptions<String, Question.Data> receiverOptions = ReceiverOptions.<String, Question.Data>create(config)
                        .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, session.getId())
                        .consumerProperty(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, Question.Data.class)
                        .subscription(Collections.singletonList(topicName));

                Flux<ReceiverRecord<String, Question.Data>> kafkaMessages = KafkaReceiver.create(receiverOptions).receive();

                Flux<WebSocketMessage> webSocketMessages = kafkaMessages.map(message -> {
                    ObjectNode json = objectMapper.createObjectNode();

                    json.put("url", message.key());
                    json.put("title", message.value().title);
                    json.put("favorite_count", message.value().favoriteCount);
                    json.put("view_count", message.value().viewCount);

                    return session.textMessage(json.toString());
                });

                return session.send(webSocketMessages);
            };

            return new SimpleUrlHandlerMapping(Collections.singletonMap("/questions", webSocketHandler), 0);
        }

    }

}