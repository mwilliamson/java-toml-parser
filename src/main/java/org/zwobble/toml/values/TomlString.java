package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

public record TomlString(String value, SourceRange sourceRange) implements TomlValue {
}
