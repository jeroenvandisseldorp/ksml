# Kafka Streams Compatibility

Quick
reference
for Kafka
Streams
DSL
compatibility
in
StoatFlow (
StoatFlow).

**Target
Version:
** Kafka
Streams
4.1.x

## Legend

- ✅
  Implemented
- ⚠️
  Partial (
  stub or
  limited)
- ❌ Not
  implemented
  yet
- 🛑
  Incompatible;
  or
  won't
  be
  implemented
- ↘️
  Deprecated
  in KS
- 🚀
  StoatFlow
  extension (
  not in
  KS)

## StreamsBuilder

| Method                                                          | Status | Notes                                            |
|-----------------------------------------------------------------|--------|--------------------------------------------------|
| stream(topic, consumed?)                                        | ✅      | Single topic subscription                        |
| stream(Collection\<String\>)                                    | ✅      | Multiple topics subscription                     |
| stream(Pattern)                                                 | ✅      | Pattern-based subscription                       |
| table(topic, consumed?)                                         | ✅      |                                                  |
| table(topic, consumed?, materialized)                           | ✅      | With state store                                 |
| globalTable(topic, consumed?)                                   | ✅      | Wraps KTable (ADR-007)                           |
| globalTable(topic, materialized)                                | ✅      | Without Consumed parameter                       |
| build()                                                         | ✅      | Returns Topology                                 |
| build(Properties)                                               | ✅      | Logs warning, delegates to build()               |
| addStateStore(storeBuilder)                                     | ✅      | Register standalone store                        |
| addStateStore(storeBuilder, keySerde, valueSerde)               | ✅      | With changelog Serdes                            |
| addStateStore(storeSupplier, keySerde, valueSerde)              | ✅      | Convenience overload                             |
| addGlobalStore(storeBuilder, topic, consumed, processor)        | ✅      | Custom update processor                          |
| addGlobalStore(storeBuilder, topic, consumed)                   | ✅      | Default put/delete semantics                     |
| addReadOnlyStateStore(storeBuilder, topic, consumed, processor) | ✅      | KIP-813: Custom processor (ADR-035)              |
| addReadOnlyStateStore(storeBuilder, topic, consumed)            | ✅      | KIP-813: Default put/delete semantics            |
| scheduled(interval, type, emitter)                              | 🚀     | Interval-based periodic record source (ADR-039)  |
| scheduled(interval, type, named, emitter)                       | 🚀     | With explicit naming                             |
| scheduled(interval, type, keySerde, emitter)                    | 🚀     | With explicit key serde for lane assignment      |
| scheduled(cron, emitter)                                        | 🚀     | Cron-based calendar scheduling (ADR-039)         |
| scheduled(cron, named, emitter)                                 | 🚀     | Cron with explicit naming                        |
| scheduled(cron, keySerde, emitter)                              | 🚀     | Cron with explicit key serde for lane assignment |

## Topology

In Kafka
Streams,
`Topology`
serves
dual
purposes: (

1) a
   container
   for
   the
   topology
   graph
   returned
   by
   `StreamsBuilder.build()`,
   and (
   2) a
   low-level
   builder
   for
   the
   Processor
   API.
   StoatFlow
   now
   supports
   both
   approaches -
   you
   can
   use
   either
   `StreamsBuilder` (
   high-level
   DSL)
   or
   `Topology`
   directly (
   low-level
   Processor
   API).

| Method                                   | Status | Notes                                                                   |
|------------------------------------------|--------|-------------------------------------------------------------------------|
| describe()                               | ✅      | Returns TopologyDescription with sub-topologies                         |
| addSource(name, topics...)               | ✅      | 14 overloads: offset reset, timestamp extractor, deserializers, pattern |
| addProcessor(name, supplier, parents...) | ✅      | User-defined processor with ProcessorSupplier                           |
| addSink(name, topic, parents...)         | ✅      | 8 overloads: serializers, partitioner, TopicNameExtractor               |
| addStateStore(builder, processors...)    | ✅      | Register store and optionally connect to processors                     |
| addGlobalStore(...)                      | ✅      | 2 variants: with/without TimestampExtractor                             |
| addReadOnlyStateStore(...)               | ✅      | 2 variants (KIP-813): source topic is changelog                         |
| connectProcessorAndStateStores(...)      | ✅      | Connect existing store to processor                                     |

*

*
Processor
API
Example:
**

```kotlin
val topology =
    Topology()
        .addSource(
            "source",
            "input-topic"
        )
        .addProcessor<String, String, String, String>(
            "processor",
            ProcessorSupplier { MyProcessor() },
            "source"
        )
        .addSink(
            "sink",
            "output-topic",
            "processor"
        )

// Test with TopologyTestDriver
val driver =
    TopologyTestDriver.fromTopology(
        topology
    )
```

**Note:**
For most
use
cases,
the DSL
approach
via
`StreamsBuilder`
is
recommended.
The
Processor
API
provides
low-level
control
for
advanced
scenarios
where the
DSL is
not
expressive
enough.

## KStream - Stateless Transformations

| Method                            | Status | Notes                                          |
|-----------------------------------|--------|------------------------------------------------|
| filter(predicate)                 | ✅      |                                                |
| filterNot(predicate)              | ✅      |                                                |
| map(mapper)                       | ✅      | Key-changing → sub-topology boundary (ADR-005) |
| mapValues(ValueMapper)            | ✅      |                                                |
| mapValues(ValueMapperWithKey)     | ✅      | Key-aware value mapper variant                 |
| flatMap(mapper)                   | ✅      | Key-changing → sub-topology boundary           |
| flatMapValues(ValueMapper)        | ✅      |                                                |
| flatMapValues(ValueMapperWithKey) | ✅      | Key-aware value mapper variant                 |
| selectKey(keySelector)            | ✅      | Key-changing → sub-topology boundary           |
| peek(action)                      | ✅      | Java BiConsumer overload                       |
| merge(other)                      | ✅      |                                                |
| split()                           | ✅      | Returns BranchedKStream                        |
| repartition()                     | ✅      | In-memory only (ADR-010)                       |
| repartition(Repartitioned)        | ✅      | Custom partitioner, naming                     |
| print()                           | ✅      | Configurable via Printed                       |

## KStream - Terminal Operations

| Method                            | Status | Notes                           |
|-----------------------------------|--------|---------------------------------|
| forEach(action)                   | ✅      | Java BiConsumer overload        |
| to(topic, produced?)              | ✅      | Static topic routing            |
| to(TopicNameExtractor, produced?) | ✅      | Dynamic topic routing (ADR-022) |
| toTable()                         | ✅      | Auto-generates store name       |
| toTable(named?, materialized?)    | ✅      | With naming and materialization |

## KStream - Processor API

| Method                           | Status | Notes                                       |
|----------------------------------|--------|---------------------------------------------|
| process(processorSupplier)       | ✅      | Key-changing; ProcessorSupplier             |
| processValues(processorSupplier) | ✅      | Non-key-changing; FixedKeyProcessorSupplier |
| transform()                      | ↘️     | Deprecated in KS; use process()             |
| transformValues()                | ↘️     | Deprecated in KS; use processValues()       |

## ProcessorContext

The
context
provided
to
user-defined
processors
for state
access,
record
forwarding,
and
scheduling.

| Method                                                | Status | Notes                                                          |
|-------------------------------------------------------|--------|----------------------------------------------------------------|
| forward(record)                                       | ✅      | Forward to downstream processors                               |
| forward(record, childName)                            | ✅      | Forward to specific child                                      |
| getStateStore(name)                                   | ✅      | Access connected state stores                                  |
| schedule(interval, type, punctuator)                  | ✅      | Kafka Streams compatible punctuator (ADR-039)                  |
| schedule(interval, type, mode, punctuator)            | 🚀     | StoatFlow extension with BLOCKING/NON_BLOCKING mode            |
| schedule(interval, startTime, type, punctuator)       | ✅      | KIP-1146: Anchored punctuation with grid-aligned fire times    |
| schedule(interval, startTime, type, mode, punctuator) | ✅      | KIP-1146: Anchored punctuation with BLOCKING/NON_BLOCKING mode |
| recordMetadata()                                      | ✅      | Source partition, offset, topic                                |
| currentSystemTimeMs()                                 | ✅      | Wall-clock time via `System.currentTimeMillis()`               |
| currentStreamTimeMs()                                 | ✅      | Delegates to `currentWatermarkMs()` (global watermark)         |
| currentWatermarkMs()                                  | 🆕     | StoatFlow extension: global watermark in epoch millis          |
| timerService()                                        | 🚀     | Key-based timer access (Flink-inspired, ADR-039)               |

*

*
Punctuator
Scheduling:
**

```kotlin
context.schedule(
    Duration.ofMinutes(
        1
    ),
    PunctuationType.WALL_CLOCK_TIME
) { timestamp ->
    // Periodic callback - read-only state access
    val store =
        context.getStateStore<ReadOnlyKeyValueStore<String, Long>>(
            "my-store"
        )
    store.all()
        .forEach { kv ->
            logger.info(
                "Key: ${kv.key}, Value: ${kv.value}"
            )
        }
}
```

**Note:**
Punctuators
run
outside
key-affinity
lanes on
dedicated
virtual
threads
with
read-only
state
access.
For write
access,
use
key-based
timers
via
`timerService()`.

## Processor Interfaces

Interfaces
and base
classes
for
user-defined
processors.

| Type                                      | Status | Notes                                                         |
|-------------------------------------------|--------|---------------------------------------------------------------|
| Processor<KIn,VIn,KOut,VOut>              | ✅      | Key-changing processor interface                              |
| FixedKeyProcessor<KIn,VIn,VOut>           | ✅      | Key-preserving processor interface                            |
| ContextualProcessor<KIn,VIn,KOut,VOut>    | ✅      | Abstract base class managing ProcessorContext                 |
| ContextualFixedKeyProcessor<KIn,VIn,VOut> | ✅      | Abstract base class managing FixedKeyProcessorContext         |
| ProcessorSupplier<KIn,VIn,KOut,VOut>      | ✅      | Factory for Processor instances                               |
| ProcessorSupplier.stores()                | ✅      | KS 4.x pattern for declaring stores inline with the processor |
| FixedKeyProcessorSupplier<KIn,VIn,VOut>   | ✅      | Factory for FixedKeyProcessor instances                       |

*

*
ContextualProcessor
Example:
**

```kotlin
class MyProcessor :
    ContextualProcessor<String, Int, String, String>() {
    override fun process(
        record: Record<String, Int>
    ) {
        // Context managed automatically - no init() override needed
        context().forward(
            "key",
            "value: ${record.value}"
        )
    }

    override fun onTimer(
        timestamp: Long,
        key: String,
        ctx: TimerContext<String, String>
    ) {
        ctx.forward(
            key,
            "timer fired at $timestamp"
        )
    }
}
```

## Functional Interfaces (ADR-047)

KS-compatible
functional
interfaces
for
Java-friendly
DSL
overloads.

| KS Interface                        | Status | Used in                                |
|-------------------------------------|--------|----------------------------------------|
| `KeyValueMapper<K, V, VR>`          | ✅      | `map`, `flatMap`, `groupBy`, FK joins  |
| `ValueMapper<V, VR>`                | ✅      | `KTable.mapValues`, FK join extractors |
| `ValueMapperWithKey<K, V, VR>`      | ✅      | `mapValues` with key access            |
| `Predicate<K, V>`                   | ✅      | `filter`, `filterNot`                  |
| `ForeachAction<K, V>`               | ✅      | `peek`, `forEach`                      |
| `Initializer<VA>`                   | ✅      | All `aggregate()`                      |
| `Aggregator<K, V, VA>`              | ✅      | All `aggregate()`                      |
| `Reducer<V>`                        | ✅      | All `reduce()`                         |
| `Subtractor<K, V, VA>`              | ✅      | `KGroupedTable.aggregate()`            |
| `Merger<K, V>`                      | ✅      | Session window `aggregate()`           |
| `ValueJoiner<V1, V2, VR>`           | ✅      | All joins                              |
| `ValueJoinerWithKey<K, V1, V2, VR>` | ✅      | Key-aware joins                        |

## KStream - Grouping

| Method                        | Status | Notes                                |
|-------------------------------|--------|--------------------------------------|
| groupByKey()                  | ✅      | Returns KGroupedStream               |
| groupByKey(Grouped)           | ✅      | With Serdes configuration            |
| groupBy(keySelector)          | ✅      | Key-changing → sub-topology boundary |
| groupBy(keySelector, Grouped) | ✅      | With Serdes configuration            |

## KGroupedStream

| Method                             | Status | Notes                              |
|------------------------------------|--------|------------------------------------|
| count()                            | ✅      | With Materialized overloads        |
| reduce(reducer)                    | ✅      | With Materialized overloads        |
| aggregate(initializer, aggregator) | ✅      | With Materialized overloads        |
| windowedBy(TimeWindows)            | ✅      | Tumbling/hopping windows (ADR-016) |
| windowedBy(SlidingWindows)         | ✅      | Sliding windows (ADR-016)          |
| windowedBy(SessionWindows)         | ✅      | Session windows (ADR-016)          |
| cogroup()                          | ✅      | Returns CogroupedKStream           |

## CogroupedKStream

Combines
multiple
grouped
streams
with
different
value
types
into a
single
aggregation.

| Method                              | Status | Notes                                   |
|-------------------------------------|--------|-----------------------------------------|
| cogroup(KGroupedStream, aggregator) | ✅      | Add another stream to cogroup           |
| aggregate(initializer)              | ✅      | With Materialized overloads             |
| windowedBy(TimeWindows)             | ✅      | Returns TimeWindowedCogroupedKStream    |
| windowedBy(SessionWindows)          | ✅      | Returns SessionWindowedCogroupedKStream |

## TimeWindowedCogroupedKStream

| Method                 | Status | Notes                                   |
|------------------------|--------|-----------------------------------------|
| aggregate(initializer) | ✅      | With Materialized overloads             |
| emitStrategy()         | ✅      | OnWindowUpdate (default), OnWindowClose |

## SessionWindowedCogroupedKStream

| Method                                | Status | Notes                                   |
|---------------------------------------|--------|-----------------------------------------|
| aggregate(initializer, sessionMerger) | ✅      | With Materialized overloads             |
| emitStrategy()                        | ✅      | OnWindowUpdate (default), OnWindowClose |

## TimeWindowedKStream

| Method                             | Status | Notes                                             |
|------------------------------------|--------|---------------------------------------------------|
| count()                            | ✅      | With Materialized overloads (ADR-016)             |
| reduce(reducer)                    | ✅      | With Materialized overloads (ADR-016)             |
| aggregate(initializer, aggregator) | ✅      | With Materialized overloads (ADR-016)             |
| emitStrategy()                     | ✅      | OnWindowUpdate (default), OnWindowClose (ADR-015) |

## SlidingWindowedKStream

| Method                             | Status | Notes                                             |
|------------------------------------|--------|---------------------------------------------------|
| count()                            | ✅      | KIP-450 event-driven semantics (ADR-016)          |
| reduce(reducer)                    | ✅      | With Materialized overloads (ADR-016)             |
| aggregate(initializer, aggregator) | ✅      | With Materialized overloads (ADR-016)             |
| emitStrategy()                     | ✅      | OnWindowUpdate (default), OnWindowClose (ADR-015) |

## SessionWindowedKStream

| Method                                            | Status | Notes                                             |
|---------------------------------------------------|--------|---------------------------------------------------|
| count()                                           | ✅      | With session merger (ADR-016)                     |
| reduce(reducer)                                   | ✅      | Reducer doubles as merger (ADR-016)               |
| aggregate(initializer, aggregator, sessionMerger) | ✅      | With session merger (ADR-016)                     |
| emitStrategy()                                    | ✅      | OnWindowUpdate (default), OnWindowClose (ADR-015) |

## KGroupedTable

| Method                             | Status | Notes                              |
|------------------------------------|--------|------------------------------------|
| count()                            | ✅      | Changelog semantics (ADR-024)      |
| reduce(adder, subtractor)          | ✅      | Kafka Streams compatible (ADR-024) |
| aggregate(init, adder, subtractor) | ✅      | Kafka Streams compatible (ADR-024) |

*

*
Changelog
Semantics (
ADR-024):
**

StoatFlow's
`KGroupedTable`
uses
proper *
*
changelog
semantics
**,
matching
Kafka
Streams
behavior:

```kotlin
// When a user moves from department A to department B:
// - Department A's count decrements by 1
// - Department B's count increments by 1

usersTable
    .groupBy { userId, user ->
        KeyValue(
            user.department,
            user
        )
    }
    .count()  // Correctly handles department changes
```

For
`count()`:

- Insert:
  increment
  count
-

Update (
key
change):
decrement
old key's
count,
increment
new key's
count

- Delete:
  decrement
  count

For
`reduce(adder, subtractor)`
and
`aggregate(init, adder, subtractor)`:

1.

`subtractor(agg, oldValue)`
to undo
the old
value (if
present)

2.

`adder(agg, newValue)`
to apply
the new
value (if
present)

*

*
Requirements:
**

- Source
  `KTable`
  must be
  materialized (
  have a
  state
  store)
  for
  `groupBy()`
  to
  track
  old
  values
- Use
  `StreamsBuilder.table(topic, materialized)`
  to
  create
  a
  materialized
  table

**Note:**
`KGroupedTable`
is the
result of
`KTable.groupBy()`.
The
implementation
uses
`Change<V>`
records
internally
to track
both old
and new
values.

**KIP-914
Out-of-Order
Filtering (
Versioned
Stores):
**
When the
source
table is
backed by
a
versioned
store,
out-of-order
records (
records
with
timestamp <
last seen
timestamp
for key)
are
ignored
and do
not
update
the
aggregation.
This
prevents
aggregate
corruption
from
late-arriving
records.

## KStream - Joins

| Method                                                        | Status | Notes                          |
|---------------------------------------------------------------|--------|--------------------------------|
| join(KStream, ValueJoiner, windows)                           | ✅      | Stream-stream windowed join    |
| join(KStream, ValueJoiner, windows, streamJoined)             | ✅      | With StreamJoined config       |
| join(KStream, ValueJoinerWithKey, windows)                    | ✅      | Key-aware joiner variant       |
| join(KStream, ValueJoinerWithKey, windows, streamJoined)      | ✅      | Key-aware joiner with config   |
| leftJoin(KStream, ValueJoiner, windows)                       | ✅      | Stream-stream left join        |
| leftJoin(KStream, ValueJoiner, windows, streamJoined)         | ✅      | With StreamJoined config       |
| leftJoin(KStream, ValueJoinerWithKey, windows)                | ✅      | Key-aware joiner variant       |
| leftJoin(KStream, ValueJoinerWithKey, windows, streamJoined)  | ✅      | Key-aware joiner with config   |
| outerJoin(KStream, ValueJoiner, windows)                      | ✅      | Stream-stream outer join       |
| outerJoin(KStream, ValueJoiner, windows, streamJoined)        | ✅      | With StreamJoined config       |
| outerJoin(KStream, ValueJoinerWithKey, windows)               | ✅      | Key-aware joiner variant       |
| outerJoin(KStream, ValueJoinerWithKey, windows, streamJoined) | ✅      | Key-aware joiner with config   |
| join(KTable, ValueJoiner)                                     | ✅      | Stream-table inner join        |
| join(KTable, ValueJoinerWithKey)                              | ✅      | Key-aware joiner variant       |
| leftJoin(KTable, ValueJoiner)                                 | ✅      | Stream-table left join         |
| leftJoin(KTable, ValueJoinerWithKey)                          | ✅      | Key-aware joiner variant       |
| join(GlobalKTable, keySelector, ValueJoiner)                  | ✅      | Stream-GlobalKTable inner join |
| join(GlobalKTable, keySelector, ValueJoinerWithKey)           | ✅      | Key-aware joiner variant       |
| leftJoin(GlobalKTable, keySelector, ValueJoiner)              | ✅      | Stream-GlobalKTable left join  |
| leftJoin(GlobalKTable, keySelector, ValueJoinerWithKey)       | ✅      | Key-aware joiner variant       |

*

*Stream-Stream
Joins:**
Stream-stream
joins
correlate
records
from two
streams
within a
configurable
time
window:

- Records
  are
  matched
  by key
  within
  the
  `JoinWindows`
  time
  bounds
- Both
  streams
  are
  buffered
  in
  window
  stores
- Inner
  join
  emits
  only
  when
  both
  sides
  have
  matches
- Left
  join
  always
  emits
  left
  records (
  with
  null
  right
  value
  if
  unmatched)
- Outer
  join
  emits
  all
  records (
  with
  null
  for
  unmatched
  side)
-

Unmatched
records
for
LEFT/OUTER
joins
emit when
window
closes
via
watermark

*

*Stream-Table
Joins:**
Stream-table
joins are
point-in-time
lookups.
Unlike
KTable-KTable
joins,
table
updates
do NOT
re-trigger
the join.

**KIP-914
Temporal
Join
Semantics (
Versioned
Stores):
**
When the
backing
table
uses a
versioned
store (
KIP-889),
stream-table
joins
perform
temporal
lookups
using the
stream
record's
timestamp.
This
returns
the table
value
that was
valid at
the
stream
event
time,
enabling
correct
results
even when
records
arrive
out of
order.

## BranchedKStream

| Method                      | Status | Notes                |
|-----------------------------|--------|----------------------|
| branch(predicate)           | ✅      |                      |
| branch(predicate, branched) | ✅      | With Branched config |
| defaultBranch()             | ✅      |                      |
| defaultBranch(branched)     | ✅      | With Branched config |
| noDefaultBranch()           | ✅      |                      |

## KTable - Transformations

| Method                                             | Status | Notes                                                            |
|----------------------------------------------------|--------|------------------------------------------------------------------|
| filter(predicate)                                  | ✅      | Derived table (no state store)                                   |
| filter(predicate, Named)                           | ✅      | With naming                                                      |
| filter(predicate, Materialized)                    | ✅      | With materialization (ADR-023)                                   |
| filter(predicate, Named, Materialized)             | ✅      | With naming and materialization (ADR-023)                        |
| filterNot(predicate)                               | ✅      | Inverse of filter                                                |
| filterNot(predicate, Named)                        | ✅      | With naming                                                      |
| filterNot(predicate, Materialized)                 | ✅      | With materialization (ADR-023)                                   |
| filterNot(predicate, Named, Materialized)          | ✅      | With naming and materialization (ADR-023)                        |
| mapValues(ValueMapper)                             | ✅      | Derived table (no state store)                                   |
| mapValues(ValueMapper, Named)                      | ✅      | With naming                                                      |
| mapValues(ValueMapper, Materialized)               | ✅      | With materialization (ADR-023)                                   |
| mapValues(ValueMapper, Named, Materialized)        | ✅      | With naming and materialization (ADR-023)                        |
| mapValues(ValueMapperWithKey)                      | ✅      | Key-aware value mapper variant (ADR-023)                         |
| mapValues(ValueMapperWithKey, Named)               | ✅      | With naming (ADR-023)                                            |
| mapValues(ValueMapperWithKey, Materialized)        | ✅      | With materialization (ADR-023)                                   |
| mapValues(ValueMapperWithKey, Named, Materialized) | ✅      | With naming and materialization (ADR-023)                        |
| toStream()                                         | ✅      | Same key type                                                    |
| toStream(KeyValueMapper)                           | ✅      | Key-changing variant; sub-topology boundary (ADR-005, ADR-023)   |
| queryableStoreName()                               | ✅      | Returns store name if materialized                               |
| groupBy(keyValueSelector)                          | ✅      | Selector returns KeyValue<KR, VR>; returns KGroupedTable<KR, VR> |
| groupBy(keyValueSelector, Grouped)                 | ✅      | With Serdes configuration                                        |
| suppress(Suppressed)                               | ✅      | untilWindowCloses, untilTimeLimit (ADR-015, KIP-914 validation)  |
| transformValues()                                  | ↘️     | Deprecated in KS                                                 |

## KTable - Primary Key Joins

Signature:
`join(other: KTable<K, VO>, joiner)`
where
both
tables
share key
type `K`

| Method                                         | Status | Notes                                 |
|------------------------------------------------|--------|---------------------------------------|
| join(KTable, joiner)                           | ✅      | Inner join; both must be materialized |
| join(KTable, joiner, Materialized)             | ✅      | With materialization of join result   |
| join(KTable, joiner, Named, Materialized)      | ✅      | With naming and materialization       |
| leftJoin(KTable, joiner)                       | ✅      | Left table always emits               |
| leftJoin(KTable, joiner, Materialized)         | ✅      | With materialization of join result   |
| leftJoin(KTable, joiner, Named, Materialized)  | ✅      | With naming and materialization       |
| outerJoin(KTable, joiner)                      | ✅      | Full outer join                       |
| outerJoin(KTable, joiner, Materialized)        | ✅      | With materialization of join result   |
| outerJoin(KTable, joiner, Named, Materialized) | ✅      | With naming and materialization       |

**Note:**
No
co-partitioning
required (
global
state
model,
ADR-007)

**KIP-914
Semantics (
Versioned
Stores):
**
When BOTH
tables
are
backed by
versioned
stores,
out-of-order
records
do not
trigger
join
evaluation.
This
prevents
inconsistent
results
when
records
arrive
out of
order.
Temporal
lookups
are used
to get
the
correct
value
from the
other
table.

## KTable - Foreign Key Joins

Signature:
`join(other: KTable<KO, VO>, foreignKeyExtractor, joiner)`
where
`foreignKeyExtractor: (V) -> KO?`

| Method                                                           | Status | Notes                               |
|------------------------------------------------------------------|--------|-------------------------------------|
| join(KTable, fkExtractor, joiner)                                | ✅      | Inner FK join                       |
| join(KTable, fkExtractor, joiner, Materialized)                  | ✅      | With materialization of join result |
| join(KTable, fkExtractor, joiner, TableJoined, Materialized)     | ✅      | With naming and materialization     |
| leftJoin(KTable, fkExtractor, joiner)                            | ✅      | Left FK join                        |
| leftJoin(KTable, fkExtractor, joiner, Materialized)              | ✅      | With materialization of join result |
| leftJoin(KTable, fkExtractor, joiner, TableJoined, Materialized) | ✅      | With naming and materialization     |
| outerJoin(KTable, fkExtractor, joiner)                           | ⚠️     | Not supported in KS either          |

*

*
Implementation:
** Uses
in-memory
reverse
index for
O(k)
right-side
lookups (
ADR-053).
KS uses a
persistent
subscription
store (
KIP-213);
StoatFlow
achieves
the same
O(k)
complexity
via
in-memory
maps
rebuilt
on
startup.

## GlobalKTable

| Method               | Status | Notes                                 |
|----------------------|--------|---------------------------------------|
| queryableStoreName() | ✅      | Returns underlying table's store name |
| asKTable()           | 🚀     | StoatFlow extension to unwrap         |

**Note:**
In
StoatFlow,
GlobalKTable
wraps
KTable
internally.
All state
is
global (
ADR-007),
so
behavior
is
identical
to
KTable.

## Helper Classes

### Predicate

| Method           | Status | Notes                                                                     |
|------------------|--------|---------------------------------------------------------------------------|
| test(key, value) | ✅      | KS-compatible `Predicate<K, V>` functional interface for filter/filterNot |

### Consumed

| Method                                                      | Status | Notes                                                                                                                                                                                                     |
|-------------------------------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| with(keySerde, valueSerde)                                  | ✅      |                                                                                                                                                                                                           |
| with(keySerde, valueSerde, timestampExtractor, resetPolicy) | 🛑     | Use WatermarkStrategy instead                                                                                                                                                                             |
| keySerde(serde)                                             | ✅      |                                                                                                                                                                                                           |
| valueSerde(serde)                                           | ✅      |                                                                                                                                                                                                           |
| as(name)                                                    | ✅      |                                                                                                                                                                                                           |
| withWatermarkStrategy(strategy)                             | 🚀     | StoatFlow extension (ADR-006)                                                                                                                                                                             |
| withTimestampExtractor()                                    | 🛑     | Use WatermarkStrategy instead                                                                                                                                                                             |
| withOffsetResetPolicy()                                     | ✅      | Per-source offset reset                                                                                                                                                                                   |
| withNumberOfLanes(int)                                      | 🆕     | StoatFlow extension: sets the lane count of **this chain's** source sub-topology (ADR-112; per-chain since ADR-114 — multi-source topologies size lanes per chain, cross-chain values no longer conflict) |
| withLaneQueueCapacity(int)                                  | 🆕     | StoatFlow extension: sets the per-lane queue capacity of **this chain's** source sub-topology (ADR-113; per-chain since ADR-114)                                                                          |

### Grouped

| Method                           | Status | Notes                                                                                                                   |
|----------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------|
| as(name)                         | ✅      |                                                                                                                         |
| keySerde(serde)                  | ✅      |                                                                                                                         |
| valueSerde(serde)                | ✅      |                                                                                                                         |
| with(keySerde, valueSerde)       | ✅      |                                                                                                                         |
| with(name, keySerde, valueSerde) | ✅      |                                                                                                                         |
| withName(name)                   | ✅      |                                                                                                                         |
| withKeySerde(serde)              | ✅      |                                                                                                                         |
| withValueSerde(serde)            | ✅      |                                                                                                                         |
| withNumberOfLanes(int)           | 🆕     | StoatFlow extension: sets the lane count of the downstream sub-topology after `groupBy(keySelector, Grouped)` (ADR-112) |
| withLaneQueueCapacity(int)       | 🆕     | StoatFlow extension: sets the per-lane queue capacity of the downstream sub-topology (ADR-113)                          |

**Note:**
`Grouped`
provides
optional
naming
and
Serdes
for
grouping
operations.
Serdes
from
`Grouped`
are used
as
defaults
by
downstream
aggregations
unless
overridden
by
`Materialized`.

### Produced

| Method                         | Status | Notes |
|--------------------------------|--------|-------|
| with(keySerde, valueSerde)     | ✅      |       |
| keySerde(serde)                | ✅      |       |
| valueSerde(serde)              | ✅      |       |
| as(name)                       | ✅      |       |
| streamPartitioner(partitioner) | ✅      |       |

### Materialized

| Method                           | Status | Notes                                                    |
|----------------------------------|--------|----------------------------------------------------------|
| as(storeName)                    | ✅      |                                                          |
| as(storeSupplier)                | ✅      |                                                          |
| as(DslStoreSuppliers)            | ✅      | Store type supplier (ADR-029)                            |
| with(keySerde, valueSerde)       | ✅      |                                                          |
| keySerde(serde)                  | ✅      |                                                          |
| valueSerde(serde)                | ✅      |                                                          |
| withCachingEnabled()             | ✅      | Suppress downstream emissions (default enabled, ADR-020) |
| withCachingDisabled()            | ✅      | Emit all intermediate updates downstream (ADR-020)       |
| withLoggingEnabled()             | ✅      | Enable changelog with optional topic config              |
| withLoggingDisabled()            | ✅      | Disable changelog for this store                         |
| withRetention()                  | ✅      | Override computed retention for windowed stores          |
| withStoreType(DslStoreSuppliers) | ✅      | Store type selection (ADR-029)                           |

### Branched

| Method                 | Status | Notes                  |
|------------------------|--------|------------------------|
| as(name)               | ✅      |                        |
| withName(name)         | ✅      |                        |
| withFunction(chain)    | ✅      |                        |
| withConsumer(consumer) | ✅      | Java Consumer overload |

### Named

| Method   | Status | Notes |
|----------|--------|-------|
| as(name) | ✅      |       |

### Printed

| Method                     | Status | Notes              |
|----------------------------|--------|--------------------|
| toSysOut()                 | ✅      | Default stdout     |
| toFile(filePath)           | ✅      | File output        |
| withLabel(label)           | ✅      | Prefix label       |
| withKeyValueMapper(mapper) | ✅      | Custom formatting  |
| withName(processorName)    | ✅      | Set processor name |

### Joined

| Method                                      | Status | Notes                   |
|---------------------------------------------|--------|-------------------------|
| as(name)                                    | ✅      |                         |
| withName(name)                              | ✅      |                         |
| with(keySerde, valueSerde, otherValueSerde) | 🛑     | Not needed in StoatFlow |
| withKeySerde(serde)                         | 🛑     | Not needed in StoatFlow |
| withValueSerde(serde)                       | 🛑     | Not needed in StoatFlow |
| withOtherValueSerde(serde)                  | 🛑     | Not needed in StoatFlow |
| gracePeriod()                               | ↘️     | Deprecated getter       |
| keySerde()                                  | ↘️     | Deprecated getter       |
| valueSerde()                                | ↘️     | Deprecated getter       |
| otherValueSerde()                           | ↘️     | Deprecated getter       |

### TableJoined

| Method                 | Status | Notes                   |
|------------------------|--------|-------------------------|
| as(name)               | ✅      |                         |
| withPartitioner()      | 🛑     | N/A for single-instance |
| withOtherPartitioner() | 🛑     | N/A for single-instance |

### JoinWindows

| Method                                          | Status | Notes                                       |
|-------------------------------------------------|--------|---------------------------------------------|
| of(timeDifference)                              | ↘️     | Deprecated; use ofTimeDifferenceWithNoGrace |
| ofTimeDifferenceWithNoGrace(timeDifference)     | ✅      | Symmetric window, no grace                  |
| ofTimeDifferenceAndGrace(timeDifference, grace) | ✅      | Symmetric window with grace                 |
| before(timeDifference)                          | ✅      | Asymmetric window (look back)               |
| after(timeDifference)                           | ✅      | Asymmetric window (look forward)            |
| grace(duration)                                 | ↘️     | Deprecated; use ofTimeDifferenceAndGrace    |

**Note:**
`JoinWindows`
configures
time
bounds
for
stream-stream
joins.

### StreamJoined

| Method                                          | Status | Notes                                                |
|-------------------------------------------------|--------|------------------------------------------------------|
| as(name)                                        | ✅      | Create with processor name                           |
| with(keySerde, leftValueSerde, rightValueSerde) | ✅      | Create with Serdes                                   |
| create()                                        | ✅      | Create with defaults                                 |
| withName(name)                                  | ✅      | Set processor name                                   |
| withKeySerde(serde)                             | ✅      | Set key Serde                                        |
| withLeftValueSerde(serde)                       | ✅      | Set left stream value Serde                          |
| withRightValueSerde(serde)                      | ✅      | Set right stream value Serde                         |
| withThisStoreSupplier(storeSupplier)            | ✅      | Custom left window store                             |
| withOtherStoreSupplier(storeSupplier)           | ✅      | Custom right window store                            |
| withLoggingEnabled()                            | ✅      | Enable changelog logging (ADR-026)                   |
| withLoggingEnabled(config)                      | ✅      | Enable changelog logging with topic config (ADR-026) |
| withLoggingDisabled()                           | ✅      | Disable changelog logging (ADR-026)                  |
| withDslStoreSuppliers(suppliers)                | ✅      | Both join stores use same type (ADR-029)             |
| withStoreName(name)                             | ✅      | Set base store name (ADR-026)                        |

**Note:**
`StreamJoined`
configures
stream-stream
joins (
naming,
Serdes,
stores).
Changelog
logging
config
applies
to both
join
stores.

### Repartitioned

| Method                             | Status | Notes                                                                                                                                 |
|------------------------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------|
| as(name)                           | ✅      |                                                                                                                                       |
| streamPartitioner(partitioner)     | ✅      | numPartitions = numLanes                                                                                                              |
| withStreamPartitioner(partitioner) | ✅      |                                                                                                                                       |
| withName(name)                     | ✅      |                                                                                                                                       |
| with(keySerde, valueSerde)         | ✅      | keySerde used for lane assignment hashing                                                                                             |
| withKeySerde()                     | ✅      | Used for lane assignment hashing at sub-topology boundary                                                                             |
| withValueSerde()                   | ✅      | Reserved for future use                                                                                                               |
| withNumberOfPartitions(int)        | 🆕     | KS-compat alias of [withNumberOfLanes](#repartitionedwithnumberoflanes); sets the lane count of the downstream sub-topology (ADR-112) |
| numberOfPartitions(int)            | 🆕     | KS-compat static factory; alias of [numberOfLanes](#repartitionednumberoflanes) (ADR-112)                                             |
| withNumberOfLanes(int)             | 🆕     | StoatFlow extension: sets the lane count of the downstream sub-topology (ADR-112)                                                     |
| numberOfLanes(int)                 | 🆕     | StoatFlow static factory parity: alias of `withNumberOfLanes` (ADR-112)                                                               |
| withLaneQueueCapacity(int)         | 🆕     | StoatFlow extension: sets the per-lane queue capacity of the downstream sub-topology (ADR-113)                                        |

### EmitStrategy

| Method           | Status | Notes                                  |
|------------------|--------|----------------------------------------|
| onWindowUpdate() | ✅      | Default; emit on every update          |
| onWindowClose()  | ✅      | Emit only when window closes (ADR-015) |

**Note:**
`OnWindowClose`
requires
watermark
propagation.
Currently
works in
`TopologyTestDriver`;
production
runtime
integration
pending.

### Suppressed

| Method                                 | Status | Notes                          |
|----------------------------------------|--------|--------------------------------|
| untilWindowCloses(bufferConfig)        | ✅      | For windowed KTables (ADR-015) |
| untilTimeLimit(duration, bufferConfig) | ✅      | Rate-limits per-key updates    |

**Note:**
Requires
watermark
propagation.
Currently
works in
`TopologyTestDriver`;
production
runtime
integration
pending.

*

*KIP-914:
**
`suppress()`
throws
`TopologyValidationException`
for
tables
backed by
versioned
stores.
Versioned
stores
provide
temporal
semantics
that
conflict
with
suppression.

### BufferConfig

| Method              | Status | Notes                             |
|---------------------|--------|-----------------------------------|
| unbounded()         | ✅      | No limits (use with caution)      |
| maxRecords(count)   | ✅      | Bounded by record count           |
| maxBytes(bytes)     | ✅      | Bounded by byte size              |
| emitEarlyWhenFull() | ✅      | Emit instead of evict on overflow |
| shutDownWhenFull()  | ✅      | Shut down on buffer overflow      |

### PunctuationType

| Value           | Status | Notes                                           |
|-----------------|--------|-------------------------------------------------|
| STREAM_TIME     | ✅      | Fires based on event-time watermark advancement |
| WALL_CLOCK_TIME | ✅      | Fires based on system clock                     |

### PunctuatorMode

| Value        | Status | Notes                                                 |
|--------------|--------|-------------------------------------------------------|
| BLOCKING     | 🚀     | Barriers wait for punctuator completion before commit |
| NON_BLOCKING | 🚀     | Barriers proceed independently (default)              |

**Note:**
`PunctuatorMode`
is a
StoatFlow
extension.
Kafka
Streams
punctuators
are
always
non-blocking.

### Punctuator

| Method               | Status | Notes                              |
|----------------------|--------|------------------------------------|
| punctuate(timestamp) | ✅      | Callback invoked at scheduled time |

### Cancellable

| Method   | Status | Notes                                |
|----------|--------|--------------------------------------|
| cancel() | ✅      | Cancel scheduled punctuator or timer |

### TimerService

| Method                                      | Status | Notes                                             |
|---------------------------------------------|--------|---------------------------------------------------|
| registerEventTimeTimer(key, timestamp)      | 🚀     | Fire when watermark >= timestamp (Flink-inspired) |
| registerProcessingTimeTimer(key, timestamp) | 🚀     | Fire at wall-clock time                           |
| deleteEventTimeTimer(key, timestamp)        | 🚀     | Cancel event-time timer                           |
| deleteProcessingTimeTimer(key, timestamp)   | 🚀     | Cancel processing-time timer                      |
| currentWatermark()                          | 🚀     | Current event-time watermark                      |
| currentProcessingTime()                     | 🚀     | Current wall-clock time                           |

*

*
Key-Based
Timers (
Flink-Inspired):
**

```kotlin
// In Processor.process()
context.timerService()
    .registerEventTimeTimer(
        record.key,
        record.timestamp + 60_000
    )

// In Processor.onTimer()
override fun onTimer(
    timestamp: Long,
    key: K,
    context: ProcessorContext<K, V>
) {
    // Timer fires in the key's lane with full state read/write access
    val store =
        context.getStateStore<KeyValueStore<K, State>>(
            "my-store"
        )
    store.put(
        key,
        computeNewState(
            key,
            timestamp
        )
    )
    context.forward(
        Record(
            key,
            result,
            timestamp
        )
    )
}
```

**Note:**
Key-based
timers
fire in
the key's
affinity
lane,
enabling
full
state
read/write
access.
Same (
key,
timestamp)
pair
fires
only
once (
deduplication).

### ScheduledEmitter

| Method        | Status | Notes                                              |
|---------------|--------|----------------------------------------------------|
| emit(context) | 🚀     | Called at scheduled time; emit records via context |

### ScheduledEmitterContext

| Method                         | Status | Notes                                                        |
|--------------------------------|--------|--------------------------------------------------------------|
| forward(key, value)            | 🚀     | Emit record with current wall-clock time                     |
| forward(key, value, timestamp) | 🚀     | Emit record with explicit timestamp                          |
| forward(record)                | 🚀     | Emit record with key, value, timestamp, and headers          |
| getStateStore(name)            | 🚀     | Access read-only state store                                 |
| currentWatermarkMs()           | 🆕     | Current watermark (global event-time progress)               |
| currentStreamTimeMs()          | ✅      | Delegates to `currentWatermarkMs()` (KS compat)              |
| currentSystemTimeMs()          | ✅      | Wall-clock time via `System.currentTimeMillis()` (KS compat) |
| currentWallClockTime()         | 🚀     | Current wall-clock time                                      |

*

*
ScheduledSource
Example:
**

```kotlin
// Interval-based: emit heartbeat every 30 seconds
builder.scheduled<String, String>(
    interval = Duration.ofSeconds(
        30
    ),
    type = PunctuationType.WALL_CLOCK_TIME
) { ctx ->
    ctx.forward(
        "heartbeat",
        "ping"
    )
}

// Cron-based: emit daily at midnight
builder.scheduled<String, String>(
    cron = CronExpression.unix(
        "0 0 * * *"
    )
) { ctx ->
    ctx.forward(
        "daily-job",
        "trigger"
    )
}

// With headers using Record
builder.scheduled<String, String>(
    interval = Duration.ofMinutes(
        1
    ),
    type = PunctuationType.WALL_CLOCK_TIME
) { ctx ->
    val headers =
        RecordHeaders().apply {
            add(
                "source",
                "scheduler".toByteArray()
            )
        }
    ctx.forward(
        Record(
            "key",
            "value",
            ctx.currentWallClockTime(),
            headers
        )
    )
}
```

### CronExpression

| Method                                  | Status | Notes                                            |
|-----------------------------------------|--------|--------------------------------------------------|
| unix(expression)                        | 🚀     | Parse Unix cron (5 fields: min hour dom mon dow) |
| quartz(expression)                      | 🚀     | Parse Quartz cron (6 fields with seconds)        |
| spring(expression)                      | 🚀     | Parse Spring cron (6 fields with seconds)        |
| parse(expression, cronType)             | 🚀     | Parse with explicit type                         |
| nextExecutionTime(afterMs, zoneId?)     | 🚀     | Calculate next fire time                         |
| timeUntilNextExecution(fromMs, zoneId?) | 🚀     | Duration until next fire                         |

**Note:**
`CronExpression`
wraps the
cron-utils
library.
Cron-based
scheduling
always
uses
wall-clock
time.

### WindowedSerdes

| Method                                   | Status | Notes                                                          |
|------------------------------------------|--------|----------------------------------------------------------------|
| timeWindowedSerdeFrom(Class)             | ⚠️     | Takes `Serde<T>` instead of `Class<T>` (no serde registry)     |
| timeWindowedSerdeFrom(Class, windowSize) | ⚠️     | Takes `Serde<T>` only; window size encoded in serialized bytes |
| sessionWindowedSerdeFrom(Class)          | ⚠️     | Takes `Serde<T>` instead of `Class<T>`                         |
| TimeWindowedSerde(inner)                 | ✅      | `WindowedSerdes.TimeWindowedSerde(serde)`                      |
| TimeWindowedSerde(inner, windowSize)     | ⚠️     | Window size not required (encoded in serialized bytes)         |
| SessionWindowedSerde(inner)              | ✅      | `WindowedSerdes.SessionWindowedSerde(serde)`                   |

**Note:**
StoatFlow
factory
methods
take
`Serde<T>`
instead
of KS's
`Class<T>` (
no serde
registry).
Window
size
parameter
is
omitted
because
window
bounds
are
encoded
in the
serialized
bytes
directly.
The
`forChangelog()`
method on
`TimeWindowedSerde`
is not
needed (
StoatFlow
uses
identical
encoding
for
client
and
changelog).

## State Stores

### WindowStore

Kafka
Streams'
`org.apache.kafka.streams.state.WindowStore`
compatible
interface.

| Method                                                            | Status | Notes                                      |
|-------------------------------------------------------------------|--------|--------------------------------------------|
| put(key, value, windowStartTime)                                  | ✅      |                                            |
| fetch(key, windowStartTime)                                       | ✅      | Point lookup                               |
| fetch(key, timeFrom: Long, timeTo: Long)                          | ✅      | Single key time range                      |
| fetch(key, timeFrom: Instant, timeTo: Instant)                    | ✅      | Instant variant                            |
| backwardFetch(key, timeFrom: Long, timeTo: Long)                  | ✅      | Reverse iteration                          |
| backwardFetch(key, timeFrom: Instant, timeTo: Instant)            | ✅      | Instant variant                            |
| fetch(keyFrom, keyTo, timeFrom: Long, timeTo: Long)               | ✅      | Key range + time range                     |
| fetch(keyFrom, keyTo, timeFrom: Instant, timeTo: Instant)         | ✅      | Instant variant                            |
| backwardFetch(keyFrom, keyTo, timeFrom: Long, timeTo: Long)       | ✅      | Reverse key range                          |
| backwardFetch(keyFrom, keyTo, timeFrom: Instant, timeTo: Instant) | ✅      | Instant variant                            |
| fetchAll(timeFrom: Long, timeTo: Long)                            | ✅      | All keys time range                        |
| fetchAll(timeFrom: Instant, timeTo: Instant)                      | ✅      | Instant variant                            |
| backwardFetchAll(timeFrom: Long, timeTo: Long)                    | ✅      | Reverse all keys                           |
| backwardFetchAll(timeFrom: Instant, timeTo: Instant)              | ✅      | Instant variant                            |
| all()                                                             | ✅      | All entries                                |
| backwardAll()                                                     | ✅      | Reverse iteration                          |
| windowSizeMs                                                      | 🚀     | StoatFlow extension for window size access |
| retentionMs                                                       | 🚀     | StoatFlow extension for retention access   |
| expireWindows(watermark)                                          | 🚀     | StoatFlow extension for manual expiration  |
| approximateNumEntries()                                           | 🚀     | StoatFlow extension for entry count        |

**Note:**
Key range
queries
require
`K : Comparable<K>`.
All
Instant
variants
are
default
interface
methods
that
delegate
to Long
variants.

### ReadOnlyWindowStore

Read-only
interface
for
Interactive
Queries (
IQ)access
pattern.

| Method                             | Status | Notes                                                    |
|------------------------------------|--------|----------------------------------------------------------|
| All query methods from WindowStore | ✅      | 16 query methods total                                   |
| containsKey(key, windowStartTime)  | 🚀     | StoatFlow extension (ADR-050); Long and Instant variants |

### ReadOnlySessionStore

Read-only
interface
for
Interactive
Queries (
IQ)access
pattern.

| Method                                   | Status | Notes                                                    |
|------------------------------------------|--------|----------------------------------------------------------|
| All query methods from SessionStore      | ✅      | Query methods for IQ access                              |
| containsSession(key, startTime, endTime) | 🚀     | StoatFlow extension (ADR-050); Long and Instant variants |

### SessionStore

| Method                                                                 | Status | Notes                                        |
|------------------------------------------------------------------------|--------|----------------------------------------------|
| put(sessionKey, aggregate)                                             | ✅      | Windowed key                                 |
| remove(sessionKey)                                                     | ✅      | Windowed key                                 |
| fetchSession(key, startTime, endTime)                                  | ✅      | Long and Instant variants                    |
| findSessions(key, earliestEndTime, latestStartTime)                    | ✅      | Long and Instant variants                    |
| backwardFindSessions(key, earliestEndTime, latestStartTime)            | ✅      | Long and Instant variants                    |
| findSessions(keyFrom, keyTo, earliestEndTime, latestStartTime)         | ✅      | Key range queries; Long and Instant variants |
| backwardFindSessions(keyFrom, keyTo, earliestEndTime, latestStartTime) | ✅      | Key range queries; Long and Instant variants |
| fetch(key)                                                             | ✅      | All sessions for single key                  |
| backwardFetch(key)                                                     | ✅      | Reverse order                                |
| fetch(keyFrom, keyTo)                                                  | ✅      | Key range queries                            |
| backwardFetch(keyFrom, keyTo)                                          | ✅      | Reverse order                                |
| findAll()                                                              | 🚀     | StoatFlow extension                          |
| expireSessions(watermark)                                              | 🚀     | StoatFlow extension                          |
| approximateNumEntries()                                                | 🚀     | StoatFlow extension                          |

### KeyValueStore

| Method                               | Status | Notes                                                                                               |
|--------------------------------------|--------|-----------------------------------------------------------------------------------------------------|
| get(key)                             | ✅      |                                                                                                     |
| put(key, value)                      | ✅      |                                                                                                     |
| putIfAbsent(key, value)              | ✅      |                                                                                                     |
| putAll(entries)                      | ✅      |                                                                                                     |
| delete(key)                          | ✅      |                                                                                                     |
| compute(key, remappingFunction)      | 🚀     | StoatFlow extension: atomic read-compute-write (ADR-051); Kotlin lambda + Java BiFunction overloads |
| merge(key, value, remappingFunction) | 🚀     | StoatFlow extension: atomic merge (ADR-051); Kotlin lambda + Java BiFunction overloads              |
| all()                                | ✅      | Read-your-writes semantics                                                                          |
| range(from, to)                      | ✅      | Read-your-writes semantics                                                                          |
| reverseAll()                         | ✅      | Read-your-writes semantics                                                                          |
| reverseRange(from, to)               | ✅      | Read-your-writes semantics                                                                          |
| approximateNumEntries()              | ✅      |                                                                                                     |
| prefixScan()                         | ✅      | KIP-614; read-your-writes semantics                                                                 |

### ReadOnlyKeyValueStore

Read-only
interface
for
Interactive
Queries (
IQ)access
pattern.

| Method                         | Status | Notes                                                                |
|--------------------------------|--------|----------------------------------------------------------------------|
| get(key)                       | ✅      |                                                                      |
| containsKey(key)               | 🚀     | StoatFlow extension (ADR-050); bloom filter optimization for RocksDB |
| all()                          | ✅      |                                                                      |
| range(from, to)                | ✅      |                                                                      |
| reverseAll()                   | ✅      |                                                                      |
| reverseRange(from, to)         | ✅      |                                                                      |
| approximateNumEntries()        | ✅      |                                                                      |
| prefixScan(prefix, serializer) | ✅      | KIP-614                                                              |

### Stores Factory

Kafka
Streams'
`org.apache.kafka.streams.state.Stores`
factory
methods.

| Method                                                               | Status | Notes                                                    |
|----------------------------------------------------------------------|--------|----------------------------------------------------------|
| persistentKeyValueStore(name)                                        | ✅      | RocksDB key-value store                                  |
| inMemoryKeyValueStore(name)                                          | ✅      | In-memory key-value store                                |
| persistentWindowStore(name, retention, windowSize, retainDuplicates) | ✅      | RocksDB window store                                     |
| inMemoryWindowStore(name, retention, windowSize, retainDuplicates)   | ✅      | In-memory window store                                   |
| persistentSessionStore(name, retentionPeriod)                        | ✅      | RocksDB session store                                    |
| inMemorySessionStore(name, retentionPeriod)                          | ✅      | In-memory session store                                  |
| persistentTimestampedKeyValueStore(name)                             | ✅      | Timestamped key-value store (KIP-258, ADR-027)           |
| persistentTimestampedWindowStore(name, ...)                          | ✅      | Timestamped window store (KIP-258, ADR-027)              |
| inMemoryTimestampedKeyValueStore(name)                               | ✅      | In-memory timestamped key-value store (KIP-258, ADR-027) |
| inMemoryTimestampedWindowStore(name, ...)                            | ✅      | In-memory timestamped window store (KIP-258, ADR-027)    |
| persistentVersionedKeyValueStore(name, historyRetention)             | ✅      | Versioned store - RocksDB (KIP-889, ADR-033)             |
| inMemoryVersionedKeyValueStore(name, historyRetention)               | ✅      | Versioned store - In-memory (KIP-889, ADR-033)           |
| lruMap(name, maxCacheSize)                                           | ✅      | LRU cache store with bounded entries                     |
| keyValueStoreBuilder(supplier, keySerde, valueSerde)                 | ✅      | Store builder factory                                    |
| windowStoreBuilder(supplier, keySerde, valueSerde)                   | ✅      | Store builder factory                                    |
| sessionStoreBuilder(supplier, keySerde, aggSerde)                    | ✅      | Store builder factory                                    |

### KeyLockManager

Striped
lock
utility
for
multi-key
atomic
sections
in
user-defined
Processors (
ADR-051).
Not in
Kafka
Streams.

| Method                 | Status | Notes                                            |
|------------------------|--------|--------------------------------------------------|
| withLock(key, block)   | 🚀     | Single-key lock scope                            |
| withLocks(keys, block) | 🚀     | Multi-key lock scope with deadlock-free ordering |

**Note:**
`KeyLockManager`
is a
user-managed
utility —
instantiate
one per
store (or
share
across
stores
for
cross-store
atomicity).
Uses
deterministic
lock
ordering
to
prevent
deadlocks.
Intended
for
Processor
API use
cases
requiring
multi-key
read-modify-write.

## Interactive Queries API

Interactive
Queries (
IQ)
provide
read-only
access to
state
stores
for
external
queries.

### StoatFlow Methods

| Method                      | Status | Notes                                            |
|-----------------------------|--------|--------------------------------------------------|
| store(StoreQueryParameters) | ✅      | Returns read-only store view (ADR-025)           |
| storeNames()                | ✅      | List available store names                       |
| pause()                     | 🚀     | Pause processing with drain semantics (ADR-045)  |
| unpause()                   | 🚀     | Resume processing after pause (ADR-045)          |
| awaitState(state, timeout)  | 🚀     | Wait for target state with timeout (ADR-045)     |
| stateTransitionHistory()    | 🚀     | Returns recent state transitions with timestamps |

### StoreQueryParameters

| Method                      | Status | Notes                             |
|-----------------------------|--------|-----------------------------------|
| fromNameAndType(name, type) | ✅      | Factory method                    |
| enableStaleStores()         | ✅      | Allow queries during RESTORING    |
| withPartition(partition)    | 🛑     | N/A for single-instance (ADR-007) |

### QueryableStoreTypes

| Method                     | Status | Notes                                                                     |
|----------------------------|--------|---------------------------------------------------------------------------|
| keyValueStore()            | ✅      | Returns ReadOnlyKeyValueStore                                             |
| windowStore()              | ✅      | Returns ReadOnlyWindowStore                                               |
| sessionStore()             | ✅      | Returns ReadOnlySessionStore                                              |
| timestampedKeyValueStore() | ✅      | Returns ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>> (KIP-258, ADR-027) |
| timestampedWindowStore()   | ✅      | Returns ReadOnlyWindowStore<K, ValueAndTimestamp<V>> (KIP-258, ADR-027)   |
| versionedKeyValueStore()   | ✅      | Returns ReadOnlyVersionedKeyValueStore<K, V> (KIP-889, ADR-033)           |

**Note:**
StoatFlow's
global
state
model (
ADR-007)
eliminates
partition
routing -
all state
is
locally
accessible.
No
`withPartition()`
or RPC
needed.

## Exception Handlers

| Handler                         | Status | Notes                                     |
|---------------------------------|--------|-------------------------------------------|
| DeserializationExceptionHandler | ✅      | LogAndFail (default), LogAndContinue, DLQ |
| ProcessingExceptionHandler      | ✅      | LogAndFail (default), LogAndContinue, DLQ |
| ProductionExceptionHandler      | ✅      | Intelligent retry logic                   |

## Lifecycle

| Feature              | Status | Notes                                                                      |
|----------------------|--------|----------------------------------------------------------------------------|
| State enum           | ✅      | CREATED, STARTING, RESTORING, RUNNING, STOPPING, STOPPED, ERROR            |
| StateListener        | ✅      | setStateListener() with Kotlin lambda and Java BiConsumer support          |
| StateRestoreListener | ✅      | Full Kafka Streams compatible: onRestoreStart/onBatchRestored/onRestoreEnd |

## Configuration

| Aspect                     | Status | Notes                                                                                                                                                    |
|----------------------------|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| StreamsConfig builder      | ✅      | Type-safe builder pattern                                                                                                                                |
| KafkaClientSupplier        | ✅      | Custom client factory for instrumentation/testing                                                                                                        |
| DefaultKafkaClientSupplier | ✅      | Standard implementation creating KafkaConsumer/KafkaProducer/Admin                                                                                       |
| RocksDB config             | ✅      | Bounded memory by default (ADR-008)                                                                                                                      |
| Serdes                     | ✅      | Compatible                                                                                                                                               |
| Default key/value Serdes   | ✅      | `StreamsConfig.defaultKeySerde`/`defaultValueSerde` propagate through DSL; `configureDefaultSerdes()` calls `serde.configure()` with Schema Registry URL |
| Consumer/Producer config   | ✅      | Pass-through maps                                                                                                                                        |

## Key Architectural Differences

| Aspect             | Kafka Streams              | StoatFlow                                    | ADR              |
|--------------------|----------------------------|----------------------------------------------|------------------|
| Scaling            | Multiple instances         | Single instance only                         | ADR-001          |
| State model        | Partition-scoped           | Global                                       | ADR-007          |
| Repartitioning     | Kafka topics               | In-memory queues                             | ADR-010          |
| Table joins        | Require co-partitioning    | No co-partitioning needed                    | ADR-007          |
| Key-changing ops   | Internal repartition       | Sub-topology boundary                        | ADR-005          |
| RocksDB memory     | Unbounded default          | Bounded default (256MB)                      | ADR-008          |
| Entry point        | KafkaStreams               | StoatFlow                                    | ADR-011          |
| Exactly-once       | Complex config             | Default                                      | ADR-004          |
| Window closure     | Stream-time based          | Watermark-based                              | ADR-015, ADR-016 |
| Grace period       | Retention after close      | Extends acceptance window                    | ADR-016          |
| Watermark strategy | N/A                        | Flink-style WatermarkStrategy                | ADR-015          |
| Punctuators        | Partition-scoped, writable | Global, read-only state                      | ADR-039          |
| Key-based timers   | N/A                        | Flink-inspired TimerService                  | ADR-039          |
| Scheduled sources  | N/A                        | Interval & cron-based emission               | ADR-039          |
| Runtime control    | N/A                        | pause()/unpause() for DRAINING/PAUSED states | ADR-045          |

## Related ADRs

-

ADR-001:
Single
Instance
Architecture

-

ADR-004:
Commit-Barrier
Exactly-Once

-

ADR-005:
Sub-Topology
Boundaries

-

ADR-006:
Timestamp-Ordered
Processing

-

ADR-007:
Global
State
Model

-

ADR-008:
RocksDB
Persistent
State

-

ADR-010:
In-Memory
Repartitioning

-

ADR-011:
Kafka
Streams
DSL
Compatibility

-

ADR-015:
Flink-Style
Watermark
Strategy

-

ADR-016:
Window
Lifecycle
and
Event-Time
Semantics

-

ADR-022:
Dynamic
Topic
Routing

-

ADR-023:
KTable
Materialized
Transformations

-

ADR-024:
KGroupedTable
Changelog
Semantics

-

ADR-025:
Interactive
Queries

-

ADR-027:
Timestamped
Stores (
KIP-258)

-

ADR-029:
DslStoreSuppliers

-

ADR-033:
Versioned
Key-Value
Store (
KIP-889)

-

ADR-034:
KIP-914
Versioned
Store
Processor
Semantics

-

ADR-039:
Timer and
Punctuator
System

-

ADR-045:
Pause/Unpause
and
Startup
Optimization

-

ADR-051:
Atomic
State
Store
Operations
