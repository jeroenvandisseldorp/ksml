# Working with Aggregations in KSML

This tutorial explores how to compute statistics, summaries, and time-based analytics from streaming data using KSML's aggregation operations.

## Introduction

Aggregations are stateful operations that combine multiple records into summary values. They are fundamental to stream processing, enabling real-time analytics from continuous data streams.

KSML aggregations(stateful processing capabilities) are built on top of Kafka Streams aggregation operations.

## Prerequisites

Before starting this tutorial:

- Please familiarise yourself with [Stream types (KStream, KTable)](../../reference/stream-type-reference.md)
- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)

## Core Aggregation Concepts

### Grouping Requirements

All aggregations require data to be grouped by key first:

```yaml
# Group by existing key
- type: groupByKey

# Group by new key using mapper function
- type: groupBy
  mapper:
    expression: value.get("category")
    resultType: string
```

**Kafka Streams equivalents:**

- `groupByKey` → `KStream.groupByKey()`
- `groupBy` → `KStream.groupBy()`

### State Stores

Aggregations maintain state in local stores that are fault-tolerant through changelog topics. A changelog is a compacted Kafka topic that records every state change, allowing the state to be rebuilt if an instance fails or restarts. This provides exactly-once processing guarantees and enables automatic state recovery.

```yaml
store:
  name: my_aggregate_store
  type: keyValue        # or 'window' for windowed aggregations
  caching: true         # Enable caching for performance
  loggingDisabled: false  # Keep changelog for fault tolerance
```

### Function Types in Aggregations

**Initializer Function**
Creates the initial state value when a key is seen for the first time.
```yaml
initializer:
  expression: {"count": 0, "sum": 0}  # Initial state
  resultType: json
```

**Aggregator Function**
Updates the aggregate state by combining the current aggregate with a new incoming value.
```yaml
aggregator:
  code: |
    # aggregatedValue: current aggregate
    # value: new record to add
    result = {
      "count": aggregatedValue["count"] + 1,
      "sum": aggregatedValue["sum"] + value["amount"]
    }
  expression: result
  resultType: json
```

**Reducer Function** (for reduce operations)
Combines two values of the same type into a single value, used when no initialization is needed.
```yaml
reducer:
  code: |
    # value1, value2: values to combine
    combined = value1 + value2
  expression: combined
  resultType: long
```

## Types of Aggregations in KSML

KSML supports several aggregation types, each with specific use cases:

### 1. Count
Counts the number of records per key.

**Kafka Streams equivalent:** `KGroupedStream.count()`

**When to use:**

- Tracking event frequencies
- Monitoring activity levels
- Simple counting metrics

### 2. Reduce
Combines values using a reducer function without an initial value.

**Kafka Streams equivalent:** `KGroupedStream.reduce()`

**When to use:**

- Summing values
- Finding min/max
- Combining values of the same type

### 3. Aggregate
Builds complex aggregations with custom initialization and aggregation logic.

**Kafka Streams equivalent:** `KGroupedStream.aggregate()`

**When to use:**

- Computing statistics (avg, stddev)
- Building complex state
- Transforming value types during aggregation

### 4. Cogroup
Aggregates multiple grouped streams together into a single result.

**Kafka Streams equivalent:** `CogroupedKStream`

**When to use:**

- Combining data from multiple sources
- Building unified aggregates from different streams
- Complex multi-stream analytics

## Count Example

Simple counting of events per key:

??? info "User actions producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/producer-user-actions.yaml"
    %}
    ```

??? info "Count user actions processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/processor-count.yaml"
    %}
    ```

### How Count Works

The count operation:

1. Groups messages by key using `groupByKey`
2. Maintains a counter per unique key
3. Increments the counter for each message
4. Outputs the current count as a KTable

## Reduce Example

Combining values without initialization:

??? info "Financial transactions producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/producer-transactions.yaml"
    %}
    ```

??? info "Sum transaction amounts processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/processor-reduce.yaml"
    %}
    ```

### Reduce vs Aggregate

Choose **reduce** when:

- Values are of the same type as the result
- No initialization is needed
- Simple combination logic (sum, min, max)

Choose **aggregate** when:

- Result type differs from input type
- Custom initialization is required
- Complex state management is needed

## Aggregate Example

Building complex statistics:

??? info "Payment events producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/producer-payments.yaml"
    %}
    ```

??? info "Calculate payment statistics processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/processor-aggregate-stats.yaml"
    %}
    ```

### Aggregate Components

1. **Initializer**: Creates empty/initial state
2. **Aggregator**: Updates state with each new value
3. **Result**: Continuously updated aggregate in state store

## Windowed Aggregations

Aggregations can be windowed to compute time-based analytics:

### Window Types

#### Tumbling Windows
Non-overlapping, fixed-size time windows.

```yaml
- type: windowByTime
  windowType: tumbling
  duration: 1h  # 1-hour windows
  grace: 5m     # Allow 5 minutes for late data
```

**Use cases:** Hourly reports, daily summaries

#### Hopping Windows
Overlapping, fixed-size windows that advance by a hop interval.

```yaml
- type: windowByTime
  windowType: hopping
  duration: 1h    # Window size
  advance: 15m    # Hop interval
  grace: 5m
```

**Use cases:** Moving averages, overlapping analytics

#### Session Windows
Dynamic windows based on periods of inactivity.

```yaml
- type: windowBySession
  inactivityGap: 30m  # Close window after 30 min of inactivity
  grace: 5m
```

**Use cases:** User sessions, activity bursts

### Windowed Aggregation Example

??? info "Sensor readings producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/producer-sensors.yaml"
    %}
    ```

??? info "Hourly sensor statistics processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/processor-windowed.yaml"
    %}
    ```

### Window Store Configuration

For windowed aggregations, use window stores:

```yaml
store:
  name: windowed_store
  type: window
  windowSize: 1h      # Must match window duration
  retention: 25h      # How long to keep old windows
  retainDuplicates: false  # Usually false for aggregations
```

## Advanced: Cogroup Operation

Cogroup allows combining multiple grouped streams into a single aggregation. This is useful when you need to aggregate data from different sources into one unified result.

??? info "Orders, Refunds, and Bonuses Producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/producer-cogroup.yaml"
    %}
    ```

??? info "Cogroup Processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/processor-cogroup.yaml"
    %}
    ```

### How Cogroup Works

The cogroup operation:

1. Groups each stream independently by key
2. Combines the grouped streams using cogroup operations
3. Each stream contributes to the aggregate with its own aggregator function
4. Final aggregate operation computes the combined result

> **Note:** Cogroup is an advanced feature that requires careful coordination between multiple streams. Ensure all streams are properly grouped and that aggregator functions handle null values appropriately.

## Complex Example: Sales Analytics

Multi-level aggregation with rekeying and windowing:

??? info "Sales events producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/producer-sales.yaml"
    %}
    ```

??? info "Regional sales analytics processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/aggregations/processor-sales-analytics.yaml"
    %}
    ```

This pipeline demonstrates:

1. **Rekeying** by region using `keyValueMapper`
2. **Windowing** with daily tumbling windows
3. **Complex aggregation** tracking multiple metrics
4. **Nested data structures** for per-product breakdowns

## Performance Considerations

### State Store Types

**RocksDB (default)**

- Persistent, can handle large state
- Slower than in-memory
- Survives restarts

**In-Memory**

- Fast but limited by heap size
- Lost on restart (rebuilt from changelog)
- Good for small, temporary state

### Optimization Strategies

1. **Enable Caching**
   ```yaml
   store:
     caching: true  # Reduces downstream updates
   ```

2. **Tune Commit Intervals**
       - Longer intervals = better throughput, higher latency
       - Shorter intervals = lower latency, more overhead
   
       To configure commit intervals, change your Kafka broker settings:
       ```yaml
       # In your ksml-runner.yaml or application config
       kafka:
         commit.interval.ms: 30000  # 30 seconds for better throughput
         # or
         commit.interval.ms: 100    # 100ms for lower latency
       ```

3. **Pre-filter Data**
       - Filter before grouping to reduce state size
       - Remove unnecessary fields early

4. **Choose Appropriate Window Sizes**
       - Smaller windows = less memory
       - Consider business requirements vs resources

### Memory Management

Monitor state store sizes:

- Each unique key requires memory
- Windowed aggregations multiply by number of windows
- Use retention policies to limit window history

## Common Pitfalls and Solutions

### Forgetting to Group

**Problem:** Aggregation operations require grouped streams

**Solution:** Always use `groupByKey` or `groupBy` before aggregating

### Null Value Handling
**Problem:** Null values can cause aggregation failures

**Solution:** Check for nulls in aggregator functions:
```yaml
code: |
  if value is None:
    return aggregatedValue
  # ... rest of logic
```

### Type Mismatches
**Problem:** Result type doesn't match expression output

**Solution:** Ensure `resultType` matches what your expression returns

### Window Size vs Retention

**Problem:** Confusion between window size and retention

**Solution:** 

- Window size = duration of each window
- Retention = how long to keep old windows
- Retention should be > window size

### Late Arriving Data
**Problem:** Data arrives after window closes

**Solution:** Configure appropriate grace periods:
```yaml
grace: 5m  # Allow 5 minutes for late data
```

## Conclusion

KSML aggregations enable powerful real-time analytics:

- **Count** for frequency analysis
- **Reduce** for simple combinations
- **Aggregate** for complex statistics
- **Windowed operations** for time-based analytics
- **Cogroup** for multi-stream aggregations

Choose the appropriate aggregation type based on your use case, and always consider state management and performance implications.

## Further Reading

- [Core Concepts: Operations](../../core-concepts/operations.md)
- [Reference: Aggregation Operations](../../reference/operation-reference.md)
- [Windowing Tutorial](windowing.md)