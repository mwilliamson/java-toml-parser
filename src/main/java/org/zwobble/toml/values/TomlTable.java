package org.zwobble.toml.values;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class TomlTable implements TomlValue, Iterable<TomlKeyValuePair> {
    public static TomlTable of(LinkedHashMap<String, TomlKeyValuePair> pairs) {
        return new TomlTable(pairs);
    }

    private final LinkedHashMap<String, TomlKeyValuePair> pairs;

    public TomlTable(LinkedHashMap<String, TomlKeyValuePair> pairs) {
        this.pairs = pairs;
    }

    public Iterable<TomlKeyValuePair> keyValuePairs() {
        return this.pairs.values();
    }

    @Override
    public Iterator<TomlKeyValuePair> iterator() {
        return this.keyValuePairs().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TomlTable that = (TomlTable) o;
        return Objects.equals(pairs, that.pairs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pairs);
    }

    @Override
    public String toString() {
        return "TomlTable(" +
            "pairs=" + pairs +
            ')';
    }
}
