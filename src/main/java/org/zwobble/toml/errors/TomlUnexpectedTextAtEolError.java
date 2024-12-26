package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlUnexpectedTextAtEolError extends TomlParseError {
    private final String unexpectedText;

    public TomlUnexpectedTextAtEolError(String unexpectedText, SourceRange sourceRange) {
        super("Unexpected text after value: " + unexpectedText, sourceRange);
        this.unexpectedText = unexpectedText;
    }

    public String unexpectedText() {
        return unexpectedText;
    }
}