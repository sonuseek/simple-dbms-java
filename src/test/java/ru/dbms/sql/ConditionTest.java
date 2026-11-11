package ru.dbms.sql;

import org.junit.jupiter.api.Test;
import ru.dbms.model.Row;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionTest {

    private Row student() {
        Row row = new Row();
        row.set("id", 1);
        row.set("name", "Нейгум София");
        row.set("scholarship", 8000.0);
        row.set("birth_date", LocalDate.of(2006, 3, 15));
        row.set("is_budget", true);
        row.set("group_id", null);
        return row;
    }

    private boolean matches(String column, String operator, Object value) {
        return new Condition(column, Operator.fromText(operator), value).matches(student());
    }

    @Test
    void comparesNumbers() {
        assertTrue(matches("id", "=", 1));
        assertTrue(matches("id", ">", 0));
        assertTrue(matches("id", "<", 5));
        assertTrue(matches("id", ">=", 1));
        assertTrue(matches("id", "<=", 1));
        assertTrue(matches("id", "<>", 2));
        assertFalse(matches("id", "=", 2));
    }

    @Test
    void comparesIntegerWithDouble() {
        assertTrue(matches("scholarship", "=", 8000));
        assertTrue(matches("scholarship", ">", 5000));
        assertTrue(matches("id", "=", 1.0));
    }

    @Test
    void treatsNotEqualsAliases() {
        assertTrue(matches("id", "!=", 2));
        assertTrue(matches("id", "<>", 2));
    }

    @Test
    void comparesStrings() {
        assertTrue(matches("name", "=", "Нейгум София"));
        assertTrue(matches("name", "<>", "Иванов"));
    }

    @Test
    void comparesDates() {
        assertTrue(matches("birth_date", "=", LocalDate.of(2006, 3, 15)));
        assertTrue(matches("birth_date", ">", LocalDate.of(2000, 1, 1)));
        assertFalse(matches("birth_date", ">", LocalDate.of(2026, 1, 1)));
    }

    @Test
    void comparesBooleans() {
        assertTrue(matches("is_budget", "=", true));
        assertTrue(matches("is_budget", "<>", false));
        assertFalse(matches("is_budget", "=", false));
    }

    @Test
    void handlesNullValues() {
        assertFalse(matches("group_id", "=", 5));
        assertTrue(matches("group_id", "<>", 5));
        assertTrue(matches("group_id", "=", null));
    }

    @Test
    void handlesMissingColumn() {
        assertFalse(matches("unknown", "=", 1));
        assertTrue(matches("unknown", "<>", 1));
    }

    @Test
    void failsOnIncompatibleTypes() {
        assertThrows(SqlException.class, () -> matches("name", "=", 5));
    }

    @Test
    void failsOnUnknownOperator() {
        assertThrows(SqlException.class, () -> Operator.fromText("~"));
    }

    @Test
    void failsOnEmptyColumnName() {
        assertThrows(SqlException.class, () -> new Condition("", Operator.EQUALS, 1));
    }
}