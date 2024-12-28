package org.zwobble.toml;

import org.zwobble.toml.errors.TomlCannotDefineSubKeyOfNonTableError;
import org.zwobble.toml.errors.TomlDuplicateKeyError;
import org.zwobble.toml.values.TomlArray;
import org.zwobble.toml.values.TomlKeyValuePair;
import org.zwobble.toml.values.TomlTable;
import org.zwobble.toml.values.TomlValue;

import java.util.*;

class TomlTableBuilder {
    private final TomlTable table;
    private final LinkedHashMap<String, TomlKeyValuePair> keyValuePairs;
    private final Map<String, TomlTableBuilder> subTableBuilders;
    private final Map<String, List<TomlValue>> arrayOfTables;

    TomlTableBuilder() {
        this.keyValuePairs = new LinkedHashMap<>();
        this.table = new TomlTable(this.keyValuePairs);
        this.subTableBuilders = new HashMap<>();
        this.arrayOfTables = new HashMap<>();
    }

    TomlTable toTable() {
        return this.table;
    }

    TomlTableBuilder getOrCreateSubTable(TomlKey key) {
        var subTable = this.subTableBuilders.get(key.value());

        if (subTable == null) {
            if (this.keyValuePairs.containsKey(key.value())) {
                throw new TomlCannotDefineSubKeyOfNonTableError(key.sourceRange());
            }

            subTable = new TomlTableBuilder();
            this.subTableBuilders.put(key.value(), subTable);
            var pair = TomlKeyValuePair.of(key.value(), subTable.table);
            this.keyValuePairs.put(key.value(), pair);
        }

        return subTable;
    }

    TomlTableBuilder createArraySubTable(TomlKey key) {
        // TODO: handle inline array

        if (!this.arrayOfTables.containsKey(key.value())) {
            if (this.keyValuePairs.containsKey(key.value())) {
                throw new TomlDuplicateKeyError(key.value(), key.sourceRange());
            }

            var arrayOfTables = new ArrayList<TomlValue>();
            this.arrayOfTables.put(key.value(), arrayOfTables);
            var pair = TomlKeyValuePair.of(key.value(), TomlArray.of(arrayOfTables));
            this.keyValuePairs.put(key.value(), pair);
        }

        var subTable = new TomlTableBuilder();
        this.arrayOfTables.get(key.value()).add(subTable.table);
        this.subTableBuilders.put(key.value(), subTable);
        return subTable;
    }

    void add(TomlKey key, TomlValue value) {
        var pair = TomlKeyValuePair.of(key.value(), value);
        var currentValue = this.keyValuePairs.putIfAbsent(key.value(), pair);
        if (currentValue != null) {
            throw new TomlDuplicateKeyError(key.value(), key.sourceRange());
        }
    }
}
