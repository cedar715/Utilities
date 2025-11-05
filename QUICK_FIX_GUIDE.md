# Quick Fix - Minimal Retry Mechanism

## Essential Changes Only

### 1. Add These Critical Kafka Timeouts to `producerConfigs()` Method

In your `OutboundFlowKafkaConfig.java`, add these lines to the `producerConfigs()` method:

```java
// ADD THESE LINES TO FIX TIMEOUT ISSUES:
properties.put(REQUEST_TIMEOUT_MS_CONFIG, getEnv("KAFKA_REQUEST_TIMEOUT_MS", 30000)); // 30 seconds
properties.put(DELIVERY_TIMEOUT_MS_CONFIG, getEnv("KAFKA_DELIVERY_TIMEOUT_MS", 120000)); // 2 minutes  
properties.put(MAX_BLOCK_MS_CONFIG, getEnv("KAFKA_MAX_BLOCK_MS", 60000)); // 1 minute

// ADD THESE FOR BETTER RETRY BEHAVIOR:
properties.put(RETRY_BACKOFF_MS_CONFIG, getEnv("KAFKA_RETRY_BACKOFF_MS", 1000)); // 1 second between retries
properties.put(RECONNECT_BACKOFF_MS_CONFIG, getEnv("KAFKA_RECONNECT_BACKOFF_MS", 1000)); 
properties.put(RECONNECT_BACKOFF_MAX_MS_CONFIG, getEnv("KAFKA_RECONNECT_BACKOFF_MAX_MS", 10000));
```

### 2. Update Your `kafkaMessageHandler()` Method

Add timeout settings to prevent indefinite blocking:

```java
@Bean
public KafkaProducerMessageHandler<String, SolaceSyslogEvent> kafkaMessageHandler() {
    KafkaProducerMessageHandler<String, SolaceSyslogEvent> handler = 
        new KafkaProducerMessageHandler<>(kafkaTemplate());
    handler.setMessageKeyExpression(new LiteralExpression(APP_NAME));
    handler.setTopicExpression(new LiteralExpression(kafkaConfig.getTopic()));
    handler.setSendFailureChannel(producerErrorChannel());
    
    // ADD THESE TWO LINES:
    handler.setSendTimeout(30000); // 30 seconds
    handler.setSync(false); // Async sending for better resilience
    
    return handler;
}
```

### 3. Replace Your `producerErrorHandlingFlow()` Method

Replace your existing error handling flow with this one that includes retry logic:

```java
@Bean
public IntegrationFlow producerErrorHandlingFlow() {
    return IntegrationFlow.from(producerErrorChannel())
            .log(LoggingHandler.Level.ERROR, "ErrorLogger",
                    message -> String.format("Error sending data to Kafka. Payload: %s", message.getPayload()))
            // Simple retry logic for failed messages
            .handle(message -> {
                if (message.getPayload() instanceof ErrorMessage) {
                    ErrorMessage errorMessage = (ErrorMessage) message.getPayload();
                    Message<?> originalMessage = errorMessage.getOriginalMessage();
                    
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
                                    
                            producerChannel().send(retryMessage, 5000); // 5 second timeout
                        } else {
                            log.error("Max retries exceeded. Dropping message.");
                        }
                    }
                }
            })
            .get();
}
```

### 4. Add This Import

Make sure you have this import at the top of your file:
```java
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
```

## That's It!

These minimal changes will:
- **Prevent indefinite timeouts** with proper timeout configurations
- **Retry failed messages** up to 3 times with exponential backoff (1s, 2s, 4s)
- **Keep your app running** even when Kafka has temporary issues

## Set These Environment Variables (Optional but Recommended)

```bash
export KAFKA_REQUEST_TIMEOUT_MS=30000
export KAFKA_DELIVERY_TIMEOUT_MS=120000
export KAFKA_MAX_BLOCK_MS=60000
export KAFKA_RETRY_BACKOFF_MS=1000
```

## How It Works

1. **Timeouts prevent hanging**: Your app won't get stuck waiting forever for Kafka
2. **Automatic retries**: Failed messages retry 3 times with increasing delays
3. **Graceful degradation**: After 3 retries, messages are dropped (you can add file logging later)

## Test It

To test the retry mechanism:
1. Stop Kafka temporarily
2. Send some messages 
3. You'll see retry logs: "Retrying message, attempt 1", "attempt 2", etc.
4. Start Kafka again
5. Messages should go through

This quick fix should solve your immediate problem without overwhelming changes!