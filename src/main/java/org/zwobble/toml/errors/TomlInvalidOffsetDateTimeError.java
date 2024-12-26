package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidOffsetDateTimeError extends TomlParseError {
    private final String offsetDateTimeString;

    public TomlInvalidOffsetDateTimeError(String offsetDateTimeString, SourceRange sourceRange) {
        super("Invalid offset date-time: " + offsetDateTimeString, sourceRange);
        this.offsetDateTimeString = offsetDateTimeString;
    }

    public String offsetDateTimeString() {
        return offsetDateTimeString;
    }
}