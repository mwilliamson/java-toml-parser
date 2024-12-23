package org.zwobble.toml.errors;

public class TomlParseError extends RuntimeException {
    public TomlParseError(String message) {
        super(message);
    }
}
