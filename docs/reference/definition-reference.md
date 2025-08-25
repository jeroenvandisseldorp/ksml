# KSML Definition Reference

This comprehensive reference guide covers everything you need to know about writing KSML definitions - from basic syntax to advanced data modeling.

## KSML File Structure

Every KSML definition file is written in YAML and consists of these top-level sections:

```yaml
{% include "../definitions/reference/template-definition.yaml" %}
```

## Application Metadata

| Property      | Type   | Required | Description                          |
|---------------|--------|----------|--------------------------------------|
| `name`        | String | No       | The name of the KSML definition      |
| `version`     | String | No       | The version of the KSML definition   |
| `description` | String | No       | A description of the KSML definition |

```yaml
name: "order-processing-app"
version: "1.2.3"
description: "Processes orders from the order topic and enriches them with customer data"
```

## Data Sources and Targets

KSML supports three types of data streams, each with different characteristics:

### Streams (KStream)

**Use for:** Event-based processing where each record is an independent event.

```yaml
streams:
  user_clicks_stream:
    topic: user-clicks
    keyType: string
    valueType: json
    offsetResetPolicy: earliest  # Optional: earliest, latest, none, by_duration:<duration>
    timestampExtractor: click_timestamp_extractor  # Optional
    partitioner: click_partitioner  # Optional
```

**Key characteristics:**
- Records are immutable and processed individually
- Ideal for processing user actions, sensor readings, transactions
- Records arrive in order and are processed one at a time

| Property             | Type   | Required | Description                                                                  |
|----------------------|--------|----------|------------------------------------------------------------------------------|
| `topic`              | String | Yes      | The Kafka topic to read from or write to                                     |
| `keyType`            | String | Yes      | The type of the record key                                                   |
| `valueType`          | String | Yes      | The type of the record value                                                 |
| `offsetResetPolicy`  | String | No       | The offset reset policy (`earliest`, `latest`, `none`, `by_duration:<dur>`) |
| `timestampExtractor` | String | No       | Function to extract timestamps from records                                  |
| `partitioner`        | String | No       | Function that determines message partitioning                                |

### Tables (KTable)

**Use for:** State-based processing where records represent updates to entities.

```yaml
tables:
  user_profiles_table:
    topic: user-profiles
    keyType: string
    valueType: avro:UserProfile
    store: user_profiles_store  # Optional state store name
```

**Key characteristics:**
- Records with the same key represent updates to the same entity
- Only the latest record for each key is retained (compacted)
- Ideal for user profiles, inventory levels, configuration settings

| Property             | Type   | Required | Description                                                                  |
|----------------------|--------|----------|------------------------------------------------------------------------------|
| `topic`              | String | Yes      | The Kafka topic to read from or write to                                     |
| `keyType`            | String | Yes      | The type of the record key                                                   |
| `valueType`          | String | Yes      | The type of the record value                                                 |
| `offsetResetPolicy`  | String | No       | The offset reset policy                                                      |
| `timestampExtractor` | String | No       | Function to extract timestamps from records                                  |
| `partitioner`        | String | No       | Function that determines message partitioning                                |
| `store`              | String | No       | The name of the key/value state store to use                                 |

### Global Tables (GlobalKTable)

**Use for:** Reference data that needs to be available on all application instances.

```yaml
globalTables:
  product_catalog:
    topic: product-catalog
    keyType: string
    valueType: avro:Product
    store: product_catalog_store  # Optional state store name
```

**Key characteristics:**
- Fully replicated on each application instance (not partitioned)
- Allows joins without requiring co-partitioning
- Ideal for product catalogs, country codes, small to medium reference datasets

| Property             | Type   | Required | Description                                                                  |
|----------------------|--------|----------|------------------------------------------------------------------------------|
| `topic`              | String | Yes      | The Kafka topic to read from                                                 |
| `keyType`            | String | Yes      | The type of the record key                                                   |
| `valueType`          | String | Yes      | The type of the record value                                                 |
| `offsetResetPolicy`  | String | No       | The offset reset policy                                                      |
| `timestampExtractor` | String | No       | Function to extract timestamps from records                                  |
| `partitioner`        | String | No       | Function that determines message partitioning                                |
| `store`              | String | No       | The name of the key/value state store to use                                 |

### Choosing the Right Stream Type

| If you need to...                                         | Consider using... |
|-----------------------------------------------------------|-------------------|
| Process individual events as they occur                   | KStream           |
| Maintain the latest state of entities                     | KTable            |
| Join with data that's needed across all partitions        | GlobalKTable      |
| Process time-ordered events                               | KStream           |
| Track changes to state over time                          | KTable            |
| Access reference data without worrying about partitioning | GlobalKTable      |

## Function Definitions

Functions define reusable Python logic that can be referenced in pipelines:

| Property     | Type      | Required  | Description                                                                                                                                                      |
|--------------|-----------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`       | String    | Yes       | The type of function (predicate, mapper, aggregator, etc.)                                                                                                       |
| `parameters` | Array     | No        | Parameters for the function                                                                                                                                      |
| `globalCode` | String    | No        | Python code executed once upon startup                                                                                                                           |
| `code`       | String    | No        | Python code implementing the function                                                                                                                            |
| `expression` | String    | No        | An expression that the function will return as value                                                                                                             |
| `resultType` | Data type | Sometimes | The data type returned by the function. Required when it cannot be derived from function type.                                                                  |

```yaml
functions:
  is_valid_order:
    type: "predicate"
    expression: value.get("total") > 0 and value.get("items") is not None

  calculate_total:
    type: "valueTransformer"
    code: |
      total = sum(item.get("price", 0) * item.get("quantity", 0) 
                  for item in value.get("items", []))
      return {**value, "total": total}
    resultType: struct
```

## Pipeline Definitions

Pipelines define the flow of data through your application with three main components:

| Property | Type         | Required | Description                                    |
|----------|--------------|----------|------------------------------------------------|
| `from`   | String/Array | Yes      | The source stream(s), table(s), or pipeline(s)|
| `via`    | Array        | No       | The operations to apply to the data            |
| `to`     | String/Array | No*      | The destination stream(s)                      |
| `as`     | String       | No*      | Save result for use in later pipelines        |
| `branch` | Array        | No*      | Split pipeline based on conditions            |
| `print`  | Boolean      | No*      | Output messages for debugging                  |

*At least one sink type is required.

```yaml
pipelines:
  process_orders:
    from: orders
    via:
      - type: filter
        if:
          functionRef: is_valid_order
      - type: transformValue
        mapper: calculate_total
      - type: join
        with: customers
        valueJoiner:
          expression: {**value1, "customer": value2}
    to: processed_orders
```

## Data Types

KSML supports comprehensive data typing for both keys and values:

### Primitive Types
- `boolean`, `byte`, `short`, `int`, `long`, `float`, `double`, `string`, `bytes`, `null`

### Complex Types
- `struct`: Key-value map (like JSON objects)
- `[type]`: List of elements (e.g., `[string]`, `[struct]`)
- `(type1, type2, ...)`: Tuple of different types (e.g., `(string, int)`)
- `enum(val1, val2, ...)`: Enumeration of allowed values
- `union(type1, type2, ...)`: Union of possible types
- `windowed(type)`: Windowed keys from time-based operations
- `?` or `any`: Any type (use with caution)

### Data Format Notations

KSML supports various data formats through notation prefixes:

- `json`: JSON format
- `avro:SchemaName`: AVRO with schema registry
- `csv:SchemaName`: CSV with defined schema
- `xml:SchemaName`: XML with defined schema
- `protobuf:MessageType`: Protocol Buffers

```yaml
streams:
  events:
    topic: events
    keyType: string
    valueType: avro:EventData  # Uses AVRO schema from registry
```

## Operations

Operations are the building blocks of pipelines that transform, filter, or aggregate data:

### Stateless Operations
- `map`, `mapValues`: Transform records
- `filter`: Keep records matching conditions
- `flatMap`, `flatMapValues`: Transform to multiple records
- `peek`: Side effects without changing records
- `selectKey`: Change record keys

### Stateful Operations
- `aggregate`, `count`, `reduce`: Combine records by key
- `join`, `leftJoin`, `outerJoin`: Combine streams/tables
- `groupBy`, `groupByKey`: Group records for aggregation
- `windowedBy`: Create time-based windows

For complete operation details, see [Operation Reference](operation-reference.md).

## Best Practices

### 1. Choose Appropriate Stream Types
```yaml
# Good: Use KStream for events, KTable for state
streams:
  user_actions:    # Events - use KStream
    topic: clicks
    keyType: string
    valueType: json

tables:
  user_profiles:   # State - use KTable
    topic: profiles
    keyType: string
    valueType: avro:UserProfile
```

### 2. Use Meaningful Names
```yaml
# Good: Descriptive names
pipelines:
  enrich_orders_with_customer_data:
    from: orders
    # ...

# Avoid: Generic names
pipelines:
  process_data:
    from: stream1
    # ...
```

### 3. Keep Functions Focused
```yaml
# Good: Single responsibility
functions:
  is_high_value_order:
    type: predicate
    expression: value.get("total") > 1000

  calculate_discount:
    type: valueTransformer
    expression: {**value, "discount": value.get("total") * 0.1}
```

### 4. Use Appropriate Data Types
- Be specific about types (avoid `any` when possible)
- Use AVRO with schema registry for production
- Use JSON for development and debugging
- Consider schema evolution when choosing formats

## Complete Example

```yaml
streams:
  orders:
    topic: incoming_orders
    keyType: string
    valueType: json

tables:
  customers:
    topic: customer_profiles
    keyType: string
    valueType: avro:Customer

functions:
  calculate_total:
    type: valueTransformer
    code: |
      total = sum(item.get("price", 0) * item.get("quantity", 0) 
                  for item in value.get("items", []))
      return {**value, "total": total}
    resultType: struct

pipelines:
  process_orders:
    from: orders
    via:
      - type: transformValue
        mapper: calculate_total
      - type: filter
        if:
          expression: value.get("total") > 0
      - type: join
        with: customers
        valueJoiner:
          expression: {**value1, "customer": value2}
    to: processed_orders
```

## Related Topics

- [Pipeline Reference](pipeline-reference.md) - Detailed pipeline syntax and patterns
- [Operation Reference](operation-reference.md) - Complete operation documentation
- [Function Reference](function-reference.md) - Python function integration
- [Data Type Reference](data-type-reference.md) - Comprehensive type system
- [Configuration Reference](configuration-reference.md) - Runtime configuration options