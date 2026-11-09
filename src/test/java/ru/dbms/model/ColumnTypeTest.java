package ru.dbms.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnTypeTest {

    @Test
    void acceptsCorrectValues() {
        assertTrue(ColumnType.INT.accepts(5));
        assertTrue(ColumnType.DOUBLE.accepts(25000.0));
        assertTrue(ColumnType.TEXT.accepts("Toyota"));
        assertTrue(ColumnType.BOOLEAN.accepts(true));
        assertTrue(ColumnType.DATE.accepts(LocalDate.of(2026, 11, 9)));
    }

    @Test
    void rejectsWrongValues() {
        assertFalse(ColumnType.INT.accepts("пять"));
        assertFalse(ColumnType.TEXT.accepts(5));
        assertFalse(ColumnType.BOOLEAN.accepts("да"));
        assertFalse(ColumnType.DOUBLE.accepts(5));
    }

    @Test
    void acceptsNullForAnyType() {
        assertTrue(ColumnType.INT.accepts(null));
        assertTrue(ColumnType.TEXT.accepts(null));
        assertTrue(ColumnType.DATE.accepts(null));
    }

    @Test
    void returnsJavaType() {
        assertEquals(Integer.class, ColumnType.INT.getJavaType());
        assertEquals(String.class, ColumnType.TEXT.getJavaType());
    }
}