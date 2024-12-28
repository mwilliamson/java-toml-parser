package org.zwobble.toml.values;

/**
 * A value in a TOML document.
 * <p>
 * This is a sealed interface, allowing exhaustive matching against the possible
 * types of value.
 */
public sealed interface TomlValue permits TomlArray, TomlBool, TomlFloat, TomlInt, TomlLocalDate, TomlLocalDateTime, TomlLocalTime, TomlOffsetDateTime, TomlString, TomlTable {

}
