package org.zwobble.toml;

import org.junit.jupiter.api.Test;
import org.zwobble.precisely.Matcher;
import org.zwobble.toml.sources.SourceRange;
import org.zwobble.toml.values.*;

import java.io.IOException;
import java.io.StringReader;

import static org.zwobble.precisely.AssertThat.assertThat;
import static org.zwobble.precisely.Matchers.*;

public class TomlParserTests {
    @Test
    public void emptyFile() throws IOException {
        var result = parse("");

        assertThat(result, isTable(isSequence()));
    }

    @Test
    public void onlyLf() throws IOException {
        var result = parse("\n");

        assertThat(result, isTable(isSequence()));
    }

    @Test
    public void onlyCrLf() throws IOException {
        var result = parse("\r\n");

        assertThat(result, isTable(isSequence()));
    }

    // == Keys ==

    @Test
    public void bareKeyCanBeLowercaseAsciiLetters() throws IOException {
        var result = parse("abc = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("abc", isBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeUppercaseAsciiLetters() throws IOException {
        var result = parse("ABC = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("ABC", isBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeAsciiDigits() throws IOException {
        var result = parse("123 = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("123", isBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeHyphens() throws IOException {
        var result = parse("--- = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("---", isBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeUnderscores() throws IOException {
        var result = parse("___ = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("___", isBool(true))
        )));
    }

    @Test
    public void keyCanBeBasicString() throws IOException {
        // Leave the testing of basic string parsing to the string value tests.
        var result = parse("\"one two\" = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("one two", isBool(true))
        )));
    }

    @Test
    public void keyCanBeLiteralString() throws IOException {
        // Leave the testing of literal string parsing to the string value tests.
        var result = parse("'one two' = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("one two", isBool(true))
        )));
    }

    // == Booleans ==

    @Test
    public void valueTrue() throws IOException {
        var result = parse("x = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isBool(true, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void valueFalse() throws IOException {
        var result = parse("x = false");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isBool(false, isSourceRange(4, 9)))
        )));
    }

    // == Integers ==

    @Test
    public void integerZero() throws IOException {
        var result = parse("x = 0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(0, isSourceRange(4, 5)))
        )));
    }

    @Test
    public void integerPositive() throws IOException {
        var result = parse("x = 12");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(12, isSourceRange(4, 6)))
        )));
    }

    @Test
    public void integerNegative() throws IOException {
        var result = parse("x = -12");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(-12, isSourceRange(4, 7)))
        )));
    }

    @Test
    public void intUnderscores() throws IOException {
        var result = parse("x = 1_23_4");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(1234, isSourceRange(4, 10)))
        )));
    }

    @Test
    public void intBinary() throws IOException {
        var result = parse("x = 0b1101");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(13, isSourceRange(4, 10)))
        )));
    }

    @Test
    public void intBinaryUnderscores() throws IOException {
        var result = parse("x = 0b1_10_1");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(13, isSourceRange(4, 12)))
        )));
    }

    @Test
    public void intOctal() throws IOException {
        var result = parse("x = 0o701");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(449, isSourceRange(4, 9)))
        )));
    }

    @Test
    public void intOctalUnderscores() throws IOException {
        var result = parse("x = 0o7_0_1");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(449, isSourceRange(4, 11)))
        )));
    }

    @Test
    public void intHexLowercase() throws IOException {
        var result = parse("x = 0xf01");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(3841, isSourceRange(4, 9)))
        )));
    }

    @Test
    public void intHexUppercase() throws IOException {
        var result = parse("x = 0xF01");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(3841, isSourceRange(4, 9)))
        )));
    }

    @Test
    public void intHexUnderscores() throws IOException {
        var result = parse("x = 0xf_0_1");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(3841, isSourceRange(4, 11)))
        )));
    }

    // == Floats ==

    @Test
    public void floatZero() throws IOException {
        var result = parse("x = 0.0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(0, isSourceRange(4, 7)))
        )));
    }

    @Test
    public void floatPositive() throws IOException {
        var result = parse("x = 12.34");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(12.34, isSourceRange(4, 9)))
        )));
    }

    @Test
    public void floatNegative() throws IOException {
        var result = parse("x = -12.34");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(-12.34, isSourceRange(4, 10)))
        )));
    }

    @Test
    public void floatExponentLowercase() throws IOException {
        var result = parse("x = 5e2");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(500, isSourceRange(4, 7)))
        )));
    }

    @Test
    public void floatExponentUppercase() throws IOException {
        var result = parse("x = 5E2");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(500, isSourceRange(4, 7)))
        )));
    }

    @Test
    public void floatPositiveExponentLowercase() throws IOException {
        var result = parse("x = 5e+2");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(500, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatPositiveExponentUppercase() throws IOException {
        var result = parse("x = 5E+2");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(500, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatNegativeExponentLowercase() throws IOException {
        var result = parse("x = 5e-2");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(0.05, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatNegativeExponentUppercase() throws IOException {
        var result = parse("x = 5E-2");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(0.05, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatUnderscores() throws IOException {
        var result = parse("x = -1_2.3_4");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(-12.34, isSourceRange(4, 12)))
        )));
    }

    @Test
    public void floatNan() throws IOException {
        var result = parse("x = nan");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(Double.NaN, isSourceRange(4, 7)))
        )));
    }

    @Test
    public void floatPositiveNan() throws IOException {
        var result = parse("x = +nan");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(Double.NaN, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatNegativeNan() throws IOException {
        var result = parse("x = -nan");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(Double.NaN, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatInf() throws IOException {
        var result = parse("x = inf");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(Double.POSITIVE_INFINITY, isSourceRange(4, 7)))
        )));
    }

    @Test
    public void floatPositiveInf() throws IOException {
        var result = parse("x = +inf");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(Double.POSITIVE_INFINITY, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatNegativeInf() throws IOException {
        var result = parse("x = -inf");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(Double.NEGATIVE_INFINITY, isSourceRange(4, 8)))
        )));
    }

    // == Strings ==

    @Test
    public void emptyBasicString() throws IOException {
        var result = parse(
            """
            x = ""
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 6)))
        )));
    }

    @Test
    public void basicStringWithAscii() throws IOException {
        var result = parse(
            """
            x = "abc"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc", isSourceRange(4, 9)))
        )));
    }

    @Test
    public void basicStringWithCompactEscapeSequence() throws IOException {
        var result = parse(
            """
            backspace = "\\b"
            tab = "\\t"
            linefeed = "\\n"
            formfeed = "\\f"
            carriagereturn = "\\r"
            quote = "\\""
            backslash = "\\\\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("backspace", isString("\b")),
            isKeyValuePair("tab", isString("\t")),
            isKeyValuePair("linefeed", isString("\n")),
            isKeyValuePair("formfeed", isString("\f")),
            isKeyValuePair("carriagereturn", isString("\r")),
            isKeyValuePair("quote", isString("\"")),
            isKeyValuePair("backslash", isString("\\"))
        )));
    }

    @Test
    public void basicStringWithFourDigitLowercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = "\\u03c0"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void basicStringWithFourDigitUppercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = "\\u03C0"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void basicStringWithEightDigitLowercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = "\\U0001f967"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void basicStringWithEightDigitUppercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = "\\U0001F967"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void emptyLiteralString() throws IOException {
        var result = parse(
            """
            x = ''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 6)))
        )));
    }

    @Test
    public void literalStringWithAscii() throws IOException {
        var result = parse(
            """
            x = 'abc'
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc", isSourceRange(4, 9)))
        )));
    }

    @Test
    public void literalStringWithCompactEscapeSequence() throws IOException {
        var result = parse(
            """
            backspace = '\\b'
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("backspace", isString("\\b"))
        )));
    }

    // == Arrays ==

    @Test
    public void emptyInlineArray() throws IOException {
        var result = parse("x = []");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence()))
        )));
    }

    @Test
    public void singletonArray() throws IOException {
        var result = parse("x = [true]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isBool(true)
            )))
        )));
    }

    @Test
    public void arrayWithMultipleValuesAndNoTrailingComma() throws IOException {
        var result = parse("x = [true,false,1]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isBool(true),
                isBool(false),
                isInt(1)
            )))
        )));
    }

    @Test
    public void arrayWithMultipleValuesAndTrailingComma() throws IOException {
        var result = parse("x = [true,false,1,]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isBool(true),
                isBool(false),
                isInt(1)
            )))
        )));
    }

    @Test
    public void arrayWithWhitespaceAroundCommas() throws IOException {
        var result = parse("x = [  true  , false , 1 , ]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isBool(true),
                isBool(false),
                isInt(1)
            )))
        )));
    }

    @Test
    public void arrayCanSpanMultipleLines() throws IOException {
        var result = parse("""
            x = [
                true,
                false
                
                ,
                1,
            ]
            """);

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isBool(true),
                isBool(false),
                isInt(1)
            )))
        )));
    }

    // == Comments ==

    @Test
    public void onlyLineCommentNoEol() throws IOException {
        var result = parse("# a");

        assertThat(result, isTable(isSequence()));
    }

    @Test
    public void onlyLineCommentWithLf() throws IOException {
        var result = parse("# a\n");

        assertThat(result, isTable(isSequence()));
    }

    @Test
    public void onlyLineCommentWithCrLf() throws IOException {
        var result = parse("# a\r\n");

        assertThat(result, isTable(isSequence()));
    }

    @Test
    public void lineCommentWithLf() throws IOException {
        var result = parse("a = true\n# a\nb = false");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isBool(true)),
            isKeyValuePair("b", isBool(false))
        )));
    }

    @Test
    public void lineCommentWithCrLf() throws IOException {
        var result = parse("a = true\n# a\r\nb = false");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isBool(true)),
            isKeyValuePair("b", isBool(false))
        )));
    }

    private TomlTable parse(String text) throws IOException {
        return TomlParser.parseReader(new StringReader(text));
    }

    private Matcher<TomlValue> isBool(boolean value) {
        return instanceOf(
            TomlBool.class,
            has("value", x -> x.value(), equalTo(value))
        );
    }

    private Matcher<TomlValue> isBool(boolean value, Matcher<SourceRange> sourceRange) {
        return instanceOf(
            TomlBool.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isInt(long value) {
        return instanceOf(
            TomlInt.class,
            has("value", x -> x.value(), equalTo(value))
        );
    }

    private Matcher<TomlValue> isInt(long value, Matcher<SourceRange> sourceRange) {
        return instanceOf(
            TomlInt.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isFloat(double value) {
        return instanceOf(
            TomlFloat.class,
            has("value", x -> x.value(), equalTo(value))
        );
    }

    private Matcher<TomlValue> isFloat(double value, Matcher<SourceRange> sourceRange) {
        return instanceOf(
            TomlFloat.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isString(String value) {
        return instanceOf(
            TomlString.class,
            has("value", x -> x.value(), equalTo(value))
        );
    }

    private Matcher<TomlValue> isString(String value, Matcher<SourceRange> sourceRange) {
        return instanceOf(
            TomlString.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isArray(
        Matcher<Iterable<? extends TomlValue>> elements
    ) {
        return instanceOf(
            TomlArray.class,
            has("elements", x -> x.elements(), elements)
        );
    }

    private Matcher<TomlValue> isTable(
        Matcher<Iterable<? extends TomlKeyValuePair>> keyValuePairs
    ) {
        return instanceOf(
            TomlTable.class,
            has("keyValuePairs", x -> x.keyValuePairs(), keyValuePairs)
        );
    }

    private Matcher<TomlKeyValuePair> isKeyValuePair(
        String key,
        Matcher<TomlValue> value
    ) {
        return allOf(
            has("key", x -> x.key(), equalTo(key)),
            has("value", x -> x.value(), value)
        );
    }

    private Matcher<SourceRange> isSourceRange(
        int codePointStartIndex,
        int codePointEndIndex
    ) {
        return allOf(
            has("start.codePointIndex", x -> x.start().codePointIndex(), equalTo(codePointStartIndex)),
            has("end.codePointIndex", x -> x.end().codePointIndex(), equalTo(codePointEndIndex))
        );
    }
}
