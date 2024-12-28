package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlUnclosedStringError extends TomlParseError {
    public TomlUnclosedStringError(SourceRange sourceRange) {
        super("Unclosed string", sourceRange);
    }
}
