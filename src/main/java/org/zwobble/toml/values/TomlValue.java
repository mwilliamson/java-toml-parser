package org.zwobble.toml.values;

public sealed interface TomlValue permits TomlBool, TomlString, TomlTable {
}
