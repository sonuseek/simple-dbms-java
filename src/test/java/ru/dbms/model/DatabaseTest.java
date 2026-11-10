package ru.dbms.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    private Table students() {
        return new Table("students", List.of(
                new Column("id", ColumnType.INT),
                new Column("name", ColumnType.TEXT),
                new Column("group_id", ColumnType.INT)), "id");
    }

    private Table groups() {
        return new Table("groups", List.of(
                new Column("id", ColumnType.INT),
                new Column("name", ColumnType.TEXT)), "id");
    }

    @Test
    void startsEmpty() {
        Database database = new Database();
        assertEquals(0, database.size());
        assertFalse(database.hasTable("students"));
        assertNull(database.getTable("students"));
    }

    @Test
    void createsAndFindsTables() {
        Database database = new Database();
        database.createTable(groups());
        database.createTable(students());

        assertEquals(2, database.size());
        assertTrue(database.hasTable("students"));
        assertEquals("id", database.getTable("students").getPrimaryKey());
    }

    @Test
    void keepsCreationOrder() {
        Database database = new Database();
        database.createTable(groups());
        database.createTable(students());

        assertEquals("[groups, students]", database.getTableNames().toString());
    }

    @Test
    void rejectsDuplicateTable() {
        Database database = new Database();
        database.createTable(students());

        assertThrows(IllegalArgumentException.class, () -> database.createTable(students()));
    }

    @Test
    void rejectsNullTable() {
        Database database = new Database();
        assertThrows(IllegalArgumentException.class, () -> database.createTable(null));
    }

    @Test
    void dropsTable() {
        Database database = new Database();
        database.createTable(students());

        assertTrue(database.dropTable("students"));
        assertFalse(database.dropTable("students"));
        assertEquals(0, database.size());
    }

    @Test
    void tableNamesAreUnmodifiable() {
        Database database = new Database();
        database.createTable(students());

        assertThrows(UnsupportedOperationException.class,
                () -> database.getTableNames().add("groups"));
    }
}