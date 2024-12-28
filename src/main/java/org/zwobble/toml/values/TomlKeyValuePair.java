package org.zwobble.toml.values;

/**
 * A mapping from a string key to a TOML value in a TOML table.
 * @param key The string key to map from.
 * @param value The TOML value to map to.
 */
public record TomlKeyValuePair(String key, TomlValue value) {
    public static TomlKeyValuePair of(String key, TomlValue value) {
        return new TomlKeyValuePair(key, value);
    }
}
