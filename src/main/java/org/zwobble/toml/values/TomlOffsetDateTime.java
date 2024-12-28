package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.OffsetDateTime;

/**
 * A TOML offset date-time.
 * @param value The offset date-time represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlOffsetDateTime(
    OffsetDateTime value,
    SourceRange sourceRange
) implements TomlValue {
}
