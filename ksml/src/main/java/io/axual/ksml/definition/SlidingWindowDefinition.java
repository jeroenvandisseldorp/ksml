package io.axual.ksml.definition;

import java.time.Duration;

public record SlidingWindowDefinition(Duration timeDifference, Duration grace) {
}
