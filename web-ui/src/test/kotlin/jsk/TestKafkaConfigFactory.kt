package jsk

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import javax.annotation.PreDestroy


@Component
class TestZookeeperContainer :
    GenericContainer<TestZookeeperContainer>(DockerImageName.parse("confluentinc/cp-zookeeper:6.1.0")) {

    val testNetwork: Network = Network.newNetwork()

    val port = 2181

    init {
        withNetwork(testNetwork)
        withEnv("ZOOKEEPER_CLIENT_PORT", port.toString())
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestKafkaContainer(testZookeeperContainer: TestZookeeperContainer) :
    KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.1.0")) {

    val port = 9092

    init {
        withNetwork(testZookeeperContainer.network)
        withExternalZookeeper(testZookeeperContainer.networkAliases.first() + ":" + testZookeeperContainer.port)
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
        withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
        withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestSchemaRegistryContainer(testKafkaContainer: TestKafkaContainer) :
    GenericContainer<TestSchemaRegistryContainer>(DockerImageName.parse("confluentinc/cp-schema-registry:6.1.0")) {

    fun url() = "http://${networkAliases.first()}:${exposedPorts.first()}"

    init {
        withNetwork(testKafkaContainer.network)
        withExposedPorts(8081)
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "${testKafkaContainer.networkAliases.first()}:${testKafkaContainer.port}")
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestWsToKafkaContainer(testKafkaContainer: TestKafkaContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer) :
    GenericContainer<TestWsToKafkaContainer>(DockerImageName.parse("ws-to-kafka")) {

    init {
        // todo: logger
        withNetwork(testKafkaContainer.network)
        withEnv("KAFKA_BOOTSTRAP_SERVERS", "${testKafkaContainer.networkAliases.first()}:${testKafkaContainer.port}")
        withEnv("SCHEMA_REGISTRY_URL", testSchemaRegistryContainer.url())
        withEnv("SERVERLESS_KOTLIN_KAFKA_MYTOPIC_REPLICAS", "1")
        withEnv("SERVERLESS_KOTLIN_KAFKA_MYTOPIC_PARTITIONS", "3")
        waitingFor(Wait.forLogMessage(".*Kafka startTimeMs.*", 1))
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Configuration
class TestKafkaConfigFactory {

    @Bean
    fun kafkaConfig(testKafkaContainer: TestKafkaContainer): KafkaConfig {
        return KafkaConfig(testKafkaContainer.bootstrapServers)
    }

    @Bean
    fun schemaRegistryConfig(testSchemaRegistryContainer: TestSchemaRegistryContainer): SchemaRegistryConfig {
        return SchemaRegistryConfig("http://${testSchemaRegistryContainer.host}:${testSchemaRegistryContainer.firstMappedPort}/")
    }

}
