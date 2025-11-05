package com.yourcompany.collector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "collector")
public class CollectorConfig {
    
    // TCP Server Configuration
    private int syslogPort = 514;
    private int socketTimeout = 60000; // 60 seconds
    private int bufferSize = 65536; // 64KB
    private String codecType = "lf"; // lf, crlf, or lh
    private int maxMessageSize = 1048576; // 1MB
    
    // Thread Pool Configuration
    private int corePoolSize = 10;
    private int maxPoolSize = 50;
    private long keepAliveTime = 60; // seconds
    private int threadQueueSize = 1000;
    
    // Channel Configuration
    private int channelQueueSize = 10000;
    private int errorQueueSize = 1000;
    
    // Retry Configuration
    private int maxRetries = 5;
    private long retryBackoffMs = 1000;
    private long maxRetryBackoffMs = 30000;
    
    // Dead Letter Queue Configuration
    private boolean deadLetterQueueEnabled = true;
    private String deadLetterQueuePath = "/var/log/dlq/";
    
    // Monitoring Configuration
    private boolean metricsEnabled = true;
    private long metricsReportingIntervalMs = 60000; // 1 minute
    
    // Circuit Breaker Configuration
    private int circuitBreakerFailureThreshold = 10;
    private long circuitBreakerTimeoutMs = 30000; // 30 seconds
    private long circuitBreakerHalfOpenRequests = 3;
    
    // Backpressure Configuration
    private double backpressureThreshold = 0.8; // 80% queue utilization
    private boolean backpressureEnabled = true;
    
    // Health Check Configuration
    private boolean healthCheckEnabled = true;
    private long healthCheckIntervalMs = 30000; // 30 seconds
    private long healthCheckTimeoutMs = 5000; // 5 seconds
}