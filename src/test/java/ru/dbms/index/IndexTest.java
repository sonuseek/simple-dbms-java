package ru.dbms.index;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexTest {

    private Index filled() {
        Index index = new Index("idx_city", "city");
        index.add("Москва", 1);
        index.add("Москва", 2);
        index.add("Казань", 3);
        index.add(null, 4);
        return index;
    }

    @Test
    void storesNameAndColumn() {
        Index index = new Index("idx_city", "city");
        assertEquals("idx_city", index.getName());
        assertEquals("city", index.getColumnName());
        assertEquals(0, index.size());
    }

    @Test
    void findsRowsByEqualValue() {
        Index index = filled();
        assertEquals(2, index.findEqual("Москва").size());
        assertEquals(Set.of(3), index.findEqual("Казань"));
        assertTrue(index.findEqual("Тверь").isEmpty());
    }

    @Test
    void keepsRowsWithNullValueSeparately() {
        Index index = filled();
        assertEquals(Set.of(4), index.findEqual(null));
        assertEquals(3, index.size());
    }

    @Test
    void findsRowsByLessThan() {
        Index index = filled();
        assertEquals(Set.of(3), index.findLess("Москва", false));
        assertEquals(3, index.findLess("Москва", true).size());
    }

    @Test
    void findsRowsByGreaterThan() {
        Index index = filled();
        assertEquals(2, index.findGreater("Казань", false).size());
        assertEquals(3, index.findGreater("Казань", true).size());
    }

    @Test
    void removesRows() {
        Index index = filled();

        index.remove("Москва", 1);
        assertEquals(Set.of(2), index.findEqual("Москва"));

        index.remove("Москва", 2);
        assertTrue(index.findEqual("Москва").isEmpty());
        assertEquals(2, index.size());
    }

    @Test
    void ignoresRemovalOfMissingRow() {
        Index index = filled();
        index.remove("Тверь", 99);
        assertEquals(3, index.size());
    }

    @Test
    void clearsEverything() {
        Index index = filled();
        index.clear();
        assertEquals(0, index.size());
    }

    @Test
    void rejectsEmptyNames() {
        assertThrows(IllegalArgumentException.class, () -> new Index("", "city"));
        assertThrows(IllegalArgumentException.class, () -> new Index("idx", ""));
    }
}
