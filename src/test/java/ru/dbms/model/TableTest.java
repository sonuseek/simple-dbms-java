package ru.dbms.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest {

    private Table students() {
        return new Table("students", List.of(
                new Column("id", ColumnType.INT),
                new Column("name", ColumnType.TEXT),
                new Column("scholarship", ColumnType.DOUBLE),
                new Column("group_id", ColumnType.INT)), "id");
    }

    private Row student(int id, String name) {
        Row row = new Row();
        row.set("id", id);
        row.set("name", name);
        return row;
    }

    @Test
    void storesSchema() {
        Table table = students();
        assertEquals("students", table.getName());
        assertEquals("id", table.getPrimaryKey());
        assertEquals(4, table.getColumns().size());
        assertEquals(0, table.size());
    }

    @Test
    void findsColumnByName() {
        Table table = students();
        assertEquals(ColumnType.TEXT, table.getColumn("name").getType());
        assertNull(table.getColumn("unknown"));
    }

    @Test
    void insertsAndFindsRow() {
        Table table = students();
        table.insert(student(1, "Нейгум Софья"));

        assertEquals(1, table.size());
        assertEquals("Нейгум Софья", table.findByKey(1).get("name"));
    }

    @Test
    void fillsMissingColumnsWithNull() {
        Table table = students();
        table.insert(student(1, "Нейгум Софья"));

        Row stored = table.findByKey(1);
        assertTrue(stored.has("scholarship"));
        assertNull(stored.get("scholarship"));
        assertEquals("[id, name, scholarship, group_id]",
                stored.getValues().keySet().toString());
    }

    @Test
    void rejectsDuplicatePrimaryKey() {
        Table table = students();
        table.insert(student(1, "Нейгум Софья"));

        assertThrows(IllegalArgumentException.class, () -> table.insert(student(1, "Иванов")));
    }

    @Test
    void rejectsNullPrimaryKey() {
        Table table = students();
        Row row = new Row();
        row.set("name", "Без ключа");

        assertThrows(IllegalArgumentException.class, () -> table.insert(row));
    }

    @Test
    void rejectsUnknownColumn() {
        Table table = students();
        Row row = student(1, "Нейгум Софья");
        row.set("unknown", 5);

        assertThrows(IllegalArgumentException.class, () -> table.insert(row));
    }

    @Test
    void rejectsWrongType() {
        Table table = students();
        Row row = student(1, "Нейгум Софья");
        row.set("scholarship", "много");

        assertThrows(IllegalArgumentException.class, () -> table.insert(row));
    }

    @Test
    void deletesByKey() {
        Table table = students();
        table.insert(student(1, "Нейгум Софья"));

        assertTrue(table.deleteByKey(1));
        assertFalse(table.deleteByKey(1));
        assertEquals(0, table.size());
    }

    @Test
    void iteratesInInsertionOrder() {
        Table table = students();
        table.insert(student(1, "Нейгум Софья"));
        table.insert(student(2, "Иванов"));

        List<Object> names = new ArrayList<>();
        for (Row row : table) {
            names.add(row.get("name"));
        }

        assertEquals("[Нейгум Софья, Иванов]", names.toString());
    }

    @Test
    void rejectsInvalidSchema() {
        assertThrows(IllegalArgumentException.class,
                () -> new Table("", List.of(new Column("id", ColumnType.INT)), "id"));
        assertThrows(IllegalArgumentException.class,
                () -> new Table("students", List.of(), "id"));
        assertThrows(IllegalArgumentException.class,
                () -> new Table("students", List.of(new Column("id", ColumnType.INT)), "code"));
    }

    @Test
    void schemaIsUnmodifiable() {
        Table table = students();
        assertThrows(UnsupportedOperationException.class,
                () -> table.getColumns().add(new Column("extra", ColumnType.INT)));
    }
}