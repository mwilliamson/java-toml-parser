package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

import java.time.LocalDate;

/**
 * A TOML local date.
 * @param value The local date represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlLocalDate(
    LocalDate value,
    SourceRange sourceRange
) implements TomlValue {
}
