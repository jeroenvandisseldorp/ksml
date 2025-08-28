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

#### Union

A union type can be one of several possible types.

**Syntax:**
```yaml
valueType: "union(<type1>, <type2>, ...)"
```

**Example:**
```yaml
streams:
  optional_message_stream:
    topic: optional-messages
    keyType: string
    valueType: "union(null, string)"
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

## Best Practices

1. **Be specific about your types**: Avoid using `any` when possible
2. **Use complex types for structured data**: Use structs, lists, etc. for complex data structures
3. **Consider schema evolution**: Use AVRO with a schema registry for production systems that need backward compatibility
4. **Choose the right notation**: Consider compatibility with upstream and downstream systems
5. **Validate data**: Check that data conforms to expected types and formats
6. **Handle missing or null values**: Always handle the case where a value might be null
7. **Document your schemas**: Add comments to explain complex type structures
8. **Use local schemas for development**: Keep schemas in version control for better change management
9. **Schema registry for production**: Use schema registry in production for centralized schema management
10. **Test type conversions**: Verify that automatic conversions work as expected in your pipelines

## Common Patterns

### Working with Optional Fields
```yaml
functions:
  handle_optional:
    type: valueTransformer
    code: |
      # Handle potentially missing fields
      name = value.get("name", "Unknown")
      age = value.get("age")
      if age is not None and age > 0:
        return {"name": name, "age": age, "valid": True}
    expression: {"name": name, "valid": False}
    resultType: struct
```

### Multi-Format Processing
```yaml
pipelines:
  process_multiple_formats:
    from: mixed_input
    via:
      - type: branch
        branches:
          - if: 
              code: isinstance(value, dict)
            via:
              - type: convertValue
                into: json
            to: json_output
          - if:
              code: isinstance(value, str)
            via:
              - type: convertValue
                into: csv:RecordSchema
            to: csv_output
```

### Schema Validation
```yaml
functions:
  validate_schema:
    type: valueTransformer
    code: |
      required_fields = ["id", "name", "timestamp"]
      if not isinstance(value, dict):
        raise ValueError("Value must be a dictionary")
      
      for field in required_fields:
        if field not in value:
          raise ValueError(f"Missing required field: {field}")
      
      return value
    resultType: struct
```