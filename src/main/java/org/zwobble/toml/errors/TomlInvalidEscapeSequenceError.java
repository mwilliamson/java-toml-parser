package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidEscapeSequenceError extends TomlParseError {
    public TomlInvalidEscapeSequenceError(SourceRange sourceRange) {
        super("Invalid escape sequence", sourceRange);
    }
}
