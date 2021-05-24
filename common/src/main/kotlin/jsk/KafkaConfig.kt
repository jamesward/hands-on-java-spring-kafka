package jsk

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

data class KafkaTopicConfig(val replicas: Int, val partitions: Int, val name: String)

data class KafkaConfig(val bootstrapServers: String, val username: String? = null, val password: String? = null) {

    val authConfig = if (username.isNullOrEmpty() && password.isNullOrEmpty()) {
        emptyMap()
    }
    else {
        mapOf(
            "ssl.endpoint.identification.algorithm" to "https",
            "sasl.mechanism" to "PLAIN",
            "sasl.jaas.config" to "org.apache.kafka.common.security.plain.PlainLoginModule required username='$username' password='$password';",
            "security.protocol" to "SASL_SSL",
        )
    }

    val config = mapOf(
        "bootstrap.servers" to bootstrapServers,
    ) + authConfig

}

data class SchemaRegistryConfig(val url: String, val username: String? = null, val password: String? = null) {
    val authConfig = if (username.isNullOrEmpty() && password.isNullOrEmpty()) {
        emptyMap()
    }
    else {
        mapOf(
            "basic.auth.credentials.source" to "USER_INFO",
            "schema.registry.basic.auth.user.info" to "$username:$password",
        )
    }

    val config = mapOf(
        "schema.registry.url" to url
    ) + authConfig
}

@Configuration
class KafkaConfigs {

    @Bean
    @ConditionalOnProperty(name = ["serverless.kotlin.kafka.mytopic.name", "serverless.kotlin.kafka.mytopic.replicas", "serverless.kotlin.kafka.mytopic.partitions"])
    @ConditionalOnMissingBean(KafkaTopicConfig::class)
    fun kafkaTopicConfig(
        @Value("\${serverless.kotlin.kafka.mytopic.name}") name: String,
        @Value("\${serverless.kotlin.kafka.mytopic.replicas}") replicas: Int,
        @Value("\${serverless.kotlin.kafka.mytopic.partitions}") partitions: Int,
    ): KafkaTopicConfig {
        return KafkaTopicConfig(replicas, partitions, name)
    }

    @Bean
    @ConditionalOnProperty(name = ["kafka.bootstrap.servers"])
    fun kafkaConfig(
        @Value("\${kafka.bootstrap.servers}") bootstrapServers: String,
        @Value("\${kafka.username:}") username: String,
        @Value("\${kafka.password:}") password: String,
    ): KafkaConfig {
        return KafkaConfig(bootstrapServers, username, password)
    }

    @Bean
    @ConditionalOnProperty(name = ["schema.registry.url"])
    fun schemaRegistryConfig(
        @Value("\${schema.registry.url}") url: String,
        @Value("\${schema.registry.key:}") key: String,
        @Value("\${schema.registry.password:}") password: String,
    ): SchemaRegistryConfig {
        return SchemaRegistryConfig(url, key, password)
    }

}
