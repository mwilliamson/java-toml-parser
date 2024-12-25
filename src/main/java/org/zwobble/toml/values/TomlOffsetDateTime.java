package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.OffsetDateTime;

public record TomlOffsetDateTime(
    OffsetDateTime value,
    SourceRange sourceRange
) implements TomlValue {
}