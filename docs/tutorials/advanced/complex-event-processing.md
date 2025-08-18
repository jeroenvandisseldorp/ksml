# Complex Event Processing in KSML

This tutorial explores how to implement complex event processing (CEP) patterns in KSML, allowing you to detect meaningful patterns across multiple events and streams in real-time.

## Introduction to Complex Event Processing

Complex Event Processing (CEP) is a method of tracking and analyzing streams of data to identify patterns, correlate events, and derive higher-level insights. CEP enables real-time decision making by processing events as they occur rather than in batch.

Key capabilities of CEP in KSML:

- **Pattern detection**: Identify sequences of events that form meaningful patterns
- **Temporal analysis**: Detect time-based patterns and relationships
- **Event correlation**: Connect related events from different sources
- **Anomaly detection**: Identify unusual patterns or deviations
- **Stateful processing**: Maintain context across multiple events

## Prerequisites

Before starting this tutorial:

- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)
- Add the following topics to your `kafka-setup` service in docker-compose.yml to run the examples:

??? info "Topic creation commands - click to expand"

    ```yaml
    # Pattern Detection
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic pattern_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic detected_patterns && \
    # Temporal Pattern Matching
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic temporal_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic temporal_patterns && \
    # Event Correlation
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic user_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic system_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic correlated_events && \
    # Anomaly Detection
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic sensor_metrics && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic anomalies_detected && \
    # Fraud Detection System
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic credit_card_transactions && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic fraud_alerts && \
    ```

## Pattern Detection

Pattern detection identifies specific sequences of events within a stream. This example detects an A→B→C pattern across events.

**What it does**:

- Generates JSON events with event types, session IDs, and metadata
- Tracks event sequences per session using state stores
- Detects when events occur in the pattern A→B→C
- Returns JSON results with pattern completion status and timing information
- Resets tracking when pattern is complete or broken

??? info "Pattern Events Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/producer-pattern-detection.yaml"
    %}
    ```

??? info "Pattern Detection Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/processor-pattern-detection.yaml"
    %}
    ```

**Key concepts demonstrated**:

- State store usage for tracking partial patterns
- Sequential event processing
- Pattern completion and reset logic

## Temporal Pattern Matching

Temporal patterns add time constraints to event sequences. This example detects quick checkout behavior (cart→checkout within 5 minutes).

**What it does**:

- Generates JSON shopping events with action types, product details, and timestamps
- Monitors user shopping behavior using state stores
- Detects when checkout occurs within 5 minutes of adding to cart
- Returns JSON correlation results with time differences and strength indicators
- Distinguishes between quick and slow checkouts

??? info "Temporal Events Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/producer-temporal-events.yaml"
    %}
    ```

??? info "Temporal Pattern Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/processor-temporal-pattern.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Time-based pattern constraints
- Timestamp extraction and comparison
- Temporal window analysis

## Event Correlation

Event correlation combines related events from different streams to provide enriched context.

**What it does**:

- Generates JSON events for both user activities and system events
- Uses stream-table joins to correlate user context with system events
- Detects specific correlation patterns (form errors, page load queries, API calls)
- Returns JSON correlation results with event details, timing analysis, and relationship strength
- Identifies cause-and-effect relationships between user actions and system behavior

??? info "Correlation Events Producer (generates both user and system events) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/producer-correlation-events.yaml"
    %}
    ```

??? info "Event Correlation Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/processor-event-correlation.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Stream-table joins for context enrichment
- Cross-stream event correlation
- Cause-effect relationship detection

## Anomaly Detection

Anomaly detection identifies unusual patterns using statistical analysis.

**What it does**:

- Generates JSON sensor metrics with values, sensor metadata, and timestamps
- Calculates running statistics (mean, standard deviation, z-scores) for each sensor
- Detects statistical anomalies when values fall outside 3 standard deviations
- Returns JSON anomaly reports with statistical analysis, severity levels, and sensor details
- Identifies both spikes and drops in sensor readings

??? info "Metrics Producer (with occasional anomalies) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/producer-metrics.yaml"
    %}
    ```

??? info "Anomaly Detection Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/processor-anomaly-detection.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Statistical anomaly detection (z-score)
- Running statistics calculation
- Threshold-based alerting

## Fraud Detection System

A practical example combining multiple CEP techniques for real-time fraud detection.

**What it does**:

- Generates JSON credit card transactions with transaction metadata
- Tracks transaction history and cardholder patterns using state stores
- Detects multiple fraud patterns: high amounts, rapid transactions, location changes, suspicious merchants
- Calculates fraud risk scores based on multiple factors
- Returns JSON fraud alerts with transaction details, detected patterns, risk analysis, and recommendations
- Generates both high-risk (FRAUD_ALERT) and medium-risk (SUSPICIOUS_TRANSACTION) alerts

??? info "Credit Card Transactions Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/producer-transactions.yaml"
    %}
    ```

??? info "Fraud Detection Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/complex-event-processing/processor-fraud-detection.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Multi-factor pattern analysis
- Risk scoring algorithms
- Transaction velocity checks
- Geographic anomaly detection

## Conclusion

Complex Event Processing in KSML provides capabilities for real-time pattern detection and analysis. By combining state management, temporal operations, and correlation techniques, you can build sophisticated event processing applications that derive actionable insights from streaming data.