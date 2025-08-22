# Function Reference

This document provides a comprehensive reference for all function types available in KSML. Functions in KSML allow you to implement custom logic for processing your streaming data using Python, making stream processing accessible to data scientists, analysts, and developers who may not be familiar with Java or Kafka Streams API.

## What are Functions in KSML?

Functions provide the flexibility to go beyond built-in operations and implement specific business logic, transformations, and data processing requirements. They are written in Python and executed within the KSML runtime, combining the power of Kafka Streams with the simplicity and expressiveness of Python.

## Writing Python Functions

### Example Function Definition
??? info "Example Function Definition"

      ```yaml
      functions:
        # Example of a complete function definition with all components
        process_sensor_data:
          type: valueTransformer
          globalCode: |
            # This code runs once when the application starts
            import json
            import time
            
            # Initialize global variables
            sensor_threshold = 25.0
            alert_count = 0
            
          code: |
            # This code runs for each message
            global alert_count
            
            # Process the sensor value
            if value is None:
              return None
              
            temperature = value.get("temperature", 0)
            
            # Convert Celsius to Fahrenheit
            temperature_f = (temperature * 9/5) + 32
            
            # Check for alerts
            is_alert = temperature > sensor_threshold
            if is_alert:
              alert_count += 1
              log.warn("High temperature detected: {}°C", temperature)
            
            # Return enriched data
            result = {
              "original_temp_c": temperature,
              "temp_fahrenheit": temperature_f,
              "is_alert": is_alert,
              "total_alerts": alert_count,
              "processed_at": int(time.time() * 1000)
            }
            
            return result
            
          resultType: json
          
        # Example of a simple expression-based function
        is_high_priority:
          type: predicate
          expression: value.get("priority", 0) > 7
          resultType: boolean
      ```

KSML functions are defined in the `functions` section of your KSML definition file. A typical function definition includes:

- **Type**: Specifies the function's purpose and behavior
- **Parameters**: Input parameters the function accepts (defined by the function type)
- **GlobalCode**: Python code executed only once upon application start
- **Code**: Python code implementing the function's logic
- **Expression**: Shorthand for simple return expressions
- **ResultType**: The expected return type of the function

## Function Definition Formats

KSML supports two formats for defining functions:

### Expression Format

For simple, one-line functions:

```yaml
functions:
  is_valid:
    type: predicate
    code: |
      # Code is optional here
    expression: value.get("status") == "ACTIVE"
```

### Code Block Format

For more complex functions:

```yaml
functions:
  process_transaction:
    type: keyValueMapper
    code: |
      result = {}

      # Copy basic fields
      result["transaction_id"] = value.get("id")
      result["amount"] = value.get("amount", 0)

      # Calculate fee
      amount = value.get("amount", 0)
      if amount > 1000:
        result["fee"] = amount * 0.02
      else:
        result["fee"] = amount * 0.03

      # Add timestamp
      result["processed_at"] = int(time.time() * 1000)

      return result
    resultType: struct
```


## Function Execution Context

When your Python functions execute, they have access to:

- **Logger**: For outputting information to application logs
      - `log.<log-level>("Debug message")` - <log_level> can be debug, info, warn, error, trace

- **Metrics**: For monitoring function performance and behavior
      - `metrics.counter("name").increment()` - Count occurrences
      - `metrics.gauge("name").record(value)` - Record values
      - `with metrics.timer("name"):` - Measure execution time

- **State Stores**: For maintaining state between function invocations (when configured)
      - `store.get(key)` - Retrieve value from store
      - `store.put(key, value)` - Store a value
      - `store.delete(key)` - Remove a value
      - Must be declared in the function's `stores` parameter

This execution context provides the tools needed for debugging, monitoring, and implementing stateful processing.

## Function Types Overview

KSML supports 21 function types, each designed for specific purposes in stream processing. Functions can range from simple one-liners to complex implementations with multiple operations:

| Function Type                                                           | Purpose                                              | Used In                                     |
|-------------------------------------------------------------------------|------------------------------------------------------|---------------------------------------------|
| **Functions for stateless operations**                                  |                                                      |                                             |
| [forEach](#foreach)                                                     | Process each message for side effects                | peek                                        |
| [keyTransformer](#keytransformer)                                       | Convert a key to another type or value               | mapKey, selectKey, toStream, transformKey   |
| [keyValueToKeyValueListTransformer](#keyvaluetokeyvaluelisttransformer) | Convert key and value to a list of key/values        | flatMap, transformKeyValueToKeyValueList    |
| [keyValueToValueListTransformer](#keyvaluetovaluelisttransformer)       | Convert key and value to a list of values            | flatMapValues, transformKeyValueToValueList |
| [keyValueTransformer](#keyvaluetransformer)                             | Convert key and value to another key and value       | flatMapValues, transformKeyValueToValueList |
| [predicate](#predicate)                                                 | Return true/false based on message content           | filter, branch                              |
| [valueTransformer](#valuetransformer)                                   | Convert value to another type or value               | mapValue, mapValues, transformValue         |
|                                                                         |                                                      |                                             |
| **Functions for stateful operations**                                   |                                                      |                                             |
| [aggregator](#aggregator)                                               | Incrementally build aggregated results               | aggregate                                   |
| [initializer](#initializer)                                             | Provide initial values for aggregations              | aggregate                                   |
| [merger](#merger)                                                       | Merge two aggregation results into one               | aggregate                                   |
| [reducer](#reducer)                                                     | Combine two values into one                          | reduce                                      |
|                                                                         |                                                      |                                             |
| **Special Purpose Functions**                                           |                                                      |                                             |
| [foreignKeyExtractor](#foreignkeyextractor)                             | Extract a key from a join table's record             | join, leftJoin                              |
| [generator](#generator)                                                 | Function used in producers to generate a message     | producer                                    |
| [keyValueMapper](#keyvaluemapper)                                       | Convert key and value into a single output value     | groupBy, join, leftJoin                     |
| [keyValuePrinter](#keyvalueprinter)                                     | Output key and value                                 | print                                       |
| [metadataTransformer](#metadatatransformer)                             | Convert Kafka headers and timestamps                 | transformMetadata                           |
| [valueJoiner](#valuejoiner)                                             | Combine data from multiple streams                   | join, leftJoin, outerJoin                   |
|                                                                         |                                                      |                                             |
| **Stream Related Functions**                                            |                                                      |                                             |
| [timestampExtractor](#timestampextractor)                               | Extract timestamps from messages                     | stream, table, globalTable                  |
| [topicNameExtractor](#topicnameextractor)                               | Derive a target topic name from key and value        | toTopicNameExtractor                        |
| [streamPartitioner](#streampartitioner)                                 | Determine to which partition(s) a record is produced | stream, table, globalTable                  |
|                                                                         |                                                      |                                             |
| **Other Functions**                                                     |                                                      |                                             |
| [generic](#generic)                                                     | Generic custom function                              |                                             |

## Functions for stateless operations

### forEach

Processes each message for side effects like logging, without changing the message.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

None (the function is called for its side effects)

#### Example

```yaml
{% include "../definitions/reference/functions/foreach-example.yaml" %}
```

**See how `forEach` is used in an example definition**: 

- [Tutorial: Filtering and Transforming Example](../tutorials/beginner/filtering-transforming.md#complex-filtering-techniques)

### keyTransformer

Transforms a key/value into a new key, which then gets combined with the original value as a new message on the output
stream.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

New key for the output message

#### Example

??? info "Producer - Regional Transaction Data (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/keytransformer-producer.yaml"
    %}
    ```

??? info "Processor - Repartition by Region (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/keytransformer-processor.yaml"
    %}
    ```

**See how `keyTransformer` is used in an example definition**:

- [Tutorial: Stream Table Join Example](../tutorials/intermediate/joins.md#stream-table-join)

### keyValueToKeyValueListTransformer

Takes one message and converts it into a list of output messages, which then get sent to the output stream.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

A list of key-value pairs `[(key1, value1), (key2, value2), ...]`

#### Example

```yaml
{% include "../definitions/reference/functions/keyvaluetokeyvaluelisttransformer.yaml" %}
```

**See it in action**: 

- [Reference: Function Examples](../definitions/reference/functions/) - comprehensive function examples
- [Tutorial: Advanced Processing](../tutorials/beginner/filtering-transforming.md#advanced-transformation) - function usage patterns

### keyValueToValueListTransformer

Takes one message and converts it into a list of output values, which then get combined with the original key and sent
to the output stream.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

A list of values `[value1, value2, ...]` that will be combined with the original key

#### Example

??? info "Producer - Order Data with Items (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/keyvaluetovaluelisttransformer-producer.yaml"
    %}
    ```

??? info "Processor - Explode Order Items (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/keyvaluetovaluelisttransformer-processor.yaml"
    %}
    ```

### keyValueTransformer

Takes one message and converts it into another message, which may have different key/value types.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

A tuple of (new_key, new_value)

#### Example

```yaml
functions:
  transform_order:
    type: keyValueTransformer
    code: |
      if value is None:
        return (None, None)

      # Create a new key based on customer ID
      new_key = value.get("customer_id", "unknown")

      # Create a new value with selected fields
      new_value = {
        "order_id": value.get("order_id"),
        "total_amount": value.get("total_amount", 0),
        "item_count": len(value.get("items", [])),
        "processed_at": int(time.time() * 1000)
      }

      return (new_key, new_value)
    resultType: "(string,struct)"
```

### predicate

Returns true or false based on message content. Used for filtering and branching operations.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

Boolean (true or false)

#### Example

```yaml
functions:
  is_adult:
    type: predicate
    expression: value.get("age") >= 18

  is_valid_transaction:
    type: predicate
    code: |
      if value is None:
        return False

      amount = value.get("amount")
      if amount is None or amount <= 0:
        return False

      return True

  deduplicate_events:
    type: predicate
    code: |
      # Access a state store to check for duplicates
      event_id = value.get("event_id")
      if event_id is None:
        return True

      # Check if we've seen this event before
      seen_before = event_store.get(event_id)
      if seen_before:
        # Skip duplicate event
        return False

      # Mark this event as seen
      stateStore.put(event_id, True)

      # Process the event
      return True
    stores:
      - event_store
```

**See it in action**: 

- [Tutorial: Filtering and Transforming](../tutorials/beginner/filtering-transforming.md#complex-filtering-techniques) - predicate functions for data filtering
- [Tutorial: Branching](../tutorials/intermediate/branching.md#conditional-routing) - predicates for stream routing

### valueTransformer

Transforms a key/value into a new value, which is combined with the original key and sent to the output stream.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

New value for the output message

#### Example

```yaml
functions:
  enrich_user:
    type: valueTransformer
    code: |
      return {
        "id": value.get("user_id"),
        "full_name": value.get("first_name") + " " + value.get("last_name"),
        "email": value.get("email"),
        "age": value.get("age"),
        "is_adult": value.get("age", 0) >= 18,
        "processed_at": int(time.time() * 1000)
      }
    resultType: struct

```

**See it in action**: 

- [Tutorial: Filtering and Transforming](../tutorials/beginner/filtering-transforming.md#advanced-transformation-techniques) - valueTransformer for data enrichment
- [Tutorial: Data Formats](../tutorials/beginner/data-formats.md#format-conversion) - value transformation between formats

## Functions for stateful operations

### aggregator

Incrementally builds aggregated results from multiple messages.

#### Parameters

| Parameter       | Type | Description                                |
|-----------------|------|--------------------------------------------|
| key             | Any  | The key of the record being processed      |
| value           | Any  | The value of the record being processed    |
| aggregatedValue | Any  | The current aggregated value (can be None) |

#### Return Value

New aggregated value

#### Example

```yaml
functions:
  average_calculator:
    type: aggregator
    code: |
      if aggregatedValue is None:
        return {"count": 1, "sum": value.get("amount", 0), "average": value.get("amount", 0)}
      else:
        count = aggregatedValue.get("count", 0) + 1
        sum = aggregatedValue.get("sum", 0) + value.get("amount", 0)
        return {
          "count": count,
          "sum": sum,
          "average": sum / count
        }
    resultType: struct
```

**See it in action**: 

- [Tutorial: Aggregations](../tutorials/intermediate/aggregations.md#aggregate-example) - comprehensive aggregator function examples
- [Tutorial: Windowing](../tutorials/intermediate/windowing.md#windowed-aggregations) - aggregators in time windows

### initializer

Provides initial values for aggregations.

#### Parameters

None

#### Return Value

Initial value for aggregation

#### Example

```yaml
functions:
  counter_initializer:
    type: initializer
    expression: { "count": 0, "sum": 0, "average": 0 }
    resultType: struct
```

### merger

Merges two aggregation results into one. Used in aggregation operations to combine partial results.

#### Parameters

| Parameter | Type | Description                           |
|-----------|------|---------------------------------------|
| key       | Any  | The key of the record being processed |
| value1    | Any  | The value of the first aggregation    |
| value2    | Any  | The value of the second aggregation   |

#### Return Value

The merged aggregation result

#### Example

```yaml
functions:
  merge_stats:
    type: merger
    code: |
      # Merge two statistics objects
      if value1 is None:
        return value2
      if value2 is None:
        return value1

      # Combine counts and sums
      count = value1.get("count", 0) + value2.get("count", 0)
      sum = value1.get("sum", 0) + value2.get("sum", 0)
      result = {
        "count": count,
        "sum": sum,
        "average": sum/count if count>0 else 0
      }

      return result
    resultType: struct
```

### reducer

Combines two values into one.

#### Parameters

| Parameter | Type | Description                 |
|-----------|------|-----------------------------|
| value1    | Any  | The first value to combine  |
| value2    | Any  | The second value to combine |

#### Return Value

Combined value

#### Example

```yaml
functions:
  sum_reducer:
    type: reducer
    code: |
      count = value1.get("count", 0) + value2.get("count", 0)
      sum = value1.get("sum", 0) + value2.get("sum", 0)
      return {
        "count": count,
        "sum": sum,
        "average": sum/count if count>0 else 0
      }
    resultType: struct
```

## Special Purpose Functions

### foreignKeyExtractor

Extracts a key from a join table's record. Used during join operations to determine which records to join.

#### Parameters

| Parameter | Type | Description                               |
|-----------|------|-------------------------------------------|
| value     | Any  | The value of the record to get a key from |

#### Return Value

The key to look up in the table being joined with

#### Example

```yaml
functions:
  extract_customer_id:
    type: foreignKeyExtractor
    code: |
      # Extract customer ID from an order to join with customer table
      if value is None:
        return None

      return value.get("customer_id")
    resultType: string
```

### generator

Function used in producers to generate messages. It takes no input parameters and produces key-value pairs.

#### Parameters

None

#### Return Value

A tuple of (key, value) representing the generated message

#### Example

```yaml
functions:
  generate_sensordata_message:
    type: generator
    globalCode: |
      import time
      import random
      sensorCounter = 0
    code: |
      global sensorCounter

      key = "sensor"+str(sensorCounter)           # Set the key to return ("sensor0" to "sensor9")
      sensorCounter = (sensorCounter+1) % 10      # Increase the counter for next iteration

      # Generate some random sensor measurement data
      types = { 0: { "type": "AREA", "unit": random.choice([ "m2", "ft2" ]), "value": str(random.randrange(1000)) },
                1: { "type": "HUMIDITY", "unit": random.choice([ "g/m3", "%" ]), "value": str(random.randrange(100)) },
                2: { "type": "LENGTH", "unit": random.choice([ "m", "ft" ]), "value": str(random.randrange(1000)) },
                3: { "type": "STATE", "unit": "state", "value": random.choice([ "off", "on" ]) },
                4: { "type": "TEMPERATURE", "unit": random.choice([ "C", "F" ]), "value": str(random.randrange(-100, 100)) }
              }

      # Build the result value using any of the above measurement types
      value = { "name": key, "timestamp": str(round(time.time()*1000)), **random.choice(types) }
      value["color"] = random.choice([ "black", "blue", "red", "yellow", "white" ])
      value["owner"] = random.choice([ "Alice", "Bob", "Charlie", "Dave", "Evan" ])
      value["city"] = random.choice([ "Amsterdam", "Xanten", "Utrecht", "Alkmaar", "Leiden" ])

      if random.randrange(10) == 0:
        key = None
      if random.randrange(10) == 0:
        value = None
    expression: (key, value)                      # Return a message tuple with the key and value
    resultType: (string, struct)                  # Indicate the type of key and value
```

**See it in action**: 

- [Tutorial: Filtering and Transforming](../tutorials/beginner/filtering-transforming.md#creating-test-data) - generator functions for test data
- [Tutorial: Performance Testing](../tutorials/advanced/performance-optimization.md#data-generation) - generators for load testing

### keyValueMapper

Transforms both the key and value of a record.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

Tuple of (new_key, new_value)

#### Example

```yaml
functions:
  repartition_by_user_id:
    type: keyValueMapper
    code: |
      new_key = value.get("user_id")
      new_value = value
      return (new_key, new_value)
    resultType: "(string, struct)"
```

### keyValuePrinter

Converts a message to a string for output to a file or stdout.

#### Parameters

| Parameter | Type | Description                             |
|-----------|------|-----------------------------------------|
| key       | Any  | The key of the record being processed   |
| value     | Any  | The value of the record being processed |

#### Return Value

String to be written to file or stdout

#### Example

??? info "Producer - Financial Transaction Data (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/keyvalueprinter-producer.yaml"
    %}
    ```

??? info "Processor - Format Transaction Reports (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/keyvalueprinter-processor.yaml"
    %}
    ```

**See it in action**: 

- [Reference: Transaction Reports](../definitions/reference/functions/keyvalueprinter-processor.yaml) - formatting financial data for output
- [Tutorial: Logging and Monitoring](../tutorials/beginner/logging-monitoring.md#output-formatting) - keyValuePrinter for readable logs

### metadataTransformer

Transforms a message's metadata (headers and timestamp).

#### Parameters

| Parameter | Type   | Description                                       |
|-----------|--------|---------------------------------------------------|
| key       | Any    | The key of the record being processed             |
| value     | Any    | The value of the record being processed           |
| metadata  | Object | Contains the headers and timestamp of the message |

#### Return Value

Modified metadata for the output message

#### Example

??? info "Producer - API Events (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/metadatatransformer-producer.yaml"
    %}
    ```

??? info "Processor - Enrich Headers and Metadata (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/metadatatransformer-processor.yaml"
    %}
    ```

**See it in action**: 

- [Reference: API Event Enrichment](../definitions/reference/functions/metadatatransformer-processor.yaml) - adding processing headers and metadata
- [Example: Metadata Transformation](../../examples/16-example-transform-metadata.yaml) - comprehensive metadata modification patterns

### valueJoiner

Combines values from two streams or tables during join operations, creating enriched records that contain data from both sources. The function must handle cases where one or both values might be null, depending on the join type (inner, left, outer). This function has access to the join key for context-aware value combination.

#### Parameters

| Parameter | Type | Description                      |
|-----------|------|----------------------------------|
| key       | Any  | The join key used to match records |
| value1    | Any  | The value from the first stream/table |
| value2    | Any  | The value from the second stream/table |

#### Return Value

Combined value

#### Example

```yaml
functions:
  join_order_with_customer:
    type: valueJoiner
    code: |
      order = value1
      customer = value2

      if customer is None:
        customer_name = "Unknown"
        customer_email = "Unknown"
      else:
        customer_name = customer.get("name", "Unknown")
        customer_email = customer.get("email", "Unknown")

      return {
        "order_id": order.get("order_id"),
        "customer_id": order.get("customer_id"),
        "customer_name": customer_name,
        "customer_email": customer_email,
        "items": order.get("items", []),
        "total": order.get("total", 0),
        "status": order.get("status", "PENDING")
      }
    resultType: struct
```

**See it in action**: 

- [Tutorial: Joins](../tutorials/intermediate/joins.md#core-join-concepts) - valueJoiner functions for stream enrichment
- [Tutorial: Stream-Table Joins](../tutorials/intermediate/joins.md#stream-table-join-example) - practical valueJoiner examples

## Stream Related Functions

### timestampExtractor

Extracts timestamps from messages for time-based operations.

#### Parameters

| Parameter         | Type   | Description                                      |
|-------------------|--------|--------------------------------------------------|
| record            | Object | The ConsumerRecord containing key, value, timestamp, and metadata |
| previousTimestamp | Long   | The previous timestamp (can be used as fallback) |

#### Return Value

Timestamp in milliseconds (long)

#### Example

??? info "Producer - Events with Custom Timestamps (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/timestampextractor-producer.yaml"
    %}
    ```

??? info "Processor - Extract Event Time (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/timestampextractor-processor.yaml"
    %}
    ```

**See it in action**: 

- [Reference: Event Time Processing](../definitions/reference/functions/timestampextractor-processor.yaml) - extracting custom timestamps for time-based operations
- [Example: Timestamp Extraction](../../examples/18-example-timestamp-extractor.yaml) - comprehensive timestamp extraction patterns
- [Tutorial: Windowing](../tutorials/intermediate/windowing.md#time-based-processing) - timestampExtractor in windowed operations

### topicNameExtractor

Dynamically determines the target topic for message routing based on record content. This enables intelligent message distribution, multi-tenancy support, and content-based routing patterns without requiring separate processing pipelines. The function has access to record context for advanced routing decisions.

#### Parameters

| Parameter     | Type   | Description                             |
|---------------|--------|-----------------------------------------|
| key           | Any    | The key of the record being processed   |
| value         | Any    | The value of the record being processed |
| recordContext | Object | Record metadata and processing context  |

#### Return Value

String representing the topic name to send the message to

#### Example

??? info "Producer - Mixed Sensor Data (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/topicnameextractor-producer.yaml"
    %}
    ```

??? info "Processor - Dynamic Topic Routing (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/functions/topicnameextractor-processor.yaml"
    %}
    ```

**See it in action**: 

- [Reference: Sensor Data Routing](../definitions/reference/functions/topicnameextractor-processor.yaml) - dynamic topic routing by sensor type and priority
- [Example: Dynamic Routing](../../examples/05-example-route.yaml) - comprehensive topic routing patterns

### streamPartitioner

Determines to which partition a record is produced. Used to control the partitioning of output topics.

#### Parameters

| Parameter     | Type    | Description                                  |
|---------------|---------|----------------------------------------------|
| topic         | String  | The topic of the message                     |
| key           | Any     | The key of the record being processed        |
| value         | Any     | The value of the record being processed      |
| numPartitions | Integer | The number of partitions in the output topic |

#### Return Value

Integer representing the partition number to which the message will be sent

#### Example

```yaml
functions:
  custom_partitioner:
    type: streamPartitioner
    code: |
      # Partition by the first character of the key (if it's a string)
      if key is None:
        # Use default partitioning for null keys
        return None

      if isinstance(key, str) and len(key) > 0:
        # Use the first character's ASCII value modulo number of partitions
        return ord(key[0]) % numPartitions

      # For non-string keys, use a hash of the string representation
      return hash(str(key)) % numPartitions
```

## Other Functions

### generic

Generic custom function that can be used for any purpose. It can accept custom parameters and return any type of value.

#### Parameters

User-defined parameters

#### Return Value

Any value, depending on the function's purpose

#### Example

```yaml
functions:
  calculate_discount:
    type: generic
    parameters:
      - name: basePrice
        type: double
      - name: discountPercentage
        type: double
    code: |
      # Calculate the discounted price
      discountAmount = basePrice * (discountPercentage / 100)
      finalPrice = basePrice - discountAmount

      # Return both the final price and the discount amount
      return {
        "finalPrice": finalPrice,
        "discountAmount": discountAmount,
        "discountPercentage": discountPercentage
      }
    resultType: struct
```



## Best Practices

1. **Keep functions focused**: Each function should do one thing well
2. **Handle errors gracefully**: Use try/except blocks to prevent pipeline failures
3. **Consider performance**: Python functions introduce some overhead, so keep them efficient
4. **Use appropriate function types**: Choose the right function type for your use case
5. **Leverage state stores**: For complex stateful operations, use state stores rather than global variables
6. **Document your functions**: Add comments to explain complex logic and business rules
7. **Test thoroughly**: Write unit tests for your functions to ensure they behave as expected

## How KSML Functions Relate to Kafka Streams

KSML functions are Python implementations that map directly to Kafka Streams Java interfaces. Understanding this relationship helps you leverage Kafka Streams documentation and concepts:

### Direct Mappings

| KSML Function Type | Kafka Streams Interface | Purpose |
|-------------------|-------------------------|---------|
| predicate | `Predicate<K,V>` | Filter records based on conditions |
| valueTransformer | `ValueTransformer<V,VR>` / `ValueMapper<V,VR>` | Transform values |
| keyTransformer | `KeyValueMapper<K,V,KR>` | Transform keys |
| keyValueTransformer | `KeyValueMapper<K,V,KeyValue<KR,VR>>` | Transform both key and value |
| forEach | `ForeachAction<K,V>` | Process records for side effects |
| aggregator | `Aggregator<K,V,VA>` | Aggregate records incrementally |
| initializer | `Initializer<VA>` | Provide initial aggregation values |
| reducer | `Reducer<V>` | Combine values of same type |
| merger | `Merger<K,V>` | Merge aggregation results |
| valueJoiner | `ValueJoiner<V1,V2,VR>` | Join values from two streams |
| timestampExtractor | `TimestampExtractor` | Extract event time from records |
| streamPartitioner | `StreamPartitioner<K,V>` | Custom partitioning logic |
| foreignKeyExtractor | `Function<V,FK>` | Extract foreign key for joins |
| topicNameExtractor | `TopicNameExtractor<K,V>` | Dynamic topic routing |