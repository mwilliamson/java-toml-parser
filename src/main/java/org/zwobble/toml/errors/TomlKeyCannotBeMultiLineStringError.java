package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlKeyCannotBeMultiLineStringError extends TomlParseError {
    public TomlKeyCannotBeMultiLineStringError(SourceRange sourceRange) {
        super("Keys cannot be multi-line strings", sourceRange);
    }
}
