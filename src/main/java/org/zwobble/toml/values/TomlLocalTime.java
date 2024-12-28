package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.LocalTime;

/**
 * A TOML local time.
 * @param value The local time represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlLocalTime(
    LocalTime value,
    SourceRange sourceRange
) implements TomlValue {
}
