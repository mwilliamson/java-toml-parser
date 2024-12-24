package org.zwobble.toml.values;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class TomlArray implements TomlValue, Iterable<TomlValue> {
    public static TomlArray of(List<TomlValue> elements) {
        return new TomlArray(elements);
    }

    private final List<TomlValue> elements;

    private TomlArray(List<TomlValue> elements) {
        this.elements = elements;
    }

    public Iterable<TomlValue> elements() {
        return this.elements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TomlArray tomlArray = (TomlArray) o;
        return Objects.equals(elements, tomlArray.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

    @Override
    public String toString() {
        return "TomlArray(" +
            "elements=" + elements +
            ')';
    }

    @Override
    public Iterator<TomlValue> iterator() {
        return elements.iterator();
    }
}
