package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

import static org.zwobble.toml.parser.UnicodeCodePoints.formatCodePoint;

public class TomlUnexpectedControlCharacterError extends TomlParseError {
    private final int controlCharacter;

    public TomlUnexpectedControlCharacterError(int controlCharacter, SourceRange sourceRange) {
        super(
            "Unexpected control character: " + formatCodePoint(controlCharacter),
            sourceRange
        );
        this.controlCharacter = controlCharacter;
    }

    public int controlCharacter() {
        return controlCharacter;
    }
}
