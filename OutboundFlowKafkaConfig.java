package com.yourcompany.collector.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.MessageChannels;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice.MessageAttributeNames;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbound flow -> producerChannel -> Kafka with:
 * - SI-level retry (exponential backoff + listener)
 * - sendFailureChannel for async errors
 * - DLQ channel for messages that give up
 * - Kafka producer timeouts tuned for slow brokers
 * - Configurable sync/async send mode
 */
@Slf4j
@Configuration
@EnableIntegration
@IntegrationComponentScan
@RequiredArgsConstructor
@Profile("kafka")
public class OutboundFlowKafkaConfig {

    private static final String APP_NAME = "solace-otel-collector";
    private final KafkaConfig kafkaConfig;  // your existing @ConfigurationProperties class

    /**
     * Main outbound flow:
     * producerChannel -> kafkaMessageHandler (with retry advice)
     */
    @Bean
    public IntegrationFlow kafkaIntegrationFlow(
            @Qualifier("producerChannel") MessageChannel producerChannel,
            KafkaProducerMessageHandler<String, SolaceSyslogEvent> kafkaMessageHandler,
            RequestHandlerRetryAdvice kafkaRetryAdvice
    ) {
        return IntegrationFlows.from(producerChannel)
                .handle(kafkaMessageHandler, e -> e.advice(kafkaRetryAdvice))
                .get();
    }

    /**
     * Error flow for async send failures (producer.setSendFailureChannel(...))
     */
    @Bean
    public IntegrationFlow producerErrorHandlingFlow(@Qualifier("producerErrorChannel") MessageChannel errorChannel) {
        return IntegrationFlows.from(errorChannel)
                .log(LoggingHandler.Level.ERROR, "KafkaErrorLogger",
                        m -> String.format("Error sending data to Kafka. Payload: %s", m.getPayload()))
                .get();
    }

    /**
     * DLQ flow – handles messages that exhausted retries.
     * Currently logs, but ready for file/Kafka/DB integration.
     */
    @Bean
    public IntegrationFlow kafkaDlqFlow(@Qualifier("kafkaDlqChannel") MessageChannel dlqChannel) {
        return IntegrationFlows.from(dlqChannel)
                .log(LoggingHandler.Level.ERROR, "KafkaDLQ",
                        m -> "Message sent to DLQ: " + m.getPayload())
                .handle(message -> {
                    // --- Option 1: Write to local DLQ file (implement this later)
                    // writeToDlqFile(message);

                    // --- Option 2: Send to Kafka DLQ topic
                    // kafkaTemplate().send(kafkaConfig.getTopic() + ".dlq", message.getPayload());

                    // --- Option 3: Persist in DB for later reprocessing
                    // dlqRepository.save(new DlqEntry(message));
                })
                .get();
    }

    @Bean
    public MessageChannel producerErrorChannel() {
        return MessageChannels.queue("producerErrorChannel", 100).getObject();
    }

    @Bean
    public MessageChannel kafkaDlqChannel() {
        return MessageChannels.queue("kafkaDlqChannel", 1000).getObject();
    }

    /**
     * Spring Integration retry on the handler itself.
     * Includes RetryListener for visibility and uses proper MessageAttributeNames.MESSAGE.
     */
    @Bean
    public RequestHandlerRetryAdvice kafkaRetryAdvice(@Qualifier("kafkaDlqChannel") MessageChannel dlqChannel) {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

        RetryTemplate template = new RetryTemplate();

        int maxAttempts = kafkaConfig.getMaxRetries() != null ? kafkaConfig.getMaxRetries() : 5;

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(
                kafkaConfig.getRetryBackoffMs() != null ? kafkaConfig.getRetryBackoffMs() : 1_000
        );
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(30_000);

        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        // ✅ Retry listener for visibility
        template.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Kafka SI retry attempt {}/{} failed: {}",
                        context.getRetryCount(), maxAttempts, throwable.getMessage());
            }
        });

        advice.setRetryTemplate(template);

        // ✅ Proper recovery callback using constant
        advice.setRecoveryCallback(ctx -> {
            Message<?> failed = (Message<?>) ctx.getAttribute(MessageAttributeNames.MESSAGE);
            if (failed != null) {
                log.error("Message failed after {} attempts, sending to DLQ", maxAttempts);
                dlqChannel.send(failed);
            }
            return null;
        });

        return advice;
    }

    /**
     * Kafka handler – now sync mode configurable from kafkaConfig.
     */
    @Bean
    public KafkaProducerMessageHandler<String, SolaceSyslogEvent> kafkaMessageHandler() {
        KafkaProducerMessageHandler<String, SolaceSyslogEvent> handler =
                new KafkaProducerMessageHandler<>(kafkaTemplate());
        handler.setTopicExpression(new LiteralExpression(kafkaConfig.getTopic()));
        handler.setMessageKeyExpression(new LiteralExpression(APP_NAME));
        handler.setSendFailureChannel(producerErrorChannel());
        handler.setSync(Boolean.TRUE.equals(kafkaConfig.getSyncMode())); // configurable
        handler.setSendTimeout(30_000); // ensures producer thread won’t block forever
        return handler;
    }

    @Bean
    public KafkaTemplate<String, SolaceSyslogEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, SolaceSyslogEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        // base
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBrokers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // critical timeouts
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                kafkaConfig.getRequestTimeoutMs() != null ? kafkaConfig.getRequestTimeoutMs() : 30_000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                kafkaConfig.getDeliveryTimeoutMs() != null ? kafkaConfig.getDeliveryTimeoutMs() : 120_000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,
                kafkaConfig.getMaxBlockMs() != null ? kafkaConfig.getMaxBlockMs() : 60_000);

        // retries
        props.put(ProducerConfig.RETRIES_CONFIG,
                kafkaConfig.getMaxRetries() != null ? kafkaConfig.getMaxRetries() : Integer.MAX_VALUE);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
                kafkaConfig.getRetryBackoffMs() != null ? kafkaConfig.getRetryBackoffMs() : 1_000);

        // performance
        props.put(ProducerConfig.LINGER_MS_CONFIG,
                kafkaConfig.getLingerMs() != null ? kafkaConfig.getLingerMs() : 100);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,
                kafkaConfig.getBatchSize() != null ? kafkaConfig.getBatchSize() : 32 * 1024);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                StringUtils.hasText(kafkaConfig.getCompressionType()) ? kafkaConfig.getCompressionType() : "snappy");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG,
                kafkaConfig.getBufferMemory() != null ? kafkaConfig.getBufferMemory() : 64L * 1024 * 1024);

        // reliability
        props.put(ProducerConfig.ACKS_CONFIG,
                StringUtils.hasText(kafkaConfig.getAcks()) ? kafkaConfig.getAcks() : "all");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                kafkaConfig.getMaxInFlightRequests() != null ? kafkaConfig.getMaxInFlightRequests() : 1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // SSL if enabled
        if (Boolean.TRUE.equals(kafkaConfig.getSslEnabled())) {
            props.put("security.protocol", kafkaConfig.getSecurityProtocol());
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaConfig.getSslTruststoreLocation());
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaConfig.getSslTruststorePassword());
            // add keystore if configured...
        }

        return props;
    }
}
