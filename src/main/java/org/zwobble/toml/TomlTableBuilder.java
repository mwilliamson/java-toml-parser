package org.zwobble.toml;

import org.zwobble.toml.errors.TomlDuplicateKeyError;
import org.zwobble.toml.values.TomlArray;
import org.zwobble.toml.values.TomlKeyValuePair;
import org.zwobble.toml.values.TomlTable;
import org.zwobble.toml.values.TomlValue;

import java.util.*;

class TomlTableBuilder {
    enum DefinedBy {
        SUPER_TABLE,
        DOTTED_KEY,
        TABLE,
    }

    private final TomlTable table;
    private final LinkedHashMap<String, TomlKeyValuePair> keyValuePairs;
    private final Map<String, TomlTableBuilder> subTableBuilders;
    private final Map<String, List<TomlValue>> arrayOfTables;
    private DefinedBy definedBy;

    TomlTableBuilder(DefinedBy definedBy) {
        this.keyValuePairs = new LinkedHashMap<>();
        this.table = new TomlTable(this.keyValuePairs);
        this.subTableBuilders = new HashMap<>();
        this.arrayOfTables = new HashMap<>();
        this.definedBy = definedBy;
    }

    TomlTable toTable() {
        return this.table;
    }

    TomlTableBuilder getOrCreateSubTable(TomlKey key, DefinedBy definedBy) {
        var subTable = this.subTableBuilders.get(key.value());

        if (subTable == null) {
            if (this.keyValuePairs.containsKey(key.value())) {
                throw new TomlDuplicateKeyError(key.value(), key.sourceRange());
            }

            subTable = new TomlTableBuilder(definedBy);
            this.subTableBuilders.put(key.value(), subTable);
            var pair = TomlKeyValuePair.of(key.value(), subTable.table);
            this.keyValuePairs.put(key.value(), pair);
        } else if (subTable.definedBy == DefinedBy.SUPER_TABLE) {
            subTable.definedBy = definedBy;
        } else if (
            !((definedBy == DefinedBy.SUPER_TABLE) ||
                (definedBy == DefinedBy.DOTTED_KEY && subTable.definedBy == DefinedBy.DOTTED_KEY))
        ) {
            throw new TomlDuplicateKeyError(key.value(), key.sourceRange());
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

        var subTable = new TomlTableBuilder(DefinedBy.TABLE);
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
