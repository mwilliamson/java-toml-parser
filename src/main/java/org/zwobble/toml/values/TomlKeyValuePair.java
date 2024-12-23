package org.zwobble.toml.values;

public record TomlKeyValuePair(String key, TomlValue value) {
    public static TomlKeyValuePair of(String key, TomlValue value) {
        return new TomlKeyValuePair(key, value);
    }
}
