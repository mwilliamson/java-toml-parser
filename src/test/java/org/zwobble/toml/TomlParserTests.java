package org.zwobble.toml;

import org.junit.jupiter.api.Test;
import org.zwobble.toml.values.TomlBool;
import org.zwobble.toml.values.TomlKeyValuePair;
import org.zwobble.toml.values.TomlString;
import org.zwobble.toml.values.TomlTable;

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
