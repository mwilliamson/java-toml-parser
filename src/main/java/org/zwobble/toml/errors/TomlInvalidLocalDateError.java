package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidLocalDateError extends TomlParseError {
    private final String localDateString;

    public TomlInvalidLocalDateError(String localDateString, SourceRange sourceRange) {
        super("Invalid local date: " + localDateString, sourceRange);
        this.localDateString = localDateString;
    }

    public String localDateString() {
        return localDateString;
    }
}