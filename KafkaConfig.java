package com.yourcompany.collector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    
    // Basic Kafka Configuration
    private String brokers = "localhost:9092";
    private String topic = "syslog-events";
    private String groupId = "syslog-collector-group";
    
    // Producer Configuration
    private int maxRetries = 5;
    private long retryBackoffMs = 1000;
    private int requestTimeoutMs = 30000;
    private int deliveryTimeoutMs = 120000;
    private int maxBlockMs = 60000;
    
    // Performance Configuration
    private int lingerMs = 100;
    private int batchSize = 32768; // 32KB
    private String compressionType = "snappy";
    private String acks = "1";
    private int maxInFlightRequests = 5;
    private long bufferMemory = 67108864L; // 64MB
    
    // Connection Configuration
    private int reconnectBackoffMs = 1000;
    private int reconnectBackoffMaxMs = 10000;
    private int connectionsMaxIdleMs = 540000;
    private int metadataMaxAgeMs = 300000;
    
    // SSL/Security Configuration
    private boolean sslEnabled = false;
    private String sslTruststoreLocation;
    private String sslTruststorePassword;
    private String sslKeystoreLocation;
    private String sslKeystorePassword;
    private String sslKeyPassword;
    private String securityProtocol = "PLAINTEXT";
    private String saslMechanism;
    private String saslJaasConfig;
    
    // Dead Letter Queue Configuration
    private String dlqTopic = "syslog-events-dlq";
    private boolean dlqEnabled = true;
    
    // Health Check Configuration
    private String healthCheckTopic = "health-check-topic";
    private boolean healthCheckEnabled = true;
    
    /**
     * Get SSL configuration map for Kafka producer.
     */
    public Map<String, Object> sslConfig() {
        Map<String, Object> sslConfig = new HashMap<>();
        
        if (sslEnabled) {
            sslConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
            
            if (sslTruststoreLocation != null) {
                sslConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
                sslConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);
            }
            
            if (sslKeystoreLocation != null) {
                sslConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
                sslConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
                sslConfig.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
            }
            
            if (saslMechanism != null) {
                sslConfig.put("sasl.mechanism", saslMechanism);
                sslConfig.put("sasl.jaas.config", saslJaasConfig);
            }
        }
        
        return sslConfig;
    }
}