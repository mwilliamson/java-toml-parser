package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.LocalDateTime;

/**
 * A TOML local date-time.
 * @param value The local date-time represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlLocalDateTime(
    LocalDateTime value,
    SourceRange sourceRange
) implements TomlValue {
}
