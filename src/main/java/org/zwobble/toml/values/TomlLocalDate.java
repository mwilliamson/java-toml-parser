package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.LocalDate;

public record TomlLocalDate(
    LocalDate value,
    SourceRange sourceRange
) implements TomlValue {
}