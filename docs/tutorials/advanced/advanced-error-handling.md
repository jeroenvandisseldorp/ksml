# Advanced Error Handling in KSML

This tutorial explores sophisticated error handling techniques in KSML, enabling you to build robust, resilient stream processing applications that can gracefully handle failures in distributed environments.

## Introduction

Error handling in stream processing applications is critical because:

- Stream processing applications often run continuously for extended periods
- They process large volumes of data from multiple sources  
- They operate in distributed environments where partial failures are common
- Failures in one component can cascade throughout the system
- Data loss or corruption can have significant business impacts

KSML provides several patterns and techniques for handling errors robustly, allowing your applications to continue functioning even when components fail.

## Prerequisites

Before starting this tutorial:

- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)
- Add the following topics to your `kafka-setup` service in docker-compose.yml to run the examples:

??? info "Topic creation commands - click to expand"

    ```yaml
    # Circuit Breaker Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic api-requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic circuit-breaker-state && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic api-responses && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic failed-api-requests && \
    # Retry Strategies
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic tasks-to-process && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic processed-tasks && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic failed-tasks && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic tasks-retry-queue && \
    # Dead Letter Queue Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic business-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic dlq-reprocess-requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic processed-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic dead-letter-queue && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic dlq-alerts && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic reprocessed-events && \
    # Error Monitoring
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic application-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic error-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic error-metrics && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic error-alerts && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic error-dashboard-data && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic sla-violations && \
    # Payment Processing (Error Recovery)
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment-requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic merchant-configuration && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment-methods && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic processed-payments && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment-failures && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment-retry-queue && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic fraud-alerts && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment-reconciliation && \
    # Error Recovery Workflows
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic recovery-actions && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic unrecoverable-errors && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic error-audit-log && \
    # Partial Failures
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic distributed-transactions && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic component-health-status && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic component-registry && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic transaction-state && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic completed-transactions && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic partial-failure-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic compensation-actions && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic consistency-violations && \
    # Saga Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic travel-booking-requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic saga-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic booking-confirmations && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic booking-failures && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic saga-commands && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic saga-compensation-commands && \
    # Compensating Transactions
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic order-requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic compensation-requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic completed-orders && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic failed-orders && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic compensation-events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic compensation-results && \
    ```

## Core Error Handling Patterns

### Circuit Breaker Pattern

The circuit breaker pattern prevents cascading failures by "breaking the circuit" when a downstream service is failing.

**Key concepts demonstrated:**

- Monitoring failure rates and implementing failure thresholds
- Circuit states: CLOSED (normal), OPEN (failing), HALF_OPEN (testing recovery)
- Automatic recovery testing and circuit reset mechanisms
- State persistence for tracking service health across restarts

??? info "Service requests producer (failure simulation) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/advanced-error-handling/producer-service-requests.yaml"
    %}
    ```

??? info "Circuit breaker processor (failure prevention) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/advanced-error-handling/processor-circuit-breaker.yaml"
    %}
    ```

**Circuit breaker benefits:**

- **Prevents cascading failures**: Stops calling failing services to prevent system-wide issues
- **Automatic recovery**: Tests service recovery and resets circuit when service is healthy
- **Graceful degradation**: Allows applications to continue with reduced functionality
- **Configurable thresholds**: Customize failure counts and timeout periods per service
- **JSON observability**: Structured JSON messages provide clear visibility in Kowl UI for monitoring circuit states, failure reasons, and processing times

### Retry Strategies

Implementing sophisticated retry strategies can help recover from transient failures with exponential backoff and jitter.

**Key concepts demonstrated:**

- Exponential backoff with configurable base delay and maximum delay
- Jitter to prevent thundering herd problems
- Retryable vs non-retryable error classification
- Retry state persistence and attempt tracking

??? info "Failing operations producer (transient errors) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/advanced-error-handling/producer-failing-operations.yaml"
    %}
    ```

??? info "Retry strategies processor (intelligent retries) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/advanced-error-handling/processor-retry-strategies.yaml"
    %}
    ```

**Retry strategy features:**

- **Exponential backoff**: Increasing delays prevent overwhelming failing services
- **Jitter addition**: Random variance prevents synchronized retry storms
- **Error classification**: Different retry policies for different error types
- **Maximum attempts**: Prevents infinite retry loops
- **JSON structure**: Rich JSON format enables detailed tracking of retry attempts, backoff delays, and operation metadata in monitoring tools

### Dead Letter Queue Pattern

Dead letter queues handle messages that cannot be processed after all retry attempts are exhausted.

**Key concepts demonstrated:**

- Message validation with multiple failure scenarios
- Routing valid and invalid messages to different topics
- Detailed error information capture for debugging
- Comprehensive message format validation

??? info "Mixed messages producer (validation testing) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/advanced-error-handling/producer-mixed-messages.yaml"
    %}
    ```

??? info "Dead letter queue processor (message validation) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/advanced-error-handling/processor-dead-letter-queue.yaml"
    %}
    ```

**Dead letter queue advantages:**

- **Prevents data loss**: Captures unprocessable messages for later analysis
- **System stability**: Prevents bad messages from blocking processing pipeline
- **Error tracking**: Detailed error information for debugging and monitoring
- **Recovery mechanisms**: Ability to reprocess fixed messages from DLQ
- **JSON diagnostics**: Structured JSON entries include original messages, error reasons, timestamps, and retry recommendations for comprehensive debugging

## Advanced Error Handling Techniques

### Error Classification

Classify errors to apply appropriate handling strategies:

- **Transient errors** (temporary issues - retry with backoff):
      - Network timeouts
      - Connection errors
      - Rate limiting
  
- **Permanent errors** (persistent issues - send to Dead Letter Queue):
      - Invalid data format
      - Authentication errors
      - Authorization errors
  
- **Critical errors** (system issues - immediate escalation):
      - Memory errors
      - Disk full
      - Database down

### State Recovery

Design state stores for recovery after failures:

```yaml
stores:
  recovery_state_store:
    type: keyValue
    keyType: string
    valueType: string
    persistent: true      # Enable persistence for recovery
    caching: true         # Performance with reliability
    logConfig:
      segment.bytes: 104857600
      cleanup.policy: compact  # Keep latest state only
```

### Timeout Management

Set appropriate timeouts for all external operations:

```yaml
functions:
  timeout_handler:
    type: valueTransformer
    globalCode: |
      import signal
      
      def timeout_handler(signum, frame):
        raise TimeoutError("Operation timed out")
      
      def execute_with_timeout(operation, timeout_seconds=5):
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(timeout_seconds)
        try:
          result = operation()
          signal.alarm(0)  # Cancel alarm
          return True, result
        except TimeoutError:
          return False, "timeout"
        except Exception as e:
          signal.alarm(0)
          return False, str(e)
```

## Monitoring Error Handling

### Key Metrics to Track

Monitor these metrics to ensure error handling effectiveness:

- **Error rates by category**: Track different types of errors separately
- **Circuit breaker states**: Monitor when circuits open/close
- **Retry success rates**: Measure retry effectiveness
- **Dead letter queue size**: Monitor unprocessable message volume

### Alert Configuration

Set up alerts in your monitoring system for error handling anomalies:

- **High error rate**: Alert when error rate exceeds 10% for 5 minutes (warning)
- **Circuit breaker open**: Alert immediately when circuit breaker opens (critical)
- **Dead letter queue growth**: Alert when DLQ size exceeds 1000 messages for 10 minutes (warning)

### Testing Error Scenarios

Test your error handling thoroughly:

```yaml
# Example error injection for testing
functions:
  error_injection:
    type: valueTransformer
    globalCode: |
      import random
      
      # Configuration for error injection testing
      ERROR_INJECTION_RATE = 0.1  # 10% error rate for testing
      
    code: |
      # Inject errors for testing
      if random.random() < ERROR_INJECTION_RATE:
        # Simulate different error types
        error_type = random.choice(["timeout", "connection_error", "invalid_data"])
        raise Exception(f"Injected error: {error_type}")
      
      # Normal processing
      return process_message(value)
```

## Conclusion

Advanced error handling in KSML enables you to build resilient stream processing applications that can withstand failures in distributed environments. By implementing patterns like circuit breakers, sophisticated retry strategies, and dead letter queues, you can ensure your applications remain available and maintain data integrity even when components fail.