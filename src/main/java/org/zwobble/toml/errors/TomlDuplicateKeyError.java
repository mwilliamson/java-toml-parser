package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlDuplicateKeyError extends TomlParseError {
    public TomlDuplicateKeyError(String key, SourceRange sourceRange) {
        super("Duplicate key: " + key, sourceRange);
    }
}
