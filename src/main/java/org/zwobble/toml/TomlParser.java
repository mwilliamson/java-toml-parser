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
            return parseReader(fileReader);
        }
    }

    public static TomlTable parseReader(java.io.Reader rawReader) throws IOException {
        var reader = new Reader(rawReader);
        var keyValuePairs = new ArrayList<TomlKeyValuePair>();

        // TODO: handle surrogate pairs
        reader.read();

        while (true) {
            if (reader.isEndOfFile()) {
                return new TomlTable(keyValuePairs);
            }

            if (trySkipComment(reader)) {
                // Comment for the entire line
            } else if (reader.codePoint == '\n') {
                // Blank line with LF
            } else if (reader.codePoint == '\r') {
                // Blank line with CRLF
            } else if (isBareKeyCodePoint(reader.codePoint) || reader.codePoint == '\"') {
                var key = reader.codePoint == '\"'
                    ? parseStringValue(reader)
                    : readBareKey(reader);
                skipWhitespace(reader);
                reader.skip('=');
                skipWhitespace(reader);
                var value = readValue(reader);
                keyValuePairs.add(TomlKeyValuePair.of(key, value));
                skipWhitespace(reader);
                trySkipComment(reader);
            } else {
                throw new TomlParseError("TODO: " + formatCodePoint(reader.codePoint));
            }

            if (reader.isEndOfFile()) {
                return new TomlTable(keyValuePairs);
            }

            if (reader.codePoint == '\r') {
                reader.skip('\r');
            }
            reader.skip('\n');
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
        } else if (reader.codePoint == '"') {
            var string = parseStringValue(reader);
            return new TomlString(string);
        } else if (reader.codePoint == '[') {
            return parseArray(reader);
        } else {
            throw new TomlParseError("??");
        }
    }

    private static String parseStringValue(Reader reader) throws IOException {
        reader.skip('"');

        var string = new StringBuilder();
        while (reader.codePoint != '"') {
            if (reader.codePoint == '\\') {
                reader.read();
                switch (reader.codePoint) {
                    case 'b' -> {
                        string.appendCodePoint('\b');
                        reader.read();
                    }
                    case 't' -> {
                        string.appendCodePoint('\t');
                        reader.read();
                    }
                    case 'n' -> {
                        string.appendCodePoint('\n');
                        reader.read();
                    }
                    case 'f' -> {
                        string.appendCodePoint('\f');
                        reader.read();
                    }
                    case 'r' -> {
                        string.appendCodePoint('\r');
                        reader.read();
                    }
                    case '"' -> {
                        string.appendCodePoint('"');
                        reader.read();
                    }
                    case '\\' -> {
                        string.appendCodePoint('\\');
                        reader.read();
                    }
                    default -> {
                        throw new TomlParseError("TODO");
                    }
                }
            } else {
                string.appendCodePoint(reader.codePoint);
                reader.read();
            }
        }

        reader.skip('"');

        return string.toString();
    }

    private static TomlValue parseArray(Reader reader) throws IOException {
        reader.skip('[');
        var elements = new ArrayList<TomlValue>();
        while (reader.codePoint != ']') {
            var element = readValue(reader);
            elements.add(element);

            if (reader.codePoint == ',') {
                reader.read();
            } else {
                break;
            }
        }
        reader.skip(']');
        return TomlArray.of(elements);
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
        if (codePoint == '\r') {
            return "CR";
        } else if (codePoint == '\n') {
            return "LF";
        } else {
            return new String(new int[] {codePoint}, 0, 1);
        }
    }

    private static class Reader {
        private final java.io.Reader reader;
        private int codePoint;

        private Reader(java.io.Reader reader) {
            this.reader = reader;
        }

        public void read() throws IOException {
            this.codePoint = reader.read();
        }

        public void skip(int expectedCodePoint) throws IOException {
            expect(expectedCodePoint);
            read();
        }

        public void expect(int expectedCodePoint) {
            if (this.codePoint != expectedCodePoint) {
                throw new TomlParseError(String.format(
                    "Expected %s but got %s",
                    formatCodePoint(expectedCodePoint),
                    formatCodePoint(this.codePoint)
                ));
            }
        }

        public boolean isEndOfFile() {
            return codePoint == -1;
        }
    }
}
