package org.zwobble.toml;

import org.zwobble.toml.errors.TomlParseError;
import org.zwobble.toml.sources.SourcePosition;
import org.zwobble.toml.values.*;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.lang.System.identityHashCode;

public class TomlParser {
    private TomlParser() {
    }

    public static TomlTable parseFile(Path path) throws IOException {
        try (var fileReader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
            return parseReader(fileReader);
        }
    }

    public static TomlTable parseReader(java.io.Reader rawReader) throws IOException {
        var reader = new Reader(rawReader);

        var rootTable = new TomlTableBuilder();

        // TODO: handle surrogate pairs
        reader.read();

        while (true) {
            if (reader.isEndOfFile()) {
                return rootTable.toTable();
            }

            if (trySkipComment(reader)) {
                // Comment for the entire line
            } else if (reader.codePoint == '\n') {
                // Blank line with LF
            } else if (reader.codePoint == '\r') {
                // Blank line with CRLF
            } else if (isBareKeyCodePoint(reader.codePoint) || reader.codePoint == '\"' || reader.codePoint == '\'') {
                var keysValuePair = parseKeyValuePair(reader);
                var table = rootTable;
                for (var i = 0; i < keysValuePair.keys.size() - 1; i++) {
                    var key = keysValuePair.keys.get(i);
                    table = table.getOrCreateSubTable(key);
                }
                table.add(keysValuePair.keys.getLast(), keysValuePair.value());
            } else {
                throw new TomlParseError("TODO: " + formatCodePoint(reader.codePoint));
            }

            if (reader.isEndOfFile()) {
                return rootTable.toTable();
            }

            if (reader.codePoint == '\r') {
                reader.skip('\r');
            }
            reader.skip('\n');
        }
    }

    private record KeysValuePair(List<String> keys, TomlValue value) {}

    private static KeysValuePair parseKeyValuePair(Reader reader) throws IOException {
        var keys = new ArrayList<String>();
        while (true) {
            var key =
                reader.codePoint == '\"' ? parseBasicStringValue(reader) :
                reader.codePoint == '\'' ? parseLiteralStringValue(reader) :
                parseBareKey(reader);

            skipWhitespace(reader);

            keys.add(key);

            if (reader.codePoint == '.') {
                reader.read();
                skipWhitespace(reader);
            } else {
                break;
            }
        }

        reader.skip('=');
        skipWhitespace(reader);
        var value = parseValue(reader);
        skipWhitespace(reader);
        trySkipComment(reader);

        return new KeysValuePair(keys, value);
    }

    private static void skipWhitespace(Reader reader) throws IOException {
        while (isTomlWhitespace(reader.codePoint)) {
            reader.read();
        }
    }

    private static String parseBareKey(Reader reader) throws IOException {
        var key = new StringBuilder();
        while (isBareKeyCodePoint(reader.codePoint)) {
            key.appendCodePoint(reader.codePoint);
            reader.read();
        }
        return key.toString();
    }

    private static TomlValue parseValue(Reader reader) throws IOException {
        if (reader.codePoint == 't') {
            return parseTrue(reader);
        } else if (reader.codePoint == 'f') {
            return parseFalse(reader);
        } else if (reader.codePoint == 'n') {
            return parseNan(reader);
        } else if (reader.codePoint == 'i') {
            return parseInf(reader);
        } else if (isAsciiDigitCodePoint(reader.codePoint) || reader.codePoint == '+' || reader.codePoint == '-') {
            return parseNumber(reader);
        } else if (reader.codePoint == '"') {
            var start = reader.position();

            var string = parseBasicStringValue(reader);

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlString(string, sourceRange);
        } else if (reader.codePoint == '\'') {
            var start = reader.position();

            var string = parseLiteralStringValue(reader);

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlString(string, sourceRange);
        } else if (reader.codePoint == '[') {
            return parseArray(reader);
        } else {
            throw new TomlParseError("??");
        }
    }

    private static TomlBool parseTrue(Reader reader) throws IOException {
        var start = reader.position();

        reader.skip(new int[] {'t', 'r', 'u', 'e'});

        var end = reader.position();
        var sourceRange = start.to(end);

        return new TomlBool(true, sourceRange);
    }

    private static TomlBool parseFalse(Reader reader) throws IOException {
        var start = reader.position();

        reader.skip(new int[] {'f', 'a', 'l', 's', 'e'});

        var end = reader.position();
        var sourceRange = start.to(end);

        return new TomlBool(false, sourceRange);
    }

    private static TomlFloat parseNan(Reader reader) throws IOException {
        var start = reader.position();

        reader.skip(new int[] {'n', 'a', 'n'});

        var end = reader.position();
        var sourceRange = start.to(end);

        return new TomlFloat(Double.NaN, sourceRange);
    }

    private static TomlFloat parseInf(Reader reader) throws IOException {
        var start = reader.position();

        reader.skip(new int[] {'i', 'n', 'f'});

        var end = reader.position();
        var sourceRange = start.to(end);

        return new TomlFloat(Double.POSITIVE_INFINITY, sourceRange);
    }

    private static TomlValue parseNumber(Reader reader) throws IOException {
        var start = reader.position();

        var isFloat = false;
        var numberString = new StringBuilder();

        numberString.appendCodePoint(reader.codePoint);
        reader.read();

        if (reader.codePoint == 'n' && (numberString.charAt(0) == '-' || numberString.charAt(0) == '+')) {
            reader.skip(new int[] {'n', 'a', 'n'});

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlFloat(Double.NaN, sourceRange);
        }

        if (reader.codePoint == 'i' && numberString.charAt(0) == '-') {
            reader.skip(new int[] {'i', 'n', 'f'});

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlFloat(Double.NEGATIVE_INFINITY, sourceRange);
        }

        if (reader.codePoint == 'i' && numberString.charAt(0) == '+') {
            reader.skip(new int[] {'i', 'n', 'f'});

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlFloat(Double.POSITIVE_INFINITY, sourceRange);
        }

        if (reader.codePoint == 'b') {
            reader.read();

            while (true) {
                if (reader.codePoint == '0' || reader.codePoint == '1') {
                    numberString.appendCodePoint(reader.codePoint);
                    reader.read();
                } else if (reader.codePoint == '_') {
                    reader.read();
                } else {
                    var end = reader.position();
                    var sourceRange = start.to(end);

                    var intValue = Long.parseLong(numberString.toString(), 2);
                    return new TomlInt(intValue, sourceRange);
                }
            }
        }

        if (reader.codePoint == 'o') {
            reader.read();

            while (true) {
                if (reader.codePoint >= '0' && reader.codePoint <= '7') {
                    numberString.appendCodePoint(reader.codePoint);
                    reader.read();
                } else if (reader.codePoint == '_') {
                    reader.read();
                } else {
                    var end = reader.position();
                    var sourceRange = start.to(end);

                    var intValue = Long.parseLong(numberString.toString(), 8);
                    return new TomlInt(intValue, sourceRange);
                }
            }
        }

        if (reader.codePoint == 'x') {
            reader.read();

            while (true) {
                if (
                    (reader.codePoint >= '0' && reader.codePoint <= '9') ||
                        (reader.codePoint >= 'a' && reader.codePoint <= 'f') ||
                        (reader.codePoint >= 'A' && reader.codePoint <= 'F')
                ) {
                    numberString.appendCodePoint(reader.codePoint);
                    reader.read();
                } else if (reader.codePoint == '_') {
                    reader.read();
                } else {
                    var end = reader.position();
                    var sourceRange = start.to(end);

                    var intValue = Long.parseLong(numberString.toString(), 16);
                    return new TomlInt(intValue, sourceRange);
                }
            }
        }

        while (
            isAsciiDigitCodePoint(reader.codePoint) ||
                reader.codePoint == '_' ||
                reader.codePoint == '.' ||
                reader.codePoint == 'e' ||
                reader.codePoint == 'E'
        ) {
            var isExponent = reader.codePoint == 'e' || reader.codePoint == 'E';
            if (reader.codePoint != '_') {
                if (reader.codePoint == '.' || isExponent) {
                    isFloat = true;
                }
                numberString.appendCodePoint(reader.codePoint);
            }
            reader.read();

            if (isExponent) {
                if (reader.codePoint == '-' || reader.codePoint == '+') {
                    numberString.appendCodePoint(reader.codePoint);
                    reader.read();
                }
            }
        }

        var end = reader.position();
        var sourceRange = start.to(end);

        if (isFloat) {
            var value = Double.parseDouble(numberString.toString());
            return new TomlFloat(value, sourceRange);
        } else {
            var integer = Long.parseLong(numberString.toString());
            return new TomlInt(integer, sourceRange);
        }
    }

    private static String parseBasicStringValue(Reader reader) throws IOException {
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
                    case 'u' -> {
                        reader.read();
                        var codePoint = parseHex(reader, 4);
                        string.appendCodePoint(codePoint);
                    }
                    case 'U' -> {
                        reader.read();
                        var codePoint = parseHex(reader, 8);
                        string.appendCodePoint(codePoint);
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

    private static int parseHex(Reader reader, int codePointCount) throws IOException {
        var codePoint = 0;
        for (var i = 0; i < codePointCount; i++) {
            codePoint <<= 4;
            if (reader.codePoint >= '0' && reader.codePoint <= '9') {
                codePoint += reader.codePoint - '0';
            } else if (reader.codePoint >= 'a' && reader.codePoint <= 'f') {
                codePoint += reader.codePoint - 'a' + 10;
            } else if (reader.codePoint >= 'A' && reader.codePoint <= 'F') {
                codePoint += reader.codePoint - 'A' + 10;
            } else {
                throw new TomlParseError("TODO");
            }
            reader.read();
        }
        return codePoint;
    }

    private static String parseLiteralStringValue(Reader reader) throws IOException {
        reader.skip('\'');

        var string = new StringBuilder();
        while (reader.codePoint != '\'') {
            string.appendCodePoint(reader.codePoint);
            reader.read();
        }

        reader.skip('\'');

        return string.toString();
    }

    private static TomlValue parseArray(Reader reader) throws IOException {
        reader.skip('[');
        skipArrayWhitespace(reader);
        var elements = new ArrayList<TomlValue>();
        while (reader.codePoint != ']') {
            var element = parseValue(reader);
            skipArrayWhitespace(reader);
            elements.add(element);

            if (reader.codePoint == ',') {
                reader.read();
                skipArrayWhitespace(reader);
            } else {
                break;
            }
        }

        reader.skip(']');
        return TomlArray.of(elements);
    }

    private static void skipArrayWhitespace(Reader reader) throws IOException {
        while (isArrayWhitespace(reader.codePoint)) {
            reader.read();
        }
    }

    private static boolean isArrayWhitespace(int codePoint) {
        return isTomlWhitespace(codePoint) || codePoint == '\r' || codePoint == '\n';
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
        private int codePointIndex;

        private Reader(java.io.Reader reader) {
            this.reader = reader;
            this.codePointIndex = -1;
        }

        public void read() throws IOException {
            this.codePoint = reader.read();
            codePointIndex += 1;
        }

        public void skip(int expectedCodePoint) throws IOException {
            expect(expectedCodePoint);
            read();
        }

        public void skip(int[] expectedCodePoints) throws IOException {
            for (var expectedCodePoint : expectedCodePoints) {
                skip(expectedCodePoint);
            }
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

        public SourcePosition position() {
            return new SourcePosition(codePointIndex);
        }
    }

}
