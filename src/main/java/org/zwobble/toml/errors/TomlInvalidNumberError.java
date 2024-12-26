package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidNumberError extends TomlParseError {
    private final String numberString;

    public TomlInvalidNumberError(String numberString, SourceRange sourceRange) {
        super("Invalid number: " + numberString, sourceRange);
        this.numberString = numberString;
    }

    public String numberString() {
        return numberString;
    }
}