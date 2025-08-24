# Operation Reference

This document provides a comprehensive reference for all operations available in KSML. Each operation is described with its parameters, behavior, and examples.

## Introduction

Operations are the building blocks of stream processing in KSML. They define how data is transformed, filtered, aggregated, and otherwise processed as it flows through your application. Operations form the middle part of pipelines, taking input from the previous operation and producing output for the next operation.

Understanding the different types of operations and when to use them is crucial for building effective stream processing applications.

## Operations Overview

KSML supports 28 operations for stream processing. Each operation serves a specific purpose in transforming, filtering, aggregating, or routing data:

| Operation | Purpose | Common Use Cases |
|-----------|---------|------------------|
| **Stateless Transformation Operations** | | |
| [map](#map) | Transform both key and value | Change message format, enrich data |
| [mapValues](#mapvalues) | Transform only the value (preserves key) | Modify payload without affecting partitioning |
| [mapKey](#mapkey) | Transform only the key | Change partitioning key |
| [flatMap](#flatmap) | Transform one record into multiple records | Split batch messages, expand arrays |
| [selectKey](#selectkey) | Select a new key from the value | Extract key from message content |
| [transformKey](#transformkey) | Transform key using custom function | Complex key transformations |
| [transformValue](#transformvalue) | Transform value using custom function | Complex value transformations |
| | | |
| **Filtering Operations** | | |
| [filter](#filter) | Keep records that match a condition | Remove unwanted messages |
| [filterNot](#filternot) | Remove records that match a condition | Exclude specific messages |
| | | |
| **Format Conversion Operations** | | |
| [convertKey](#convertkey) | Convert key format (e.g., JSON to Avro) | Change serialization format |
| [convertValue](#convertvalue) | Convert value format (e.g., JSON to Avro) | Change serialization format |
| | | |
| **Grouping & Partitioning Operations** | | |
| [groupBy](#groupby) | Group by a new key | Prepare for aggregation with new key |
| [groupByKey](#groupbykey) | Group by existing key | Prepare for aggregation |
| [repartition](#repartition) | Redistribute records across partitions | Improve parallelism, rebalance data |
| | | |
| **Stateful Aggregation Operations** | | |
| [aggregate](#aggregate) | Build custom aggregations | Complex calculations, custom state |
| [count](#count) | Count records per key | Track occurrences |
| [reduce](#reduce) | Combine records with same key | Accumulate values |
| | | |
| **Join Operations** | | |
| [join](#join) | Inner join two streams | Correlate related events |
| [leftJoin](#leftjoin) | Left outer join two streams | Include all left records |
| [outerJoin](#outerjoin) | Full outer join two streams | Include all records from both sides |
| | | |
| **Windowing Operations** | | |
| [windowByTime](#windowbytime) | Group into fixed time windows | Time-based aggregations |
| [windowBySession](#windowbysession) | Group into session windows | User session analysis |
| | | |
| **Output Operations** | | |
| [to](#to) | Send to a specific topic | Write results to Kafka |
| [toTopicNameExtractor](#topicnameextractor) | Send to dynamically determined topic | Route to different topics |
| [forEach](#foreach) | Process without producing output | Side effects, external calls |
| [print](#print) | Print to console | Debugging, monitoring |
| | | |
| **Control Flow Operations** | | |
| [branch](#branch) | Split stream into multiple branches | Conditional routing |
| [peek](#peek) | Observe records without modification | Logging, debugging |

## Choosing the Right Operation

When designing your KSML application, consider these factors:

- **State Requirements**: Stateful operations (aggregations, joins) require state stores and more resources
- **Partitioning**: Operations like `groupBy` and `repartition` may trigger data redistribution
- **Performance**: Some operations are more computationally expensive than others
- **Error Handling**: Use `try` operations to handle potential failures gracefully

## Stateless Operations

Stateless operations process each record independently, without maintaining any state between records.

### `filter`

Keeps only records that satisfy a condition.

#### Parameters

| Parameter | Type   | Required | Description             |
|-----------|--------|----------|-------------------------|
| `if`      | Object | Yes      | Specifies the condition |

The `if` can be defined using:

- `expression`: A simple boolean expression
- `code`: A Python code block returning a boolean

#### Example

```yaml
- type: filter
  if:
    expression: value.get("age") >= 18
```

```yaml
- type: filter
  if:
    code: |
      if value.get("status") == "ACTIVE" and value.get("age") >= 18:
        return True
      return False
```

##### **See it in action**:

- [Tutorial: Filtering and Transforming](../tutorials/beginner/filtering-transforming.md#complex-filtering-techniques)

### `flatMap`

Transforms each record into zero or more records, useful for splitting batch messages into individual records.

#### Parameters

| Parameter | Type   | Required | Description                                                  |
|-----------|--------|----------|--------------------------------------------------------------|
| `mapper`  | Object | Yes      | Specifies how to transform each record into multiple records |

The `mapper` must specify:

- `resultType`: Format `"[(keyType,valueType)]"` indicating list of tuples
- `code`: Python code returning a list of tuples `[(key, value), ...]`

#### Example

This example splits order batches containing multiple items into individual item records:

??? info "Producer definition (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/operations/flatmap-producer.yaml"
    %}
    ```

??? info "Processor definition (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/operations/flatmap-processor.yaml"
    %}
    ```

**What this example does:** 

- The producer generates order batches containing multiple items.
- The processor uses `flatMap` to split each order batch into individual item records - transforming 1 input record into 3 output records (one per item).
- Each output record has a unique key combining order ID and item ID, with calculated total prices per item.

### `map`

Transforms both the key and value of each record.

#### Parameters

| Parameter | Type   | Required | Description                                  |
|-----------|--------|----------|----------------------------------------------|
| `mapper`  | Object | Yes      | Specifies how to transform the key and value |

The `mapper` can be defined using:

- `expression`: A simple expression returning a tuple (key, value)
- `code`: A Python code block returning a tuple (key, value)

#### Example

```yaml
- type: map
  mapper:
    code: |
      new_key = value.get("id")
      new_value = {
        "name": value.get("firstName") + " " + value.get("lastName"),
        "age": value.get("age")
      }
      return (new_key, new_value)
```

### `mapValues`

Transforms the value of each record without changing the key.

#### Parameters

| Parameter | Type   | Required | Description                          |
|-----------|--------|----------|--------------------------------------|
| `mapper`  | Object | Yes      | Specifies how to transform the value |

The `mapper` can be defined using:

- `expression`: A simple expression
- `code`: A Python code block

#### Example

```yaml
- type: mapValues
  mapper:
    expression: {"name": value.get("firstName") + " " + value.get("lastName"), "age": value.get("age")}
```

```yaml
- type: mapValues
  mapper:
    code: |
      return {
        "full_name": value.get("firstName") + " " + value.get("lastName"),
        "age_in_months": value.get("age") * 12
      }
```

### `peek`

Performs a side effect on each record without changing it.

#### Parameters

| Parameter | Type   | Required | Description                                    |
|-----------|--------|----------|------------------------------------------------|
| `forEach` | Object | Yes      | Specifies the action to perform on each record |

The `forEach` can be defined using:

- `expression`: A simple expression (rarely used for peek)
- `code`: A Python code block performing the side effect

#### Example

```yaml
- type: peek
  forEach:
    code: |
      log.info("Processing record with key={}, value={}", key, value)
```

### `selectKey`

Changes the key of each record without modifying the value.

#### Parameters

| Parameter     | Type   | Required | Description                         |
|---------------|--------|----------|-------------------------------------|
| `keySelector` | Object | Yes      | Specifies how to select the new key |

The `keySelector` can be defined using:

- `expression`: A simple expression returning the new key
- `code`: A Python code block returning the new key

#### Example

```yaml
- type: selectKey
  keySelector:
    expression: value.get("userId")
```

### `filterNot`

Excludes records that satisfy a condition (opposite of filter). Records are kept when the condition returns false.

#### Parameters

| Parameter | Type   | Required | Description             |
|-----------|--------|----------|-------------------------|
| `if`      | Object | Yes      | Specifies the condition |

The `if` parameter must reference a predicate function that returns a boolean.

#### Example

This example filters out products with "inactive" status, keeping all other products:

??? info "Producer definition (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/operations/filternot-producer.yaml"
    %}
    ```

??? info "Processor definition (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/operations/filternot-processor.yaml"
    %}
    ```

**What this example does:**

- The producer generates products with different statuses: active, inactive, pending, discontinued
- The processor uses `filterNot` with a predicate function to exclude products with "inactive" status
- Products with other statuses (active, pending, discontinued) are kept and passed through to the output topic

### `mapKey`

Transforms the key of each record without modifying the value.

#### Parameters

| Parameter | Type   | Required | Description                        |
|-----------|--------|----------|-----------------------------------|
| `mapper`  | Object | Yes      | Specifies how to transform the key |

The `mapper` can be defined using:
- `expression`: A simple expression returning the new key
- `code`: A Python code block returning the new key

#### Example

```yaml
- type: mapKey
  mapper:
    expression: key.upper()
```

### `convertKey`

Converts the key to a different data format.

#### Parameters

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| `into`    | String | Yes      | Target format for the key |

#### Example

```yaml
- type: convertKey
  into: json
```

### `convertValue`

Converts the value to a different data format.

#### Parameters

| Parameter | Type   | Required | Description                |
|-----------|--------|----------|----------------------------|
| `into`    | String | Yes      | Target format for the value |

#### Example

```yaml
- type: convertValue
  into: avro:UserRecord
```

### `transformKey`

Transforms the key using a custom transformer function.

#### Parameters

| Parameter | Type   | Required | Description                        |
|-----------|--------|----------|-----------------------------------|
| `mapper`  | String | Yes      | Name of the key transformer function |

#### Example

```yaml
- type: transformKey
  mapper: normalize_key
```

### `transformValue`

Transforms the value using a custom transformer function.

#### Parameters

| Parameter | Type   | Required | Description                          |
|-----------|--------|----------|--------------------------------------|
| `mapper`  | String | Yes      | Name of the value transformer function |

#### Example

```yaml
- type: transformValue
  mapper: enrich_user_data
```

## Stateful Operations

Stateful operations maintain state between records, typically based on the record key.

### `aggregate`

Aggregates records by key using a custom aggregation function.

#### Parameters

| Parameter     | Type   | Required | Description                                                    |
|---------------|--------|----------|----------------------------------------------------------------|
| `initializer` | Object | Yes      | Specifies the initial value for the aggregation                |
| `aggregator`  | Object | Yes      | Specifies how to combine the current record with the aggregate |

Both `initializer` and `aggregator` can be defined using:

- `expression`: A simple expression
- `code`: A Python code block

#### Example

```yaml
- type: aggregate
  initializer:
    expression: {"count": 0, "sum": 0}
  aggregator:
    code: |
      if aggregate is None:
        return {"count": 1, "sum": value.get("amount", 0)}
      else:
        return {
          "count": aggregate.get("count", 0) + 1,
          "sum": aggregate.get("sum", 0) + value.get("amount", 0)
        }
```

### `count`

Counts the number of records for each key.

#### Parameters

None.

#### Example

```yaml
- type: groupByKey
- type: count
```

### `groupByKey`

Groups records by their existing key for subsequent aggregation operations.

#### Parameters

None. This operation is typically followed by an aggregation operation.

#### Example

```yaml
- type: groupByKey
- type: count
```

### `groupBy`

Groups records by a new key derived from the record.

#### Parameters

| Parameter     | Type   | Required | Description                         |
|---------------|--------|----------|-------------------------------------|
| `keySelector` | Object | Yes      | Specifies how to select the new key |

The `keySelector` can be defined using:
- `expression`: A simple expression returning the grouping key
- `code`: A Python code block returning the grouping key

#### Example

```yaml
- type: groupBy
  keySelector:
    expression: value.get("category")
```

### `repartition`

Redistributes records across partitions based on the key.

#### Parameters

| Parameter | Type    | Required | Description                           |
|-----------|---------|----------|---------------------------------------|
| `partitions` | Integer | No    | Number of partitions (optional)      |

#### Example

```yaml
- type: repartition
  partitions: 4
```

### `reduce`

Combines records with the same key using a reducer function.

#### Parameters

| Parameter | Type   | Required | Description                         |
|-----------|--------|----------|-------------------------------------|
| `reducer` | Object | Yes      | Specifies how to combine two values |

The `reducer` can be defined using:

- `expression`: A simple expression
- `code`: A Python code block

#### Example

```yaml
- type: reduce
  reducer:
    code: |
      return {
        "count": value1.get("count", 0) + value2.get("count", 0),
        "sum": value1.get("sum", 0) + value2.get("sum", 0)
      }
```

## Join Operations

Join operations combine data from multiple streams based on keys.

### `join`

Performs an inner join between two streams.

#### Parameters

| Parameter    | Type   | Required | Description                                                           |
|--------------|--------|----------|-----------------------------------------------------------------------|
| `with`       | String | Yes      | The name of the stream to join with                                   |
| `windowSize` | Long   | No       | The size of the join window in milliseconds (for stream-stream joins) |

#### Example

```yaml
- type: join
  with: customers
```

### `leftJoin`

Performs a left join between two streams.

#### Parameters

| Parameter    | Type   | Required | Description                                                           |
|--------------|--------|----------|-----------------------------------------------------------------------|
| `with`       | String | Yes      | The name of the stream to join with                                   |
| `windowSize` | Long   | No       | The size of the join window in milliseconds (for stream-stream joins) |

#### Example

```yaml
- type: leftJoin
  with: customers
```

### `outerJoin`

Performs an outer join between two streams.

#### Parameters

| Parameter    | Type   | Required | Description                                                           |
|--------------|--------|----------|-----------------------------------------------------------------------|
| `with`       | String | Yes      | The name of the stream to join with                                   |
| `windowSize` | Long   | No       | The size of the join window in milliseconds (for stream-stream joins) |

#### Example

```yaml
- type: outerJoin
  with: customers
  windowSize: 60000  # 1 minute
```

## Windowing Operations

Windowing operations group records into time-based windows.

### `windowBySession`

Groups records into session windows, where events with timestamps within `inactivityGap` durations are seen as belonging
to the same session.

#### Parameters

| Parameter       | Type     | Required | Description                                                                                  |
|-----------------|----------|----------|----------------------------------------------------------------------------------------------|
| `inactivityGap` | Duration | Yes      | The maximum duration between events before they are seen as belonging to a different session |
| `grace`         | Long     | No       | Grace period for late-arriving data                                                          |

#### Example

```yaml
- type: windowBySession
  inactivityGap: 1m  # 1 minute window
```

```yaml
- type: windowBySession
  inactivityGap: 1m  # 1 minute window
  grace: 15s         # 15 seconds grace
```

### `windowByTime`

Groups records into time windows.

#### Parameters

| Parameter        | Type     | Required | Description                                                          |
|------------------|----------|----------|----------------------------------------------------------------------|
| `windowType`     | String   | No       | The type of window (`tumbling`, `hopping`, or `sliding`)             |
| `timeDifference` | Duration | Yes      | The duration of the window                                           |
| `advanceBy`      | Long     | No       | Only required for `hopping` windows, how often to advance the window |
| `grace`          | Long     | No       | Grace period for late-arriving data                                  |

#### Example

```yaml
- type: windowByTime
  windowType: tumbling
  timeDifference: 60000  # 1 minute window
```

```yaml
- type: windowByTime
  windowType: hopping
  timeDifference: 5m  # 5 minute window
  advanceBy: 1m       # Advance every 1 minute
  grace: 15s          # 15 seconds grace
```

## Terminal Operations

Terminal operations represent the end of a pipeline or perform side effects.

### `forEach`

Processes each record with a side effect, typically used for logging or external actions. This is a terminal operation that does not forward records.

#### Parameters

| Parameter | Type   | Required | Description                                    |
|-----------|--------|----------|------------------------------------------------|
| `forEach` | Object | Yes      | Specifies the action to perform on each record |

The `forEach` can be defined using:
- `code`: A Python code block performing the side effect

#### Example

```yaml
pipelines:
  log_pipeline:
    from: input_stream
    forEach:
      code: |
        log.info("Final processing: key={}, value={}", key, value)
        # Can also call external services here
```

### `print`

Prints each record to stdout for debugging purposes.

#### Parameters

| Parameter | Type   | Required | Description                           |
|-----------|--------|----------|---------------------------------------|
| `prefix`  | String | No       | Optional prefix for the printed output |

#### Example

```yaml
pipelines:
  debug_pipeline:
    from: input_stream
    via:
      - type: filter
        if:
          expression: value.get("debug") == true
    print:
      prefix: "DEBUG: "
```

### `to`

Sends records to a specific Kafka topic.

#### Parameters

| Parameter | Type   | Required | Description                       |
|-----------|--------|----------|-----------------------------------|
| `topic`   | String | Yes      | The name of the target topic      |
| `keyType` | String | No       | The data type of the key          |
| `valueType` | String | No     | The data type of the value        |

#### Example

```yaml
pipelines:
  output_pipeline:
    from: input_stream
    to:
      topic: output_topic
      keyType: string
      valueType: json
```

### `toTopicNameExtractor`

Sends records to topics determined dynamically based on the record content.

#### Parameters

| Parameter              | Type   | Required | Description                                           |
|------------------------|--------|----------|-------------------------------------------------------|
| `topicNameExtractor`   | String | Yes      | Name of the function that determines the topic name   |

#### Example

```yaml
functions:
  route_by_type:
    type: topicNameExtractor
    code: |
      if value.get("type") == "error":
        return "error_topic"
      elif value.get("type") == "warning":
        return "warning_topic"
      else:
        return "info_topic"

pipelines:
  routing_pipeline:
    from: input_stream
    toTopicNameExtractor:
      topicNameExtractor: route_by_type
```

## Branch Operations

Branch operations split a stream into multiple substreams.

### `branch`

Splits a stream into multiple substreams based on conditions.

#### Parameters

| Parameter  | Type  | Required | Description                                              |
|------------|-------|----------|----------------------------------------------------------|
| `branches` | Array | Yes      | List of conditions and handling pipeline for each branch |

The tag `branches` does not exist in the KSML language, but is meant to represent a composite object here that consists of two elements:


| Parameter  | Type      | Required | Description                                                                                                |
|------------|-----------|----------|------------------------------------------------------------------------------------------------------------|
| `if`       | Predicate | Yes      | A condition which can evaluate to True or False. When True, the message is sent down the branch's pipeline |
| `pipeline` | Pipeline  | Yes      | A pipeline that contains a list of processing steps to send the message through                            |

#### Example

```yaml
- branch:
    - if: predicate1
      via:
        - type: transformValue
          mapper: my_value_transformer
      to: target_topic
    - if: predicate2
      as: some_name_to_refer_to_by_another_pipeline
    - if: predicate3
      toTopicNameExtractor: my_topic_name_extractor
```


## Combining Operations

Operations can be combined in various ways to create complex processing pipelines.

### Sequential Operations

Operations are executed in sequence, with each operation processing the output of the previous operation.

```yaml
pipelines:
  my_pipeline:
    from: input_stream
    via:
      - type: filter
        if:
          expression: value.get("amount") > 0
      - type: transformValue
        mapper:
          code: enrich_transaction(value)
      - type: peek
        forEach:
          code: |
            log.info("Processed transaction: {}", value)
    to: output_stream
```

### Branching and Merging

You can create complex topologies by branching streams and merging them back together.

```yaml
pipelines:
  branch_pipeline:
    from: input_stream
    branch:
      - if:
          expression: value.get("type") == "A"
        as: type_a_stream
      - if:
          expression: value.get("type") == "B"
        as: type_b_stream

  process_a_pipeline:
    from: type_a_stream
    via:
      - type: mapValues
        mapper:
          code: process_type_a(value)
    to: merged_stream

  process_b_pipeline:
    from: type_b_stream
    via:
      - type: mapValues
        mapper:
          code: process_type_b(value)
    to: merged_stream
```

## Best Practices

- **Chain operations thoughtfully**: Consider the performance implications of chaining multiple operations.
- **Use stateless operations when possible**: Stateless operations are generally more efficient than stateful ones.
- **Be careful with window sizes**: Large windows can consume significant memory.
- **Handle errors gracefully**: Use error handling operations to prevent pipeline failures.
- **Monitor performance**: Keep an eye on throughput and latency, especially for stateful operations.
