package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.LocalTime;

public record TomlLocalTime(
    LocalTime value,
    SourceRange sourceRange
) implements TomlValue {
}