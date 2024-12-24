package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

public record TomlInt(long value, SourceRange sourceRange) implements TomlValue {

}
