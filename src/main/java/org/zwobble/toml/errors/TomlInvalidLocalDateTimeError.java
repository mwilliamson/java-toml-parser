package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidLocalDateTimeError extends TomlParseError {
    private final String localDateTimeString;

    public TomlInvalidLocalDateTimeError(String localDateTimeString, SourceRange sourceRange) {
        super("Invalid local date-time: " + localDateTimeString, sourceRange);
        this.localDateTimeString = localDateTimeString;
    }

    public String localDateTimeString() {
        return localDateTimeString;
    }
}