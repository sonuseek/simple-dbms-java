package ru.dbms.sql;

import org.junit.jupiter.api.Test;
import ru.dbms.model.ColumnType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    private Statement parse(String sql) {
        return new Parser(sql).parse();
    }

    @Test
    void parsesInsertWithAllValues() {
        InsertStatement statement = (InsertStatement)
                parse("INSERT INTO students VALUES (1, 'Нейгум Софья', 8000.0, TRUE);");

        assertEquals("students", statement.tableName());
        assertFalse(statement.hasColumnNames());
        assertEquals(1, statement.rowCount());
        assertEquals(List.of(1, "Нейгум Софья", 8000.0, Boolean.TRUE), statement.rows().get(0));
    }

    @Test
    void parsesInsertWithColumnNames() {
        InsertStatement statement = (InsertStatement)
                parse("INSERT INTO students (id, name) VALUES (2, 'Иванов');");

        assertEquals(List.of("id", "name"), statement.columnNames());
        assertEquals(List.of(2, "Иванов"), statement.rows().get(0));
    }

    @Test
    void parsesInsertWithSeveralRows() {
        InsertStatement statement = (InsertStatement)
                parse("INSERT INTO students VALUES (1, 'Софья'), (2, 'Иван'), (3, 'Пётр');");

        assertEquals(3, statement.rowCount());
        assertEquals(List.of(1, "Софья"), statement.rows().get(0));
        assertEquals(List.of(3, "Пётр"), statement.rows().get(2));
    }

    @Test
    void parsesInsertWithSeveralRowsAndColumnNames() {
        InsertStatement statement = (InsertStatement)
                parse("INSERT INTO students (id, name) VALUES (1, 'Софья'), (2, 'Иван');");

        assertEquals(List.of("id", "name"), statement.columnNames());
        assertEquals(2, statement.rowCount());
    }

    @Test
    void rejectsRowsWithDifferentLength() {
        assertThrows(SqlException.class,
                () -> parse("INSERT INTO students VALUES (1, 'Софья'), (2);"));
    }

    @Test
    void parsesSelectAll() {
        SelectStatement statement = (SelectStatement) parse("SELECT * FROM students;");

        assertTrue(statement.isSelectAll());
        assertNull(statement.condition());
    }

    @Test
    void parsesSelectWithColumnsAndWhere() {
        SelectStatement statement = (SelectStatement)
                parse("SELECT name, scholarship FROM students WHERE scholarship > 5000;");

        assertEquals(List.of("name", "scholarship"), statement.columnNames());
        assertEquals("scholarship > 5000", statement.condition().toString());
    }

    @Test
    void parsesUpdate() {
        UpdateStatement statement = (UpdateStatement)
                parse("UPDATE students SET scholarship = 12000 WHERE id = 1;");

        assertEquals(Map.of("scholarship", 12000), statement.assignments());
        assertEquals("id = 1", statement.condition().toString());
    }

    @Test
    void parsesUpdateWithSeveralAssignments() {
        UpdateStatement statement = (UpdateStatement)
                parse("UPDATE students SET scholarship = 1.5, name = 'X', is_budget = FALSE;");

        assertEquals(3, statement.assignments().size());
        assertEquals("[scholarship, name, is_budget]", statement.assignments().keySet().toString());
        assertNull(statement.condition());
    }

    @Test
    void parsesDelete() {
        DeleteStatement statement = (DeleteStatement)
                parse("DELETE FROM students WHERE name = 'Иванов';");

        assertEquals("name = Иванов", statement.condition().toString());
    }

    @Test
    void parsesDeleteWithoutWhere() {
        assertNull(((DeleteStatement) parse("DELETE FROM students;")).condition());
    }

    @Test
    void parsesCreateTable() {
        CreateTableStatement statement = (CreateTableStatement) parse(
                "CREATE TABLE students (id INT PRIMARY KEY, name TEXT, scholarship DOUBLE, "
                        + "birth_date DATE, is_budget BOOLEAN, group_id INT);");

        assertEquals(6, statement.columns().size());
        assertEquals("id", statement.primaryKey());
        assertEquals(ColumnType.DOUBLE, statement.columns().get(2).getType());
        assertEquals(ColumnType.DATE, statement.columns().get(3).getType());
        assertEquals(ColumnType.BOOLEAN, statement.columns().get(4).getType());
    }

    @Test
    void parsesPrimaryKeyInAnyPosition() {
        CreateTableStatement statement =
                (CreateTableStatement) parse("CREATE TABLE t (code TEXT, id INT PRIMARY KEY);");

        assertEquals("id", statement.primaryKey());
    }

    @Test
    void parsesDropTable() {
        assertEquals("students", ((DropTableStatement) parse("DROP TABLE students;")).tableName());
    }

    @Test
    void parsesCreateIndex() {
        CreateIndexStatement statement =
                (CreateIndexStatement) parse("CREATE INDEX idx_students_name ON students (name);");

        assertEquals("idx_students_name", statement.indexName());
        assertEquals("students", statement.tableName());
        assertEquals("name", statement.columnName());
    }

    @Test
    void ignoresCaseAndWhitespace() {
        assertTrue(((SelectStatement) parse("select * from students")).isSelectAll());
        assertTrue(((SelectStatement) parse("SeLeCt * FrOm students")).isSelectAll());
        assertTrue(((SelectStatement) parse("SELECT *\n  FROM students")).isSelectAll());
    }

    @Test
    void parsesAllValueKinds() {
        InsertStatement statement = (InsertStatement)
                parse("INSERT INTO t VALUES (1, 2.5, 'x', TRUE, FALSE, NULL);");

        List<Object> values = statement.rows().get(0);
        assertEquals(1, values.get(0));
        assertEquals(2.5, values.get(1));
        assertEquals("x", values.get(2));
        assertEquals(Boolean.TRUE, values.get(3));
        assertEquals(Boolean.FALSE, values.get(4));
        assertNull(values.get(5));
    }

    @Test
    void keepsDateAsTextUntilExecution() {
        InsertStatement statement = (InsertStatement) parse("INSERT INTO t VALUES ('2026-03-15');");

        assertEquals("2026-03-15", statement.rows().get(0).get(0));
    }

    @Test
    void parsesAllOperators() {
        assertEquals(Operator.GREATER_OR_EQUALS, where("a >= 1"));
        assertEquals(Operator.LESS_OR_EQUALS, where("a <= 1"));
        assertEquals(Operator.NOT_EQUALS, where("a <> 1"));
        assertEquals(Operator.NOT_EQUALS, where("a != 1"));
        assertEquals(Operator.LESS, where("a < 1"));
        assertEquals(Operator.GREATER, where("a > 1"));
        assertEquals(Operator.EQUALS, where("a = 1"));
    }

    private Operator where(String condition) {
        return ((SelectStatement) parse("SELECT * FROM t WHERE " + condition)).condition().operator();
    }

    @Test
    void failsOnEmptyQuery() {
        assertThrows(SqlException.class, () -> parse(""));
        assertThrows(SqlException.class, () -> parse(";"));
    }

    @Test
    void failsOnUnknownCommand() {
        assertThrows(SqlException.class, () -> parse("UPSERT INTO t VALUES (1)"));
    }

    @Test
    void failsOnBrokenSelect() {
        assertThrows(SqlException.class, () -> parse("SELECT * students"));
        assertThrows(SqlException.class, () -> parse("SELECT * FROM"));
    }

    @Test
    void failsOnBrokenInsert() {
        assertThrows(SqlException.class, () -> parse("INSERT students VALUES (1)"));
        assertThrows(SqlException.class, () -> parse("INSERT INTO t (1)"));
        assertThrows(SqlException.class, () -> parse("INSERT INTO t VALUES (1, 2"));
        assertThrows(SqlException.class, () -> parse("INSERT INTO t VALUES (abc)"));
    }

    @Test
    void failsOnBrokenUpdate() {
        assertThrows(SqlException.class, () -> parse("UPDATE t WHERE id = 1"));
        assertThrows(SqlException.class, () -> parse("UPDATE t SET a 1"));
    }

    @Test
    void failsOnBrokenCreateTable() {
        assertThrows(SqlException.class, () -> parse("CREATE TABLE t (id NUMBER PRIMARY KEY)"));
        assertThrows(SqlException.class, () -> parse("CREATE TABLE t (id INT, name TEXT)"));
        assertThrows(SqlException.class,
                () -> parse("CREATE TABLE t (id INT PRIMARY KEY, code TEXT PRIMARY KEY)"));
    }

    @Test
    void failsOnBrokenWhere() {
        assertThrows(SqlException.class, () -> parse("SELECT * FROM t WHERE a 1"));
        assertThrows(SqlException.class, () -> parse("SELECT * FROM t WHERE a ="));
    }

    @Test
    void failsOnExtraTextAfterQuery() {
        assertThrows(SqlException.class, () -> parse("SELECT * FROM t; DROP TABLE t;"));
    }
}