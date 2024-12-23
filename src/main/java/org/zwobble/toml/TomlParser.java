package org.zwobble.toml;

import org.zwobble.toml.errors.TomlParseError;
import org.zwobble.toml.values.TomlBool;
import org.zwobble.toml.values.TomlKeyValuePair;
import org.zwobble.toml.values.TomlTable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class TomlParser {
    private TomlParser() {
    }

    public static TomlTable parseFile(Path path) throws IOException {
        try (var reader = new FileReader(path.toFile())) {
            var keyValuePairs = new ArrayList<TomlKeyValuePair>();

            // TODO: handle surrogate pairs
            var codePoint = reader.read();

            while (true) {
                if (codePoint == -1) {
                    return new TomlTable(keyValuePairs);
                }

                if (isBareKeyCharacter(codePoint)) {
                    var key = new StringBuilder();
                    while (isBareKeyCharacter(codePoint)) {
                        key.appendCodePoint(codePoint);
                        codePoint = reader.read();
                    }

                    while (isTomlWhitespace(codePoint)) {
                        codePoint = reader.read();
                    }

                    if (codePoint != '=') {
                        throw new TomlParseError(String.format(
                            "Expected '=' but got %s",
                            formatCodePoint(codePoint)
                        ));
                    }
                    codePoint = reader.read();

                    while (isTomlWhitespace(codePoint)) {
                        codePoint = reader.read();
                    }

                    if (codePoint == 't') {
                        codePoint = reader.read();
                        codePoint = reader.read();
                        codePoint = reader.read();
                        codePoint = reader.read();
                        keyValuePairs.add(TomlKeyValuePair.of(key.toString(), new TomlBool(true)));
                    } else if (codePoint == 'f') {
                        codePoint = reader.read();
                        codePoint = reader.read();
                        codePoint = reader.read();
                        codePoint = reader.read();
                        codePoint = reader.read();
                        keyValuePairs.add(TomlKeyValuePair.of(key.toString(), new TomlBool(false)));
                    } else {
                        throw new TomlParseError("??");
                    }

                    if (codePoint != '\n') {
                        throw new TomlParseError(String.format(
                            "Expected newline but got %s",
                            formatCodePoint(codePoint)
                        ));
                    }
                    codePoint = reader.read();
                } else {
                    throw new TomlParseError("TODO");
                }
            }
        }
    }

    private static boolean isTomlWhitespace(int character) {
        return character == 0x09 || character == 0x20;
    }

    private static boolean isBareKeyCharacter(int character) {
        return (character >= 0x30 && character <= 0x39) ||
            (character >= 0x41 && character <= 0x5a) ||
            (character >= 0x61 && character <= 0x7a);
    }

    private static String formatCodePoint(int codePoint) {
        return new String(new int[] {codePoint}, 0, 1);
    }
}
