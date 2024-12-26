package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlParseError extends RuntimeException {
    private final SourceRange sourceRange;

    public TomlParseError(String message, SourceRange sourceRange) {
        super(message);
        this.sourceRange = sourceRange;
    }

    public SourceRange sourceRange() {
        return sourceRange;
    }
}