package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlKeyValuePairMissingEqualsSignError extends TomlParseError {
    private final String actual;

    public TomlKeyValuePairMissingEqualsSignError(String actual, SourceRange sourceRange) {
        super(
            String.format("Expected '=' but got '{0}'", actual),
            sourceRange
        );
        this.actual = actual;
    }

    public String actual() {
        return actual;
    }
}