package org.zwobble.toml;

import org.zwobble.toml.errors.*;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static org.zwobble.toml.UnicodeCodePoints.formatCodePoint;

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

            if (trySkipToNextLineOrEndOfFile(reader)) {
                // Blank line
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
                    skipToNextLine(reader);
                } else {
                    skipWhitespace(reader);
                    var keys = parseKeys(reader);
                    activeTable = rootTable;
                    for (var key : keys) {
                        activeTable = activeTable.getOrCreateSubTable(key);
                    }
                    reader.skip(']');
                    skipWhitespace(reader);
                    skipToNextLine(reader);
                }
            } else {
                var position = reader.position();
                var sourceRange = position.to(position);
                throw new TomlParseError(
                    "TODO: " + formatCodePoint(reader.codePoint),
                    sourceRange
                );
            }

            if (reader.isEndOfFile()) {
                return rootTable.toTable();
            }
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
        parseKeyValuePairEqualsSign(reader);
        skipWhitespace(reader);
        var value = parseValue(reader);
        skipWhitespace(reader);
        skipToNextLine(reader);

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

    private static void parseKeyValuePairEqualsSign(Reader reader) throws IOException {
        if (reader.codePoint != '=') {
            throw new TomlKeyValuePairMissingEqualsSignError(
                formatCodePoint(reader.codePoint),
                reader.position().toSourceRange()
            );
        }
        reader.read();
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
            var position = reader.position();
            var sourceRange = position.to(position);
            throw new TomlUnspecifiedValueError(sourceRange);
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
                    var localDateString = valueString.toString();
                    try {
                        var value = LocalDate.parse(localDateString);
                        return new TomlLocalDate(value, sourceRange);
                    } catch (DateTimeParseException exception) {
                        throw new TomlInvalidLocalDateError(
                            localDateString,
                            sourceRange
                        );
                    }
                }

                readTime(reader, valueString);

                // Try to parse an offset
                var isOffset = readDateTimeOffset(reader, valueString);

                var end = reader.position();
                var sourceRange = start.to(end);

                if (isOffset) {
                    var offsetDateTimeString = valueString.toString();
                    try {
                        var value = OffsetDateTime.parse(offsetDateTimeString);
                        return new TomlOffsetDateTime(value, sourceRange);
                    } catch (DateTimeParseException exception) {
                        throw new TomlInvalidOffsetDateTimeError(
                            offsetDateTimeString,
                            sourceRange
                        );
                    }
                } else {
                    var localDateTimeString = valueString.toString();
                    try {
                        var value = LocalDateTime.parse(localDateTimeString);
                        return new TomlLocalDateTime(value, sourceRange);
                    } catch (DateTimeParseException exception) {
                        throw new TomlInvalidLocalDateTimeError(
                            localDateTimeString,
                            sourceRange
                        );
                    }
                }
            } else if (reader.codePoint == ':') {
                valueString.appendCodePoint(reader.codePoint);
                reader.read();

                readTimeFromMinutes(reader, valueString);

                var end = reader.position();
                var sourceRange = start.to(end);
                var localTimeString = valueString.toString();
                try {
                    var value = LocalTime.parse(localTimeString);
                    return new TomlLocalTime(value, sourceRange);
                } catch (DateTimeParseException exception) {
                    throw new TomlInvalidLocalTimeError(
                        localTimeString,
                        sourceRange
                    );
                }
            } else {
                break;
            }
        }

        var end = reader.position();
        var sourceRange = start.to(end);

        if (isFloat) {
            var floatString = valueString.toString();
            try {
                var value = Double.parseDouble(floatString);
                return new TomlFloat(value, sourceRange);
            } catch (NumberFormatException exception) {
                throw new TomlInvalidNumberError(
                    floatString,
                    sourceRange
                );
            }
        } else {
            var integerString = valueString.toString();
            if (integerStringHasLeadingZeroes(integerString)) {
                throw new TomlInvalidNumberError(
                    integerString,
                    sourceRange
                );
            }
            try {
                var integer = Long.parseLong(integerString);
                return new TomlInt(integer, sourceRange);
            } catch (NumberFormatException exception) {
                throw new TomlInvalidNumberError(
                    integerString,
                    sourceRange
                );
            }
        }
    }

    private static boolean integerStringHasLeadingZeroes(String integerString) {
        if (integerString.length() > 1 && integerString.charAt(0) == '0') {
            return true;
        }

        if (integerString.length() <= 2) {
            return false;
        }

        var hasSign = integerString.charAt(0) == '+' || integerString.charAt(0) == '-';
        if (hasSign && integerString.charAt(1) == '0') {
            return true;
        }

        return false;
    }

    private static boolean readDateTimeOffset(
        Reader reader,
        StringBuilder valueString
    ) throws IOException {
        if (reader.codePoint == 'z' || reader.codePoint == 'Z') {
            valueString.appendCodePoint(reader.codePoint);
            reader.read();

            return true;
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

            return true;
        } else {
            return false;
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
        return parseStringValue(reader, '"', true);
    }

    private static String parseLiteralStringValue(Reader reader) throws IOException {
        return parseStringValue(reader, '\'', false);
    }

    private static String parseStringValue(
        Reader reader,
        char quote,
        boolean allowEscaping
    ) throws IOException {
        reader.skip(quote);

        var isMultiLine = false;

        var string = new StringBuilder();
        if (reader.codePoint == quote) {
            reader.read();

            if (reader.codePoint == quote) {
                reader.read();
                isMultiLine = true;

                trySkipNewLine(reader);
            } else {
                return "";
            }
        }

        while (true) {
            if (reader.codePoint == quote) {
                reader.read();
                if (!isMultiLine) {
                    return string.toString();
                }

                var quoteCount = 1;
                while (quoteCount < 5 && reader.codePoint == quote) {
                    quoteCount += 1;
                    reader.read();
                }

                if (quoteCount <= 2) {
                    for (var i = 0; i < quoteCount; i++) {
                        string.appendCodePoint(quote);
                    }
                } else {
                    for (var i = 0; i < quoteCount - 3; i++) {
                        string.appendCodePoint(quote);
                    }
                    return string.toString();
                }

            } else if (reader.codePoint == '\\' && allowEscaping) {
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
                        while (true) {
                            if (trySkipNewLine(reader)) {
                                skipMultiLineStringWhitespace(reader);
                                break;
                            } else if (isTomlWhitespace(reader.codePoint)) {
                                reader.read();
                            } else {
                                var position = reader.position();
                                var sourceRange = position.to(position);
                                throw new TomlParseError("TODO", sourceRange);
                            }
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
        while (true) {
            if (isTomlWhitespace(reader.codePoint)) {
                reader.read();
            } else if (trySkipNewLine(reader)) {
                // Do nothing
            } else {
                return;
            }
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
                var position = reader.position();
                var sourceRange = position.to(position);
                throw new TomlParseError("TODO", sourceRange);
            }
            reader.read();
        }
        return codePoint;
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
            while (isTomlWhitespace(reader.codePoint)) {
                reader.read();
            }

            if (!trySkipToNextLineOrEndOfFile(reader) || reader.isEndOfFile()) {
                return;
            }
        }
    }

    private static void skipToNextLine(Reader reader) throws IOException {
        if (trySkipToNextLineOrEndOfFile(reader)) {
            return;
        }

        var unexpectedTextStart = reader.position();
        var unexpectedTextEnd = reader.position();
        var unexpectedText = new StringBuilder();
        var unexpectedWhitespace = new StringBuilder();

        while (
            reader.codePoint != -1 &&
                reader.codePoint != '#' &&
                reader.codePoint != '\n'
        ) {
            unexpectedText.append(unexpectedWhitespace);
            unexpectedWhitespace.setLength(0);
            unexpectedText.appendCodePoint(reader.codePoint);
            reader.read();
            unexpectedTextEnd = reader.position();

            while (isTomlWhitespace(reader.codePoint)) {
                unexpectedWhitespace.appendCodePoint(reader.codePoint);
                reader.read();
            }
        }

        throw new TomlUnexpectedTextAtEolError(
            unexpectedText.toString(),
            unexpectedTextStart.to(unexpectedTextEnd)
        );
    }

    private static boolean trySkipToNextLineOrEndOfFile(Reader reader) throws IOException {
        if (trySkipNewLineOrEndOfFile(reader)) {
            return true;
        }

        if (reader.codePoint == '#') {
            reader.read();

            while (true) {
                if (trySkipNewLineOrEndOfFile(reader)) {
                    return true;
                }

                if (reader.codePoint != '\t' && isControlCharacter(reader.codePoint)) {
                    var controlCharacter = reader.codePoint;
                    var start = reader.position();
                    reader.read();
                    var end = reader.position();
                    var sourceRange = start.to(end);

                    throw new TomlUnexpectedControlCharacterError(
                        controlCharacter,
                        sourceRange
                    );
                }
                reader.read();
            }
        } else {
            return false;
        }
    }

    private static boolean trySkipNewLineOrEndOfFile(Reader reader) throws IOException {
        if (reader.isEndOfFile()) {
            return true;
        }

        return trySkipNewLine(reader);
    }

    private static boolean trySkipNewLine(Reader reader) throws IOException {
        if (reader.codePoint == '\n') {
            reader.read();
            return true;
        }

        if (reader.codePoint == '\r') {
            var start = reader.position();
            reader.read();
            var end = reader.position();
            var sourceRange = start.to(end);

            if (reader.codePoint == '\n') {
                reader.read();
                return true;
            } else {
                throw new TomlUnexpectedControlCharacterError(
                    '\r',
                    sourceRange
                );
            }
        }
        return false;
    }

    private static boolean isControlCharacter(int codePoint) {
        return (codePoint >= 0 && codePoint <= 0x1f) || codePoint == 0x7f;
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
                var position = this.position();
                var sourceRange = position.to(position);
                throw new TomlParseError(String.format(
                    "Expected %s but got %s",
                    formatCodePoint(expectedCodePoint),
                    formatCodePoint(this.codePoint)
                ), sourceRange);
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
