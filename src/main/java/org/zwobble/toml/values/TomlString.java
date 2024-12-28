package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

/**
 * A TOML string.
 * @param value The string represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlString(String value, SourceRange sourceRange) implements TomlValue {
}
