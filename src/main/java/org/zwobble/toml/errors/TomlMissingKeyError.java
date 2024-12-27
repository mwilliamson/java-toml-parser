package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlMissingKeyError extends TomlParseError {
    public TomlMissingKeyError(SourceRange sourceRange) {
        super("Missing a key", sourceRange);
    }
}
