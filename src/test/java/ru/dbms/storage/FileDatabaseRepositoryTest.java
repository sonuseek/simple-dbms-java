package ru.dbms.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.dbms.model.Column;
import ru.dbms.model.ColumnType;
import ru.dbms.model.Database;
import ru.dbms.model.Row;
import ru.dbms.model.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDatabaseRepositoryTest {

    @TempDir
    Path tempDir;

    private Database sample() {
        Database database = new Database();
        database.createTable(new Table("groups", List.of(
                new Column("id", ColumnType.INT),
                new Column("name", ColumnType.TEXT)), "id"));
        database.createTable(new Table("students", List.of(
                new Column("id", ColumnType.INT),
                new Column("name", ColumnType.TEXT),
                new Column("scholarship", ColumnType.DOUBLE),
                new Column("birth_date", ColumnType.DATE),
                new Column("is_budget", ColumnType.BOOLEAN),
                new Column("group_id", ColumnType.INT)), "id"));

        Row student = new Row();
        student.set("id", 1);
        student.set("name", "Нейгум Софья");
        student.set("scholarship", 8000.0);
        student.set("birth_date", LocalDate.of(2006, 3, 15));
        student.set("is_budget", true);
        student.set("group_id", 1);
        database.getTable("students").insert(student);

        return database;
    }

    @Test
    void returnsEmptyDatabaseWhenFileMissing() {
        DatabaseRepository repository = new FileDatabaseRepository(tempDir.resolve("db.dat"));
        assertEquals(0, repository.load().size());
    }

    @Test
    void savesAndLoadsDatabase() {
        Path file = tempDir.resolve("db.dat");
        DatabaseRepository repository = new FileDatabaseRepository(file);

        repository.save(sample());
        Database loaded = repository.load();

        assertTrue(Files.exists(file));
        assertEquals(2, loaded.size());
        assertEquals(1, loaded.getTable("students").size());
    }

    @Test
    void keepsAllValueTypes() {
        DatabaseRepository repository = new FileDatabaseRepository(tempDir.resolve("db.dat"));
        repository.save(sample());

        Row student = repository.load().getTable("students").findByKey(1);

        assertEquals("Нейгум Софья", student.get("name"));
        assertEquals(8000.0, student.get("scholarship"));
        assertEquals(LocalDate.of(2006, 3, 15), student.get("birth_date"));
        assertEquals(true, student.get("is_budget"));
        assertEquals(1, student.get("group_id"));
    }

    @Test
    void keepsSchema() {
        DatabaseRepository repository = new FileDatabaseRepository(tempDir.resolve("db.dat"));
        repository.save(sample());

        Table students = repository.load().getTable("students");

        assertEquals(6, students.getColumns().size());
        assertEquals("id", students.getPrimaryKey());
    }

    @Test
    void loadedDatabaseStillValidatesKeys() {
        DatabaseRepository repository = new FileDatabaseRepository(tempDir.resolve("db.dat"));
        repository.save(sample());

        Table students = repository.load().getTable("students");
        Row duplicate = new Row();
        duplicate.set("id", 1);

        assertThrows(IllegalArgumentException.class, () -> students.insert(duplicate));
    }

    @Test
    void removesTemporaryFileAfterSave() {
        Path file = tempDir.resolve("db.dat");
        new FileDatabaseRepository(file).save(sample());

        assertFalse(Files.exists(tempDir.resolve("db.dat.tmp")));
    }

    @Test
    void failsOnCorruptedFile() throws IOException {
        Path broken = tempDir.resolve("broken.dat");
        Files.writeString(broken, "это не база данных");

        DatabaseRepository repository = new FileDatabaseRepository(broken);

        assertThrows(StorageException.class, repository::load);
    }

    @Test
    void createsMissingDirectories() {
        Path nested = tempDir.resolve("data/inner/db.dat");
        new FileDatabaseRepository(nested).save(sample());

        assertTrue(Files.exists(nested));
    }
}