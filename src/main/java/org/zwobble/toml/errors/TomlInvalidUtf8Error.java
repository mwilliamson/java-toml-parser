package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidUtf8Error extends TomlParseError {
    public TomlInvalidUtf8Error(SourceRange sourceRange) {
        super("Invalid UTF-8", sourceRange);
    }
}
