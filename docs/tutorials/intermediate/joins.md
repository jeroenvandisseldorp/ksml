# Implementing Joins in KSML

This tutorial explores how to implement join operations in KSML, allowing you to combine data from multiple streams or tables to create enriched datasets.

## Introduction

Joins are fundamental operations in stream processing that combine data from multiple sources based on common keys. KSML provides three main types of joins, each serving different use cases in real-time data processing.

## Prerequisites

Before starting this tutorial, you should understand:

- [Stream types (KStream, KTable, GlobalKTable)](../../reference/stream-type-reference.md)
- Basic KSML concepts from the [Basics Tutorial](../../getting-started/basics-tutorial.md)
- [Stateful Operations](../../core-concepts/operations.md#stateful-operations)

## Types of Joins in KSML

KSML supports three main categories of joins, each with specific characteristics and use cases:

### 1. Stream-Stream Joins
Join two event streams within a time window to correlate related events.

**When to use:**

- Correlating events from different systems
- Tracking user behavior across multiple actions
- Detecting patterns that span multiple event types

**Key characteristics:**

- Requires time windows for correlation
- Both streams must be co-partitioned (same number of partitions, same key)
- Results are emitted when matching events occur within the window

### 2. Stream-Table Joins
Enrich a stream of events with the latest state from a changelog table.

**When to use:**

- Enriching events with reference data
- Adding current state information to events
- Looking up the latest value for a key

**Key characteristics:**

- Stream events are enriched with the latest table value
- Table provides point-in-time lookups
- Requires co-partitioning between stream and table

### 3. Stream-GlobalTable Joins
Enrich events using replicated reference data available on all instances.

**When to use:**

- Joining with reference data (product catalogs, configuration)
- Foreign key joins where keys don't match directly
- Avoiding co-partitioning requirements

**Key characteristics:**

- GlobalTable is replicated to all application instances
- Supports foreign key extraction via mapper functions
- No co-partitioning required

## Stream-Stream Join

Stream-stream joins correlate events from two streams within a specified time window. This is essential for detecting patterns and relationships between different event types.

### Use Case: User Behavior Analysis

Track user shopping behavior by correlating clicks and purchases within a 30-minute window to understand the customer journey.

??? info "Producer: Clicks and Purchases (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/producer-clicks-purchases.yaml" %}
    ```

??? info "Processor: Stream-Stream Join (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/processor-stream-stream-join-working.yaml" %}
    ```

### Key Configuration Points

The aim here is to show how time windows must be used to correlate events from different streams. The configuration demonstrates:

- **timeDifference**: 30m - Maximum time gap between correlated events
- **Window Stores**: Both streams need stores with `retainDuplicates: true`
- **Window Size**: Must be `2 × timeDifference` (60m) to buffer events from both streams
- **Retention**: Must be `2 × timeDifference + grace` (65m) for proper state cleanup
- **Grace Period**: 5m allowance for late-arriving events

This configuration ensures events are only correlated within a reasonable time frame while managing memory efficiently.

## Stream-Table Join

Stream-table joins enrich streaming events with the latest state from a changelog table. This pattern is common for adding reference data to events.

### Use Case: Order Enrichment

Enrich order events with customer information by joining the orders stream with a customers table.

**Implementation Challenge:** Orders naturally use order_id as the key, but joining requires customer_id. The solution uses a three-step pattern:

1. **Rekey** orders from order_id to customer_id
2. **Join** with the customers table
3. **Rekey** back to order_id for downstream processing

??? info "Producer: Orders (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/producer-orders.yaml" %}
    ```

??? info "Producer: Customers (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/producer-customers.yaml" %}
    ```

??? info "Processor: Stream-Table Join (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/processor-stream-table-join.yaml" %}
    ```

### Rekeying Pattern

The rekeying pattern is essential when join keys don't match naturally:

- Use `transformKey` to extract the join key from the stream
- Perform the join operation
- Optionally restore the original key for downstream consistency

## Stream-GlobalTable Join

GlobalTable joins enable enrichment with reference data that's replicated across all instances, supporting foreign key relationships.

### Use Case: Product Catalog Enrichment

Enrich orders with product details using a foreign key join with a global product catalog.

??? info "Producer: Orders and Products (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/producer-orders-products.yaml" %}
    ```

??? info "Processor: Foreign Key Join (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/processor-foreign-key-join.yaml" %}
    ```

### Foreign Key Extraction

The `mapper` function extracts the foreign key from stream records:

- **Function Type**: `keyValueMapper` (not `foreignKeyExtractor`)
- **Input**: Stream's key and value
- **Output**: Key to lookup in the GlobalTable
- **Example**: Extract product_id from order to join with product catalog

## Advanced Configuration

### ValueJoiner Functions

All join types require a valueJoiner to combine data:

```yaml
valueJoiner:
  type: valueJoiner
  code: |
    # value1: left stream/table
    # value2: right stream/table
    result = {...}
  expression: result
  resultType: json
```

Key requirements:

- Must include `expression` and `resultType` fields
- Handle null values gracefully
- Return appropriate data structure

### Window Store Configuration

For stream-stream joins, configure window stores carefully:

```yaml
thisStore:
  name: stream1_store
  type: window
  windowSize: 60m         # 2 × timeDifference
  retention: 65m          # 2 × timeDifference + grace
  retainDuplicates: true  # Required for joins
```

### Co-partitioning Requirements

Stream-stream and stream-table joins require co-partitioning:

- Same number of partitions in both topics
- Same partitioning strategy
- Same key type and serialization

GlobalTable joins don't require co-partitioning since data is replicated.

## Conclusion

KSML's join operations enable powerful data enrichment patterns:

- **Stream-stream joins** correlate events within time windows
- **Stream-table joins** enrich events with current state
- **Stream-GlobalTable joins** provide foreign key lookups without co-partitioning

Choose the appropriate join type based on your data characteristics and business requirements.

## Further Reading

- [Core Concepts: Operations](../../core-concepts/operations.md)
- [Reference: Join Operations](../../reference/operation-reference.md)
- [Stream Type Reference](../../reference/stream-type-reference.md)