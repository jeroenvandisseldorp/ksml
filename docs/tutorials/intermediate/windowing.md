# Working with Windowed Operations in KSML

This tutorial explores how to implement time-based windowing operations in KSML for processing streaming data within specific time boundaries. Windowing is fundamental to stream processing analytics.

## Introduction to Windowed Operations

Windowing divides continuous data streams into finite chunks based on time, enabling:

- **Time-based aggregations**: Calculate metrics within time periods (hourly counts, daily averages)
- **Pattern detection**: Identify trends and anomalies over time windows
- **Late data handling**: Process out-of-order events with configurable grace periods
- **State management**: Maintain temporal state for complex analytics
- **Resource control**: Limit memory usage by automatically expiring old windows

**Kafka Streams Foundation**: KSML windowing operations are built on Kafka Streams' windowing capabilities, providing exactly-once processing guarantees and fault-tolerant state management.

## Prerequisites

Before starting this tutorial:

- Understand basic KSML concepts (streams, functions, pipelines)
- Familiarity with [Aggregations](aggregations.md) is helpful
- Have [Docker Compose KSML environment setup running](../../getting-started/basics-tutorial.md#choose-your-setup-method)

## Window Types in KSML

KSML supports three types of time windows, each suited for different use cases:

### Tumbling Windows

**Concept**: Non-overlapping, fixed-size windows where each record belongs to exactly one window.

```yaml
- type: windowByTime
  windowType: tumbling
  duration: 5m      # Window size
  grace: 30s        # Accept late data up to 30 seconds
```

**Characteristics**:
- No overlap between windows
- Clear, distinct time boundaries
- Memory efficient (fewer windows to maintain)
- Deterministic results

**Use cases**:
- Hourly/daily reports
- Billing periods
- Compliance reporting
- Clear time-boundary analytics

**Kafka Streams equivalent**: `TimeWindows.of(Duration.ofMinutes(5))`

### Hopping Windows

**Concept**: Fixed-size windows that overlap by advancing less than the window size, creating a "sliding" effect.

```yaml
- type: windowByTime
  windowType: hopping
  duration: 10m     # Window size (how much data to include)
  advanceBy: 2m     # Advance every 2 minutes (overlap control)
  grace: 1m         # Late data tolerance
```

**Characteristics**:
- Windows overlap (each record appears in multiple windows)
- Smooth transitions between time periods
- Higher memory usage (more windows active)
- Good for trend analysis

**Use cases**:
- Moving averages
- Smooth metric transitions
- Real-time dashboards
- Anomaly detection with context

**Kafka Streams equivalent**: `TimeWindows.of(Duration.ofMinutes(10)).advanceBy(Duration.ofMinutes(2))`

### Session Windows

**Concept**: Dynamic windows that group events based on activity periods, automatically closing after inactivity gaps.

```yaml
- type: windowBySession
  inactivityGap: 30m  # Close window after 30 minutes of no activity
  grace: 10s          # Accept late arrivals
```

**Characteristics**:
- Variable window sizes based on activity
- Automatically merge overlapping sessions
- Perfect for user behavior analysis
- Complex state management

**Use cases**:
- User browsing sessions
- IoT device activity periods
- Fraud detection sessions
- Activity-based analytics

**Kafka Streams equivalent**: `SessionWindows.with(Duration.ofMinutes(30))`

## Windowing Examples

### Tumbling Window: Click Counting

This example demonstrates tumbling windows by counting user clicks in 5-minute non-overlapping windows.

**What it does**:
1. **Generates user clicks**: Simulates users clicking on different pages
2. **Groups by user**: Each user's clicks are processed separately
3. **Windows by time**: Creates 5-minute tumbling windows
4. **Counts events**: Uses simple count aggregation per window
5. **Handles window keys**: Converts WindowedString keys for output compatibility

**Key KSML concepts demonstrated**:
- `windowByTime` with tumbling windows
- Window state stores with retention policies
- `convertKey` for windowed key compatibility
- Grace periods for late-arriving data

??? info "User clicks producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/windowing/producer-user-clicks.yaml"
    %}
    ```

??? info "Tumbling window count processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/windowing/processor-tumbling-count-working.yaml"
    %}
    ```

**Understanding the window key type**:

The `convertKey` operation transforms the internal `WindowedString` to `json:windowed(string)` format, which contains:
- `key`: Original key (user ID)
- `start`/`end`: Window boundaries in milliseconds
- `startTime`/`endTime`: Human-readable timestamps

### Hopping Window: Moving Averages

This example calculates moving averages using overlapping 2-minute windows that advance every 30 seconds.

**What it does**:
1. **Generates sensor readings**: Simulates temperature, humidity, and pressure sensors
2. **Groups by sensor**: Each sensor's readings are processed independently
3. **Overlapping windows**: 2-minute windows advance every 30 seconds (4x overlap)
4. **Calculates averages**: Maintains sum and count, then computes final average
5. **Smooth transitions**: Provides continuous updates every 30 seconds

**Key KSML concepts demonstrated**:
- `windowByTime` with hopping windows and `advanceBy`
- Custom aggregation with initialization and aggregator functions
- `mapValues` for post-aggregation processing
- Multiple overlapping windows for the same data

??? info "Sensor data producer (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/windowing/producer-sensor-data.yaml"
    %}
    ```

??? info "Hopping window average processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/windowing/processor-hopping-average-working.yaml"
    %}
    ```

**Why hopping windows for averages?**
- **Smooth updates**: New average every 30 seconds instead of waiting 2 minutes
- **Trend detection**: Easier to spot gradual changes
- **Real-time dashboards**: Continuous data flow for visualization
- **Reduced noise**: 2-minute window smooths out brief spikes

### Session Window: User Activity Analysis

This example uses session windows to analyze user browsing patterns by grouping clicks within activity periods.

**What it does**:
1. **Tracks user clicks**: Monitors page visits and click patterns
2. **Detects sessions**: Groups clicks separated by no more than 2 minutes
3. **Aggregates activity**: Counts clicks, tracks unique pages, measures duration
4. **Session boundaries**: Automatically closes sessions after inactivity
5. **Rich analytics**: Provides comprehensive session summaries

**Key KSML concepts demonstrated**:
- `windowBySession` with inactivity gap detection
- Session state stores for variable-length windows
- Complex aggregation state with lists and timestamps
- Automatic session merging and boundary detection

??? info "User session analysis processor (click to expand)"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/windowing/processor-session-activity.yaml"
    %}
    ```

**Session window behavior**:
- **Dynamic sizing**: Windows grow and shrink based on activity
- **Automatic merging**: Late-arriving data can extend or merge sessions
- **Activity-based**: Perfect for user behavior analysis
- **Variable retention**: Different sessions can have different lifespans

??? info \"User session analysis processor (click to expand)\"

    ```yaml
    {%
      include "../../definitions/intermediate-tutorial/windowing/processor-session-activity.yaml"
    %}
    ```

**Session window characteristics observed**:
- **Activity-driven boundaries**: Sessions start with first click and extend with each new click
- **Inactivity-based closure**: Sessions close after 2 minutes of no activity
- **Variable duration**: Sessions can be seconds to hours depending on user behavior
- **Real-time updates**: Each click updates the session end time and click count
- **User-specific**: Each user maintains independent session windows

**Example output**:
```
USER SESSION - user=alice, clicks=15  (session: 23:44:05Z to 23:46:12Z)
USER SESSION - user=bob, clicks=8    (session: 23:44:01Z to 23:45:33Z) 
USER SESSION - user=alice, clicks=23 (session: 23:47:01Z to 23:49:15Z)
```

This shows how Alice had two separate sessions with an inactivity gap between them, while Bob had one continuous session.

## Advanced Windowing Concepts

### Grace Periods and Late Data

**Problem**: In distributed systems, data doesn't always arrive in chronological order. Network delays, system failures, and clock skew can cause "late" data.

**Solution**: Grace periods allow windows to accept late-arriving data for a specified time after the window officially closes.

```yaml
- type: windowByTime
  windowType: tumbling
  duration: 5m
  grace: 1m        # Accept data up to 1 minute late
```

**Configuration guidelines**:
- **grace**: How long to wait for late data (trade-off: accuracy vs. latency)
- **retention**: How long to keep window state (must be ≥ window size + grace period)
- **caching**: Enable for better performance with frequent updates

**Example**: A 5-minute window ending at 10:05 AM will accept data timestamped before 10:05 AM until 10:06 AM (1-minute grace), then close permanently.

### Window State Management

Windowed operations require persistent state to:
- Track aggregations across window boundaries
- Handle late-arriving data
- Provide exactly-once processing guarantees
- Enable fault tolerance and recovery

**State Store Configuration**:

```yaml
store:
  name: my_window_store
  type: window              # Required for windowed operations
  windowSize: 5m           # Must match window duration
  retention: 30m           # Keep expired windows (≥ windowSize + grace)
  caching: true            # Reduce downstream updates
  retainDuplicates: false  # Keep only latest value per window
```

### Window Key Types and Conversion

Windowed aggregations produce complex keys containing both the original key and window metadata:

**Internal key structure**:
```yaml
WindowedString:
  key: "user123"                    # Original key
  start: 1634567400000             # Window start (epoch millis)
  end: 1634567700000               # Window end (epoch millis) 
  startTime: "2021-10-18T14:30:00Z" # Human-readable start
  endTime: "2021-10-18T14:35:00Z"   # Human-readable end
```

**Output compatibility**:
```yaml
- type: convertKey
  into: json:windowed(string)    # Convert for downstream compatibility
```

**Alternative approaches**:
```yaml
# Extract just the original key
- type: map
  mapper:
    expression: key.key()        # Get original key only
    resultType: string

# Create custom key format
- type: map
  mapper:
    code: |
      window_key = f"{key.key()}_{key.start()}_{key.end()}"
    expression: window_key
    resultType: string
```

## Performance Considerations

### Memory Usage

**Window count calculation**:
- **Tumbling**: `retention / window_size` windows per key
- **Hopping**: `retention / advance_by` windows per key  
- **Session**: Variable, depends on activity patterns

**Example**: 1-hour retention with 5-minute tumbling windows = 12 windows per key

### Optimization Strategies

1. **Right-size windows**:
   ```yaml
   # Too small: High overhead, frequent updates
   duration: 10s
   
   # Too large: High memory, delayed results  
   duration: 24h
   
   # Balanced: Matches business requirements
   duration: 5m
   ```

2. **Tune grace periods**:
   ```yaml
   # Minimal: Fast processing, may lose late data
   grace: 5s
   
   # Conservative: Handles most late data, slower processing
   grace: 5m
   ```

3. **Enable caching**:
   ```yaml
   store:
     caching: true     # Reduces downstream updates
   ```

4. **Optimize retention**:
   ```yaml
   store:
     retention: 1h     # Minimum: windowSize + grace
     # Longer retention = more memory usage
   ```

## Troubleshooting Common Issues

### Missing Data in Windows

**Symptoms**: Expected data doesn't appear in window results

**Causes & Solutions**:
1. **Clock skew**: Ensure producer/consumer clocks are synchronized
2. **Grace period too short**: Increase grace period for late data
3. **Wrong timestamps**: Verify timestamp field extraction
4. **Retention too short**: Data expired before processing

### High Memory Usage

**Symptoms**: OutOfMemory errors, slow processing

**Solutions**:
1. **Reduce retention periods**
2. **Increase window sizes** (fewer windows)
3. **Enable caching** to reduce state store pressure
4. **Filter data earlier** in the pipeline

### Inconsistent Results

**Symptoms**: Different runs produce different window contents

**Causes**:
1. **Late data**: Some runs receive different late arrivals
2. **Grace period timing**: Data arrives just at grace boundary
3. **System clock differences**: Inconsistent time sources

**Solutions**:
1. Use consistent time sources
2. Implement proper grace periods
3. Consider event-time vs processing-time semantics

## Best Practices

### Window Design

1. **Match business requirements**:
   - Reporting periods → Tumbling windows
   - Real-time dashboards → Hopping windows
   - User behavior → Session windows

2. **Balance trade-offs**:
   - **Latency vs. Accuracy**: Shorter grace = faster results, potential data loss
   - **Memory vs. Precision**: Larger windows = more memory, less granular data
   - **Updates vs. Performance**: More overlapping windows = more updates

3. **Plan for scale**:
   - **Key cardinality**: More unique keys = more state
   - **Event rate**: Higher rates need more memory
   - **Window overlap**: Hopping windows multiply memory usage

### State Management

1. **Configure retention properly**:
   ```yaml
   # Rule: retention ≥ windowSize + grace + buffer
   windowSize: 1h
   grace: 5m
   retention: 2h      # Safe buffer for late data
   ```

2. **Monitor state store sizes**:
   - Use Kafka Streams metrics
   - Alert on unusual growth
   - Plan storage capacity

3. **Handle state recovery**:
   - Kafka Streams rebuilds state from changelog topics
   - Plan for recovery time after failures
   - Consider standby replicas for faster recovery

## Conclusion

Windowing enables sophisticated time-based analytics in streaming applications. Each window type serves specific use cases:

### Window Type Selection Guide

| Window Type | Best For | Key Benefits | Trade-offs |
|-------------|----------|--------------|------------|
| **Tumbling** | Periodic reports, billing cycles, compliance | Clear boundaries, memory efficient, deterministic | Less granular, potential data delays |
| **Hopping** | Moving averages, real-time dashboards, trend analysis | Smooth updates, continuous metrics | Higher memory usage, more computation |
| **Session** | User behavior, IoT device activity, fraud detection | Activity-driven, variable length, natural boundaries | Complex state management, harder to predict |

### Getting Started

1. **Start simple**: Begin with tumbling windows for periodic analytics
2. **Add smoothness**: Use hopping windows when you need continuous updates
3. **Handle activity**: Implement session windows for behavior-driven analysis
4. **Optimize gradually**: Tune grace periods, retention, and caching based on requirements

### Key Success Factors

- **Understand your data patterns**: Event frequency, late arrivals, time skew
- **Plan for scale**: Consider key cardinality and window overlap
- **Monitor state stores**: Track memory usage and performance metrics
- **Test thoroughly**: Verify window boundaries and grace period handling

Windowing transforms infinite streams into manageable, time-bounded analytics that power real-time applications and business intelligence.

## Further Reading

- [Core Concepts: Operations](../../core-concepts/operations.md)
- [Reference: Windowing Operations](../../reference/operation-reference.md)