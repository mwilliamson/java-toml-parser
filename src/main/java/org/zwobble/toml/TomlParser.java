package org.zwobble.toml;

import org.zwobble.toml.errors.TomlParseError;
import org.zwobble.toml.sources.SourcePosition;
import org.zwobble.toml.values.*;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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
        var activeTable = rootTable;

        // TODO: handle surrogate pairs
        reader.read();

        while (true) {
            skipWhitespace(reader);

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
                addKeysValuePair(activeTable, keysValuePair);
            } else if (reader.codePoint == '[') {
                reader.read();

                if (reader.codePoint == '[') {
                    reader.read();
                    skipWhitespace(reader);

                    activeTable = rootTable;
                    var keys = parseKeys(reader);
                    for (var key : keys.subList(0, keys.size() - 1)) {
                        activeTable = activeTable.getOrCreateSubTable(key);
                    }
                    activeTable = activeTable.createArraySubTable(keys.getLast());

                    reader.skip(']');
                    reader.skip(']');
                    skipWhitespace(reader);
                    trySkipComment(reader);
                } else {
                    skipWhitespace(reader);
                    var keys = parseKeys(reader);
                    activeTable = rootTable;
                    for (var key : keys) {
                        activeTable = activeTable.getOrCreateSubTable(key);
                    }
                    reader.skip(']');
                    skipWhitespace(reader);
                    trySkipComment(reader);
                }
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

    private static void addKeysValuePair(TomlTableBuilder activeTable, KeysValuePair keysValuePair) {
        var table = activeTable;
        for (var i = 0; i < keysValuePair.keys.size() - 1; i++) {
            var key = keysValuePair.keys.get(i);
            table = table.getOrCreateSubTable(key);
        }
        table.add(keysValuePair.keys.getLast(), keysValuePair.value());
    }

    private static KeysValuePair parseKeyValuePair(Reader reader) throws IOException {
        var keys = parseKeys(reader);
        reader.skip('=');
        skipWhitespace(reader);
        var value = parseValue(reader);
        skipWhitespace(reader);
        trySkipComment(reader);

        return new KeysValuePair(keys, value);
    }

    private static ArrayList<String> parseKeys(Reader reader) throws IOException {
        var keys = new ArrayList<String>();

        while (true) {
            var key = parseKey(reader);

            skipWhitespace(reader);

            keys.add(key);

            if (reader.codePoint == '.') {
                reader.read();
                skipWhitespace(reader);
            } else {
                break;
            }
        }

        return keys;
    }

    private static String parseKey(Reader reader) throws IOException {
        if (reader.codePoint == '\"') {
            return parseBasicStringValue(reader);
        } else if (reader.codePoint == '\'') {
            return parseLiteralStringValue(reader);
        } else {
            return parseBareKey(reader);
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
        } else if (reader.codePoint == '{') {
            return parseInlineTable(reader);
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

        var valueString = new StringBuilder();

        valueString.appendCodePoint(reader.codePoint);
        reader.read();

        if (reader.codePoint == 'n' && (valueString.charAt(0) == '-' || valueString.charAt(0) == '+')) {
            reader.skip(new int[] {'n', 'a', 'n'});

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlFloat(Double.NaN, sourceRange);
        }

        if (reader.codePoint == 'i' && valueString.charAt(0) == '-') {
            reader.skip(new int[] {'i', 'n', 'f'});

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlFloat(Double.NEGATIVE_INFINITY, sourceRange);
        }

        if (reader.codePoint == 'i' && valueString.charAt(0) == '+') {
            reader.skip(new int[] {'i', 'n', 'f'});

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlFloat(Double.POSITIVE_INFINITY, sourceRange);
        }

        if (reader.codePoint == 'b') {
            reader.read();

            var intValue = parseBinaryDigits(reader);

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlInt(intValue, sourceRange);
        }

        if (reader.codePoint == 'o') {
            reader.read();

            var intValue = parseOctalDigits(reader);

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlInt(intValue, sourceRange);
        }

        if (reader.codePoint == 'x') {
            reader.read();

            var intValue = parseHexDigits(reader);

            var end = reader.position();
            var sourceRange = start.to(end);

            return new TomlInt(intValue, sourceRange);
        }

        var isFloat = false;
        while (true) {
            if (reader.codePoint == 'e' || reader.codePoint == 'E') {
                valueString.appendCodePoint(reader.codePoint);
                reader.read();
                if (reader.codePoint == '-' || reader.codePoint == '+') {
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();
                }
                isFloat = true;
            } else if (reader.codePoint == '_') {
                reader.read();
            } else if (isAsciiDigitCodePoint(reader.codePoint)) {
                valueString.appendCodePoint(reader.codePoint);
                reader.read();
            } else if (reader.codePoint == '.') {
                valueString.appendCodePoint(reader.codePoint);
                reader.read();
                isFloat = true;
            } else if (reader.codePoint == '-') {
                // Offset date-time, local date-time or local date
                valueString.appendCodePoint(reader.codePoint);
                reader.read();

                // Month
                valueString.appendCodePoint(reader.codePoint);
                reader.read();
                valueString.appendCodePoint(reader.codePoint);
                reader.read();

                // Hyphen
                valueString.appendCodePoint(reader.codePoint);
                reader.skip('-');

                // Day
                valueString.appendCodePoint(reader.codePoint);
                reader.read();
                valueString.appendCodePoint(reader.codePoint);
                reader.read();

                // Separator or end of local date
                if (reader.codePoint == ' ') {
                    // This could be ignorable whitespace, in which case we have
                    // a local date, or the separator between the date and time.
                    var end = reader.position();
                    reader.read();
                    if (isAsciiDigitCodePoint(reader.codePoint)) {
                        valueString.appendCodePoint('T');
                    } else {
                        var value = LocalDate.parse(valueString.toString());
                        var sourceRange = start.to(end);
                        return new TomlLocalDate(value, sourceRange);
                    }
                } else if (reader.codePoint == 'T' || reader.codePoint == 't') {
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();
                } else {
                    var end = reader.position();
                    var sourceRange = start.to(end);
                    var value = LocalDate.parse(valueString.toString());
                    return new TomlLocalDate(value, sourceRange);
                }

                readTime(reader, valueString);

                // Try to parse a timezone
                if (reader.codePoint == 'z' || reader.codePoint == 'Z') {
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();

                    var end = reader.position();
                    var sourceRange = start.to(end);
                    var value = OffsetDateTime.parse(valueString.toString());
                    return new TomlOffsetDateTime(value, sourceRange);
                } else if (reader.codePoint == '+' || reader.codePoint == '-') {
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();

                    // Hours
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();

                    // Colon
                    valueString.appendCodePoint(reader.codePoint);
                    reader.skip(':');

                    // Minutes
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();
                    valueString.appendCodePoint(reader.codePoint);
                    reader.read();

                    var end = reader.position();
                    var sourceRange = start.to(end);
                    var value = OffsetDateTime.parse(valueString.toString());
                    return new TomlOffsetDateTime(value, sourceRange);
                } else {
                    var end = reader.position();
                    var sourceRange = start.to(end);
                    var value = LocalDateTime.parse(valueString.toString());
                    return new TomlLocalDateTime(value, sourceRange);
                }
            } else if (reader.codePoint == ':') {
                valueString.appendCodePoint(reader.codePoint);
                reader.read();

                readTimeFromMinutes(reader, valueString);

                var end = reader.position();
                var sourceRange = start.to(end);
                var value = LocalTime.parse(valueString.toString());
                return new TomlLocalTime(value, sourceRange);

            } else {
                break;
            }
        }

        var end = reader.position();
        var sourceRange = start.to(end);

        if (isFloat) {
            var value = Double.parseDouble(valueString.toString());
            return new TomlFloat(value, sourceRange);
        } else {
            var integer = Long.parseLong(valueString.toString());
            return new TomlInt(integer, sourceRange);
        }
    }

    private static void readTime(Reader reader, StringBuilder valueString) throws IOException {
        // Hours
        valueString.appendCodePoint(reader.codePoint);
        reader.read();
        valueString.appendCodePoint(reader.codePoint);
        reader.read();

        // Colon
        valueString.appendCodePoint(reader.codePoint);
        reader.skip(':');

        readTimeFromMinutes(reader, valueString);
    }

    private static void readTimeFromMinutes(Reader reader, StringBuilder valueString) throws IOException {
        // Minutes
        valueString.appendCodePoint(reader.codePoint);
        reader.read();
        valueString.appendCodePoint(reader.codePoint);
        reader.read();

        // Colon
        valueString.appendCodePoint(reader.codePoint);
        reader.skip(':');

        // Seconds
        valueString.appendCodePoint(reader.codePoint);
        reader.read();
        valueString.appendCodePoint(reader.codePoint);
        reader.read();

        // Fractional seconds
        if (reader.codePoint == '.') {
            valueString.appendCodePoint(reader.codePoint);
            reader.read();
            while (isAsciiDigitCodePoint(reader.codePoint)) {
                valueString.appendCodePoint(reader.codePoint);
                reader.read();
            }
        }
    }

    private static long parseBinaryDigits(Reader reader) throws IOException {
        var numberString = new StringBuilder();
        while (true) {
            if (reader.codePoint == '0' || reader.codePoint == '1') {
                numberString.appendCodePoint(reader.codePoint);
                reader.read();
            } else if (reader.codePoint == '_') {
                reader.read();
            } else {
                return Long.parseLong(numberString.toString(), 2);
            }
        }
    }

    private static long parseOctalDigits(Reader reader) throws IOException {
        var numberString = new StringBuilder();
        while (true) {
            if (reader.codePoint >= '0' && reader.codePoint <= '7') {
                numberString.appendCodePoint(reader.codePoint);
                reader.read();
            } else if (reader.codePoint == '_') {
                reader.read();
            } else {
                return Long.parseLong(numberString.toString(), 8);
            }
        }
    }

    private static long parseHexDigits(Reader reader) throws IOException {
        var numberString = new StringBuilder();
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
                return Long.parseLong(numberString.toString(), 16);
            }
        }
    }

    private static String parseBasicStringValue(Reader reader) throws IOException {
        reader.skip('"');

        var isMultiLine = false;

        var string = new StringBuilder();
        if (reader.codePoint == '"') {
            reader.read();

            if (reader.codePoint == '"') {
                reader.read();
                isMultiLine = true;

                if (reader.codePoint == '\n') {
                    reader.read();
                } else if (reader.codePoint == '\r') {
                    reader.read();
                    if (reader.codePoint == '\n') {
                        reader.read();
                    } else {
                        string.appendCodePoint('\r');
                    }
                }
            } else {
                return "";
            }
        }

        while (true) {
            if (reader.codePoint == '"') {
                reader.read();
                if (!isMultiLine) {
                    return string.toString();
                }

                var quoteCount = 1;
                while (quoteCount < 5 && reader.codePoint == '"') {
                    quoteCount += 1;
                    reader.read();
                }

                if (quoteCount <= 2) {
                    for (var i = 0; i < quoteCount; i++) {
                        string.appendCodePoint('"');
                    }
                } else {
                    for (var i = 0; i < quoteCount - 3; i++) {
                        string.appendCodePoint('"');
                    }
                    return string.toString();
                }

            } else if (reader.codePoint == '\\') {
                reader.read();

                if (
                    isTomlWhitespace(reader.codePoint) ||
                        reader.codePoint == '\r' ||
                        reader.codePoint == '\n'
                ) {
                    while (true) {
                        if (reader.codePoint == '\n') {
                            reader.read();
                            skipMultiLineStringWhitespace(reader);
                            break;
                        } else if (
                            isTomlWhitespace(reader.codePoint) ||
                                reader.codePoint == '\r'
                        ) {
                            reader.read();
                        } else {
                            throw new TomlParseError("TODO");
                        }
                    }
                } else {
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
                }
            } else {
                string.appendCodePoint(reader.codePoint);
                reader.read();
            }
        }
    }

    private static void skipMultiLineStringWhitespace(Reader reader) throws IOException {
        while (
            isTomlWhitespace(reader.codePoint) ||
                reader.codePoint == '\r' ||
                reader.codePoint == '\n'
        ) {
            reader.read();
        }
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

    private static TomlValue parseInlineTable(Reader reader) throws IOException {
        var table = new TomlTableBuilder();

        reader.skip('{');
        skipWhitespace(reader);
        var isFirstKeyValuePair = true;

        while (reader.codePoint != '}') {
            if (!isFirstKeyValuePair) {
                reader.skip(',');
                skipWhitespace(reader);
            }

            var keys = parseKeys(reader);
            reader.skip('=');
            skipWhitespace(reader);
            var value = parseValue(reader);
            skipWhitespace(reader);
            addKeysValuePair(table, new KeysValuePair(keys, value));

            isFirstKeyValuePair = false;
        }

        reader.skip('}');

        return table.toTable();
    }

    private static void skipArrayWhitespace(Reader reader) throws IOException {
        while (true) {
            while (isArrayWhitespace(reader.codePoint)) {
                reader.read();
            }

            if (!trySkipComment(reader)) {
                return;
            }
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

    private static void skipWhitespace(Reader reader) throws IOException {
        while (isTomlWhitespace(reader.codePoint)) {
            reader.read();
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