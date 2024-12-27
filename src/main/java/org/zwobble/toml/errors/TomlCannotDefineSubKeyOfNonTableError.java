package org.zwobble.toml.errors;

import org.zwobble.toml.sources.SourceRange;

public class TomlCannotDefineSubKeyOfNonTableError extends TomlParseError {
    public TomlCannotDefineSubKeyOfNonTableError(SourceRange sourceRange) {
        super("Cannot define sub-key of a value that isn't a table", sourceRange);
    }
}
