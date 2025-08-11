# Implementing Joins in KSML

This tutorial explores how to implement join operations in KSML, allowing you to combine data from multiple streams or tables to create enriched datasets.

## Introduction to Joins

Joins are powerful operations that allow you to combine data from different sources based on a common key. In stream processing, joins enable you to:

- Enrich streaming data with reference information
- Correlate events from different systems
- Build comprehensive views of entities from fragmented data
- Implement complex business logic that depends on multiple data sources

KSML supports various types of joins, each with different semantics and use cases.

## Prerequisites

Before starting this tutorial, you should:

- Understand basic KSML concepts (streams, functions, pipelines)
- Have completed the [KSML Basics Tutorial](../../getting-started/basics-tutorial.md)
- Be familiar with [Stateful Operations](../../core-concepts/operations.md#stateful-operations)
- Understand the difference between [streams and tables](../../reference/stream-type-reference.md)

## Types of Joins in KSML

KSML supports several types of joins, each with different semantics:

### Stream-Stream Joins

Join two streams based on a common key within a specified time window:

- **Inner Join (`join`)**: Outputs a result only when both streams have matching keys within the time window
- **Left Join (`leftJoin`)**: Always outputs a result for messages from the left stream, joining with the right stream if available
- **Outer Join (`outerJoin`)**: Outputs a result whenever either stream has a message, joining them when both are available

### Stream-Table Joins

Join a stream with a table (materialized view) based on the key:

- **Inner Join (`join`)**: Outputs a result only when the stream key exists in the table
- **Left Join (`leftJoin`)**: Always outputs a result for stream messages, joining with the table value if available

### Stream-GlobalTable Joins

Join a stream with a global table, with the ability to use a foreign key:

- **Inner Join (`join`)**: Outputs a result only when the stream's foreign key exists in the global table
- **Left Join (`leftJoin`)**: Always outputs a result for stream messages, joining with the global table value if available

## Stream-Stream Join

This example correlates user clicks and purchases within a 30-minute time window to track user activity patterns.

??? info "Producer: Clicks and Purchases (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/producer-clicks-purchases.yaml" %}
    ```

??? info "Processor: Stream-Stream Join (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/processor-stream-stream-join-working.yaml" %}
    ```

### How This Example Works

This example demonstrates stream-stream joins with time windows by tracking user shopping behavior:

- **Joins on**: User ID - correlates all activities by the same user
- **Time Window**: 30 minutes - captures clicks and purchases that occur within 30 minutes of each other
- **Business Value**: Understand the user's journey - what products they browsed before making ANY purchase

For example, a user might:

1. Click on shoes at 2:00 PM
2. Click on a shirt at 2:05 PM
3. Click on a jacket at 2:10 PM
4. Purchase the shirt at 2:15 PM

This join captures that browsing pattern, showing that viewing multiple products led to a purchase, even if the purchased item wasn't the first one clicked.

### Time Window Configuration

Stream-stream joins require careful window configuration:

- **timeDifference**: 30m - Maximum time gap between correlated events
- **windowSize**: 60m - Must be 2 × timeDifference to buffer events from both streams
- **retention**: 65m - Must be (2 × timeDifference) + grace period for state cleanup
- **grace**: 5m - Allows late-arriving events to still be processed

The window ensures we only correlate recent activities while managing memory efficiently.


## Stream-Table Join

Join a stream of orders with a table of customer data to enrich orders with customer information.

Since Kafka Streams joins operate on keys, we need to:

1. **Rekey orders to customer_id** - Orders arrive keyed by order_id, but we need customer_id to join
2. **Join with customers table** - Enriches each order with customer details
3. **Rekey back to order_id** - Restore the natural key for downstream processing

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


## Stream-GlobalTable Join

Join orders with product catalog using a foreign key to enrich orders with product details.

GlobalTables are replicated across all instances, making them ideal for reference data that needs to be joined frequently. The `mapper` function extracts the join key from the stream record.

??? info "Producer: Orders and Products (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/producer-orders-products.yaml" %}
    ```

??? info "Processor: Foreign Key Join (click to expand)"

    ```yaml
    {% include "../../definitions/intermediate-tutorial/joins/processor-foreign-key-join.yaml" %}
    ```

## Key Configuration Details

### Stream-Table Join Rekeying

When joining streams with tables on different keys:

- Use `transformKey` to rekey the stream to match the table's key
- Perform the join operation
- Optionally rekey back to the original key for downstream processing

### Stream-Stream Join Requirements

- **timeDifference**: Maximum time difference between matched events
- **grace**: Grace period for late-arriving data  
- **Window Stores**: Both streams need window stores with `retainDuplicates: true`
- **Window Size**: Must equal `2 * timeDifference`
- **Retention**: Must equal `2 * timeDifference + grace`

### GlobalTable Join Mapper

For GlobalTable joins, use a `keyValueMapper` function to extract the foreign key:

- **Input**: Receives the stream's key and value
- **Output**: Returns the key to lookup in the GlobalTable
- **Example**: Extract `product_id` from order to join with product catalog

### ValueJoiner Functions

ValueJoiner functions receive two parameters:

- **value1**: Value from the left stream/table
- **value2**: Value from the right stream/table
- Must include `expression` and `resultType` fields

## Best Practices

- **State Size**: Joins maintain state - monitor memory usage with window sizes
- **Join Order**: Join with smaller datasets first when possible  
- **Error Handling**: Handle null values in valueJoiner functions
- **Windowing**: Choose appropriate window sizes balancing accuracy vs. performance

## Conclusion

Joins enable powerful data enrichment by combining streams with tables or correlating events across streams. Use stream-table joins for real-time enrichment and stream-stream joins for event correlation within time windows.

## Further Reading

- [Core Concepts: Operations](../../core-concepts/operations.md)
- [Reference: Join Operations](../../reference/operation-reference.md)
