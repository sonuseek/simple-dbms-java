package ru.dbms.exec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.dbms.model.Database;
import ru.dbms.sql.Parser;
import ru.dbms.sql.SqlException;
import ru.dbms.storage.DatabaseRepository;
import ru.dbms.storage.FileDatabaseRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверяет, что индексы не меняют результат запросов,
 * обновляются при изменении данных и сохраняются на диск.
 */
class ExecutorIndexTest {

    private static final String[] CITIES = {"Москва", "Казань", "Тверь", "Омск"};

    private Database database;
    private Executor executor;

    @BeforeEach
    void fillTables() {
        database = new Database();
        executor = new Executor(database);

        String schema = " (id INT PRIMARY KEY, city TEXT, n INT);";
        run("CREATE TABLE plain" + schema);
        run("CREATE TABLE indexed" + schema);

        for (int i = 1; i <= 40; i++) {
            String values = "(" + i + ", '" + CITIES[i % 4] + "', " + (i * 3) + ");";
            run("INSERT INTO plain VALUES " + values);
            run("INSERT INTO indexed VALUES " + values);
        }

        run("CREATE INDEX idx_city ON indexed (city);");
        run("CREATE INDEX idx_n ON indexed (n);");
    }

    private QueryResult run(String sql) {
        return executor.execute(new Parser(sql).parse());
    }

    /** Возвращает упорядоченный набор найденных ключей, чтобы порядок строк не влиял на сравнение. */
    private Set<Object> ids(String sql) {
        Set<Object> result = new TreeSet<>();
        for (List<Object> row : run(sql).rows()) {
            result.add(row.get(0));
        }
        return result;
    }

    /** Сравнивает результат одного и того же условия на таблице с индексом и без. */
    private void assertSameResult(String condition) {
        assertEquals(ids("SELECT id FROM plain WHERE " + condition),
                ids("SELECT id FROM indexed WHERE " + condition),
                "Результаты различаются для условия: " + condition);
    }

    @Test
    void indexDoesNotChangeEqualsResult() {
        assertSameResult("city = 'Москва'");
        assertSameResult("city = 'Нет такого'");
    }

    @Test
    void indexDoesNotChangeRangeResult() {
        assertSameResult("city > 'Москва'");
        assertSameResult("n >= 60");
        assertSameResult("n < 10");
        assertSameResult("n <= 9");
    }

    @Test
    void indexDoesNotChangeNotEqualsResult() {
        assertSameResult("city <> 'Москва'");
    }

    @Test
    void deleteWorksThroughIndex() {
        assertEquals(10, run("DELETE FROM indexed WHERE city = 'Омск';").affectedRows());
        assertEquals(30, database.getTable("indexed").size());
        assertEquals(0, run("SELECT id FROM indexed WHERE city = 'Омск';").rows().size());
    }

    @Test
    void updateWorksThroughIndex() {
        assertEquals(10, run("UPDATE indexed SET n = 0 WHERE city = 'Казань';").affectedRows());
        assertEquals(10, run("SELECT id FROM indexed WHERE n = 0;").rows().size());
    }

    @Test
    void rejectsIndexOnMissingTable() {
        assertThrows(SqlException.class, () -> run("CREATE INDEX idx ON nope (city);"));
    }

    @Test
    void rejectsDuplicateIndex() {
        assertThrows(SqlException.class, () -> run("CREATE INDEX idx_city ON indexed (city);"));
    }

    @Test
    void indexesSurviveSaveAndLoad(@TempDir Path tempDir) {
        DatabaseRepository repository = new FileDatabaseRepository(tempDir.resolve("db.dat"));
        repository.save(database);

        Database loaded = repository.load();
        assertEquals(2, loaded.getTable("indexed").getIndexNames().size());
        assertEquals(10, loaded.getTable("indexed").getIndex("idx_city").findEqual("Москва").size());
    }

    @Test
    void loadedIndexKeepsWorking(@TempDir Path tempDir) {
        DatabaseRepository repository = new FileDatabaseRepository(tempDir.resolve("db.dat"));
        repository.save(database);

        Database loaded = repository.load();
        Executor afterLoad = new Executor(loaded);

        assertEquals(10, afterLoad.execute(
                new Parser("SELECT id FROM indexed WHERE city = 'Москва';").parse()).rows().size());

        afterLoad.execute(new Parser("INSERT INTO indexed VALUES (500, 'Москва', 1);").parse());

        assertEquals(11, afterLoad.execute(
                new Parser("SELECT id FROM indexed WHERE city = 'Москва';").parse()).rows().size());
    }
}
