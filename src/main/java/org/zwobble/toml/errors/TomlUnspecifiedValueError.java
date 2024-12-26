package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlUnspecifiedValueError extends TomlParseError {
    private final SourceRange sourceRange;

    public TomlUnspecifiedValueError(SourceRange sourceRange) {
        super("The value for a key was not specified");
        this.sourceRange = sourceRange;
    }

    public SourceRange sourceRange() {
        return sourceRange;
    }
}