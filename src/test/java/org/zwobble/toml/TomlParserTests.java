package org.zwobble.toml;

import org.junit.jupiter.api.Test;
import org.zwobble.toml.values.TomlBool;
import org.zwobble.toml.values.TomlKeyValuePair;
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

    @Test
    public void onlyLineComment() throws IOException {
        var result = parse("# a");

        assertEquals(result, TomlTable.of(List.of()));
    }

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

    private TomlTable parse(String text) throws IOException {
        return TomlParser.parseReader(new StringReader(text));
    }
}
