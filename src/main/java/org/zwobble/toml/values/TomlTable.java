package org.zwobble.toml.values;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class TomlTable implements TomlValue, Iterable<TomlKeyValuePair> {
    public static TomlTable of(List<TomlKeyValuePair> pairs) {
        return new TomlTable(pairs);
    }

    private final List<TomlKeyValuePair> pairs;

    public TomlTable(List<TomlKeyValuePair> pairs) {
        this.pairs = pairs;
    }

    public Iterable<TomlKeyValuePair> keyValuePairs() {
        return this.pairs;
    }

    @Override
    public Iterator<TomlKeyValuePair> iterator() {
        return this.pairs.iterator();
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
