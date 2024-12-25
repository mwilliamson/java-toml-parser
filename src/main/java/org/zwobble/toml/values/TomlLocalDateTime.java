package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.LocalDateTime;

public record TomlLocalDateTime(
    LocalDateTime value,
    SourceRange sourceRange
) implements TomlValue {
}