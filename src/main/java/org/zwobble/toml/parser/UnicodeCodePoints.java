package org.zwobble.toml.parser;

public class UnicodeCodePoints {
    private UnicodeCodePoints() {
    }

    public static String formatCodePoint(int codePoint) {
        if (codePoint == '\r') {
            return "CR";
        } else if (codePoint == '\n') {
            return "LF";
        } else if (codePoint == -1) {
            return "EOF";
        } else {
            return new String(new int[] {codePoint}, 0, 1);
        }
    }
}
