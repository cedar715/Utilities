# Kafka Integration Resilience Improvements

## Overview
This package contains enhanced versions of your Kafka integration code with comprehensive resilience improvements to handle "Disconnecting from node" errors and prevent application failures.

## Files Provided

1. **InboundFlowConfig.java** - Enhanced TCP inbound flow with:
   - Better error handling and retry logic
   - Backpressure mechanism
   - Thread pool improvements
   - Dead Letter Queue support
   - Metrics collection

2. **OutboundFlowKafkaConfig.java** - Enhanced Kafka producer with:
   - Critical timeout configurations
   - Circuit breaker pattern
   - Exponential backoff retry logic
   - Dead Letter Queue (DLQ) implementation
   - Health monitoring
   - Comprehensive error recovery

3. **CollectorConfig.java** - Configuration properties for the collector
4. **KafkaConfig.java** - Kafka-specific configuration properties
5. **application.yml** - Complete application configuration

## Key Improvements

### 1. Critical Kafka Timeouts Added
- `REQUEST_TIMEOUT_MS` (30s) - Prevents indefinite waiting for broker responses
- `DELIVERY_TIMEOUT_MS` (120s) - Total time limit for message delivery including retries
- `MAX_BLOCK_MS` (60s) - Maximum blocking time for send() operations
- `RECONNECT_BACKOFF_MS/MAX_MS` - Controlled reconnection attempts

### 2. Circuit Breaker Pattern
- Automatically opens after 10 consecutive failures
- Stops sending to Kafka for 30 seconds when open
- Diverts messages to DLQ during open state
- Auto-closes after successful health check

### 3. Retry Logic with Exponential Backoff
- Retries up to 5 times with exponential delays (1s, 2s, 4s, 8s, 16s)
- Different handling for recoverable vs non-recoverable errors
- Prevents retry storms during outages

### 4. Dead Letter Queue (DLQ)
- Primary: Sends to Kafka DLQ topic (topic.dlq)
- Fallback: Writes to local file system (/var/log/kafka-dlq/)
- Preserves all failed messages for later reprocessing

### 5. Health Monitoring
- Periodic health checks every 30 seconds
- Automatic circuit breaker management
- Metrics reporting (success/failure/DLQ counts)

### 6. Backpressure Mechanism
- Monitors queue utilization
- Drops messages when >80% full to prevent memory issues
- Prevents cascade failures

### 7. Enhanced Thread Pool Management
- Custom rejection handler instead of CallerRunsPolicy
- Prevents TCP thread blocking
- Better resource management

## Implementation Steps

### 1. Replace Your Existing Files
```bash
# Backup your current files
cp InboundFlowConfig.java InboundFlowConfig.java.backup
cp OutboundFlowKafkaConfig.java OutboundFlowKafkaConfig.java.backup

# Copy the new files
cp /path/to/new/InboundFlowConfig.java src/main/java/com/yourcompany/collector/config/
cp /path/to/new/OutboundFlowKafkaConfig.java src/main/java/com/yourcompany/collector/config/
cp /path/to/new/CollectorConfig.java src/main/java/com/yourcompany/collector/config/
cp /path/to/new/KafkaConfig.java src/main/java/com/yourcompany/collector/config/
cp /path/to/new/application.yml src/main/resources/
```

### 2. Add Required Dependencies
Add these to your `pom.xml` or `build.gradle`:

```xml
<!-- For pom.xml -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```gradle
// For build.gradle
implementation 'org.springframework.retry:spring-retry'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

### 3. Create Required Directories
```bash
sudo mkdir -p /var/log/dlq
sudo mkdir -p /var/log/kafka-dlq
sudo mkdir -p /var/log/solace-otel-collector
sudo chown -R yourapp:yourapp /var/log/dlq /var/log/kafka-dlq /var/log/solace-otel-collector
```

### 4. Environment Variables
Set these critical environment variables for production:

```bash
# Critical Kafka timeouts
export KAFKA_REQUEST_TIMEOUT_MS=30000
export KAFKA_DELIVERY_TIMEOUT_MS=120000
export KAFKA_MAX_BLOCK_MS=60000

# Retry configuration
export KAFKA_RETRIES=2147483647
export KAFKA_RETRY_BACKOFF_MS=1000

# Connection resilience
export KAFKA_RECONNECT_BACKOFF_MS=1000
export KAFKA_RECONNECT_BACKOFF_MAX_MS=10000

# Performance tuning
export KAFKA_LINGER_MS=100
export KAFKA_BATCH_SIZE=32768
export KAFKA_COMPRESSION_TYPE=snappy
```

### 5. Enable Scheduled Tasks
Add `@EnableScheduling` to your main application class:

```java
@SpringBootApplication
@EnableScheduling  // Add this
public class SolaceOtelCollectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolaceOtelCollectorApplication.class, args);
    }
}
```

## Monitoring

### Health Endpoint
Access health status at: `http://localhost:8080/actuator/health`

### Metrics Endpoint
Prometheus metrics at: `http://localhost:8080/actuator/prometheus`

### Log Monitoring
Monitor these log patterns:
- "Circuit breaker OPENED" - Kafka issues detected
- "Circuit breaker CLOSED" - Recovery successful
- "Message sent to DLQ" - Failed messages being preserved
- "Backpressure active" - System under stress
- "Kafka health check failed" - Connection problems

## Testing the Resilience

### 1. Simulate Kafka Outage
```bash
# Stop Kafka
docker-compose stop kafka

# Watch the logs - should see circuit breaker open
tail -f /var/log/solace-otel-collector/app.log

# Start Kafka again
docker-compose start kafka

# Circuit breaker should close after successful health check
```

### 2. Check DLQ Files
```bash
# Check for DLQ files (created during outages)
ls -la /var/log/kafka-dlq/

# View a DLQ file
cat /var/log/kafka-dlq/dlq_*.json | jq .
```

### 3. Monitor Metrics
```bash
# Check application metrics
curl http://localhost:8080/actuator/metrics/kafka.success.count
curl http://localhost:8080/actuator/metrics/kafka.failure.count
```

## Troubleshooting

### Issue: Still Getting Timeouts
- Increase `KAFKA_REQUEST_TIMEOUT_MS` to 60000
- Increase `KAFKA_DELIVERY_TIMEOUT_MS` to 300000
- Check network latency to Kafka brokers

### Issue: High Memory Usage
- Reduce `KAFKA_BUFFER_MEMORY` to 33554432 (32MB)
- Reduce `collector.channel-queue-size` to 5000
- Enable backpressure if disabled

### Issue: Messages Being Dropped
- Check backpressure threshold
- Increase queue sizes if memory permits
- Monitor with metrics to identify bottlenecks

## Performance Tuning

### For High Throughput
```yaml
kafka:
  linger-ms: 200  # Increase batching
  batch-size: 65536  # Larger batches
  compression-type: lz4  # Faster compression
  max-in-flight-requests: 10  # More parallelism
```

### For Low Latency
```yaml
kafka:
  linger-ms: 0  # No batching delay
  batch-size: 16384  # Smaller batches
  compression-type: none  # No compression
  max-in-flight-requests: 1  # Sequential sending
```

### For High Reliability
```yaml
kafka:
  acks: all  # Wait for all replicas
  max-in-flight-requests: 1  # Prevent reordering
  enable-idempotence: true  # Exactly once semantics
```

## Support

For issues or questions:
1. Check logs in `/var/log/solace-otel-collector/app.log`
2. Review DLQ files in `/var/log/kafka-dlq/`
3. Monitor health endpoint
4. Check circuit breaker status in logs

## Version
Version 2.0 - Enhanced with comprehensive resilience features