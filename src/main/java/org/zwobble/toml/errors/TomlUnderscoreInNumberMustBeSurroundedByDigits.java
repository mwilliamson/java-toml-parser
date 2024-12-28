package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlUnderscoreInNumberMustBeSurroundedByDigits extends TomlParseError {
    public TomlUnderscoreInNumberMustBeSurroundedByDigits(SourceRange sourceRange) {
        super("Underscore in number must be surrounded by digits", sourceRange);
    }
}
