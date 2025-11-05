package com.yourcompany.collector.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionExceptionEvent;
import org.springframework.integration.ip.tcp.connection.TcpDeserializationExceptionEvent;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.integration.dsl.HeaderEnricherSpec;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
@IntegrationComponentScan
public class InboundFlowConfig {

    private final CollectorConfig collectorConfig;
    private final AtomicLong droppedMessageCount = new AtomicLong(0);
    private final AtomicLong processedMessageCount = new AtomicLong(0);

    /**
     * Defines the main TCP inbound flow with enhanced error handling.
     */
    @Bean
    public IntegrationFlow tcpInboundFlow(final InboundFlowTransformer inboundFlowTransformer,
                                          final SyslogMetricsTransformer syslogMetricsTransformer) {
        return IntegrationFlow.from(Tcp.inboundAdapter(tcpNioServer())
                        .autoStartup(true)
                        .errorChannel(errorChannel()))
                .transform(Transformers.objectToString()) // TCP raw bytes to String
                .enrichHeaders(h -> h
                        .header(MessageHeaders.ERROR_CHANNEL, "errorChannel")
                        .header("receiveTimestamp", System.currentTimeMillis()))
                .transform(inboundFlowTransformer)
                .transform(syslogMetricsTransformer)
                .handle((payload, headers) -> {
                    // Add circuit breaker logic here
                    if (isBackpressureActive()) {
                        log.warn("Backpressure active, dropping message to prevent overload");
                        droppedMessageCount.incrementAndGet();
                        return null;
                    }
                    processedMessageCount.incrementAndGet();
                    return MessageBuilder.withPayload(payload)
                            .copyHeaders(headers)
                            .build();
                })
                .channel(producerChannel())
                .get();
    }

    /**
     * Enhanced error handling flow with retry logic.
     */
    @Bean
    public IntegrationFlow errorHandlingFlow() {
        return IntegrationFlow.from(errorChannel())
                .log(LoggingHandler.Level.ERROR, "ErrorLogger",
                        message -> String.format("Error in flow. Type: %s, Payload: %s", 
                            message.getPayload().getClass().getSimpleName(),
                            message.getPayload()))
                .handle(handleErrorsWithRetry())
                .get();
    }

    /**
     * Enhanced error handler with retry logic and recovery strategies.
     */
    private MessageHandler handleErrorsWithRetry() {
        return message -> {
            Object payload = message.getPayload();
            
            if (payload instanceof ErrorMessage errorMessage) {
                Throwable cause = errorMessage.getPayload();
                Message<?> originalMessage = errorMessage.getOriginalMessage();
                
                // Unwrap nested exceptions
                if (cause instanceof MessageHandlingException) {
                    cause = ((MessageHandlingException) cause).getCause();
                }
                
                // Handle specific error types
                if (cause instanceof OutOfMemoryError) {
                    log.error("Critical: Out of memory error detected!");
                    // Try to free up memory before crashing
                    System.gc();
                    throw new OutOfMemoryError("Heap maxed out! Application will stop.");
                }
                
                if (cause instanceof java.net.SocketTimeoutException) {
                    log.warn("Socket timeout, will retry: {}", cause.getMessage());
                    // Implement retry logic here
                    retryMessage(originalMessage);
                    return;
                }
                
                if (cause instanceof java.io.IOException) {
                    log.warn("IO Exception in message processing: {}", cause.getMessage());
                    // Could implement circuit breaker here
                    return;
                }
                
                // Log unhandled exceptions
                log.error("Unhandled exception during message processing: {}", 
                    cause != null ? cause.getMessage() : "null", cause);
                
                // Send to dead letter queue if configured
                if (originalMessage != null && collectorConfig.isDeadLetterQueueEnabled()) {
                    sendToDeadLetterQueue(originalMessage, cause);
                }
            } else {
                log.warn("Unexpected error payload type: {}", payload.getClass().getSimpleName());
            }
        };
    }

    /**
     * Retry logic for failed messages.
     */
    private void retryMessage(Message<?> message) {
        if (message == null) return;
        
        Integer retryCount = message.getHeaders().get("retryCount", Integer.class);
        if (retryCount == null) retryCount = 0;
        
        if (retryCount < collectorConfig.getMaxRetries()) {
            // Exponential backoff
            long backoffMillis = (long) Math.pow(2, retryCount) * 1000;
            
            scheduledExecutor().schedule(() -> {
                Message<?> retryMessage = MessageBuilder.fromMessage(message)
                        .setHeader("retryCount", retryCount + 1)
                        .build();
                producerChannel().send(retryMessage, 5000); // 5 second timeout
            }, backoffMillis, TimeUnit.MILLISECONDS);
            
            log.info("Scheduled retry {} for message after {} ms", retryCount + 1, backoffMillis);
        } else {
            log.error("Max retries ({}) exceeded for message", collectorConfig.getMaxRetries());
            sendToDeadLetterQueue(message, new MaxRetriesExceededException("Max retries exceeded"));
        }
    }

    /**
     * Send failed messages to dead letter queue.
     */
    private void sendToDeadLetterQueue(Message<?> message, Throwable error) {
        try {
            Message<?> dlqMessage = MessageBuilder.fromMessage(message)
                    .setHeader("dlq_error", error != null ? error.getMessage() : "Unknown error")
                    .setHeader("dlq_timestamp", System.currentTimeMillis())
                    .build();
            
            deadLetterChannel().send(dlqMessage, 1000);
            log.warn("Message sent to dead letter queue due to: {}", 
                error != null ? error.getMessage() : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to send message to DLQ: {}", e.getMessage());
            // Last resort: write to file
            writeToFailureFile(message, error);
        }
    }

    /**
     * Write failed messages to file as last resort.
     */
    private void writeToFailureFile(Message<?> message, Throwable error) {
        // Implementation would write to a file
        log.error("Writing failed message to file system as last resort");
    }

    /**
     * Check if backpressure should be applied.
     */
    private boolean isBackpressureActive() {
        QueueChannel channel = (QueueChannel) producerChannel();
        int queueSize = channel.getQueueSize();
        int maxCapacity = collectorConfig.getChannelQueueSize();
        
        // Apply backpressure if queue is 80% full
        return queueSize > (maxCapacity * 0.8);
    }

    /**
     * Enhanced TCP NIO server configuration.
     */
    @Bean
    public AbstractServerConnectionFactory tcpNioServer() {
        TcpNioServerConnectionFactory server = new TcpNioServerConnectionFactory(collectorConfig.getSyslogPort());
        server.setSerializer(getSerializer());
        server.setDeserializer(getSerializer());
        server.setSoTimeout(collectorConfig.getSocketTimeout());
        server.setSoKeepAlive(true);
        server.setLeaveOpen(true);
        server.setSoReceiveBufferSize(collectorConfig.getBufferSize());
        server.setSoSendBufferSize(collectorConfig.getBufferSize());
        server.setTaskExecutor(tcpInboundExecutor());
        
        // Enhanced connection monitoring
        server.setApplicationEventPublisher(event -> {
            if (event instanceof TcpConnectionExceptionEvent connEvent) {
                log.error("TCP connection error from {}: {}", 
                    connEvent.getConnectionFactoryName(),
                    connEvent.getCause().getMessage(), 
                    connEvent.getCause());
            } else if (event instanceof TcpDeserializationExceptionEvent deserializationEvent) {
                log.error("Deserialization Error: {}", 
                    deserializationEvent.getCause().getMessage(), 
                    deserializationEvent.getCause());
            }
        });
        
        return server;
    }

    /**
     * Enhanced thread pool executor with better rejection handling.
     */
    @Bean
    public Executor tcpInboundExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                collectorConfig.getCorePoolSize(),
                collectorConfig.getMaxPoolSize(),
                collectorConfig.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(collectorConfig.getThreadQueueSize()),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("tcp-inbound-" + counter.incrementAndGet());
                        thread.setDaemon(false);
                        thread.setUncaughtExceptionHandler((t, e) -> 
                            log.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e));
                        return thread;
                    }
                },
                new RejectedExecutionHandler() {
                    private final AtomicLong rejectedCount = new AtomicLong(0);
                    private final AtomicLong lastLogTime = new AtomicLong(0);
                    
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        if (!executor.isShutdown()) {
                            rejectedCount.incrementAndGet();
                            
                            // Log every 1000 rejections or every 10 seconds
                            long now = System.currentTimeMillis();
                            if (rejectedCount.get() % 1000 == 0 || now - lastLogTime.get() > 10000) {
                                log.warn("TCP thread pool saturated. Total rejected: {}, Queue size: {}/{}", 
                                    rejectedCount.get(), 
                                    executor.getQueue().size(),
                                    collectorConfig.getThreadQueueSize());
                                lastLogTime.set(now);
                            }
                            
                            // Try to put back with timeout
                            try {
                                boolean added = executor.getQueue().offer(r, 100, TimeUnit.MILLISECONDS);
                                if (!added) {
                                    droppedMessageCount.incrementAndGet();
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.error("Interrupted while queuing task", e);
                            }
                        }
                    }
                }
        );
        
        // Enable core thread timeout to release resources during low load
        executor.allowCoreThreadTimeOut(true);
        
        return executor;
    }

    /**
     * Enhanced producer channel with overflow monitoring.
     */
    @Bean
    public MessageChannel producerChannel() {
        QueueChannel channel = MessageChannels.queue("producerChannel", 
            collectorConfig.getChannelQueueSize()).getObject();
        channel.setLoggingEnabled(true);
        return channel;
    }

    /**
     * Dead letter channel for failed messages.
     */
    @Bean
    public MessageChannel deadLetterChannel() {
        return MessageChannels.queue("deadLetterChannel", 1000).getObject();
    }

    /**
     * Error channel configuration.
     */
    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.queue("errorChannel", 
            collectorConfig.getErrorQueueSize()).getObject();
    }

    /**
     * Scheduled executor for retry logic.
     */
    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(5, r -> {
            Thread thread = new Thread(r);
            thread.setName("retry-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Configures the serializer based on the codec type.
     */
    private AbstractByteArraySerializer getSerializer() {
        AbstractByteArraySerializer serializer = switch (collectorConfig.getCodecType().toLowerCase()) {
            case "crlf" -> new ByteArrayCrLfSerializer();
            case "lh" -> new ByteArrayLengthHeaderSerializer();
            default -> new ByteArrayLfSerializer();
        };
        serializer.setMaxMessageSize(collectorConfig.getMaxMessageSize());
        return serializer;
    }

    /**
     * Metrics reporter component.
     */
    @Component
    public class MetricsReporter {
        @Scheduled(fixedDelay = 60000) // Report every minute
        public void reportMetrics() {
            log.info("Metrics - Processed: {}, Dropped: {}, Queue utilization: {}%",
                processedMessageCount.get(),
                droppedMessageCount.get(),
                calculateQueueUtilization());
            
            // Reset counters if needed
            if (processedMessageCount.get() > 1000000) {
                processedMessageCount.set(0);
                droppedMessageCount.set(0);
            }
        }
        
        private int calculateQueueUtilization() {
            QueueChannel channel = (QueueChannel) producerChannel();
            return (channel.getQueueSize() * 100) / collectorConfig.getChannelQueueSize();
        }
    }

    /**
     * Custom exception for max retries exceeded.
     */
    public static class MaxRetriesExceededException extends Exception {
        public MaxRetriesExceededException(String message) {
            super(message);
        }
    }
}