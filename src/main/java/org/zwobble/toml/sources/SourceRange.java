package org.zwobble.toml.sources;

public class SourceRange {
    private final SourcePosition start;
    private final SourcePosition end;

    SourceRange(
        SourcePosition start,
        SourcePosition end
    ) {
        this.start = start;
        this.end = end;
    }

    public SourcePosition start() {
        return start;
    }

    public SourcePosition end() {
        return end;
    }
}
