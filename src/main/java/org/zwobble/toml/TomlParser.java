package org.zwobble.toml;

import org.zwobble.toml.errors.TomlParseError;
import org.zwobble.toml.values.*;

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

                if (trySkipComment(reader)) {
                    // Do nothing
                } else if (isBareKeyCodePoint(reader.codePoint)) {
                    var key = readBareKey(reader);
                    skipWhitespace(reader);
                    reader.skip('=');
                    skipWhitespace(reader);
                    var value = readValue(reader);
                    keyValuePairs.add(TomlKeyValuePair.of(key, value));
                    trySkipComment(reader);
                } else {
                    throw new TomlParseError("TODO: " + formatCodePoint(reader.codePoint));
                }

                if (reader.isEndOfFile()) {
                    return new TomlTable(keyValuePairs);
                }

                reader.skip('\n');
            }
        }
    }

    private static void skipWhitespace(Reader reader) throws IOException {
        while (isTomlWhitespace(reader.codePoint)) {
            reader.read();
        }
    }

    private static String readBareKey(Reader reader) throws IOException {
        var key = new StringBuilder();
        while (isBareKeyCodePoint(reader.codePoint)) {
            key.appendCodePoint(reader.codePoint);
            reader.read();
        }
        return key.toString();
    }

    private static TomlValue readValue(Reader reader) throws IOException {
        if (reader.codePoint == 't') {
            reader.skip('t');
            reader.skip('r');
            reader.skip('u');
            reader.skip('e');
            return new TomlBool(true);
        } else if (reader.codePoint == 'f') {
            reader.skip('f');
            reader.skip('a');
            reader.skip('l');
            reader.skip('s');
            reader.skip('e');
            return new TomlBool(false);
        } else if (isAsciiDigitCodePoint(reader.codePoint) || reader.codePoint == '+' || reader.codePoint == '-') {
            var integerString = new StringBuilder();

            integerString.appendCodePoint(reader.codePoint);
            reader.read();

            while (isAsciiDigitCodePoint(reader.codePoint) || reader.codePoint == '_') {
                if (reader.codePoint != '_') {
                    integerString.appendCodePoint(reader.codePoint);
                }
                reader.read();
            }

            var integer = Long.parseLong(integerString.toString());
            return new TomlInt(integer);
        } else {
            throw new TomlParseError("??");
        }
    }

    private static boolean trySkipComment(Reader reader) throws IOException {
        if (reader.codePoint == '#') {
            reader.read();

            while (reader.codePoint != '\n' && reader.codePoint != -1) {
                reader.read();
            }

            return true;
        } else {
            return false;
        }
    }

    private static boolean isTomlWhitespace(int character) {
        return character == 0x09 || character == 0x20;
    }

    private static boolean isBareKeyCodePoint(int character) {
        return isAsciiDigitCodePoint(character) ||
            (character >= 0x41 && character <= 0x5a) ||
            (character >= 0x61 && character <= 0x7a) ||
            character == '_' ||
            character == '-';
    }

    private static boolean isAsciiDigitCodePoint(int character) {
        return character >= 0x30 && character <= 0x39;
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
            this.codePoint = reader.read();
        }

        public void skip(int expectedCodePoint) throws IOException {
            if (this.codePoint != expectedCodePoint) {
                throw new TomlParseError(String.format(
                    "Expected %s but got %s",
                    formatCodePoint(expectedCodePoint),
                    formatCodePoint(this.codePoint)
                ));
            }
            read();
        }

        public boolean isEndOfFile() {
            return codePoint == -1;
        }
    }
}
