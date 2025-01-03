package org.zwobble.toml.parser;

import org.junit.jupiter.api.Test;
import org.zwobble.precisely.Matcher;
import org.zwobble.toml.errors.*;
import org.zwobble.toml.sources.SourceRange;
import org.zwobble.toml.values.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    public void whenCodepointIsSurrogateThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidUtf8Error.class,
            () -> TomlParser.parseInputStream(
                new ByteArrayInputStream(new byte[] {'#', (byte)0xed, (byte)0xa0, (byte)0x80})
            )
        );

        // TODO: better source range
        assertThat(error.sourceRange(), isSourceRange(0, 0));
    }

    // == Key/Value Pairs ==

    @Test
    public void keySurroundedByNoWhitespace() throws IOException {
        var result = parse("abc=true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("abc", isBool(true))
        )));
    }

    @Test
    public void keySurroundedByWhitespace() throws IOException {
        var result = parse("  abc  =true");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("abc", isBool(true))
        )));
    }

    @Test
    public void whenKeyHasNoSubsequentTokensOnLineThenErrorIsThrown()  throws IOException {
        var error = assertThrows(
            TomlKeyValuePairMissingEqualsSignError.class,
            () -> parse("x\ny = 1")
        );

        assertThat(error.actual(), equalTo("LF"));
        assertThat(error.sourceRange(), isSourceRange(1, 1));
    }

    @Test
    public void whenKeyIsFollowedByNonEqualsSignThenErrorIsThrown()  throws IOException {
        var error = assertThrows(
            TomlKeyValuePairMissingEqualsSignError.class,
            () -> parse("x + 1\ny = 1")
        );

        assertThat(error.actual(), equalTo("+"));
        assertThat(error.sourceRange(), isSourceRange(2, 2));
    }

    @Test
    public void whenKeyHasUnspecifiedValueThenErrorIsThrow()  throws IOException {
        var error = assertThrows(
            TomlUnspecifiedValueError.class,
            () -> parse("x =")
        );

        assertThat(error.sourceRange(), isSourceRange(3, 3));
    }

    @Test
    public void whenTextFollowsKeyValuePairBeforeEofThenErrorIsThrown()  throws IOException {
        var error = assertThrows(
            TomlUnexpectedTextAtEolError.class,
            () -> parse("x = 1 y = 2")
        );

        assertThat(error.unexpectedText(), equalTo("y = 2"));
        assertThat(error.sourceRange(), isSourceRange(6, 11));
    }

    @Test
    public void whenTextFollowsKeyValuePairBeforeNewLineThenErrorIsThrown()  throws IOException {
        var error = assertThrows(
            TomlUnexpectedTextAtEolError.class,
            () -> parse("x = 1 y = 2\nz = 3")
        );

        assertThat(error.unexpectedText(), equalTo("y = 2"));
        assertThat(error.sourceRange(), isSourceRange(6, 11));
    }

    @Test
    public void whenTextFollowsKeyValuePairBeforeCommentThenErrorIsThrown()  throws IOException {
        var error = assertThrows(
            TomlUnexpectedTextAtEolError.class,
            () -> parse("x = 1 y = 2 # Invalid")
        );

        assertThat(error.unexpectedText(), equalTo("y = 2"));
        assertThat(error.sourceRange(), isSourceRange(6, 11));
    }

    @Test
    public void whenKeyIsDefinedTwiceThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = true\na = true")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(9, 10));
    }

    @Test
    public void whenKeyIsDefinedAsPrimitiveThenDefiningSubKeyThrowsError() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = true\na.b = true")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(9, 10));
    }

    // == Keys ==

    // === Bare keys ===

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

    // === Quoted keys ===

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

    @Test
    public void keyCannotBeMultiLineBasicString() throws IOException {
        var error = assertThrows(
            TomlKeyCannotBeMultiLineStringError.class,
            () -> parse("\"\"\"one two\"\"\" = true")
        );

        assertThat(error.sourceRange(), isSourceRange(2, 3));
    }

    // === Dotted keys ===

    @Test
    public void dottedKeyInRootTableCreatesSubTable() throws IOException {
        var result = parse("""
            a.b = true
            a.c = false
            b.a = 1
            """);

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isTable(isSequence(
                isKeyValuePair("b", isBool(true)),
                isKeyValuePair("c", isBool(false))
            ))),
            isKeyValuePair("b", isTable(isSequence(
                isKeyValuePair("a", isInt(1))
            )))
        )));
    }

    @Test
    public void dottedKeysMayContainWhitespace() throws IOException {
        var result = parse("""
            a  .  b = true
            """);

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isTable(isSequence(
                isKeyValuePair("b", isBool(true))
            )))
        )));
    }

    @Test
    public void dottedKeyCannotRedefineTable() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("[a.b]\n[a]\nb.c = true\n")
        );

        assertThat(error.key(), equalTo("b"));
        assertThat(error.sourceRange(), isSourceRange(10, 11));
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
    public void integerZeroWithPlusSign() throws IOException {
        var result = parse("x = +0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(0, isSourceRange(4, 6)))
        )));
    }

    @Test
    public void integerZeroWithMinusSign() throws IOException {
        var result = parse("x = -0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(0, isSourceRange(4, 6)))
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
    public void whenIntStartsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = _1")
        );

        assertThat(error.sourceRange(), isSourceRange(4, 5));
    }

    @Test
    public void whenIntHasDoubleUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 1__1")
        );

        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void whenIntEndsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 1_")
        );

        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void intBinary() throws IOException {
        var result = parse("x = 0b1101");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(13, isSourceRange(4, 10)))
        )));
    }

    @Test
    public void whenIntBinaryHasOnlyPrefixThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 0b")
        );

        assertThat(error.sourceRange(), isSourceRange(4, 6));
    }

    @Test
    public void intBinaryUnderscores() throws IOException {
        var result = parse("x = 0b1_10_1");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isInt(13, isSourceRange(4, 12)))
        )));
    }

    @Test
    public void whenIntBinaryHasUnderscoreAfterPrefixThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0b_1")
        );

        assertThat(error.sourceRange(), isSourceRange(6, 7));
    }

    @Test
    public void whenIntBinaryHasDoubleUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0b1__1")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenIntBinaryEndsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0b1_")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
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
    public void whenIntOctalHasUnderscoreAfterPrefixThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0o_1")
        );

        assertThat(error.sourceRange(), isSourceRange(6, 7));
    }

    @Test
    public void whenIntOctalHasDoubleUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0o1__1")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenIntOctalEndsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0o1_")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
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

    @Test
    public void whenIntHexHasUnderscoreAfterPrefixThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0x_1")
        );

        assertThat(error.sourceRange(), isSourceRange(6, 7));
    }

    @Test
    public void whenIntHexHasDoubleUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0x1__1")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenIntHexEndsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0x1_")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenIntIsLargerThan64BitsThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 999999999999999999999999999999999999")
        );

        assertThat(error.numberString(), equalTo("999999999999999999999999999999999999"));
        assertThat(error.sourceRange(), isSourceRange(4, 40));
    }

    @Test
    public void whenIntHasLeadingZeroThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 01")
        );

        assertThat(error.numberString(), equalTo("01"));
        assertThat(error.sourceRange(), isSourceRange(4, 6));
    }

    @Test
    public void whenIntHasLeadingZeroAfterPlusSignThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = +01")
        );

        assertThat(error.numberString(), equalTo("+01"));
        assertThat(error.sourceRange(), isSourceRange(4, 7));
    }

    @Test
    public void whenIntHasLeadingZeroAfterMinusSignThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = -01")
        );

        assertThat(error.numberString(), equalTo("-01"));
        assertThat(error.sourceRange(), isSourceRange(4, 7));
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
    public void floatZeroWithPositiveSign() throws IOException {
        var result = parse("x = +0.0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(0, isSourceRange(4, 8)))
        )));
    }

    @Test
    public void floatZeroWithNegativeSign() throws IOException {
        var result = parse("x = -0.0");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isFloat(-0.0, isSourceRange(4, 8)))
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
    public void whenFloatStartsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = _1.0")
        );

        assertThat(error.sourceRange(), isSourceRange(4, 5));
    }

    @Test
    public void whenFloatHasDoubleUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0.1__1")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenFloatEndsWithUnderscoreThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0.1_")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenFloatHasUnderscoreBeforeDotThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0_.1")
        );

        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void whenFloatHasUnderscoreAfterDotThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0_.1")
        );

        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void whenFloatHasUnderscoreBeforeEThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0.1_e1")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenFloatHasUnderscoreAfterEThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnderscoreInNumberMustBeSurroundedByDigits.class,
            () -> parse("x = 0e_1")
        );

        assertThat(error.sourceRange(), isSourceRange(6, 7));
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

    @Test
    public void whenFloatHasMultipleDotsThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 1.2.3")
        );

        assertThat(error.numberString(), equalTo("1.2.3"));
        assertThat(error.sourceRange(), isSourceRange(4, 9));
    }

    @Test
    public void whenFloatHasMultipleEsThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 1e2e3")
        );

        assertThat(error.numberString(), equalTo("1e2e3"));
        assertThat(error.sourceRange(), isSourceRange(4, 9));
    }

    @Test
    public void whenFloatHasNothingBeforeDotThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = .7")
        );

        assertThat(error.numberString(), equalTo(".7"));
        assertThat(error.sourceRange(), isSourceRange(4, 6));
    }

    @Test
    public void whenFloatHasNothingAfterDotThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 7.")
        );

        assertThat(error.numberString(), equalTo("7."));
        assertThat(error.sourceRange(), isSourceRange(4, 6));
    }

    @Test
    public void whenFloatHasDotBeforeEThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 7.e2")
        );

        assertThat(error.numberString(), equalTo("7.e2"));
        assertThat(error.sourceRange(), isSourceRange(4, 8));
    }

    @Test
    public void whenFloatHasDotAfterEThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 7e.2")
        );

        assertThat(error.numberString(), equalTo("7e.2"));
        assertThat(error.sourceRange(), isSourceRange(4, 8));
    }

    @Test
    public void whenFloatHasLeadingZeroThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = 01.0")
        );

        assertThat(error.numberString(), equalTo("01.0"));
        assertThat(error.sourceRange(), isSourceRange(4, 8));
    }

    @Test
    public void whenFloatHasPositiveSignThenLeadingZeroThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = +01.0")
        );

        assertThat(error.numberString(), equalTo("+01.0"));
        assertThat(error.sourceRange(), isSourceRange(4, 9));
    }

    @Test
    public void whenFloatHasNegativeSignThenLeadingZeroThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidNumberError.class,
            () -> parse("x = -01.0")
        );

        assertThat(error.numberString(), equalTo("-01.0"));
        assertThat(error.sourceRange(), isSourceRange(4, 9));
    }

    // == Strings ==

    // === Basic Strings ===

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
    public void basicStringWithUnicodeCodePointInBmp() throws IOException {
        var result = parse(
            """
            x = "\u03c0"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void basicStringWithUnicodeCodePointOutsideBmp() throws IOException {
        var result = parse(
            """
            x = "\ud83e\udd67"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void basicStringsMayContainTabs() throws IOException {
        var result = parse(
            """
            x = "\t"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\t"))
        )));
    }

    @Test
    public void controlCharactersBesidesTabAreNotPermittedInBasicStrings() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = "\u0007"
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int) '\u0007'));
        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void whenBasicStringIsUnclosedThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnclosedStringError.class,
            () -> parse("x = \"")
        );

        assertThat(error.sourceRange(), isSourceRange(5, 5));
    }

    // === Multi-line Basic Strings ===

    @Test
    public void emptyMultiLineBasicStringOnOneLine() throws IOException {
        var result = parse(
            """
            x = \"\"\"\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 10)))
        )));
    }

    @Test
    public void emptyMultiLineBasicStringOnTwoLinesUsingLf() throws IOException {
        var result = parse(
            """
            x = \"\"\"
            \"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 11)))
        )));
    }

    @Test
    public void emptyMultiLineBasicStringOnTwoLinesUsingCrLf() throws IOException {
        var result = parse(
            """
            x = \"\"\"\r
            \"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 12)))
        )));
    }

    @Test
    public void multiLineBasicStringStartingWithCrNoLfIsNotPermitted() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = \"\"\"\r\"\"\"
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int) '\r'));
        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void multiLineBasicStringWithAscii() throws IOException {
        var result = parse(
            """
            x = \"\"\"abc\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineBasicStringWithNewLines() throws IOException {
        var result = parse(
            """
            x = \"\"\"
            abc
            
            def
            
            
            ghi
            \"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc\n\ndef\n\n\nghi\n", isSourceRange(4, 26)))
        )));
    }

    @Test
    public void multiLineBasicStringWithLineEndingBackslashesAndLfs() throws IOException {
        var result = parse(
            """
            x = \"\"\"\\
            
            abc
            
            def\\
            
            ghi
            \\
            
            j\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc\n\ndefghi\nj", isSourceRange(4, 32)))
        )));
    }

    @Test
    public void multiLineBasicStringWithLineEndingBackslashesAndCrLfsAfterLf() throws IOException {
        var result = parse(
            """
            x = \"\"\"a\\
            \r
            
            \r
            \r
            b\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ab", isSourceRange(4, 21)))
        )));
    }

    @Test
    public void multiLineBasicStringWithLineEndingBackslashesAndCrLfs() throws IOException {
        var result = parse(
            """
            x = \"\"\"\\\r
            \r
            abc\r
            \r
            def\\\r
            \r
            ghi\r
            \\\r
            \r
            j\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc\r\n\r\ndefghi\r\nj", isSourceRange(4, 41)))
        )));
    }

    @Test
    public void multiLineBasicStringWithLineEndingBackslashesAndLfsAndWhitespace() throws IOException {
        var result = parse(
            "x = \"\"\"\\\n" +
            "\n" +
            "abc\n" +
            " \n" +
            "def\\\n" +
            "   \n" +
            "ghi\n" +
            "\\\n" +
            "   \n" +
            "j\"\"\""
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc\n \ndefghi\nj", isSourceRange(4, 39)))
        )));
    }

    @Test
    public void multiLineBasicStringWithLineEndingBackslashesAndWhitespaceBeforeContent() throws IOException {
        var result = parse(
            "x = \"\"\"\n" +
                "a\\\n" +
                "  b\"\"\""
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ab", isSourceRange(4, 17)))
        )));
    }

    @Test
    public void multiLineBasicStringWithLineEndingBackslashesFollowedByWhitespaceBeforeLf() throws IOException {
        var result = parse(
            "x = \"\"\"\n" +
                "a\\  \n" +
                "b\"\"\""
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ab", isSourceRange(4, 17)))
        )));
    }

    @Test
    public void multiLineBasicStringInitialNewLineIsIgnored() throws IOException {
        var result = parse("x = \"\"\"\na\"\"\"");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("a", isSourceRange(4, 12)))
        )));
    }

    @Test
    public void multiLineBasicStringOnlyFirstOfInitialNewLinesIsIgnored() throws IOException {
        var result = parse("x = \"\"\"\n\n\na\"\"\"");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\n\na", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineBasicStringWithOneUnescapedDoubleQuoteInMiddle() throws IOException {
        var result = parse(
            """
            x = \"\"\"a"c\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("a\"c", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineBasicStringWithTwoUnescapedDoubleQuotesInMiddle() throws IOException {
        var result = parse(
            """
            x = \"\"\"a""c\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("a\"\"c", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineBasicStringWithOneUnescapedDoubleQuoteAtStart() throws IOException {
        var result = parse(
            """
            x = \"\"\""ac\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\"ac", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineBasicStringWithTwoUnescapedDoubleQuotesAtStart() throws IOException {
        var result = parse(
            """
            x = \"\"\"""ac\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\"\"ac", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineBasicStringWithOneUnescapedDoubleQuoteAtEnd() throws IOException {
        var result = parse(
            """
            x = \"\"\"ac"\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ac\"", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineBasicStringWithTwoUnescapedDoubleQuotesAtEnd() throws IOException {
        var result = parse(
            """
            x = \"\"\"ac""\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ac\"\"", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineBasicStringOnOneLine() throws IOException {
        var result = parse(
            """
            x = \"\"\"abc\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineBasicStringOnMultipleLines() throws IOException {
        var result = parse(
            """
            x = \"\"\"abc
            def
            
            gh\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc\ndef\n\ngh", isSourceRange(4, 21)))
        )));
    }

    @Test
    public void multiLineBasicStringWithCompactEscapeSequence() throws IOException {
        var result = parse(
            """
            backspace = \"\"\"\\b\"\"\"
            tab = \"\"\"\\t\"\"\"
            linefeed = \"\"\"\\n\"\"\"
            formfeed = \"\"\"\\f\"\"\"
            carriagereturn = \"\"\"\\r\"\"\"
            quote = \"\"\"\\"\"\"\"
            backslash = \"\"\"\\\\\"\"\"
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
    public void multiLineBasicStringWithFourDigitLowercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = \"\"\"\\u03c0\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void multiLineBasicStringWithFourDigitUppercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = \"\"\"\\u03C0\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void multiLineBasicStringWithEightDigitLowercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = \"\"\"\\U0001f967\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void multiLineBasicStringWithEightDigitUppercaseUnicodeEscape() throws IOException {
        var result = parse(
            """
            x = \"\"\"\\U0001F967\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void multiLineBasicStringWithUnicodeCodePointInBmp() throws IOException {
        var result = parse(
            """
            x = \"\"\"\u03c0\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void multiLineBasicStringWithUnicodeCodePointOutsideBmp() throws IOException {
        var result = parse(
            """
            x = \"\"\"\ud83e\udd67\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void multiLineBasicStringsMayContainTabs() throws IOException {
        var result = parse(
            """
            x = \"\"\"\t\"\"\"
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\t"))
        )));
    }

    @Test
    public void controlCharactersBesidesTabAreNotPermittedInMultiLineBasicStrings() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = \"\"\"\u0007\"\"\"
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int) '\u0007'));
        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void carriageReturnWithoutLineFeedIsNotNotPermittedInMultiLineBasicString() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = \"\"\"
                \r\"\"\"
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int)'\r'));
        assertThat(error.sourceRange(), isSourceRange(8, 9));
    }

    @Test
    public void whenMultiLineBasicStringIsUnclosedThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnclosedStringError.class,
            () -> parse("x = \"\"\"")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 7));
    }

    // === Literal Strings ===

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

    @Test
    public void literalStringWithUnicodeCodePointInBmp() throws IOException {
        var result = parse(
            """
            x = '\u03c0'
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void literalStringWithUnicodeCodePointOutsideBmp() throws IOException {
        var result = parse(
            """
            x = '\ud83e\udd67'
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void literalStringsMayContainTabs() throws IOException {
        var result = parse(
            """
            x = '\t'
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\t"))
        )));
    }

    @Test
    public void controlCharactersBesidesTabAreNotPermittedInLiteralStrings() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = '\u0007'
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int) '\u0007'));
        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void whenLiteralStringIsUnclosedThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnclosedStringError.class,
            () -> parse("x = '")
        );

        assertThat(error.sourceRange(), isSourceRange(5, 5));
    }

    // == Multi-Line Literal Strings ==

    @Test
    public void emptyMultiLineLiteralStringOnOneLine() throws IOException {
        var result = parse(
            """
            x = ''''''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 10)))
        )));
    }

    @Test
    public void emptyMultiLineLiteralStringOnTwoLines() throws IOException {
        var result = parse(
            """
            x = '''
            '''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("", isSourceRange(4, 11)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithOnlyNewLines() throws IOException {
        var result = parse(
            """
            x = '''
            
            
            '''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\n\n", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithAscii() throws IOException {
        var result = parse(
            """
            x = '''abc'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc", isSourceRange(4, 13)))
        )));
    }


    @Test
    public void multiLineLiteralStringInitialNewLineIsIgnored() throws IOException {
        var result = parse("x = '''\na'''");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("a", isSourceRange(4, 12)))
        )));
    }

    @Test
    public void multiLineLiteralStringOnlyFirstOfInitialNewLinesIsIgnored() throws IOException {
        var result = parse("x = '''\n\n\na'''");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\n\na", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithOneUnescapedSingleQuoteInMiddle() throws IOException {
        var result = parse(
            """
            x = '''a'c'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("a'c", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithTwoUnescapedSingleQuotesInMiddle() throws IOException {
        var result = parse(
            """
            x = '''a''c'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("a''c", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithOneUnescapedSingleQuoteAtStart() throws IOException {
        var result = parse(
            """
            x = ''''ac'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("'ac", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithTwoUnescapedSingleQuotesAtStart() throws IOException {
        var result = parse(
            """
            x = '''''ac'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("''ac", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithOneUnescapedSingleQuoteAtEnd() throws IOException {
        var result = parse(
            """
            x = '''ac''''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ac'", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithTwoUnescapedSingleQuotesAtEnd() throws IOException {
        var result = parse(
            """
            x = '''ac'''''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("ac''", isSourceRange(4, 14)))
        )));
    }

    @Test
    public void multiLineLiteralStringOnOneLine() throws IOException {
        var result = parse(
            """
            x = '''abc'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc", isSourceRange(4, 13)))
        )));
    }

    @Test
    public void multiLineLiteralStringOnMultipleLines() throws IOException {
        var result = parse(
            """
            x = '''abc
            def
            
            gh'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("abc\ndef\n\ngh", isSourceRange(4, 21)))
        )));
    }

    @Test
    public void multiLineLiteralStringWithCompactEscapeSequence() throws IOException {
        var result = parse(
            """
            backspace = '''\\b'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("backspace", isString("\\b"))
        )));
    }

    @Test
    public void multiLineLiteralStringWithUnicodeCodePointInBmp() throws IOException {
        var result = parse(
            """
            x = '''\u03c0'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\u03c0"))
        )));
    }

    @Test
    public void multiLineLiteralStringWithUnicodeCodePointOutsideBmp() throws IOException {
        var result = parse(
            """
            x = '''\ud83e\udd67'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\ud83e\udd67"))
        )));
    }

    @Test
    public void multiLineLiteralStringsMayContainTabs() throws IOException {
        var result = parse(
            """
            x = '''\t'''
            """
        );

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isString("\t"))
        )));
    }

    @Test
    public void controlCharactersBesidesTabAreNotPermittedInMultiLineLiteralStrings() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = '''\u0007'''
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int) '\u0007'));
        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void carriageReturnWithoutLineFeedIsNotNotPermittedInMultiLineLiteralString() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse(
                """
                x = '''
                \r'''
                """
            )
        );

        assertThat(error.controlCharacter(), equalTo((int) '\r'));
        assertThat(error.sourceRange(), isSourceRange(8, 9));
    }

    @Test
    public void whenMultiLineLiteralStringIsUnclosedThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlUnclosedStringError.class,
            () -> parse("x = '''")
        );

        assertThat(error.sourceRange(), isSourceRange(7, 7));
    }

    // === Invalid Unicode codepoints ===

    @Test
    public void givenEscapeSequenceInStringWhenCodepointIsLowSurrogateThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidEscapeSequenceError.class,
            () -> parse(
                """
                x = "\\udfff"
                """
            )
        );

        assertThat(error.sourceRange(), isSourceRange(7, 11));
    }

    @Test
    public void givenEscapeSequenceInStringWhenCodepointIsHighSurrogateThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidEscapeSequenceError.class,
            () -> parse(
                """
                x = "\\ud800"
                """
            )
        );

        assertThat(error.sourceRange(), isSourceRange(7, 11));
    }

    @Test
    public void givenEscapeSequenceInStringWhenCodepointIsOverMaximumUnicodeCodepointThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidEscapeSequenceError.class,
            () -> parse(
                """
                x = "\\U00110000"
                """
            )
        );

        assertThat(error.sourceRange(), isSourceRange(7, 15));
    }

    @Test
    public void givenEscapeSequenceInStringWhenCodepointIsMaximum32BitSequenceThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidEscapeSequenceError.class,
            () -> parse(
                """
                x = "\\Uffffffff"
                """
            )
        );

        assertThat(error.sourceRange(), isSourceRange(7, 15));
    }

    @Test
    public void whenUnicodeEscapeSequenceHasInvalidCharactersThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidEscapeSequenceError.class,
            () -> parse(
                """
                x = "\\ug000"
                """
            )
        );

        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    @Test
    public void whenEscapeSequenceUsesUnrecognizedSingleCharacterThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidEscapeSequenceError.class,
            () -> parse(
                """
                x = "\\p"
                """
            )
        );

        assertThat(error.sourceRange(), isSourceRange(6, 7));
    }

    // == Offset Date-Times ==

    @Test
    public void offsetDateTimeUppercaseTSeparator() throws IOException {
        var result = parse("x = 1979-05-27T07:32:00Z");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00Z"),
                isSourceRange(4, 24)
            ))
        )));
    }

    @Test
    public void offsetDateTimeLowercaseTSeparator() throws IOException {
        var result = parse("x = 1979-05-27t07:32:00z");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00Z"),
                isSourceRange(4, 24)
            ))
        )));
    }

    @Test
    public void offsetDateTimeSpaceSeparator() throws IOException {
        var result = parse("x = 1979-05-27 07:32:00Z");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00Z"),
                isSourceRange(4, 24)
            ))
        )));
    }

    @Test
    public void offsetDateTimeUppercaseZTimezone() throws IOException {
        var result = parse("x = 1979-05-27T07:32:00Z");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00Z"),
                isSourceRange(4, 24)
            ))
        )));
    }

    @Test
    public void offsetDateTimeLowercaseZTimezone() throws IOException {
        var result = parse("x = 1979-05-27t07:32:00z");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00Z"),
                isSourceRange(4, 24)
            ))
        )));
    }

    @Test
    public void offsetDateTimePositiveOffsetTimezone() throws IOException {
        var result = parse("x = 1979-05-27t07:32:00+01:00");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00+01:00"),
                isSourceRange(4, 29)
            ))
        )));
    }

    @Test
    public void offsetDateTimeNegativeOffsetTimezone() throws IOException {
        var result = parse("x = 1979-05-27t07:32:00-01:00");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00-01:00"),
                isSourceRange(4, 29)
            ))
        )));
    }

    @Test
    public void offsetDateTimeUppercaseFractionalSeconds() throws IOException {
        var result = parse("x = 1979-05-27T07:32:00.123Z");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isOffsetDateTime(
                OffsetDateTime.parse("1979-05-27T07:32:00.123Z"),
                isSourceRange(4, 28)
            ))
        )));
    }

    @Test
    public void whenOffsetDateTimeIsInvalidThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidOffsetDateTimeError.class,
            () -> parse("x = 1979-00-27T07:32:00.123Z")
        );

        assertThat(error.offsetDateTimeString(), equalTo("1979-00-27T07:32:00.123Z"));
        assertThat(error.sourceRange(), isSourceRange(4, 28));
    }

    // == Local Date-Times ==

    @Test
    public void localDateTimeUppercaseTSeparator() throws IOException {
        var result = parse("x = 1979-05-27T07:32:00");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalDateTime(
                LocalDateTime.parse("1979-05-27T07:32:00"),
                isSourceRange(4, 23)
            ))
        )));
    }

    @Test
    public void localDateTimeLowercaseTSeparator() throws IOException {
        var result = parse("x = 1979-05-27t07:32:00");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalDateTime(
                LocalDateTime.parse("1979-05-27T07:32:00"),
                isSourceRange(4, 23)
            ))
        )));
    }

    @Test
    public void localDateTimeSpaceSeparator() throws IOException {
        var result = parse("x = 1979-05-27 07:32:00");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalDateTime(
                LocalDateTime.parse("1979-05-27T07:32:00"),
                isSourceRange(4, 23)
            ))
        )));
    }

    @Test
    public void localDateTimeUppercaseFractionalSeconds() throws IOException {
        var result = parse("x = 1979-05-27T07:32:00.123");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalDateTime(
                LocalDateTime.parse("1979-05-27T07:32:00.123"),
                isSourceRange(4, 27)
            ))
        )));
    }

    @Test
    public void whenLocalDateTimeIsInvalidThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidLocalDateTimeError.class,
            () -> parse("x = 1979-00-27T07:32:00.123")
        );

        assertThat(error.localDateTimeString(), equalTo("1979-00-27T07:32:00.123"));
        assertThat(error.sourceRange(), isSourceRange(4, 27));
    }

    // == Local Date ==

    @Test
    public void localDate() throws IOException {
        var result = parse("x = 1979-05-27");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalDate(
                LocalDate.parse("1979-05-27"),
                isSourceRange(4, 14)
            ))
        )));
    }

    @Test
    public void whenLocalDateIsInvalidThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidLocalDateError.class,
            () -> parse("x = 1979-00-27")
        );

        assertThat(error.localDateString(), equalTo("1979-00-27"));
        assertThat(error.sourceRange(), isSourceRange(4, 14));
    }

    // == Local Time ==

    @Test
    public void localTimeWithoutFractionalSeconds() throws IOException {
        var result = parse("x = 01:02:03");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalTime(
                LocalTime.parse("01:02:03"),
                isSourceRange(4, 12)
            ))
        )));
    }

    @Test
    public void localTimeWithFractionalSeconds() throws IOException {
        var result = parse("x = 01:02:03.456");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isLocalTime(
                LocalTime.parse("01:02:03.456"),
                isSourceRange(4, 16)
            ))
        )));
    }

    @Test
    public void whenLocalTimeIsInvalidThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlInvalidLocalTimeError.class,
            () -> parse("x = 99:99:99")
        );

        assertThat(error.localTimeString(), equalTo("99:99:99"));
        assertThat(error.sourceRange(), isSourceRange(4, 12));
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

    // == Tables ==

    @Test
    public void whenTableHeaderIsMissingKeyThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlMissingKeyError.class,
            () -> parse("[]")
        );

        assertThat(error.sourceRange(), isSourceRange(1, 1));
    }

    @Test
    public void emptyTable() throws IOException {
        var result = parse("[x]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence()))
        )));
    }

    @Test
    public void tableHeaderWithDottedKey() throws IOException {
        var result = parse("[x.y.z]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("y", isTable(isSequence(
                    isKeyValuePair("z", isTable(isSequence()))
                )))
            )))
        )));
    }

    @Test
    public void tableHeaderWithDottedKeyWithWhitespace() throws IOException {
        var result = parse("[  x  .  y  .  z  ]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("y", isTable(isSequence(
                    isKeyValuePair("z", isTable(isSequence()))
                )))
            )))
        )));
    }

    @Test
    public void afterTableHeaderThenKeyValuePairsBelongToTable() throws IOException {
        var result = parse("""
        [x.y.z]
        a = true
        """);

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("y", isTable(isSequence(
                    isKeyValuePair("z", isTable(isSequence(
                        isKeyValuePair("a", isBool(true))
                    )))
                )))
            )))
        )));
    }

    @Test
    public void whenTableRedefinesParentPrimitiveThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = true\n[a.b]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(10, 11));
    }

    @Test
    public void whenTableRedefinesPrimitiveThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = true\n[a]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(10, 11));
    }

    @Test
    public void tableHeaderCannotRedefineInlineTable() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = { x = true }\n[a]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(18, 19));
    }

    @Test
    public void tableHeaderCannotBeDefinedTwice() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("[a]\n[a]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(5, 6));
    }

    @Test
    public void tableHeaderCanDefineSubKeyOfExistingTableHeader() throws IOException {
        var result = parse("""
        [x]
        a = true
        [x.y]
        b = false
        """);

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("a", isBool(true)),
                isKeyValuePair("y", isTable(isSequence(
                    isKeyValuePair("b", isBool(false))
                )))
            )))
        )));
    }

    @Test
    public void tableHeaderCanDefineSuperKeyOfExistingTableHeader() throws IOException {
        var result = parse("""
        [x.y]
        b = false
        [x]
        a = true
        """);

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("y", isTable(isSequence(
                    isKeyValuePair("b", isBool(false))
                ))),
                isKeyValuePair("a", isBool(true))
            )))
        )));
    }

    @Test
    public void tableHeaderCannotDefineSuperTableOfExistingTableHeaderTwice() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("[[a.b]]\n[a]\n[a]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(13, 14));
    }

    @Test
    public void tableHeaderCannotRedefineTableDefinedByDottedKey() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a.b = true\n[a]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(12, 13));
    }

    @Test
    public void tableHeaderCanDefineSiblingTableOfTableDefinedByDottedKey() throws IOException {
        var result = parse("a.b = true\n[a.c]\n");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isTable(isSequence(
                isKeyValuePair("b", isBool(true)),
                isKeyValuePair("c", isTable(isSequence()))
            )))
        )));
    }

    @Test
    public void tableHeaderCannotDefineSiblingTableOfTableDefinedByDottedKeyTwice() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a.b = true\n[a.c]\n[a.c]\n")
        );

        assertThat(error.key(), equalTo("c"));
        assertThat(error.sourceRange(), isSourceRange(20, 21));
    }

    @Test
    public void cannotRedefineTableDefinedByArrayOfTables() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("[[a]]\n[a]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(7, 8));
    }

    // == Inline Tables ==

    @Test
    public void emptyInlineTable() throws IOException {
        var result = parse("""
            x = {}""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence()))
        )));
    }

    @Test
    public void emptyInlineTableWithWhitespace() throws IOException {
        var result = parse("""
            x = {  }""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence()))
        )));
    }

    @Test
    public void inlineTableWithSingleKeyValuePair() throws IOException {
        var result = parse("""
            x = { a = true }""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("a", isBool(true))
            )))
        )));
    }

    @Test
    public void inlineTableWithMultipleKeyValuePairs() throws IOException {
        var result = parse("""
            x = { a = true, b = false, c = 1 }""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("a", isBool(true)),
                isKeyValuePair("b", isBool(false)),
                isKeyValuePair("c", isInt(1))
            )))
        )));
    }

    // == Arrays of Tables ==

    @Test
    public void whenArrayOfTablesHeaderIsMissingKeyThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlMissingKeyError.class,
            () -> parse("[[]]")
        );

        assertThat(error.sourceRange(), isSourceRange(2, 2));
    }

    @Test
    public void emptyArrayOfTables() throws IOException {
        var result = parse("""
            [[x]]""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isTable(isSequence())
            )))
        )));
    }

    @Test
    public void emptyArrayOfTablesWithDottedKey() throws IOException {
        var result = parse("""
            [[x.y.z]]""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isTable(isSequence(
                isKeyValuePair("y", isTable(isSequence(
                    isKeyValuePair("z", isArray(isSequence(
                        isTable(isSequence())
                    )))
                )))
            )))
        )));
    }

    @Test
    public void arrayOfTablesHeadersAreRelativeToRoot() throws IOException {
        var result = parse("""
            [[a.b]]
            [[c.d]]""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isTable(isSequence(
                isKeyValuePair("b", isArray(isSequence(
                    isTable(isSequence())
                )))
            ))),
            isKeyValuePair("c", isTable(isSequence(
                isKeyValuePair("d", isArray(isSequence(
                    isTable(isSequence())
                )))
            )))
        )));
    }

    @Test
    public void arrayOfTablesSingleTableKeyValuePairs() throws IOException {
        var result = parse("""
            [[a]]
            b = true
            c = false""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isArray(isSequence(
                isTable(isSequence(
                    isKeyValuePair("b", isBool(true)),
                    isKeyValuePair("c", isBool(false))
                ))
            )))
        )));
    }

    @Test
    public void arrayOfTablesMultipleTablesKeyValuePairs() throws IOException {
        var result = parse("""
            [[a]]
            b = true
            c = false
            [[a]]
            b = false
            c = true""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isArray(isSequence(
                isTable(isSequence(
                    isKeyValuePair("b", isBool(true)),
                    isKeyValuePair("c", isBool(false))
                )),
                isTable(isSequence(
                    isKeyValuePair("b", isBool(false)),
                    isKeyValuePair("c", isBool(true))
                ))
            )))
        )));
    }

    @Test
    public void tableHeaderCanReferToExistingTableInArrayOfTables() throws IOException {
        var result = parse("""
            [[a]]
            [a.b]
            c = true
            [[a]]
            [a.b]
            c = false""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isArray(isSequence(
                isTable(isSequence(
                    isKeyValuePair("b", isTable(isSequence(
                        isKeyValuePair("c", isBool(true))
                    )))
                )),
                isTable(isSequence(
                    isKeyValuePair("b", isTable(isSequence(
                        isKeyValuePair("c", isBool(false))
                    )))
                ))
            )))
        )));
    }

    @Test
    public void arrayOfTablesHeaderWhitespace() throws IOException {
        var result = parse("""
            [[  x  ]]""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isTable(isSequence())
            )))
        )));
    }

    @Test
    public void whenArrayOfTablesRedefinesParentPrimitiveThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = true\n[[a.b]]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(11, 12));
    }

    @Test
    public void whenArrayOfTablesRedefinesPrimitiveThenErrorIsThrown() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = true\n[[a]]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(11, 12));
    }

    @Test
    public void tableOfArraysCannotAppendToInlineArray() throws IOException {
        var error = assertThrows(
            TomlDuplicateKeyError.class,
            () -> parse("a = []\n[[a]]\n")
        );

        assertThat(error.key(), equalTo("a"));
        assertThat(error.sourceRange(), isSourceRange(9, 10));
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

    @Test
    public void commentAfterKeyValuePair() throws IOException {
        var result = parse("a = true\n# a\nb = false");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isBool(true)),
            isKeyValuePair("b", isBool(false))
        )));
    }

    @Test
    public void multipleCommentsAfterKeyValuePair() throws IOException {
        var result = parse("a = true\n# a\n#b\nb = false");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isBool(true)),
            isKeyValuePair("b", isBool(false))
        )));
    }

    @Test
    public void multipleCommentsAfterArrayElement() throws IOException {
        var result = parse("a = [true# a\n#b\n,false]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isArray(isSequence(
                isBool(true),
                isBool(false)
            )))
        )));
    }

    @Test
    public void commentAfterArrayElementSeparator() throws IOException {
        var result = parse("a = [true,# a\nfalse]");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isArray(isSequence(
                isBool(true),
                isBool(false)
            )))
        )));
    }

    @Test
    public void commentAfterTableHeader() throws IOException {
        var result = parse("[a] # a");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("a", isTable(isSequence()))
        )));
    }

    @Test
    public void commentAfterArrayOfTablesHeader() throws IOException {
        var result = parse("""
            [[x]] # x""");

        assertThat(result, isTable(isSequence(
            isKeyValuePair("x", isArray(isSequence(
                isTable(isSequence())
            )))
        )));
    }

    @Test
    public void tabsArePermittedInComments() throws IOException {
        var result = parse("#\t");

        assertThat(result, isTable(isSequence()));
    }

    @Test
    public void crWithoutLfIsNotPermittedInComments() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse("#\r")
        );

        assertThat(error.controlCharacter(), equalTo(Integer.valueOf('\r')));
        assertThat(error.sourceRange(), isSourceRange(1, 2));
    }

    @Test
    public void controlCharactersBesidesTabAreNotPermittedInComments() throws IOException {
        var error = assertThrows(
            TomlUnexpectedControlCharacterError.class,
            () -> parse("#\u0007")
        );

        assertThat(error.controlCharacter(), equalTo(Integer.valueOf('\u0007')));
        assertThat(error.sourceRange(), isSourceRange(1, 2));
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

    private Matcher<TomlValue> isOffsetDateTime(
        OffsetDateTime value,
        Matcher<SourceRange> sourceRange
    ) {
        return instanceOf(
            TomlOffsetDateTime.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isLocalDateTime(
        LocalDateTime value,
        Matcher<SourceRange> sourceRange
    ) {
        return instanceOf(
            TomlLocalDateTime.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isLocalDate(
        LocalDate value,
        Matcher<SourceRange> sourceRange
    ) {
        return instanceOf(
            TomlLocalDate.class,
            has("value", x -> x.value(), equalTo(value)),
            has("sourceRange", x -> x.sourceRange(), sourceRange)
        );
    }

    private Matcher<TomlValue> isLocalTime(
        LocalTime value,
        Matcher<SourceRange> sourceRange
    ) {
        return instanceOf(
            TomlLocalTime.class,
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
