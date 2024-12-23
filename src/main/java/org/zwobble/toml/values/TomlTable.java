package org.zwobble.toml.values;

import java.util.Iterator;
import java.util.List;

public final class TomlTable implements TomlValue, Iterable<TomlKeyValuePair> {
    public static TomlTable of(List<TomlKeyValuePair> pairs) {
        return new TomlTable(pairs);
    }

    private final List<TomlKeyValuePair> pairs;

    public TomlTable(List<TomlKeyValuePair> pairs) {
        this.pairs = pairs;
    }

    @Override
    public Iterator<TomlKeyValuePair> iterator() {
        return this.pairs.iterator();
    }
}
