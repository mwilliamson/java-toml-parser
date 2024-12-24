package org.zwobble.toml.sources;

public record SourcePosition(int codePointIndex) {
    public SourceRange to(SourcePosition end) {
        return new SourceRange(this, end);
    }
}
