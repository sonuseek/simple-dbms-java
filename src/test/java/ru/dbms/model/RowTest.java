package ru.dbms.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowTest {

    @Test
    void storesAndReturnsValues() {
        Row row = new Row();
        row.set("id", 1);
        row.set("name", "Нейгум Софья");
        row.set("scholarship", 8000.0);
        row.set("birth_date", LocalDate.of(2006, 3, 15));
        row.set("is_budget", true);

        assertEquals(1, row.get("id"));
        assertEquals("Нейгум Софья", row.get("name"));
        assertEquals(8000.0, row.get("scholarship"));
        assertEquals(LocalDate.of(2006, 3, 15), row.get("birth_date"));
        assertEquals(true, row.get("is_budget"));
    }

    @Test
    void returnsNullForMissingColumn() {
        assertNull(new Row().get("group_id"));
    }

    @Test
    void overwritesExistingValue() {
        Row row = new Row();
        row.set("name", "Иванов");
        row.set("name", "Петров");
        assertEquals("Петров", row.get("name"));
    }

    @Test
    void checksColumnPresence() {
        Row row = new Row();
        row.set("id", 1);
        assertTrue(row.has("id"));
        assertFalse(row.has("name"));
    }

    @Test
    void keepsColumnOrder() {
        Row row = new Row();
        row.set("id", 1);
        row.set("name", "Нейгум Софья");
        row.set("group_id", 3);
        assertEquals("[id, name, group_id]", row.getValues().keySet().toString());
    }

    @Test
    void returnedMapIsUnmodifiable() {
        Row row = new Row();
        row.set("id", 1);
        assertThrows(UnsupportedOperationException.class,
                () -> row.getValues().put("name", "Петров"));
    }

    @Test
    void rejectsEmptyColumnName() {
        assertThrows(IllegalArgumentException.class, () -> new Row().set("", 1));
    }
}