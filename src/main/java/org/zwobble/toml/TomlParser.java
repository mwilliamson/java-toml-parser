package org.zwobble.toml;

import org.zwobble.toml.values.TomlTable;

import java.nio.file.Path;

public class TomlParser {
    private TomlParser() {
    }

    public static TomlTable parseFile(Path path) {
        return new TomlTable();
    }
}
