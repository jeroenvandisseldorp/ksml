# Integration with External Systems in KSML

This tutorial explores how to integrate KSML applications with external systems such as databases, REST APIs, and other services, allowing you to build comprehensive data processing solutions that connect to your entire technology ecosystem.

## Introduction

While Kafka Streams and KSML excel at processing data within Kafka, real-world applications often need to interact with external systems to:

- Enrich streaming data with reference information from databases
- Call external APIs to fetch additional data or trigger actions
- Integrate with legacy systems or third-party services
- Implement hybrid architectures that combine stream processing with traditional systems

This tutorial covers various patterns for integrating KSML applications with external systems while maintaining reliability, scalability, and fault-tolerance. All examples use JSON data formats to provide rich, structured messaging that enhances observability and debugging capabilities when viewing messages in tools like Kowl UI.

## Prerequisites

Before starting this tutorial:

- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)
- Add the following topics to your `kafka-setup` service in docker-compose.yml to run the examples:

??? info "Topic creation commands - click to expand"

    ```yaml
    # API Enrichment Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic user_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic enriched_user_events && \
    # Database Lookup Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic product_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic enriched_product_events && \
    # Async Integration Pattern
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic order_events && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic external_requests && \
    kafka-topics.sh --create --if-not-exists --bootstrap-server broker:9093 --partitions 1 --replication-factor 1 --topic external_responses && \
    ```

## Integration Patterns

### API Enrichment Pattern

The API enrichment pattern calls external REST APIs to add additional data to streaming events. This is useful when you need real-time data that can't be cached locally.

**What it does:**

- Generates structured JSON user events with device information, session context, referrer data, and campaign tracking for comprehensive analytics
- Makes external API calls to enrich events with user profile data including tier information, preferences, and lifetime value
- Handles API failures gracefully with detailed fallback strategies and error context
- Returns comprehensive JSON enrichment results with original event data, enriched profile information, computed metrics, recommendations, and API performance data
- Demonstrates real-time data enrichment while maintaining stream processing performance and fault tolerance

**Key concepts demonstrated:**

- Making external API calls from KSML functions with JSON data structures
- Handling API failures gracefully with structured fallback strategies
- Managing API latency and timeouts in stream processing
- Enriching events with external data while maintaining stream processing semantics

??? info "User Events Producer (API enrichment demo) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/external-integration/producer-api-enrichment.yaml"
    %}
    ```

??? info "API Enrichment Processor (external API calls) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/external-integration/processor-api-enrichment.yaml"
    %}
    ```

**API enrichment benefits:**

- **Real-time enrichment**: Access to the most current external data
- **Flexible data sources**: Can integrate with any REST API
- **Graceful degradation**: Continues processing even when external systems fail
- **Simple implementation**: Straightforward request-response pattern

### Database Lookup Pattern

The database lookup pattern uses state stores to cache reference data from external databases, providing fast lookups without blocking the stream processing pipeline.

**What it does:**

- Generates structured JSON product events with comprehensive metadata including session information, user context, interaction details, and business context
- Loads product reference data from mock database into JSON-based state stores for fast local lookups
- Enriches product events with detailed product information including names, categories, prices, and calculated business metrics
- Returns comprehensive JSON enrichment results with product details, calculated metrics, business context, cache performance data, and processing information
- Demonstrates high-performance local caching with JSON data structures for scalable reference data access

**Key concepts demonstrated:**

- Loading reference data from external databases into JSON-enabled state stores
- Using state stores as local caches for fast JSON lookups
- Enriching streaming events with cached database data using structured formats
- Managing memory usage and data freshness in cached lookups

??? info "Product Events Producer (database lookup demo) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/external-integration/producer-product-events.yaml"
    %}
    ```

??? info "Database Lookup Processor (cached reference data) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/external-integration/processor-database-lookup.yaml"
    %}
    ```

**Database lookup advantages:**

- **High performance**: Local state store lookups are extremely fast
- **Fault tolerance**: State stores are backed by changelog topics
- **Reduced database load**: Minimizes queries to external databases
- **Offline capability**: Continues working even if database is temporarily unavailable

### Async Integration Pattern

The async integration pattern uses separate Kafka topics for communication with external systems, providing loose coupling and better resilience.

**What it does:**

- Generates comprehensive JSON order events with customer information, fulfillment details, payment context, and business metadata
- Filters paid orders and creates detailed JSON external payment processing requests with correlation tracking
- Processes async responses from external systems with realistic success/failure scenarios including transaction details and financial information
- Returns structured JSON response data with processing results, business impact analysis, and follow-up action recommendations
- Demonstrates complete async integration workflow with JSON messaging for better observability and debugging

**Key concepts demonstrated:**

- Creating request topics for external system communication with JSON payloads
- Generating correlation IDs for request-response tracking
- Processing responses asynchronously through separate topics with structured data
- Building event-driven integration patterns using JSON messaging

??? info "Order Events Producer (async integration demo) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/external-integration/producer-orders.yaml"
    %}
    ```

??? info "Async Integration Processor (request-response pattern) - click to expand"

    ```yaml
    {%
      include "../../definitions/advanced-tutorial/external-integration/processor-async-integration.yaml"
    %}
    ```

**Async integration features:**

- **Loose coupling**: External systems communicate via topics, not direct calls
- **Scalability**: Multiple consumers can process requests and responses
- **Reliability**: Messages are persisted in Kafka topics
- **Monitoring**: Easy to monitor request/response flows through topic metrics

### Key Metrics to Track

Monitor these metrics to ensure integration health:

- Track **latency** of external API calls and database queries
- Monitor the **percentage of successful external system interactions**
- Track different types of **errors** (timeouts, authentication, etc.)
- For lookup patterns, monitor **state store cache hit rates**
- For async patterns, monitor request and response topic lag

### Alert Configuration

Set up alerts for integration issues:

```yaml
alerts:
  high_api_latency:
    condition: avg_api_response_time > 5000ms
    duration: 2_minutes
    severity: warning
    
  external_system_down:
    condition: api_error_rate > 50%
    duration: 1_minute
    severity: critical
    
  cache_miss_rate_high:
    condition: cache_miss_rate > 20%
    duration: 5_minutes
    severity: warning
```

## Advanced Integration Patterns

### Hybrid Event-Database Pattern

Combine streaming events with database lookups for complex business logic:

```yaml
functions:
  hybrid_processor:
    type: valueTransformer
    code: |
      # Process streaming event
      event_data = parse_event(value)
      
      # Look up reference data from cached database
      reference_data = get_reference_data(event_data.entity_id)
      
      # Apply business rules using both streaming and reference data
      result = apply_business_rules(event_data, reference_data)
      
      # Optionally trigger external API call based on result
      if result.requires_notification:
        send_notification(result)
      
      return result
```

### Multi-System Coordination

Coordinate operations across multiple external systems:

```yaml
functions:
  multi_system_coordinator:
    type: keyValueTransformer
    code: |
      # Create requests for multiple external systems
      payment_request = create_payment_request(value)
      inventory_request = create_inventory_request(value)
      
      # Use correlation ID to track related requests
      correlation_id = generate_correlation_id()
      
      # Return multiple outputs for different external systems
      return [
        (f"payment_{correlation_id}", payment_request),
        (f"inventory_{correlation_id}", inventory_request)
      ]
```

## Conclusion

External integration is vital for stream processing. Using API enrichment, database lookups, and async integration, you can build scalable KSML applications that enhance streaming data without sacrificing performance.

Choose the right pattern: API enrichment for real-time data, database lookups for reference data, and async integration for multi-system workflows.