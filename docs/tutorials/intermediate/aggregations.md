# Working with Aggregations in KSML

Learn how to compute statistics, summaries, and time-based analytics from streaming data using KSML's aggregation operations.

## Prerequisites

- Make sure there is a running Docker Compose KSML environment as described in the [Basics Tutorial](../../getting-started/basics-tutorial.md#choose-your-setup-method).

## Aggregation Types

- **Count**: Count messages per key
- **Reduce**: Combine values using a reducer function  
- **Aggregate**: Build complex statistics with initializer + aggregator functions

## Count Example

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

Counts messages with the same key using `groupByKey` then `count`.

## Reduce Example

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

Combines transaction amounts using a simple `reducer` function.

## Aggregate Example

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

Builds complex statistics using `initializer` to create empty state and `aggregator` to update it.

## Windowed Aggregations

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

Groups data into time windows using `windowByTime` before aggregating. Window types:

- **Tumbling**: Non-overlapping, fixed-size windows
- **Hopping**: Overlapping, fixed-size windows  
- **Session**: Dynamic windows based on activity gaps

## Complex Example: Sales Analytics

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

This pipeline:

1. Rekeys sales events by region using `keyValueMapper`
2. Creates daily windows with `windowByTime`
3. Aggregates sales statistics per region
4. Tracks per-product breakdowns within each region

## Best Practices

- **Memory**: Monitor state store size - each unique key consumes memory
- **Windows**: Choose appropriate window sizes to balance accuracy vs. resource usage
- **Error Handling**: Always handle exceptions in aggregator functions to avoid data loss
- **Pre-filtering**: Filter data before aggregation to reduce state size