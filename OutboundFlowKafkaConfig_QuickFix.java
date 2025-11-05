import ...

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
@Profile("kafka")
public class OutboundFlowKafkaConfig {

    private static final String APP_NAME = "solace-otel-collector";
    private final KafkaConfig kafkaConfig;

    @Bean
    public IntegrationFlow kafkaIntegrationFlow(@Qualifier("producerChannel") MessageChannel producerChannel) {
        return IntegrationFlow.from(producerChannel)
                .handle(kafkaMessageHandler())
                .get();
    }

    @Bean
    public IntegrationFlow producerErrorHandlingFlow() {
        return IntegrationFlow.from(producerErrorChannel())
                .log(LoggingHandler.Level.ERROR, "ErrorLogger",
                        message -> String.format("Error sending data to Kafka. Payload: %s", message.getPayload()))
                // ADD THIS: Simple retry logic for failed messages
                .handle(message -> {
                    if (message.getPayload() instanceof ErrorMessage) {
                        ErrorMessage errorMessage = (ErrorMessage) message.getPayload();
                        Message<?> originalMessage = errorMessage.getOriginalMessage();
                        
                        // Simple retry - send back to producer channel after delay
                        if (originalMessage != null) {
                            Integer retryCount = originalMessage.getHeaders().get("retryCount", Integer.class);
                            if (retryCount == null) retryCount = 0;
                            
                            if (retryCount < 3) { // Retry up to 3 times
                                log.warn("Retrying message, attempt {}", retryCount + 1);
                                
                                // Wait before retry (exponential backoff)
                                try {
                                    Thread.sleep((long) Math.pow(2, retryCount) * 1000); // 1s, 2s, 4s
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                
                                // Resend with incremented retry count
                                Message<?> retryMessage = MessageBuilder.fromMessage(originalMessage)
                                        .setHeader("retryCount", retryCount + 1)
                                        .build();
                                        
                                producerChannel.send(retryMessage, 5000); // 5 second timeout
                            } else {
                                log.error("Max retries exceeded for message. Dropping message.");
                                // Optional: Write to file or DLQ here
                            }
                        }
                    }
                })
                .get();
    }

    @Bean
    public MessageChannel producerErrorChannel() {
        return MessageChannels.queue("producerErrorChannel", 100).getObject();
    }

    @Bean
    public KafkaProducerMessageHandler<String, SolaceSyslogEvent> kafkaMessageHandler() {
        KafkaProducerMessageHandler<String, SolaceSyslogEvent> handler = new KafkaProducerMessageHandler<>(kafkaTemplate());
        handler.setMessageKeyExpression(new LiteralExpression(APP_NAME));
        handler.setTopicExpression(new LiteralExpression(kafkaConfig.getTopic()));
        handler.setSendFailureChannel(producerErrorChannel());
        
        // ADD THIS: Set send timeout to prevent indefinite blocking
        handler.setSendTimeout(30000); // 30 seconds
        handler.setSync(false); // Async sending for better resilience
        
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
    public Map producerConfigs() {
        Map properties = new HashMap<>();
        properties.put(BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBrokers());
        properties.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // High throughput settings (YOUR EXISTING)
        properties.put(LINGER_MS_CONFIG, getEnv("KAFKA_LINGER_MS_CONFIG", 100));
        properties.put(BATCH_SIZE_CONFIG, getEnv("KAFKA_BATCH_SIZE_CONFIG", 32 * 1024));
        properties.put(COMPRESSION_TYPE_CONFIG, getEnv("KAFKA_COMPRESSION_TYPE_CONFIG", "snappy"));
        properties.put(ACKS_CONFIG, getEnv("KAFKA_ACKS_CONFIG", "1"));
        properties.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, getEnv("KAFKA_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION", "5"));
        properties.put(RETRIES_CONFIG, getEnv("KAFKA_RETRIES_CONFIG", Integer.MAX_VALUE));
        properties.put(BUFFER_MEMORY_CONFIG, getEnv("KAFKA_BUFFER_MEMORY_CONFIG", 64 * 1024 * 1024L));

        // ADD THESE CRITICAL TIMEOUTS TO FIX YOUR ISSUE:
        properties.put(REQUEST_TIMEOUT_MS_CONFIG, getEnv("KAFKA_REQUEST_TIMEOUT_MS", 30000)); // 30 seconds
        properties.put(DELIVERY_TIMEOUT_MS_CONFIG, getEnv("KAFKA_DELIVERY_TIMEOUT_MS", 120000)); // 2 minutes
        properties.put(MAX_BLOCK_MS_CONFIG, getEnv("KAFKA_MAX_BLOCK_MS", 60000)); // 1 minute
        
        // ADD THESE FOR BETTER RETRY BEHAVIOR:
        properties.put(RETRY_BACKOFF_MS_CONFIG, getEnv("KAFKA_RETRY_BACKOFF_MS", 1000)); // 1 second between retries
        properties.put(RECONNECT_BACKOFF_MS_CONFIG, getEnv("KAFKA_RECONNECT_BACKOFF_MS", 1000)); // 1 second
        properties.put(RECONNECT_BACKOFF_MAX_MS_CONFIG, getEnv("KAFKA_RECONNECT_BACKOFF_MAX_MS", 10000)); // 10 seconds max
        
        properties.putAll(kafkaConfig.sslConfig());
        return properties;
    }

    private static String getEnv(final String envVar, final String defaultValue) {
        return System.getenv().getOrDefault(envVar, defaultValue);
    }

    private static int getEnv(final String envVar, final int defaultValue) {
        String value = System.getenv(envVar);
        return ofNullable(value).isEmpty() ? defaultValue : Integer.parseInt(value);
    }

    private static long getEnv(final String envVar, final long defaultValue) {
        String value = System.getenv(envVar);
        return ofNullable(value).isEmpty() ? defaultValue : Long.parseLong(value);
    }
}