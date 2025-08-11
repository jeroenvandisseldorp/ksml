# Using Windowed Operations in KSML

This tutorial shows how to implement time-based windowing operations in KSML for processing data within specific time boundaries.

## Introduction to Windowed Operations

Windowed operations group and process data within time intervals. They enable:
- Time-based aggregations (counts, averages per time period)
- Pattern detection over time
- Handling late-arriving data with grace periods
- State management for temporal data

## Prerequisites

- Basic KSML concepts (streams, functions, pipelines)
- Understanding of [Aggregations](aggregations.md)

## Window Types

### Tumbling Windows
Non-overlapping, fixed-size windows where each event belongs to exactly one window.

```yaml
- type: windowByTime
  windowType: tumbling
  duration: 5m
  grace: 30s  # Optional: accept late data
```

**Use cases**: Hourly statistics, clear time boundaries

### Hopping Windows  
Fixed-size windows that overlap by advancing less than the window size.

```yaml
- type: windowByTime
  windowType: hopping
  duration: 10m     # Window size
  advanceBy: 2m     # Advance every 2 minutes 
  grace: 1m
```

**Use cases**: Moving averages, smooth metrics transitions

### Session Windows
Windows that group events based on activity, closing after inactivity periods.

```yaml
- type: windowBySession
  inactivityGap: 30m  # Close after 30 minutes of inactivity
  grace: 10s
```

**Use cases**: User sessions, activity-based grouping

## Working with Windowed Operations

### Tumbling Window Count

Count user clicks in 5-minute non-overlapping windows:

{% include_relative ../definitions/intermediate-tutorial/windowing/producer-user-clicks.yaml %}

{% include_relative ../definitions/intermediate-tutorial/windowing/processor-tumbling-count-working.yaml %}

### Hopping Window Average

Calculate moving averages using overlapping 2-minute windows that advance every 30 seconds:

{% include_relative ../definitions/intermediate-tutorial/windowing/producer-sensor-data.yaml %}

{% include_relative ../definitions/intermediate-tutorial/windowing/processor-hopping-average-working.yaml %}

## Configuration Details

### Grace Periods
Handle late-arriving data by specifying grace periods:
- **grace**: Accept data up to N time units late
- **retention**: How long to keep window state (must be ≥ window size + grace)

### Window Key Types
Windowed aggregations produce keys of type `json:windowed(original_type)`. Use `convertKey` for compatibility.

## Best Practices

- **Window Size**: Balance accuracy vs. performance - larger windows use more memory
- **Grace Period**: Allow sufficient time for late data without excessive state storage
- **State Store**: Configure appropriate `retention` periods for your use case

## Conclusion

Windowed operations enable time-based analytics for streaming data. Use tumbling windows for non-overlapping periods, hopping windows for moving averages, and session windows for activity-based grouping.

## Further Reading

- [Core Concepts: Operations](../../core-concepts/operations.md)
- [Reference: Windowing Operations](../../reference/operation-reference.md)