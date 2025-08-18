# Custom State Stores in KSML

This tutorial explores how to implement and optimize custom state stores in KSML, allowing you to maintain and manage state in your stream processing applications with greater flexibility and control.

## Introduction to State Stores

State stores are a critical component of stateful stream processing applications. They allow your application to:

- **Maintain data** across multiple messages and events
- **Track historical information** for context-aware processing  
- **Implement stateful operations** like aggregations and joins
- **Build sophisticated business logic** that depends on previous events
- **Persist state** for fault tolerance and recovery

KSML provides built-in state store capabilities that integrate seamlessly with Kafka Streams, offering exactly-once processing guarantees and automatic state management.

## Prerequisites

Before starting this tutorial:

- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)
- Add the following topics to your `kafka-setup` service in docker-compose.yml to run the examples:

??? info "Topic creation commands - click to expand"

    ```yaml
    # Basic Key-Value Store Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic user_activity && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic user_session_stats && \
    # Window Store Pattern  
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic server_metrics && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic windowed_metrics && \
    # Session Store Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic user_clicks && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic session_analytics && \
    # Optimized Store Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic device_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic device_alerts && \
    # Multi-Store Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic order_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic order_processing_results && \
    ```

## Basic Key-Value Store

Key-value stores are the simplest type of state store, mapping keys to values. This example demonstrates user session tracking.

**What it does**:

- Generates JSON user activity events with comprehensive metadata including device, browser, and location information
- Tracks user sessions across multiple activities using persistent state stores
- Counts actions per user and calculates total session time with enhanced user profiling
- Detects session boundaries and transitions with detailed session analytics
- Returns JSON session statistics with user profiles, device usage patterns, and behavioral insights

??? info "User Activity Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/producer-user-sessions.yaml"
    %}
    ```

??? info "Basic Key-Value Store Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/processor-basic-keyvalue.yaml"
    %}
    ```

**Key concepts demonstrated**:

- JSON serialization for complex state objects
- Session boundary detection
- Persistent state with caching enabled

## Window Store

Window stores organize data by time windows, enabling time-based aggregations and analytics.

**What it does**:

- Generates JSON server metrics with detailed metadata including datacenter, environment, service information, and alerting thresholds
- Aggregates server metrics into 5-minute time windows using state stores for efficient time-based processing
- Calculates statistics (min, max, average, count, range) with categorical data tracking
- Returns JSON window aggregation results with alerting status, distribution analysis, and recent sampling data
- Demonstrates time-based state partitioning with enhanced observability for monitoring applications

??? info "Metrics Data Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/producer-metrics-data.yaml"
    %}
    ```

??? info "Window Store Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/processor-window-store.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Time window calculation and management
- Running aggregations (min, max, sum, count)
- Memory-efficient value storage

## Session Store

Session stores organize data by session windows, automatically handling session boundaries based on inactivity gaps.

**What it does**:

- Generates JSON click events with device information, page metadata, interaction details, and session gap simulation
- Tracks user click sessions with 30-second timeout using session state stores for automatic session boundary detection
- Automatically detects session start and end with comprehensive session lifecycle management
- Calculates detailed session metrics including duration, page count, conversion tracking, and user behavior patterns
- Returns JSON session analytics with device context, user profiling, and behavioral insights

??? info "User Clicks Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/producer-user-clicks.yaml"
    %}
    ```

??? info "Session Store Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/processor-session-store.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Session timeout handling
- Automatic session boundary detection
- Session lifecycle management

## Optimized Store Configuration

For high-volume scenarios, proper store configuration is crucial for performance.

**What it does**:

- Generates JSON device events with facility, zone, and operational metadata for better tracking and debugging
- Processes high-volume device events efficiently using optimized state stores with JSON-based state management
- Tracks device status, temperature readings, error counts, and heartbeat monitoring with comprehensive device profiling
- Returns JSON alerts for temperature warnings, status updates, errors, and heartbeat summaries
- Implements selective output to reduce volume while maintaining rich observability through data

??? info "High Volume Events Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/producer-high-volume.yaml"
    %}
    ```

??? info "Optimized Store Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/processor-optimized-store.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Compact state representation for performance
- Selective event emission
- Error counting and alerting

## Multi-Store Pattern

Complex applications often require multiple state stores working together to manage different aspects of state.

**What it does**:

- Generates JSON order events with customer details, product information, regional data, and payment methods
- Manages order processing with three separate state stores for order state, customer metrics, and inventory tracking
- Tracks complex order lifecycles, customer purchasing patterns, and real-time inventory management
- Returns JSON processing results with order status, customer analytics, inventory impact, and business context
- Demonstrates coordinated updates across multiple stores with rich data for business intelligence

??? info "Order Events Producer - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/producer-order-events.yaml"
    %}
    ```

??? info "Multi-Store Processor - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/custom-state-stores/processor-multi-store.yaml"
    %}
    ```

**Key concepts demonstrated**:

- Multiple state stores in single function
- Coordinated state updates
- Cross-store data correlation

## State Store Types Summary

| Store Type | Use Case | Key Features |
|------------|----------|--------------|
| **Key-Value** | General state, caching, counters | Simple key-to-value mapping |
| **Window** | Time-based aggregations | Automatic time partitioning |
| **Session** | User sessions, activity tracking | Inactivity-based boundaries |

## Conclusion

Custom state stores in KSML provide powerful capabilities for building stateful stream processing applications. By understanding the different store types, configuration options, and optimization techniques, you can build efficient and scalable applications that maintain state effectively across events.