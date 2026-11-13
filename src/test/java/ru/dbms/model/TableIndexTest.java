package ru.dbms.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableIndexTest {

    private Table students() {
        Table table = new Table("students", List.of(
                new Column("id", ColumnType.INT),
                new Column("name", ColumnType.TEXT),
                new Column("score", ColumnType.DOUBLE)), "id");

        for (int i = 1; i <= 5; i++) {
            Row row = new Row();
            row.set("id", i);
            row.set("name", i % 2 == 0 ? "Чётный" : "Нечётный");
            row.set("score", i * 10.0);
            table.insert(row);
        }
        return table;
    }

    @Test
    void buildsIndexFromExistingRows() {
        Table table = students();
        table.createIndex("idx_name", "name");

        assertEquals(2, table.getIndex("idx_name").findEqual("Чётный").size());
        assertEquals(3, table.getIndex("idx_name").findEqual("Нечётный").size());
    }

    @Test
    void listsAndFindsIndexes() {
        Table table = students();
        table.createIndex("idx_name", "name");

        assertEquals(Set.of("idx_name"), table.getIndexNames());
        assertNotNull(table.findIndexForColumn("name"));
        assertNull(table.findIndexForColumn("score"));
    }

    @Test
    void rejectsDuplicateIndexAndUnknownColumn() {
        Table table = students();
        table.createIndex("idx_name", "name");

        assertThrows(IllegalArgumentException.class, () -> table.createIndex("idx_name", "name"));
        assertThrows(IllegalArgumentException.class, () -> table.createIndex("idx_other", "unknown"));
    }

    @Test
    void updatesIndexOnInsert() {
        Table table = students();
        table.createIndex("idx_name", "name");

        Row row = new Row();
        row.set("id", 6);
        row.set("name", "Чётный");
        table.insert(row);

        assertEquals(3, table.getIndex("idx_name").findEqual("Чётный").size());
    }

    @Test
    void updatesIndexOnUpdate() {
        Table table = students();
        table.createIndex("idx_name", "name");

        table.updateRow(2, Map.of("name", "Новый"));

        assertEquals(1, table.getIndex("idx_name").findEqual("Чётный").size());
        assertEquals(Set.of(2), table.getIndex("idx_name").findEqual("Новый"));
    }

    @Test
    void updatesIndexOnDelete() {
        Table table = students();
        table.createIndex("idx_name", "name");

        table.deleteByKey(2);

        assertEquals(1, table.getIndex("idx_name").findEqual("Чётный").size());
    }

    @Test
    void indexFollowsPrimaryKeyChange() {
        Table table = students();
        table.createIndex("idx_name", "name");

        table.updateRow(1, Map.of("id", 100));

        assertTrue(table.getIndex("idx_name").findEqual("Нечётный").contains(100));
        assertFalse(table.getIndex("idx_name").findEqual("Нечётный").contains(1));
    }

    @Test
    void dropsIndex() {
        Table table = students();
        table.createIndex("idx_name", "name");

        assertTrue(table.dropIndex("idx_name"));
        assertFalse(table.dropIndex("idx_name"));
        assertTrue(table.getIndexNames().isEmpty());
    }
}
