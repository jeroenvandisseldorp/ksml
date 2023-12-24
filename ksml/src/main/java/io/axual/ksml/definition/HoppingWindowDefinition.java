package io.axual.ksml.definition;

import java.time.Duration;

public record HoppingWindowDefinition(Duration duration, Duration advanceBy, Duration grace) {
}
