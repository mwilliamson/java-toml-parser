package org.zwobble.toml.parser;

import org.zwobble.toml.sources.SourceRange;

record TomlKey(String value, SourceRange sourceRange) {}
