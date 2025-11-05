package com.yourcompany.collector.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.expression.common.LiteralExpression;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static java.util.Optional.ofNullable;

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
@Profile("kafka")
public class OutboundFlowKafkaConfig {

    private static final String APP_NAME = "solace-otel-collector";
    private final KafkaConfig kafkaConfig;
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong dlqCount = new AtomicLong(0);
    private final AtomicBoolean circuitBreakerOpen = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitBreakerOpenTime = new AtomicLong(0);
    
    // Circuit breaker thresholds
    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 10;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30000; // 30 seconds
    private static final String DLQ_FILE_PATH = "/var/log/kafka-dlq/";

    /**
     * Main Kafka integration flow with circuit breaker.
     */
    @Bean
    public IntegrationFlow kafkaIntegrationFlow(@Qualifier("producerChannel") MessageChannel producerChannel) {
        return IntegrationFlow.from(producerChannel)
                .handle(message -> {
                    // Check circuit breaker
                    if (isCircuitBreakerOpen()) {
                        handleCircuitBreakerOpen(message);
                        return;
                    }
                    
                    // Try to send with retry template
                    try {
                        RetryTemplate retryTemplate = kafkaRetryTemplate();
                        retryTemplate.execute(context -> {
                            sendToKafka(message);
                            return null;
                        });
                        
                        // Reset failure counter on success
                        consecutiveFailures.set(0);
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        handleKafkaFailure(message, e);
                    }
                })
                .get();
    }

    /**
     * Send message to Kafka with callbacks.
     */
    private void sendToKafka(Message<?> message) throws Exception {
        KafkaTemplate<String, SolaceSyslogEvent> template = kafkaTemplate();
        
        ListenableFuture<SendResult<String, SolaceSyslogEvent>> future = 
            template.send(kafkaConfig.getTopic(), 
                          APP_NAME, 
                          (SolaceSyslogEvent) message.getPayload());
        
        // Add timeout to future.get()
        SendResult<String, SolaceSyslogEvent> result = future.get(30, TimeUnit.SECONDS);
        
        // Log successful send
        if (log.isDebugEnabled()) {
            log.debug("Successfully sent message to Kafka partition {} offset {}", 
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        }
    }

    /**
     * Handle Kafka send failures.
     */
    private void handleKafkaFailure(Message<?> message, Exception e) {
        failureCount.incrementAndGet();
        int failures = consecutiveFailures.incrementAndGet();
        
        log.error("Failed to send message to Kafka (consecutive failures: {}): {}", 
            failures, e.getMessage());
        
        // Check if we should open circuit breaker
        if (failures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
            openCircuitBreaker();
        }
        
        // Determine if we should retry or send to DLQ
        if (shouldSendToDLQ(e)) {
            sendToDeadLetterQueue(message, e);
        } else {
            // Schedule retry with backoff
            scheduleRetry(message, e);
        }
    }

    /**
     * Check if circuit breaker is open.
     */
    private boolean isCircuitBreakerOpen() {
        if (circuitBreakerOpen.get()) {
            long openDuration = System.currentTimeMillis() - circuitBreakerOpenTime.get();
            if (openDuration > CIRCUIT_BREAKER_TIMEOUT_MS) {
                // Try to close circuit breaker
                log.info("Attempting to close circuit breaker after {} ms", openDuration);
                circuitBreakerOpen.set(false);
                consecutiveFailures.set(0);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Open circuit breaker.
     */
    private void openCircuitBreaker() {
        if (circuitBreakerOpen.compareAndSet(false, true)) {
            circuitBreakerOpenTime.set(System.currentTimeMillis());
            log.error("Circuit breaker OPENED due to {} consecutive failures", 
                consecutiveFailures.get());
        }
    }

    /**
     * Handle messages when circuit breaker is open.
     */
    private void handleCircuitBreakerOpen(Message<?> message) {
        log.warn("Circuit breaker is OPEN, sending message directly to DLQ");
        sendToDeadLetterQueue(message, 
            new CircuitBreakerOpenException("Circuit breaker is open"));
    }

    /**
     * Determine if exception warrants sending to DLQ immediately.
     */
    private boolean shouldSendToDLQ(Exception e) {
        // Some errors won't be fixed by retry
        return e instanceof RecordTooLargeException ||
               e instanceof org.apache.kafka.common.errors.SerializationException ||
               e instanceof ClassCastException;
    }

    /**
     * Schedule message retry with exponential backoff.
     */
    private void scheduleRetry(Message<?> message, Exception error) {
        Integer retryCount = message.getHeaders().get("kafkaRetryCount", Integer.class);
        if (retryCount == null) retryCount = 0;
        
        if (retryCount >= kafkaConfig.getMaxRetries()) {
            log.error("Max Kafka retries ({}) exceeded", kafkaConfig.getMaxRetries());
            sendToDeadLetterQueue(message, error);
            return;
        }
        
        // Calculate exponential backoff
        long backoffMs = Math.min(
            (long) Math.pow(2, retryCount) * 1000,
            30000 // Max 30 seconds
        );
        
        log.info("Scheduling Kafka retry {} after {} ms", retryCount + 1, backoffMs);
        
        Message<?> retryMessage = MessageBuilder.fromMessage(message)
                .setHeader("kafkaRetryCount", retryCount + 1)
                .setHeader("kafkaRetryReason", error.getMessage())
                .build();
        
        kafkaRetryExecutor().schedule(() -> {
            try {
                sendToKafka(retryMessage);
                consecutiveFailures.set(0);
                successCount.incrementAndGet();
            } catch (Exception e) {
                handleKafkaFailure(retryMessage, e);
            }
        }, backoffMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Enhanced producer error handling flow.
     */
    @Bean
    public IntegrationFlow producerErrorHandlingFlow() {
        return IntegrationFlow.from(producerErrorChannel())
                .log(LoggingHandler.Level.ERROR, "KafkaErrorLogger",
                        message -> String.format("Kafka error: %s", message.getPayload()))
                .handle(message -> {
                    if (message.getPayload() instanceof ErrorMessage) {
                        ErrorMessage errorMessage = (ErrorMessage) message.getPayload();
                        handleKafkaFailure(errorMessage.getOriginalMessage(), 
                            (Exception) errorMessage.getPayload());
                    }
                })
                .get();
    }

    /**
     * Send message to Dead Letter Queue.
     */
    private void sendToDeadLetterQueue(Message<?> message, Exception error) {
        dlqCount.incrementAndGet();
        
        try {
            // Try to send to Kafka DLQ topic first
            String dlqTopic = kafkaConfig.getTopic() + ".dlq";
            KafkaTemplate<String, Object> dlqTemplate = deadLetterKafkaTemplate();
            
            Map<String, Object> dlqHeaders = new HashMap<>();
            dlqHeaders.put("dlq_reason", error.getMessage());
            dlqHeaders.put("dlq_timestamp", System.currentTimeMillis());
            dlqHeaders.put("original_topic", kafkaConfig.getTopic());
            
            ProducerRecord<String, Object> dlqRecord = new ProducerRecord<>(
                dlqTopic, null, APP_NAME, message.getPayload(), dlqHeaders);
            
            dlqTemplate.send(dlqRecord).get(5, TimeUnit.SECONDS);
            
            log.info("Message sent to Kafka DLQ topic: {}", dlqTopic);
            
        } catch (Exception dlqException) {
            log.error("Failed to send to Kafka DLQ, writing to file: {}", 
                dlqException.getMessage());
            // Fallback to file
            writeToDeadLetterFile(message, error);
        }
    }

    /**
     * Write failed messages to file system as last resort.
     */
    private void writeToDeadLetterFile(Message<?> message, Exception error) {
        try {
            Path dlqPath = Paths.get(DLQ_FILE_PATH);
            if (!Files.exists(dlqPath)) {
                Files.createDirectories(dlqPath);
            }
            
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("dlq_%s_%d.json", 
                timestamp, System.nanoTime());
            
            Path filePath = dlqPath.resolve(filename);
            
            Map<String, Object> dlqEntry = new HashMap<>();
            dlqEntry.put("timestamp", timestamp);
            dlqEntry.put("error", error.getMessage());
            dlqEntry.put("payload", message.getPayload());
            dlqEntry.put("headers", message.getHeaders());
            
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(dlqEntry);
            
            Files.write(filePath, json.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE);
            
            log.warn("Failed message written to DLQ file: {}", filePath);
            
        } catch (Exception fileException) {
            log.error("Critical: Failed to write to DLQ file: {}", 
                fileException.getMessage());
        }
    }

    @Bean
    public MessageChannel producerErrorChannel() {
        return MessageChannels.queue("producerErrorChannel", 500).getObject();
    }

    /**
     * Kafka message handler with enhanced configuration.
     */
    @Bean
    public KafkaProducerMessageHandler<String, SolaceSyslogEvent> kafkaMessageHandler() {
        KafkaProducerMessageHandler<String, SolaceSyslogEvent> handler = 
            new KafkaProducerMessageHandler<>(kafkaTemplate());
        
        handler.setMessageKeyExpression(new LiteralExpression(APP_NAME));
        handler.setTopicExpression(new LiteralExpression(kafkaConfig.getTopic()));
        handler.setSendFailureChannel(producerErrorChannel());
        handler.setSendTimeout(30000); // 30 seconds
        handler.setSync(false); // Async sending for better throughput
        
        return handler;
    }

    @Bean
    public KafkaTemplate<String, SolaceSyslogEvent> kafkaTemplate() {
        KafkaTemplate<String, SolaceSyslogEvent> template = 
            new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic(kafkaConfig.getTopic());
        return template;
    }

    /**
     * Dead Letter Queue Kafka template.
     */
    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate() {
        return new KafkaTemplate<>(deadLetterProducerFactory());
    }

    @Bean
    public ProducerFactory<String, SolaceSyslogEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public ProducerFactory<String, Object> deadLetterProducerFactory() {
        Map<String, Object> props = new HashMap<>(producerConfigs());
        // DLQ can have different settings (e.g., lower throughput, higher reliability)
        props.put(ACKS_CONFIG, "all");
        props.put(RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Enhanced Kafka producer configuration with all critical timeouts.
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> properties = new HashMap<>();
        
        // Connection settings
        properties.put(BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBrokers());
        properties.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // CRITICAL TIMEOUT CONFIGURATIONS
        properties.put(REQUEST_TIMEOUT_MS_CONFIG, 
            getEnv("KAFKA_REQUEST_TIMEOUT_MS", 30000)); // 30 seconds
        properties.put(DELIVERY_TIMEOUT_MS_CONFIG, 
            getEnv("KAFKA_DELIVERY_TIMEOUT_MS", 120000)); // 2 minutes total
        properties.put(MAX_BLOCK_MS_CONFIG, 
            getEnv("KAFKA_MAX_BLOCK_MS", 60000)); // 1 minute for send() blocking
        properties.put(TRANSACTION_TIMEOUT_CONFIG, 
            getEnv("KAFKA_TRANSACTION_TIMEOUT_MS", 60000)); // 1 minute
        
        // Connection resilience
        properties.put(RECONNECT_BACKOFF_MS_CONFIG, 
            getEnv("KAFKA_RECONNECT_BACKOFF_MS", 1000)); // 1 second
        properties.put(RECONNECT_BACKOFF_MAX_MS_CONFIG, 
            getEnv("KAFKA_RECONNECT_BACKOFF_MAX_MS", 10000)); // 10 seconds max
        properties.put(CONNECTIONS_MAX_IDLE_MS_CONFIG, 
            getEnv("KAFKA_CONNECTIONS_MAX_IDLE_MS", 540000)); // 9 minutes
        
        // Retry configuration
        properties.put(RETRIES_CONFIG, 
            getEnv("KAFKA_RETRIES", Integer.MAX_VALUE));
        properties.put(RETRY_BACKOFF_MS_CONFIG, 
            getEnv("KAFKA_RETRY_BACKOFF_MS", 1000)); // 1 second initial
        
        // High throughput settings
        properties.put(LINGER_MS_CONFIG, 
            getEnv("KAFKA_LINGER_MS", 100)); // 100 ms
        properties.put(BATCH_SIZE_CONFIG, 
            getEnv("KAFKA_BATCH_SIZE", 32 * 1024)); // 32 KB
        properties.put(COMPRESSION_TYPE_CONFIG, 
            getEnv("KAFKA_COMPRESSION_TYPE", "snappy"));
        properties.put(ACKS_CONFIG, 
            getEnv("KAFKA_ACKS", "1")); // Leader acknowledgment
        
        // Buffer and memory settings
        properties.put(BUFFER_MEMORY_CONFIG, 
            getEnv("KAFKA_BUFFER_MEMORY", 64 * 1024 * 1024L)); // 64MB
        properties.put(SEND_BUFFER_CONFIG, 
            getEnv("KAFKA_SEND_BUFFER", 128 * 1024)); // 128KB
        properties.put(RECEIVE_BUFFER_CONFIG, 
            getEnv("KAFKA_RECEIVE_BUFFER", 64 * 1024)); // 64KB
        
        // In-flight requests
        properties.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 
            getEnv("KAFKA_MAX_IN_FLIGHT_REQUESTS", 5));
        
        // Idempotence (requires acks=all for true idempotence)
        properties.put(ENABLE_IDEMPOTENCE_CONFIG, 
            getEnv("KAFKA_ENABLE_IDEMPOTENCE", "false"));
        
        // Metadata
        properties.put(METADATA_MAX_AGE_CONFIG, 
            getEnv("KAFKA_METADATA_MAX_AGE_MS", 300000)); // 5 minutes
        properties.put(METADATA_MAX_IDLE_CONFIG, 
            getEnv("KAFKA_METADATA_MAX_IDLE_MS", 300000)); // 5 minutes
        
        // SSL configuration if needed
        properties.putAll(kafkaConfig.sslConfig());
        
        return properties;
    }

    /**
     * Retry template for Kafka operations.
     */
    @Bean
    public RetryTemplate kafkaRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        
        // Exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(30000); // Max 30 seconds
        template.setBackOffPolicy(backOffPolicy);
        
        // Retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        template.setRetryPolicy(retryPolicy);
        
        // Add retry listener for logging
        template.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Kafka retry attempt {} failed: {}", 
                    context.getRetryCount(), throwable.getMessage());
            }
        });
        
        return template;
    }

    /**
     * Scheduled executor for retries.
     */
    @Bean
    public ScheduledExecutorService kafkaRetryExecutor() {
        return Executors.newScheduledThreadPool(5, r -> {
            Thread thread = new Thread(r);
            thread.setName("kafka-retry-executor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Kafka Health Monitor Component.
     */
    @Component
    @RequiredArgsConstructor
    public class KafkaHealthMonitor {
        
        private final KafkaTemplate<String, SolaceSyslogEvent> kafkaTemplate;
        private final AtomicBoolean kafkaHealthy = new AtomicBoolean(true);
        private final AtomicLong lastHealthCheckTime = new AtomicLong(0);
        
        @Scheduled(fixedDelay = 30000) // Check every 30 seconds
        public void checkKafkaHealth() {
            try {
                // Create a health check message
                ProducerRecord<String, SolaceSyslogEvent> healthRecord = 
                    new ProducerRecord<>("health-check-topic", 
                        "health-check", null);
                
                // Send with timeout
                ListenableFuture<SendResult<String, SolaceSyslogEvent>> future = 
                    kafkaTemplate.send(healthRecord);
                
                future.get(5, TimeUnit.SECONDS);
                
                if (!kafkaHealthy.get()) {
                    log.info("Kafka connection restored");
                    kafkaHealthy.set(true);
                    // Reset circuit breaker if it was open
                    if (circuitBreakerOpen.get()) {
                        circuitBreakerOpen.set(false);
                        consecutiveFailures.set(0);
                        log.info("Circuit breaker CLOSED after successful health check");
                    }
                }
                
                lastHealthCheckTime.set(System.currentTimeMillis());
                
            } catch (Exception e) {
                log.error("Kafka health check failed: {}", e.getMessage());
                kafkaHealthy.set(false);
                
                // Consider opening circuit breaker
                if (consecutiveFailures.get() > CIRCUIT_BREAKER_FAILURE_THRESHOLD / 2) {
                    openCircuitBreaker();
                }
            }
        }
        
        public boolean isKafkaHealthy() {
            return kafkaHealthy.get();
        }
        
        public long getLastHealthCheckTime() {
            return lastHealthCheckTime.get();
        }
    }

    /**
     * Metrics Reporter for monitoring.
     */
    @Component
    public class KafkaMetricsReporter {
        
        @Scheduled(fixedDelay = 60000) // Report every minute
        public void reportMetrics() {
            log.info("Kafka Metrics - Success: {}, Failures: {}, DLQ: {}, Circuit Breaker: {}",
                successCount.get(),
                failureCount.get(),
                dlqCount.get(),
                circuitBreakerOpen.get() ? "OPEN" : "CLOSED");
            
            // Reset counters periodically to avoid overflow
            if (successCount.get() > 1000000) {
                successCount.set(0);
                failureCount.set(0);
                dlqCount.set(0);
            }
        }
    }

    // Helper methods for environment variables
    private static String getEnv(final String envVar, final String defaultValue) {
        return System.getenv().getOrDefault(envVar, defaultValue);
    }

    private static int getEnv(final String envVar, final int defaultValue) {
        String value = System.getenv(envVar);
        return ofNullable(value).map(Integer::parseInt).orElse(defaultValue);
    }

    private static long getEnv(final String envVar, final long defaultValue) {
        String value = System.getenv(envVar);
        return ofNullable(value).map(Long::parseLong).orElse(defaultValue);
    }

    /**
     * Custom exception for circuit breaker.
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}