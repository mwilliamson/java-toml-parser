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
        try (var fileReader = new FileReader(path.toFile())) {
            var reader = new Reader(fileReader);
            var keyValuePairs = new ArrayList<TomlKeyValuePair>();

            // TODO: handle surrogate pairs
            reader.read();

            while (true) {
                if (reader.isEndOfFile()) {
                    return new TomlTable(keyValuePairs);
                }

                if (isBareKeyCharacter(reader.codePoint)) {
                    var key = new StringBuilder();
                    while (isBareKeyCharacter(reader.codePoint)) {
                        key.appendCodePoint(reader.codePoint);
                        reader.read();
                    }

                    while (isTomlWhitespace(reader.codePoint)) {
                        reader.read();
                    }

                    if (reader.codePoint != '=') {
                        throw new TomlParseError(String.format(
                            "Expected '=' but got %s",
                            formatCodePoint(reader.codePoint)
                        ));
                    }
                    reader.read();

                    while (isTomlWhitespace(reader.codePoint)) {
                        reader.read();
                    }

                    if (reader.codePoint == 't') {
                        reader.read();
                        reader.read();
                        reader.read();
                        reader.read();
                        keyValuePairs.add(TomlKeyValuePair.of(key.toString(), new TomlBool(true)));
                    } else if (reader.codePoint == 'f') {
                        reader.read();
                        reader.read();
                        reader.read();
                        reader.read();
                        reader.read();
                        keyValuePairs.add(TomlKeyValuePair.of(key.toString(), new TomlBool(false)));
                    } else {
                        throw new TomlParseError("??");
                    }

                    if (reader.codePoint != '\n') {
                        throw new TomlParseError(String.format(
                            "Expected newline but got %s",
                            formatCodePoint(reader.codePoint)
                        ));
                    }
                    reader.read();
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

    private static class Reader {
        private final FileReader reader;
        private int codePoint;

        private Reader(FileReader reader) {
            this.reader = reader;
        }

        public void read() throws IOException {
            codePoint = reader.read();
        }

        public boolean isEndOfFile() {
            return codePoint == -1;
        }
    }
}
