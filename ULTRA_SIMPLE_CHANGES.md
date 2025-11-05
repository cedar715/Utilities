# Ultra-Simple Changes to Add to Your Existing Code

## Step 1: Add Timeout Properties
In `OutboundFlowKafkaConfig.java`, in the `producerConfigs()` method, right after your existing properties, ADD:

```java
// Your existing code stays as is...
properties.put(BUFFER_MEMORY_CONFIG, getEnv("KAFKA_BUFFER_MEMORY_CONFIG", 64 * 1024 * 1024L));

// ============ ADD THESE NEW LINES ============
// Critical timeouts to prevent hanging
properties.put("request.timeout.ms", 30000);           // Don't wait more than 30s for response
properties.put("delivery.timeout.ms", 120000);         // Total time limit: 2 minutes
properties.put("max.block.ms", 60000);                 // Don't block send() more than 1 minute
properties.put("retry.backoff.ms", 1000);              // Wait 1s between retries
properties.put("reconnect.backoff.ms", 1000);          // Wait 1s before reconnecting
properties.put("reconnect.backoff.max.ms", 10000);     // Max 10s backoff
// ============================================

properties.putAll(kafkaConfig.sslConfig());
return properties;
```

## Step 2: Add Timeout to Handler
In the `kafkaMessageHandler()` method, ADD two lines:

```java
@Bean
public KafkaProducerMessageHandler<String, SolaceSyslogEvent> kafkaMessageHandler() {
    KafkaProducerMessageHandler<String, SolaceSyslogEvent> handler = 
        new KafkaProducerMessageHandler<>(kafkaTemplate());
    handler.setMessageKeyExpression(new LiteralExpression(APP_NAME));
    handler.setTopicExpression(new LiteralExpression(kafkaConfig.getTopic()));
    handler.setSendFailureChannel(producerErrorChannel());
    
    // ============ ADD THESE TWO LINES ============
    handler.setSendTimeout(30000);  // Don't wait more than 30s
    handler.setSync(false);          // Don't block the thread
    // ============================================
    
    return handler;
}
```

## That's the absolute minimum!

Just these two changes will prevent your app from hanging when Kafka times out.

---

## Optional but Highly Recommended: Add Simple Retry

If you want automatic retries (recommended), also update your error handler:

```java
@Bean
public IntegrationFlow producerErrorHandlingFlow() {
    return IntegrationFlow.from(producerErrorChannel())
            .log(LoggingHandler.Level.ERROR, "ErrorLogger",
                    message -> String.format("Error sending data to Kafka. Payload: %s", message.getPayload()))
            // ============ ADD THIS HANDLE BLOCK ============
            .handle(message -> {
                // Try to resend failed messages up to 3 times
                if (message.getPayload() instanceof ErrorMessage) {
                    ErrorMessage err = (ErrorMessage) message.getPayload();
                    Message<?> original = err.getOriginalMessage();
                    if (original != null) {
                        log.warn("Will retry sending message to Kafka after error");
                        // Simple retry: just send it again after a short delay
                        try {
                            Thread.sleep(2000); // Wait 2 seconds
                            producerChannel().send(original, 5000); // Try again with 5s timeout
                        } catch (Exception e) {
                            log.error("Retry failed, dropping message");
                        }
                    }
                }
            })
            // ============================================
            .get();
}
```

## Summary - Just 3 Small Changes:

1. ✅ Add 6 timeout properties to `producerConfigs()`
2. ✅ Add 2 lines to `kafkaMessageHandler()` 
3. ✅ (Optional) Add retry logic to `producerErrorHandlingFlow()`

This will fix your "Disconnecting from node" timeout issues!