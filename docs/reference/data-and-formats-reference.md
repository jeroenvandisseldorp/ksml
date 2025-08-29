# Data Types and Notations Reference

KSML supports a wide range of data types and formats for both keys and values in your streams. This comprehensive reference covers all data types, notation formats, and type conversion capabilities available in KSML.

## Data Types in KSML

### Primitive Types

KSML supports the following primitive types:

| Type      | Description                     | Example               | Java Equivalent |
|-----------|---------------------------------|-----------------------|-----------------|
| `boolean` | True or false values            | `true`, `false`       | `Boolean`       |
| `byte`    | 8-bit integer                   | `42`                  | `Byte`          |
| `short`   | 16-bit integer                  | `1000`                | `Short`         |
| `int`     | 32-bit integer                  | `1000000`             | `Integer`       |
| `long`    | 64-bit integer                  | `9223372036854775807` | `Long`          |
| `float`   | Single-precision floating point | `3.14`                | `Float`         |
| `double`  | Double-precision floating point | `3.141592653589793`   | `Double`        |
| `string`  | Text string                     | `"Hello, World!"`     | `String`        |
| `bytes`   | Array of bytes                  | Binary data           | `byte[]`        |
| `null`    | Null value                      | `null`                | `null`          |

### Complex Types

KSML also supports several complex types that can contain multiple values:

#### Enum

An enumeration defines a set of allowed values.

**Syntax:**
```yaml
valueType: "enum(<value1>, <value2>, ...)"
```

**Example:**
```yaml
streams:
  order_status_stream:
    topic: order-statuses
    keyType: string
    valueType: "enum(PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)"
```

In Python code, an enum value is always represented as a string:
```yaml
functions:
  update_status:
    type: valueTransformer
    code: |
      if value.get("shipped"):
        return "SHIPPED"
      elif value.get("processing"):
        return "PROCESSING"
    expression: "PENDING"
    resultType: string
```

??? info "Producer - Enum example (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/data-types/enum-producer.yaml"
    %}
    ```

??? info "Processor - Enum example (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/data-types/enum-processor.yaml"
    %}
    ```

#### List

A list contains multiple elements of the same type.

**Syntax:**
```yaml
valueType: "[<element_type>]"
```

**Example:**
```yaml
streams:
  tags_stream:
    topic: tags
    keyType: string
    valueType: "[string]"
```

In Python code, a list is represented as a Python list:
```yaml
functions:
  extract_tags:
    type: keyValueToValueListTransformer
    expression: value.get("tags", [])
    resultType: "[string]"
```

**See it in action**:

- [List example](../reference/function-reference.md#keyvaluetovaluelisttransformer) - predicate functions for data filtering



#### Struct

A struct is a key-value map where all keys are strings. This is the most common complex type and is used for JSON objects, AVRO records, etc.

**Syntax:**
```yaml
valueType: struct
```

**Example:**
```yaml
streams:
  user_profiles:
    topic: user-profiles
    keyType: string
    valueType: struct
```

In Python code, a struct is represented as a dictionary:
```yaml
functions:
  create_user:
    type: valueTransformer
    expression: |
      return {
        "id": value.get("user_id"),
        "name": value.get("first_name") + " " + value.get("last_name"),
        "email": value.get("email"),
        "age": value.get("age")
      }
```

??? info "Producer - Struct example (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/data-types/struct-producer.yaml"
    %}
    ```

??? info "Processor - Struct example (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/data-types/struct-processor.yaml"
    %}
    ```

#### Tuple

A tuple combines multiple elements of different types into a single value.

**Syntax:**
```yaml
valueType: "(<type1>, <type2>, ...)"
```

**Example:**
```yaml
streams:
  sensor_stream:
    topic: sensor-data
    keyType: string
    valueType: "(string, avro:SensorData)"
```

In Python code, a tuple is represented as a Python tuple:
```yaml
functions:
  create_user_age_pair:
    type: keyValueTransformer
    expression: (value.get("name"), value.get("age"))
    resultType: "(string, int)"
```

**See it in action**:

- [Tuple example](../reference/function-reference.md#foreignkeyextractor)

#### Union

A union type can be one of several possible types.

**Syntax:**
```yaml
valueType: "union(<type1>, <type2>, ...)"
```

**Example:**

Union types are used in two main places in KSML:

**1. In stream definitions** - to specify that a stream can contain multiple types:
```yaml
streams:
  optional_messages:
    topic: optional-messages
    keyType: string
    valueType: "union(null, json)"  # This stream accepts either null OR a JSON object
```

**2. In function return types** - to specify that a function can return multiple types:
```yaml
functions:
  generate_optional:
    type: generator
    code: |
      # Can return either null or a message
      if random.random() > 0.5:
        return ("key1", {"data": "value"})
      else:
        return ("key1", None)
    resultType: "(string, union(null, json))"  # Returns a tuple with union type
```

**What union types mean:**

- `union(null, json)` means the value can be either `null` OR a JSON object
- When processing union types, your code must check which type was received and handle each case

**Complete example showing both usages:**

??? info "Producer - Union example (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/data-types/union-producer.yaml"
    %}
    ```

??? info "Processor - Union example (click to expand)"

    ```yaml
    {%
      include "../definitions/reference/data-types/union-processor.yaml"
    %}
    ```

#### Windowed

Some operations group messages on an input stream together in user-defined windows.

**Syntax:**
```yaml
keyType: "windowed(<base_type>)"
```

**Example:**
```yaml
streams:
  windowed_counts:
    topic: windowed-counts
    keyType: "windowed(string)"
    valueType: long
```

### The Any Type

The special type `?` or `any` can be used when the exact type is unknown or variable.

**Syntax:**
```yaml
resultType: "?"
# or
resultType: "any"
```

**Note:** The `any` type cannot be used on Kafka topics, as KSML would not be able to tie the type to the right serializer.

## Notation Formats

KSML uses _notations_ to allow reading/writing different message formats to Kafka topics. Notations are specified as a prefix to the schema name.

### Format Selection Guide

The choice of notation depends on your specific requirements:

| If you need...                              | Consider using... |
|---------------------------------------------|-------------------|
| Schema evolution and backward compatibility | AVRO or Protobuf  |
| Human-readable data for debugging           | JSON              |
| Integration with legacy systems             | XML or SOAP       |
| Simple tabular data                         | CSV               |
| Compact binary format                       | AVRO or Protobuf  |
| Raw binary data handling                    | Binary            |

### AVRO

AVRO is a binary format that supports schema evolution.

**Syntax:**
```yaml
valueType: "avro:<schema_name>"
# or for schema registry lookup
valueType: "avro"
```

**Example:**
```yaml
streams:
  sensor_readings:
    topic: sensor-data
    keyType: string
    valueType: avro:SensorData
```

### JSON

JSON is a text-based, human-readable format for data transfer.

**Syntax:**
```yaml
# For schemaless JSON:
valueType: "json"
# For JSON with a schema:
valueType: "json:<schema_name>"
```

**Example:**
```yaml
streams:
  user_profiles:
    topic: user-profiles
    keyType: string
    valueType: "json"

  orders:
    topic: orders
    keyType: string
    valueType: "json:Order"
```

Python functions can return JSON by returning a dictionary:
```yaml
functions:
  merge_key_value_data:
    type: valueTransformer
    expression: { 'key': key, 'value': value }
    resultType: json
```

### JSON Schema

JSON Schema adds vendor-specific schema support to JSON serialization.

**Syntax:**
```yaml
# For schema registry lookup:
valueType: "jsonschema"
# For JSON with a schema:
valueType: "jsonschema:<schema_name>"
```

**Example:**
```yaml
streams:
  user_profiles:
    topic: user-profiles
    keyType: string
    valueType: "jsonschema:UserProfile"
```

### CSV

CSV (Comma-Separated Values) is a simple tabular data format.

**Syntax:**
```yaml
# For schemaless CSV:
valueType: "csv"
# For CSV with a schema:
valueType: "csv:<schema_name>"
```

**Example:**
```yaml
streams:
  sales_data:
    topic: sales-data
    keyType: string
    valueType: "csv"

  inventory_data:
    topic: inventory-data
    keyType: string
    valueType: "csv:InventoryRecord"
```

### XML

XML (Extensible Markup Language) is used for complex hierarchical data.

**Syntax:**
```yaml
# For schemaless XML:
valueType: "xml"
# For XML with a schema:
valueType: "xml:<schema_name>"
```

**Example:**
```yaml
streams:
  customer_data:
    topic: customer-data
    keyType: string
    valueType: "xml:CustomerData"
```

### Protobuf

Protobuf is a popular encoding format developed by Google.

**Syntax:**
```yaml
# For schema registry lookup:
valueType: "protobuf"
# For Protobuf with a schema:
valueType: "protobuf:<schema_name>"
```

**Example:**
```yaml
streams:
  user_profiles:
    topic: user-profiles
    keyType: string
    valueType: "protobuf:UserProfile"
```

### Binary

Binary data represents raw bytes for custom protocols.

**Syntax:**
```yaml
valueType: "binary"
```

**Example:**
```yaml
streams:
  binary_data:
    topic: binary-messages
    keyType: string
    valueType: "binary"
```

### SOAP

SOAP (Simple Object Access Protocol) is an XML-based messaging protocol.

**Syntax:**
```yaml
valueType: "soap"
```

**Example:**
```yaml
streams:
  service_requests:
    topic: service-requests
    keyType: string
    valueType: "soap"
```

## Schema Management

When working with structured data, it's important to manage your schemas effectively.

### Local Files vs. Schema Registry

**Local Schema Files:**
When a schema is specified, KSML loads the schema from a local file from the `schemaDirectory`. The notation determines the filename extension:

- AVRO schemas: `.avsc` extension
- XML schemas: `.xsd` extension  
- CSV schemas: `.csv` extension
- JSON schemas: `.json` extension

```yaml
streams:
  sensor_data:
    topic: sensor-reading
    keyType: string
    valueType: "avro:SensorReading"  # Looks for SensorReading.avsc
```

**Schema Registry Lookup:**
When no schema is specified, KSML assumes the schema is loadable from Schema Registry:

```yaml
streams:
  sensor_data:
    topic: sensor-reading
    keyType: string
    valueType: "avro"  # Schema fetched from registry
```

## Type Conversion

KSML automatically performs type conversion wherever required and possible. This includes:

- **Number conversions**: integer to long, float to double, etc.
- **String conversions**: between strings and CSV, JSON, XML formats
- **Complex type conversions**: between different notation formats
- **Schema-based conversions**: automatic conversion using defined schemas

**Example of automatic conversion:**
```yaml
functions:
  generate_message:
    type: generator
    globalCode: |
      import random
      sensorCounter = 0
    code: |
      global sensorCounter
      key = "sensor"+str(sensorCounter)
      sensorCounter = (sensorCounter+1) % 10
      
      # Generate temperature data as CSV string
      value = "TEMPERATURE,C,"+str(random.randrange(-100, 100))
    expression: (key, value)
    resultType: (string, csv:Temperature)  # String automatically converted to CSV struct
```

### Implicit Format Conversion

KSML automatically converts between formats when stream input/output types differ:

```yaml
pipelines:
  implicit_conversion:
    from: avro_input  # Stream with Avro format
    to: json_output   # Stream with JSON format
    # Conversion happens automatically
```

### Explicit Format Conversion

Use the `convertValue` operation to explicitly transform data between formats:

```yaml
pipelines:
  explicit_conversion:
    from: avro_stream
    via:
      - type: convertValue
        into: json
      - type: convertValue  
        into: string
      - type: convertValue
        into: xml:OutputSchema
    to: xml_output
```

## Type Definition Quoting Rules

When defining types in KSML (for `keyType`, `valueType`, `resultType`, or any other type field), quotes around type expressions follow specific rules based on YAML parsing requirements:

### Quotes MUST be used for:

1. **Special type functions (these will fail without quotes):**
   ```yaml
   # enum types - MUST have quotes
   valueType: "enum(PENDING, PROCESSING, SHIPPED)"
   
   # windowed types without notation prefix - MUST have quotes  
   keyType: "windowed(string)"
   
   # union types - MUST have quotes
   valueType: "union(null, string)"
   
   # List of tuples - MUST have quotes
   resultType: "[(string, json)]"
   ```

2. **Complex type expressions with special characters:**
   ```yaml
   # Types with spaces or special chars - MUST have quotes
   resultType: "(string, avro:SensorData)"
   valueType: "list<string>"
   ```

### Quotes are OPTIONAL for:

1. **Simple basic types (work with or without quotes):**
   ```yaml
   valueType: string       # Works
   valueType: "string"     # Also works
   keyType: json          # Works
   keyType: "json"        # Also works
   valueType: struct      # Works
   resultType: bytes      # Works
   ```

2. **Simple tuples (both forms are valid):**
   ```yaml
   resultType: (string, json)      # Works without quotes
   resultType: "(string, json)"    # Also works with quotes
   ```

3. **Notation:schema combinations (notation prefix makes quotes unnecessary):**
   ```yaml
   valueType: avro:SensorData         # Works without quotes
   keyType: json:windowed(string)     # Notation prefix handles complexity
   valueType: jsonschema:UserProfile  # Works without quotes
   valueType: csv:Temperature         # Works without quotes
   ```

### Why These Rules Exist:

- **YAML parsing**: Without quotes, YAML interprets `enum(A,B,C)` as a function call, causing parsing errors. The quotes tell YAML to treat it as a string literal.
- **KSML parsing**: The UserTypeParser in KSML specifically looks for patterns like `enum(`, `windowed(`, `union(` at the start of strings to determine the type.
- **Notation prefix**: When using `json:windowed(string)`, the colon acts as a delimiter that YAML handles correctly without quotes.

### Examples from Real KSML Code:

```yaml
# Stream definitions
streams:
  sensor_data:
    topic: sensor-readings
    keyType: string                          # Simple type - no quotes needed
    valueType: avro:SensorData               # Notation:schema - no quotes needed
    
  windowed_data:
    topic: windowed-counts
    keyType: "windowed(string)"              # Complex type - quotes required
    valueType: long                          # Simple type - no quotes needed

# Function definitions
functions:
  my_generator:
    type: generator
    resultType: (string, json)               # Simple tuple - quotes optional
    
  my_transformer:
    type: valueTransformer  
    resultType: "enum(SUCCESS, FAILURE)"     # Enum type - quotes required
```

### Quick Reference:

| Type Pattern | Quotes Required? | Example |
|-------------|-----------------|----------|
| `enum(...)` | **Yes** | `"enum(ACTIVE, INACTIVE)"` |
| `windowed(...)` | **Yes** (without notation) | `"windowed(string)"` |
| `union(...)` | **Yes** | `"union(null, string)"` |
| `[(...)]` | **Yes** | `"[(string, json)]"` |
| `(type1, type2)` | **Optional** | `(string, json)` or `"(string, json)"` |
| `notation:type` | **Optional** | `avro:SensorData` |
| `notation:windowed(...)` | **Optional** | `json:windowed(string)` |
| Simple types | **Optional** | `string`, `json`, `struct` |
