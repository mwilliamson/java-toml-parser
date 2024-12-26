package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlUnspecifiedValueError extends TomlParseError {
    public TomlUnspecifiedValueError(SourceRange sourceRange) {
        super("The value for a key was not specified", sourceRange);
    }
}