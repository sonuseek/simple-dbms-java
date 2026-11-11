package ru.dbms.sql;

import org.junit.jupiter.api.Test;
import ru.dbms.model.Column;
import ru.dbms.model.ColumnType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementTest {

    @Test
    void createTableKeepsSchema() {
        CreateTableStatement statement = new CreateTableStatement("students",
                List.of(new Column("id", ColumnType.INT), new Column("name", ColumnType.TEXT)), "id");

        assertEquals("students", statement.tableName());
        assertEquals(2, statement.columns().size());
        assertEquals("id", statement.primaryKey());
    }

    @Test
    void insertWithoutColumnNames() {
        InsertStatement statement = new InsertStatement("students", null, List.of(1, "Соня"));

        assertFalse(statement.hasColumnNames());
        assertEquals(2, statement.values().size());
    }

    @Test
    void insertWithColumnNames() {
        InsertStatement statement =
                new InsertStatement("students", List.of("id", "name"), List.of(1, "Соня"));

        assertTrue(statement.hasColumnNames());
        assertEquals("id", statement.columnNames().get(0));
    }

    @Test
    void insertRejectsCountMismatch() {
        assertThrows(SqlException.class,
                () -> new InsertStatement("students", List.of("id"), List.of(1, 2)));
    }

    @Test
    void insertRejectsEmptyValues() {
        assertThrows(SqlException.class, () -> new InsertStatement("students", null, List.of()));
    }

    @Test
    void selectAllHasNoColumnList() {
        SelectStatement statement = new SelectStatement("students", null, null);

        assertTrue(statement.isSelectAll());
        assertNull(statement.condition());
    }

    @Test
    void selectKeepsColumnsAndCondition() {
        SelectStatement statement = new SelectStatement("students", List.of("name"),
                new Condition("scholarship", Operator.GREATER, 5000));

        assertFalse(statement.isSelectAll());
        assertEquals("name", statement.columnNames().get(0));
        assertEquals("scholarship > 5000", statement.condition().toString());
    }

    @Test
    void updateCopiesAssignments() {
        Map<String, Object> assignments = new LinkedHashMap<>();
        assignments.put("scholarship", 12000);

        UpdateStatement statement = new UpdateStatement("students", assignments, null);
        assignments.put("name", "изменено снаружи");

        assertEquals(1, statement.assignments().size());
    }

    @Test
    void updateRejectsEmptyAssignments() {
        assertThrows(SqlException.class, () -> new UpdateStatement("students", Map.of(), null));
    }

    @Test
    void deleteWithoutCondition() {
        assertNull(new DeleteStatement("students", null).condition());
    }

    @Test
    void createIndexKeepsParts() {
        CreateIndexStatement statement =
                new CreateIndexStatement("idx_students_name", "students", "name");

        assertEquals("idx_students_name", statement.indexName());
        assertEquals("students", statement.tableName());
        assertEquals("name", statement.columnName());
    }

    @Test
    void rejectsEmptyTableName() {
        assertThrows(SqlException.class, () -> new DropTableStatement(""));
    }
}