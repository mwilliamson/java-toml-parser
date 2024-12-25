package org.zwobble.toml;

import org.zwobble.toml.values.TomlArray;
import org.zwobble.toml.values.TomlKeyValuePair;
import org.zwobble.toml.values.TomlTable;
import org.zwobble.toml.values.TomlValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TomlTableBuilder {
    private final TomlTable table;
    private final List<TomlKeyValuePair> keyValuePairs;
    private final Map<String, TomlTableBuilder> subTableBuilders;
    private final Map<String, List<TomlValue>> arrayOfTables;

    TomlTableBuilder() {
        this.keyValuePairs = new ArrayList<>();
        this.table = new TomlTable(this.keyValuePairs);
        this.subTableBuilders = new HashMap<>();
        this.arrayOfTables = new HashMap<>();
    }

    TomlTable toTable() {
        return this.table;
    }

    TomlTableBuilder getOrCreateSubTable(String key) {
        // TODO: handle not a table
        var subTable = this.subTableBuilders.get(key);

        if (subTable == null) {
            subTable = new TomlTableBuilder();
            this.subTableBuilders.put(key, subTable);
            this.keyValuePairs.add(TomlKeyValuePair.of(key, subTable.table));
        }

        return subTable;
    }

    TomlTableBuilder createArraySubTable(String key) {
        // TODO: handle inline array or not a array

        if (!this.arrayOfTables.containsKey(key)) {
            var arrayOfTables = new ArrayList<TomlValue>();
            this.arrayOfTables.put(key, arrayOfTables);
            this.keyValuePairs.add(TomlKeyValuePair.of(key, TomlArray.of(arrayOfTables)));
        }

        var subTable = new TomlTableBuilder();
        this.arrayOfTables.get(key).add(subTable.table);
        this.subTableBuilders.put(key, subTable);
        return subTable;
    }

    void add(String key, TomlValue value) {
        this.keyValuePairs.add(TomlKeyValuePair.of(key, value));
    }
}