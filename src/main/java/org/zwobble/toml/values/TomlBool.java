package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

/**
 * A TOML boolean, either true or false.
 * @param value The boolean value represented by the TOML value.
 * @param sourceRange The portion of the TOML document that this value was
 *                    parsed from.
 */
public record TomlBool(
    boolean value,
    SourceRange sourceRange
) implements TomlValue {

}
