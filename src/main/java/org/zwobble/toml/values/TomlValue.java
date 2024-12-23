package org.zwobble.toml.values;

public sealed interface TomlValue permits TomlBool, TomlInt, TomlString, TomlTable {
}
