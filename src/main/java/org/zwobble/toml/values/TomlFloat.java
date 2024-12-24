package org.zwobble.toml.values;

import org.zwobble.toml.sources.SourceRange;

public record TomlFloat(double value, SourceRange sourceRange) implements TomlValue {
}
