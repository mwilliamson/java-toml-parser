package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlDuplicateKeyError extends TomlParseError {
    private final String key;

    public TomlDuplicateKeyError(String key, SourceRange sourceRange) {
        super("Duplicate key: " + key, sourceRange);
        this.key = key;
    }

    public String key() {
        return key;
    }
}
