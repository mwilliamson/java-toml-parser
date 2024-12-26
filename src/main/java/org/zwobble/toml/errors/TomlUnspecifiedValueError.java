package org.zwobble.toml.errors;

public class TomlUnspecifiedValueError extends TomlParseError {
    public TomlUnspecifiedValueError() {
        super("The value for a key was not specified");
    }
}