package io.axual.ksml.definition;

import java.time.Duration;

public record TumblingWindowDefinition(Duration duration, Duration grace) {
}
