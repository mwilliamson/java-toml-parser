package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlInvalidLocalTimeError extends TomlParseError {
    private final String localTimeString;

    public TomlInvalidLocalTimeError(String localTimeString, SourceRange sourceRange) {
        super("Invalid local time: " + localTimeString, sourceRange);
        this.localTimeString = localTimeString;
    }

    public String localTimeString() {
        return localTimeString;
    }
}