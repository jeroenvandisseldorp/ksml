# Pipeline Reference

This page provides comprehensive reference documentation for pipelines in KSML - the heart of stream processing logic.

## What is a Pipeline?

In KSML, a pipeline defines how data flows from one or more input streams through a series of operations and finally to one or more output destinations.

Think of a pipeline as a recipe that describes exactly how data should be processed from start to finish.

Pipelines connect:

- **Sources**: Where data comes from (Kafka topics via streams, tables, or global tables)
- **Operations**: What happens to the data (transformations, filters, aggregations, etc.)
- **Sinks**: Where the processed data goes (output topics, other pipelines, functions, etc.)

## Pipeline Structure

Every KSML pipeline has a consistent structure with three main components:

```yaml
pipelines:
  my_pipeline_name:
    from: input_stream_name    # Source
    via:                       # Operations (optional)
      - type: operation1
        # operation parameters
      - type: operation2
        # operation parameters
    to: output_stream_name     # Sink
```

This structure makes pipelines easy to read and understand, with a clear flow from source to operations to sink.

## Pipeline Properties

Each pipeline in the `pipelines` section has these main properties:

| Property               | Type           | Required | Description                                                                             |
|------------------------|----------------|----------|-----------------------------------------------------------------------------------------|
| `name`                 | String         | No       | Pipeline name. If not defined, derived from context                                     |
| `from`                 | String/Object  | Yes      | The source stream(s), table(s), or pipeline(s). Can be a reference or inline definition |
| `via`                  | Array          | No       | List of operations to apply to the data                                                 |
| `to`                   | String/Object  | No*      | The destination stream or topic. Can be a reference or inline definition                |
| `as`                   | String         | No*      | Name to save the result for later pipelines                                             |
| `branch`               | Array          | No*      | Split the pipeline based on conditions                                                  |
| `forEach`              | Object         | No*      | Process each message with a function                                                    |
| `print`                | Boolean/Object | No*      | Output messages for debugging (simple boolean or complex object)                        |
| `toTopicNameExtractor` | Object         | No*      | Dynamically determine the output topic                                                  |

*At least one sink type (`to`, `as`, `branch`, `forEach`, `print`, or `toTopicNameExtractor`) is required.

## Pipeline Components

### Sources: Where Data Comes From

The source of a pipeline is specified with the `from` keyword. It defines where the pipeline gets its data from:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `from` | String/Object | Yes | The source stream, table, or pipeline name. Can be a reference or inline definition |

```yaml
from: user_clicks_stream
```

A source can be:

- A **stream** (KStream): For event-based processing
- A **table** (KTable): For state-based processing
- A **globalTable** (GlobalKTable): For reference data
- Another **pipeline**: For chaining pipelines together

Sources must be defined elsewhere in your KSML file (in the `streams`, `tables`, or `globalTables` sections) or be the result of a previous pipeline.

**Full example with `from`**

- [Example with `from`](../tutorials/intermediate/aggregations.md#4-cogroup)

**Note:** KSML does not support multiple sources in the `from` field. For joins, use a single source and specify the join target using the `table` parameter in join operations:

```yaml
# Correct syntax for joins
from: orders_stream
via:
  - type: join
    table: customers_table  # Join target specified here
    valueJoiner: join_function
```

**Learn more about joins**

- [Joins` tutorial](../tutorials/intermediate/joins.md)

### Operations: Transforming the Data

Operations are defined in the `via` section as a list of transformations to apply to the data:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `via` | Array | No | List of operations to apply to the data |

```yaml
via:
  - type: filter
    if:
      expression: value.get('amount') > 100
  - type: mapValues
    mapper:
      expression: {"user": key, "amount": value.get('amount'), "currency": "USD"}
```

Each operation:

- Has a `type` that defines what it does
- Takes parameters specific to that operation type
- Receives data from the previous operation (or the source)
- Passes its output to the next operation (or the sink)

Operations are applied in sequence, creating a processing pipeline where data flows from one operation to the next.

For a complete list of available operations, see the [Operation Reference](operation-reference.md).

**Full example with `via`**

- [Example with `via`](../tutorials/intermediate/aggregations.md#4-cogroup)

### Sinks: Where Data Goes

The sink defines where the processed data should be sent. KSML supports several sink types:

| Parameter              | Type           | Required | Description                                               |
|------------------------|----------------|----------|-----------------------------------------------------------|
| `to`                   | String/Object  | No*      | The destination stream or topic name. Can be a reference or inline definition |
| `as`                   | String         | No*      | Name to save the result for use in later pipelines        |
| `branch`               | Array          | No*      | Split the pipeline based on conditions                    |
| `forEach`              | Object         | No*      | Process each message with a function (terminal operation) |
| `print`                | Boolean/Object | No*      | Output messages for debugging (simple boolean or complex object with filename, label, mapper) |
| `toTopicNameExtractor` | Object         | No*      | Dynamically determine the output topic                    |

*At least one sink type is required.

```yaml
to: processed_orders_stream  # Send to a predefined stream
```

Or more complex sinks:

```yaml
branch:  # Split the pipeline based on conditions
  - if:
      expression: value.get('type') == 'purchase'
    to: purchases_stream
  - if:
      expression: value.get('type') == 'refund'
    to: refunds_stream
  - to: other_events_stream  # Default branch
```

```yaml
toTopicNameExtractor:  # Dynamic topic routing
  expression: "events-" + value.get('category').lower()
```

Common sink types include:

- `to`: Send to a predefined stream, table, or topic
- `as`: Save the result under a name for use in later pipelines
- `branch`: Split the pipeline based on conditions
- `forEach`: Process each message with a function (terminal operation)
- `print`: Output messages for debugging
- `toTopicNameExtractor`: Dynamically determine the output topic

**Examples:**

- `to`, `as`: [Example with `to`, `as`](../tutorials/intermediate/aggregations.md#4-cogroup)
- `branch`: [Example with `branch`](../tutorials/intermediate/branching.md) 
- `forEach`, `print`, `toTopicNameExtractor`: [Operation examples](../reference/operation-reference.md)

## Pipeline Patterns and Techniques

### When to Use `via` vs Multiple Pipelines

One of the decisions in KSML is whether to use a single pipeline with multiple `via` operations or break processing into multiple connected pipelines. Here's when to use each approach:

#### Use `via` (Single Pipeline) When:

**✅ Operations are tightly coupled and belong together logically**
```yaml
# Good: One logical process - "validate and enrich user data"
pipelines:
  validate_and_enrich_user:
    from: raw_user_data
    via:
      - type: filter           # Remove invalid data
        if: 
          expression: value.get('email') is not None
      - type: transformValue   # Add computed field based on validation
        mapper: add_user_status
      - type: peek            # Log the final result
        forEach:
          code: |
            log.info("Processed user: {}", key)
    to: valid_users
```

**✅ Simple sequential processing with no reuse needs**
```yaml
# Good: Straightforward data transformation
pipelines:
  format_sensor_data:
    from: raw_sensor_readings
    via:
      - type: transformValue
        mapper: convert_temperature_units
      - type: transformValue  
        mapper: add_timestamp_formatting
      - type: filter
        if:
          expression: value.get('temperature') > -50
    to: formatted_sensor_data
```

#### Use Multiple Pipelines When:

**✅ You need to reuse intermediate results**
```yaml
# Good: Filtered data used by multiple downstream processes
pipelines:
  filter_high_value_orders:
    from: all_orders
    via:
      - type: filter
        if:
          expression: value.get('total') > 1000
    as: high_value_orders  # Save for reuse

  send_vip_notifications:
    from: high_value_orders  # Reuse filtered data
    via:
      - type: transformValue
        mapper: create_vip_notification
    to: vip_notifications

  update_customer_tier:
    from: high_value_orders  # Reuse same filtered data
    via:
      - type: transformValue
        mapper: calculate_customer_tier
    to: customer_tier_updates
```

**✅ Different processing responsibilities should be separated**
```yaml
# Good: Separate data cleaning from business logic
pipelines:
  # Responsibility: Data standardization and validation
  data_cleaning:
    from: raw_transactions
    via:
      - type: filter
        if:
          expression: value.get('amount') > 0
      - type: transformValue
        mapper: standardize_format
    as: clean_transactions

  # Responsibility: Business rule application
  fraud_detection:
    from: clean_transactions
    via:
      - type: transformValue
        mapper: calculate_fraud_score
      - type: filter
        if:
          expression: value.get('fraud_score') < 0.8
    to: verified_transactions
```

### Connecting Pipelines

 KSML makes it easy to connect pipelines together. The key is using the `as:` parameter to save intermediate results:

```yaml
pipelines:
  # Pipeline 1: Save intermediate result with 'as:'
  filter_high_value_orders:
    from: orders_stream
    via:
      - type: filter
        if:
          expression: value.get('total') > 1000
    as: high_value_orders  # ← Save result for other pipelines

  # Pipeline 2: Use the saved result as input
  process_high_value_orders:
    from: high_value_orders  # ← Reference the saved result
    via:
      - type: transformValue
        mapper:
          expression: {"orderId": value.get('id'), "amount": value.get('total'), "priority": "high"}
    to: priority_orders_stream
```

**When to connect pipelines:**

- Multiple consumers need the same filtered/processed data
- Different processing responsibilities should be separated  
- Complex logic benefits from being broken into stages

**When to use `via` instead:**

- Operations are part of one business process
- No need to reuse intermediate results
- You want lower latency (no intermediate topics)

### Branching Pipelines

The `branch` sink allows you to split a pipeline based on conditions:

```yaml
pipelines:
  route_messages_by_type:
    from: events_stream
    branch:
      - if:
          expression: value.get('type') == 'click'
        to: clicks_stream
      - if:
          expression: value.get('type') == 'purchase'
        to: purchases_stream
      - if:
          expression: value.get('type') == 'login'
        to: logins_stream
      - to: other_events_stream  # Default branch for anything else
```

This is useful for:

- Routing different types of events to different destinations
- Implementing content-based routing patterns
- Creating specialized processing flows for different data types

### Dynamic Output Topics

The `toTopicNameExtractor` sink allows you to dynamically determine the output topic:

```yaml
pipelines:
  route_to_dynamic_topics:
    from: events_stream
    toTopicNameExtractor:
      expression: "events-" + value.get('category').lower()
```

This is useful for:

- Partitioning data across multiple topics
- Creating topic hierarchies
- Implementing multi-tenant architectures

**Full example with `toTopicNameExtractor`**

- [Example with `toTopicNameExtractor`](../reference/operation-reference.md#totopicnameextractor)



## Input and Output Configurations

### Configuring Input Streams

When defining input streams, you can configure various properties:

```yaml
streams:
  orders_stream:
    topic: orders
    keyType: string
    valueType: json
    offsetResetPolicy: earliest  # Start from the beginning of the topic
    timestampExtractor: order_timestamp_extractor  # Custom timestamp extraction
```

Important configuration options include:

- `offsetResetPolicy`: Determines where to start consuming from when no offset is stored
- `timestampExtractor`: Defines how to extract event timestamps from messages
- `keyType` and `valueType`: Define the data types and formats

### Configuring Output Streams

Similarly, output streams can be configured:

```yaml
streams:
  processed_orders_stream:
    topic: processed-orders
    keyType: string
    valueType: avro:ProcessedOrder
```

For inline topic definitions in sinks:

```yaml
to:
  topic: dynamic-output-topic
  keyType: string
  valueType: json
```

## Duration Specification

Some pipeline operations require specifying time durations. In KSML, durations are expressed using a simple format:

```
###x
```

Where:

- `###` is a positive number
- `x` is an optional time unit:
    - (none): milliseconds
    - `s`: seconds
    - `m`: minutes
    - `h`: hours
    - `d`: days
    - `w`: weeks

Examples:
```
100   # 100 milliseconds
30s   # 30 seconds
5m    # 5 minutes
2h    # 2 hours
1d    # 1 day
4w    # 4 weeks
```

Durations are commonly used in windowing operations:

```yaml
- type: windowByTime
  windowType: hopping
  duration: 5m        # 5-minute windows
  advanceBy: 1m       # Advancing every minute (sliding window)
```