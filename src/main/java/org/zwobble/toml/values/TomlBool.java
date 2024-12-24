package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

public record TomlBool(
    boolean value,
    SourceRange sourceRange
) implements TomlValue {

}
