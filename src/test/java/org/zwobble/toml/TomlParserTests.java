package org.zwobble.toml;

import org.junit.jupiter.api.Test;
import org.zwobble.precisely.Matcher;
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
    public void keyCanBeQuoted() throws IOException {
        // Leave the testing of escape sequences to the string value tests.
        var result = parse("\"one two\" = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("one two", isBool(true))
        )));
    }

    // == Booleans ==

    @Test
    public void valueTrue() throws IOException {
        var result = parse("x = true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isBool(true))
        )));
    }

    @Test
    public void valueFalse() throws IOException {
        var result = parse("x = false");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isBool(false))
        )));
    }

    // == Integers ==

    @Test
    public void integerZero() throws IOException {
        var result = parse("x = 0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(0))
        )));
    }

    @Test
    public void integerPositive() throws IOException {
        var result = parse("x = 12");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(12))
        )));
    }

    @Test
    public void integerNegative() throws IOException {
        var result = parse("x = -12");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(-12))
        )));
    }

    @Test
    public void intUnderscores() throws IOException {
        var result = parse("x = 1_23_4");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(1234))
        )));
    }

    // == Floats ==

    @Test
    public void floatZero() throws IOException {
        var result = parse("x = 0.0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(0))
        )));
    }

    @Test
    public void floatPositive() throws IOException {
        var result = parse("x = 12.34");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(12.34))
        )));
    }

    @Test
    public void floatNegative() throws IOException {
        var result = parse("x = -12.34");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(-12.34))
        )));
    }

    @Test
    public void floatUnderscores() throws IOException {
        var result = parse("x = -1_2.3_4");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(-12.34))
        )));
    }

    // == Strings ==

    @Test
    public void emptyString() throws IOException {
        var result = parse(
            """
            x = ""
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString(""))
        )));
    }

    @Test
    public void stringWithAscii() throws IOException {
        var result = parse(
            """
            x = "abc"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc"))
        )));
    }

    @Test
    public void stringWithCompactEscapeSequence() throws IOException {
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

    private Matcher<TomlValue> isInt(long value) {
        return instanceOf(
            TomlInt.class,
            has("value", x -> x.value(), equalTo(value))
        );
    }

    private Matcher<TomlValue> isFloat(double value) {
        return instanceOf(
            TomlFloat.class,
            has("value", x -> x.value(), equalTo(value))
        );
    }

    private Matcher<TomlValue> isString(String value) {
        return instanceOf(
            TomlString.class,
            has("value", x -> x.value(), equalTo(value))
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
}
