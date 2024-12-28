package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

/**
 * A TOML float.
 * @param value The floating point value represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlFloat(double value, SourceRange sourceRange) implements TomlValue {
}
