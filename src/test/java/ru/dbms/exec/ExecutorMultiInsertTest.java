package ru.dbms.exec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.dbms.model.Database;
import ru.dbms.sql.Parser;
import ru.dbms.sql.SqlException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет вставку нескольких строк одним запросом.
 * Отдельное внимание уделено тому, что при ошибке в любой строке
 * таблица остаётся без изменений.
 */
class ExecutorMultiInsertTest {

    private Database database;
    private Executor executor;

    @BeforeEach
    void createTable() {
        database = new Database();
        executor = new Executor(database);
        run("CREATE TABLE students (id INT PRIMARY KEY, name TEXT, scholarship DOUBLE, "
                + "birth_date DATE, is_budget BOOLEAN);");
    }

    private QueryResult run(String sql) {
        return executor.execute(new Parser(sql).parse());
    }

    private int size() {
        return database.getTable("students").size();
    }

    @Test
    void insertsSingleRowAsBefore() {
        QueryResult result = run("INSERT INTO students VALUES "
                + "(1, 'Нейгум Софья', 8000.0, '2006-03-15', TRUE);");

        assertEquals(1, result.affectedRows());
        assertEquals(1, size());
    }

    @Test
    void insertsSeveralRows() {
        QueryResult result = run("INSERT INTO students VALUES "
                + "(1, 'Софья', 8000.0, '2006-03-15', TRUE), "
                + "(2, 'Иван', 5000.0, '2005-07-20', FALSE), "
                + "(3, 'Пётр', 3000.0, '2004-01-10', TRUE);");

        assertEquals(3, result.affectedRows());
        assertEquals("Добавлено строк: 3", result.message());
        assertEquals(3, size());
        assertEquals("Иван", database.getTable("students").findByKey(2).get("name"));
        assertEquals(LocalDate.of(2004, 1, 10),
                database.getTable("students").findByKey(3).get("birth_date"));
    }

    @Test
    void insertsSeveralRowsWithColumnNames() {
        run("INSERT INTO students (id, name) VALUES (1, 'Софья'), (2, 'Иван');");

        assertEquals(2, size());
        assertNull(database.getTable("students").findByKey(1).get("scholarship"));
    }

    @Test
    void convertsTypesInEveryRow() {
        run("INSERT INTO students (id, scholarship) VALUES (1, 5000), (2, 7000.5);");

        assertEquals(5000.0, database.getTable("students").findByKey(1).get("scholarship"));
        assertEquals(7000.5, database.getTable("students").findByKey(2).get("scholarship"));
    }

    @Test
    void rejectsDuplicateKeyInsideQuery() {
        assertThrows(SqlException.class, () -> run(
                "INSERT INTO students (id, name) VALUES (1, 'А'), (2, 'Б'), (1, 'В');"));

        assertEquals(0, size());
    }

    @Test
    void rejectsConflictWithExistingRow() {
        run("INSERT INTO students (id, name) VALUES (10, 'Старая');");

        assertThrows(SqlException.class, () -> run(
                "INSERT INTO students (id, name) VALUES (11, 'А'), (10, 'Б');"));

        assertEquals(1, size());
    }

    @Test
    void rejectsWrongTypeInMiddleRow() {
        assertThrows(SqlException.class, () -> run(
                "INSERT INTO students (id, scholarship) VALUES (1, 1.0), (2, 'много'), (3, 2.0);"));

        assertEquals(0, size());
    }

    @Test
    void rejectsNullPrimaryKeyInAnyRow() {
        assertThrows(SqlException.class, () -> run(
                "INSERT INTO students (id, name) VALUES (1, 'А'), (NULL, 'Б');"));

        assertEquals(0, size());
    }

    @Test
    void updatesIndexesOnMultiInsert() {
        run("CREATE INDEX idx_name ON students (name);");
        run("INSERT INTO students (id, name) VALUES (1, 'Х'), (2, 'Х'), (3, 'Y');");

        assertEquals(2, run("SELECT id FROM students WHERE name = 'Х';").rows().size());
        assertEquals(2, database.getTable("students").getIndex("idx_name").findEqual("Х").size());
    }

    @Test
    void keepsIndexIntactAfterFailedInsert() {
        run("CREATE INDEX idx_name ON students (name);");
        run("INSERT INTO students (id, name) VALUES (1, 'Х');");

        assertThrows(SqlException.class, () -> run(
                "INSERT INTO students (id, name) VALUES (2, 'Z'), (1, 'W');"));

        assertEquals(1, size());
        assertTrue(database.getTable("students").getIndex("idx_name").findEqual("Z").isEmpty());
    }

    @Test
    void acceptsMultilineInsert() {
        run("INSERT INTO students (id, name)\nVALUES\n  (1, 'А'),\n  (2, 'Б');");

        assertEquals(2, size());
    }
}
