package ru.dbms.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColumnTest {

    @Test
    void storesNameAndType() {
        Column column = new Column("price", ColumnType.DOUBLE);
        assertEquals("price", column.getName());
        assertEquals(ColumnType.DOUBLE, column.getType());
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Column("", ColumnType.INT));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Column("   ", ColumnType.INT));
    }

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Column(null, ColumnType.INT));
    }

    @Test
    void rejectsNullType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Column("id", null));
    }

    @Test
    void printsNameAndType() {
        assertEquals("price DOUBLE", new Column("price", ColumnType.DOUBLE).toString());
    }
}