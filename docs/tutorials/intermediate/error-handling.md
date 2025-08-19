# Error Handling and Recovery in KSML

This tutorial explores comprehensive strategies for handling errors and implementing recovery mechanisms in KSML applications, helping you build robust and resilient stream processing pipelines.

## Prerequisites

Before starting this tutorial:

- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)
- Add the following topics to your `kafka-setup` service in docker-compose.yml to run the examples:

??? info "Topic creation commands - click to expand"

    ```yaml
    # Validation and Filtering
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic incoming_orders && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic valid_orders && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic invalid_orders && \
    # Try-Catch Error Handling
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic sensor_readings && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic processed_sensors && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic sensor_errors && \
    # Dead Letter Queue
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic payment_requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic processed_payments && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic failed_payments && \
    # Retry Strategies
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic api_operations && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic successful_operations && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic failed_operations && \
    # Circuit Breaker
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic service_requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic service_responses && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic circuit_events && \
    ```

## Introduction to Error Handling

Error handling is critical for production stream processing applications. Common error scenarios include:

- **Data Quality Issues**: Invalid, malformed, or missing data
- **External Service Failures**: Network timeouts, API errors, service unavailability  
- **Resource Constraints**: Memory limitations, disk space issues
- **Business Rule Violations**: Data that doesn't meet application requirements
- **Transient Failures**: Temporary network issues, rate limiting, service overload

Without proper error handling, these issues can cause application crashes, data loss, incorrect results, or inconsistent state.

## Core Error Handling Patterns

### 1. Validation and Filtering

Proactively validate data and filter out problematic messages before they cause downstream errors.

??? info "Order Events Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/producer-order-validation.yaml"
    %}
    ```

??? info "Validation and Filtering Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/processor-validation-filtering.yaml"
    %}
    ```

This example demonstrates the `branch` operation for routing messages based on validation results. Key features:

- **Early validation**: Check required fields and data ranges before processing
- **Route separation**: Valid messages go to one topic, invalid to another  
- **Error classification**: Categorize different types of validation failures
- **Detailed logging**: Track processing decisions for monitoring and debugging

### 2. Try-Catch Error Handling

Use try-catch blocks in Python functions to handle exceptions gracefully and provide fallback behavior.

??? info "Sensor Data Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/producer-sensor-data.yaml"
    %}
    ```

??? info "Try-Catch Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/processor-try-catch.yaml"
    %}
    ```

This example shows exception handling within `valueTransformer` functions. Key techniques:

- **Exception handling**: Use try-catch to prevent function failures from stopping processing
- **Graceful degradation**: Return error information instead of crashing
- **Error classification**: Distinguish between validation errors and processing errors  
- **Fallback values**: Provide default values or error objects when processing fails

### 3. Dead Letter Queue Pattern

Route messages that cannot be processed to dedicated error topics for later analysis or reprocessing.

??? info "Payment Requests Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/producer-payment-requests.yaml"  
    %}
    ```

??? info "Dead Letter Queue Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/processor-dead-letter-queue.yaml"
    %}
    ```

This example demonstrates message routing based on processing results. Key concepts:

- **Error classification**: Permanent errors vs. temporary errors that can be retried
- **Rich error context**: Include original data, error details, and processing metadata  
- **Retry eligibility**: Mark messages as retryable or permanent failures
- **Audit trail**: Maintain detailed records for debugging and compliance

### 4. Retry Strategies with Exponential Backoff

Implement sophisticated retry logic for transient failures with exponential backoff and jitter.

??? info "API Operations Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/producer-api-operations.yaml"
    %}
    ```

??? info "Retry Strategies Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/processor-retry-strategies.yaml"
    %}
    ```

This example shows advanced retry logic with exponential backoff. Key features:

- **Exponential backoff**: Increasing delays (1s, 2s, 4s) prevent overwhelming failing services
- **Jitter addition**: Random variance prevents synchronized retry storms across multiple instances
- **Error classification**: Different retry policies for different error types (retryable vs permanent)
- **Maximum attempts**: Prevent infinite retry loops with configurable limits
- **Success tracking**: Monitor retry success rates for service health insights

### 5. Circuit Breaker Pattern

Prevent cascading failures by temporarily stopping calls to failing services, allowing them to recover.

??? info "Service Requests Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/producer-service-requests.yaml"
    %}
    ```

??? info "Circuit Breaker Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/error-handling/processor-circuit-breaker.yaml"
    %}
    ```

This example implements a circuit breaker using global state in functions. Key concepts:

- **Circuit states**: CLOSED (normal), OPEN (failing), HALF_OPEN (testing recovery)
- **Failure threshold**: Opens circuit after consecutive failures to prevent further damage
- **Automatic recovery**: Transitions to HALF_OPEN after timeout, then CLOSED after success
- **Fast failure**: Rejects requests immediately when circuit is open, reducing latency
- **State management**: Tracks failure counts, success rates, and recovery timing

## Error Handling Best Practices

### Data Type Recommendations
- **Use JSON types**: Provides flexibility for error objects and easy inspection in Kowl UI
- **Include context**: Add timestamps, retry counts, and error classifications to all error messages
- **Preserve original data**: Keep original messages in error objects for debugging

### Function Design Patterns  
- **Handle null values**: Always check for `None`/`null` values explicitly
- **Use appropriate exceptions**: Choose specific exception types for different error conditions
- **Provide meaningful errors**: Include context about what went wrong and potential solutions
- **Log appropriately**: Use different log levels (info/warn/error) based on severity

### Monitoring and Alerting
- **Track error rates**: Monitor error percentages by type and set appropriate thresholds
- **Circuit breaker metrics**: Alert when circuits open and track recovery times  
- **Retry success rates**: Measure effectiveness of retry strategies
- **Dead letter queue size**: Monitor unprocessable message volume

### Testing Error Scenarios
- **Simulate failures**: Test error handling code with various failure scenarios
- **Load testing**: Ensure error handling works under high load conditions
- **Recovery testing**: Verify systems can recover from failure states

## Error Pattern Selection Guide

| Pattern | Use Case | Benefits | When to Use |
|:--------|:---------|:---------|:------------|
| **Validation & Filtering** | Data quality issues | Early detection, clear routing | Input data validation, format checking |
| **Try-Catch** | Function-level errors | Graceful degradation | Type conversion, calculations, parsing |
| **Dead Letter Queue** | Permanent failures | No data loss, failure analysis | Malformed data, business rule violations |
| **Retry Strategies** | Transient failures | Fault tolerance, automatic recovery | Network timeouts, rate limits, temporary errors |
| **Circuit Breaker** | External service failures | Prevents cascading failures | API calls, database connections, service dependencies |

## Next Steps

- [Stream Processing Operations Reference](../../reference/operation-reference.md)
- [Function Types Reference](../../reference/function-reference.md)
- [Advanced Stream Processing Patterns](../advanced/)