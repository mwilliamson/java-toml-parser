package org.zwobble.toml;

import org.junit.jupiter.api.Test;
import org.zwobble.toml.values.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TomlParserTests {
    @Test
    public void emptyFile() throws IOException {
        var result = parse("");

        assertEquals(result, TomlTable.of(List.of()));
    }

    @Test
    public void onlyLf() throws IOException {
        var result = parse("\n");

        assertEquals(result, TomlTable.of(List.of()));
    }

    @Test
    public void onlyCrLf() throws IOException {
        var result = parse("\r\n");

        assertEquals(result, TomlTable.of(List.of()));
    }

    // == Keys ==

    @Test
    public void bareKeyCanBeLowercaseAsciiLetters() throws IOException {
        var result = parse("abc = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("abc", new TomlBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeUppercaseAsciiLetters() throws IOException {
        var result = parse("ABC = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("ABC", new TomlBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeAsciiDigits() throws IOException {
        var result = parse("123 = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("123", new TomlBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeHyphens() throws IOException {
        var result = parse("--- = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("---", new TomlBool(true))
        )));
    }

    @Test
    public void bareKeyCanBeUnderscores() throws IOException {
        var result = parse("___ = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("___", new TomlBool(true))
        )));
    }

    @Test
    public void keyCanBeQuoted() throws IOException {
        // Leave the testing of escape sequences to the string value tests.
        var result = parse("\"one two\" = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("one two", new TomlBool(true))
        )));
    }

    // == Booleans ==

    @Test
    public void valueTrue() throws IOException {
        var result = parse("x = true");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlBool(true))
        )));
    }

    @Test
    public void valueFalse() throws IOException {
        var result = parse("x = false");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlBool(false))
        )));
    }

    // == Floats ==

    @Test
    public void floatZero() throws IOException {
        var result = parse("x = 0.0");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlFloat(0))
        )));
    }

    @Test
    public void floatPositive() throws IOException {
        var result = parse("x = 12.34");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlFloat(12.34))
        )));
    }

    @Test
    public void floatNegative() throws IOException {
        var result = parse("x = -12.34");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlFloat(-12.34))
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

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlString(""))
        )));
    }

    @Test
    public void stringWithAscii() throws IOException {
        var result = parse(
            """
            x = "abc"
            """
        );

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", new TomlString("abc"))
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

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("backspace", new TomlString("\b")),
            TomlKeyValuePair.of("tab", new TomlString("\t")),
            TomlKeyValuePair.of("linefeed", new TomlString("\n")),
            TomlKeyValuePair.of("formfeed", new TomlString("\f")),
            TomlKeyValuePair.of("carriagereturn", new TomlString("\r")),
            TomlKeyValuePair.of("quote", new TomlString("\"")),
            TomlKeyValuePair.of("backslash", new TomlString("\\"))
        )));
    }

    // == Arrays ==

    @Test
    public void emptyInlineArray() throws IOException {
        var result = parse("x = []");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", TomlArray.of(List.of()))
        )));
    }

    @Test
    public void singletonArray() throws IOException {
        var result = parse("x = [true]");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", TomlArray.of(List.of(
                new TomlBool(true)
            )))
        )));
    }

    @Test
    public void arrayWithMultipleValuesAndNoTrailingComma() throws IOException {
        var result = parse("x = [true,false,1]");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", TomlArray.of(List.of(
                new TomlBool(true),
                new TomlBool(false),
                new TomlInt(1)
            )))
        )));
    }

    @Test
    public void arrayWithMultipleValuesAndTrailingComma() throws IOException {
        var result = parse("x = [true,false,1,]");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", TomlArray.of(List.of(
                new TomlBool(true),
                new TomlBool(false),
                new TomlInt(1)
            )))
        )));
    }

    @Test
    public void arrayWithWhitespaceAroundCommas() throws IOException {
        var result = parse("x = [  true  , false , 1 , ]");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("x", TomlArray.of(List.of(
                new TomlBool(true),
                new TomlBool(false),
                new TomlInt(1)
            )))
        )));
    }

    // == Comments ==

    @Test
    public void onlyLineCommentNoEol() throws IOException {
        var result = parse("# a");

        assertEquals(result, TomlTable.of(List.of()));
    }

    @Test
    public void onlyLineCommentWithLf() throws IOException {
        var result = parse("# a\n");

        assertEquals(result, TomlTable.of(List.of()));
    }

    @Test
    public void onlyLineCommentWithCrLf() throws IOException {
        var result = parse("# a\r\n");

        assertEquals(result, TomlTable.of(List.of()));
    }

    @Test
    public void lineCommentWithLf() throws IOException {
        var result = parse("a = true\n# a\nb = false");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("a", new TomlBool(true)),
            TomlKeyValuePair.of("b", new TomlBool(false))
        )));
    }

    @Test
    public void lineCommentWithCrLf() throws IOException {
        var result = parse("a = true\n# a\r\nb = false");

        assertEquals(result, TomlTable.of(List.of(
            TomlKeyValuePair.of("a", new TomlBool(true)),
            TomlKeyValuePair.of("b", new TomlBool(false))
        )));
    }

    private TomlTable parse(String text) throws IOException {
        return TomlParser.parseReader(new StringReader(text));
    }
}
