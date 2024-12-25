package org.zwobble.toml.values;

public sealed interface TomlValue permits TomlArray, TomlBool, TomlFloat, TomlInt, TomlLocalDate, TomlOffsetDateTime, TomlString, TomlTable {

}