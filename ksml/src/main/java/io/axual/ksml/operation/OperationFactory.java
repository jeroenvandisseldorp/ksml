package io.axual.ksml.operation;

import io.axual.ksml.exception.TopologyException;

public class OperationFactory {
    private OperationFactory() {
    }

    public static StreamOperation createOperation(OperationDefinition definition) {
        return switch (definition) {
            case AggregateOperationDefinition def -> new AggregateOperation(def);
            case AsOperationDefinition def -> new AsOperation(def);
            case BranchOperationDefinition def -> new BranchOperation(def);
            case CogroupOperationDefinition def -> new CogroupOperation(def);
            case ConvertKeyOperationDefinition def -> new ConvertKeyOperation(def);
            case ConvertKeyValueOperationDefinition def -> new ConvertKeyValueOperation(def);
            case ConvertValueOperationDefinition def -> new ConvertValueOperation(def);
            case CountOperationDefinition def -> new CountOperation(def);
            case FilterNotOperationDefinition def -> new FilterNotOperation(def);
            case FilterOperationDefinition def -> new FilterOperation(def);
            case GroupByKeyDefinition def -> new GroupByKeyOperation(def);
            case GroupByOperationDefinition def -> new GroupByOperation(def);
            case JoinWithGlobalTableOperationDefinition def -> new JoinWithGlobalTableOperation(def);
            case JoinWithStreamOperationDefinition def -> new JoinWithStreamOperation(def);
            case JoinWithTableOperationDefinition def -> new JoinWithTableOperation(def);
            case LeftJoinWithGlobalTableOperationDefinition def -> new LeftJoinWithGlobalTableOperation(def);
            case LeftJoinWithStreamOperationDefinition def -> new LeftJoinWithStreamOperation(def);
            case LeftJoinWithTableOperationDefinition def -> new LeftJoinWithTableOperation(def);
            case MergeOperationDefinition def -> new MergeOperation(def);
            case OuterJoinWithStreamOperationDefinition def -> new OuterJoinWithStreamOperation(def);
            case OuterJoinWithTableOperationDefinition def -> new OuterJoinWithTableOperation(def);
            case PeekOperationDefinition def -> new PeekOperation(def);
            case ReduceOperationDefinition def -> new ReduceOperation(def);
            case RepartitionOperationDefinition def -> new RepartitionOperation(def);
            case SuppressOperationDefinition def -> new SuppressOperation(def);
            case ToOperationDefinition def -> new ToOperation(def);
            case ToStreamOperationDefinition def -> new ToStreamOperation(def);
            case ToTableOperationDefinition def -> new ToTableOperation(def);
            case ToTopicNameExtractorOperationDefinition def -> new ToTopicNameExtractorOperation(def);
            case TransformKeyOperationDefinition def -> new TransformKeyOperation(def);
            case TransformKeyValueOperationDefinition def -> new TransformKeyValueOperation(def);
            case TransformKeyValueToKeyValueListOperationDefinition def ->
                    new TransformKeyValueToKeyValueListOperation(def);
            case TransformKeyValueToValueListOperationDefinition def -> new TransformKeyValueToValueListOperation(def);
            case TransformMetadataOperationDefinition def -> new TransformMetadataOperation(def);
            case TransformValueOperationDefinition def -> new TransformValueOperation(def);
            case WindowBySessionOperationDefinition def -> new WindowBySessionOperation(def);
            case WindowByTimeOperationDefinition def -> new WindowByTimeOperation(def);
            default -> throw new TopologyException("Unknown operation type: " + definition.operationConfig().name());
        };
    }
}
