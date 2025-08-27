# Reference Documentation

Welcome to the KSML Reference Documentation! This section provides comprehensive technical details about all aspects of KSML. It serves as a complete reference for KSML syntax, operations, functions, data types, and configuration options.

Understanding these fundamental components will give you a solid foundation for building effective stream processing applications with KSML, regardless of the specific use case or complexity level.

## Reference Sections

### [KSML Definition Reference](definition-reference.md)

Complete documentation for writing KSML definitions:

- YAML structure and formatting
- Stream types (KStream, KTable, GlobalKTable)
- Definition file organization
- Syntax rules and conventions
- Data types and schemas
- Best practices

### [Pipeline Reference](pipeline-reference.md)

Comprehensive guide to pipeline structure and data flow in KSML:

- Pipeline structure and components
- Input and output configurations
- Connecting and branching pipelines
- Best practices for pipeline design
- Duration specifications and patterns

### [Data Type Reference](data-type-reference.md)

Detailed information about all supported data types in KSML:

- Primitive data types
- Complex data types
- Schema management
- Key and value types
- Function parameter types
- Function result types
- Type conversion
- Serialization and deserialization
- Custom data types

### [Notation Reference](notation-reference.md)

Find out how to use different notations for Kafka topics:

- Key and value types
- Working with different data formats (Avro, JSON, CSV, etc.)
- Schema management
- Serialization and deserialization
- Introduction to notations
- How to configure notations
- List of available supported variations

### [Function Reference](function-reference.md)

Discover how to use Python functions in your KSML applications:

- Types of functions in KSML
- Writing Python functions
- Function parameters and return types
- Reusing functions across pipelines
- Function execution context and limitations
- Function types (forEach, mapper, predicate, etc.)
- Python code integration
- Built-in functions
- Best practices for function implementation

### [Operation Reference](operation-reference.md)

Learn about the various operations you can perform on your data:

- Stateless operations (map, filter, etc.)
- Stateful operations (aggregate, count, etc.)
- Windowing operations
- Joining streams and tables
- Sink operations
- Each operation includes:
  - Syntax and parameters
  - Return values
  - Examples
  - Common use cases
  - Performance considerations

### [State Store Reference](state-store-reference.md)

Understand how to work with stateful processing in KSML:

- State store types (KeyValue, Window, Session)
- Store configuration options
- Persistence and caching
- Using stores in functions
- Store queries and management
- Performance tuning
- Best practices for stateful operations

### [Configuration Reference](configuration-reference.md)

Complete documentation of KSML configuration options:

- Runner configuration
- Kafka client configuration
- Schema Registry configuration
- Performance tuning options
- Security settings
- Logging configuration
- Environment variables

## How to Use This Reference

You can read through these reference topics in order for a comprehensive understanding, or jump to specific topics as needed:

1. Start with [KSML Definition Reference](definition-reference.md) to understand the basic structure, stream types, and data model
2. Study [Pipeline Reference](pipeline-reference.md) to learn how data flows through your application
3. Explore [Functions](function-reference.md) to see how to implement custom logic in Python
4. Learn about [Operations](operation-reference.md) to understand all the ways you can process your data
5. Move on to [Data Types](data-type-reference.md) and [Notations](notation-reference.md) to learn about data handling
6. Understand [State Stores](state-store-reference.md) for stateful processing and data persistence
7. Review advanced tutorials for production-ready applications
8. Finish with [Configuration Reference](configuration-reference.md) for deployment settings