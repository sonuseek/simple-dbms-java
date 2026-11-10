package ru.dbms.storage;

import ru.dbms.model.Database;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Хранит базу данных в одном файле средствами стандартной сериализации Java.
 * Запись выполняется во временный файл с последующей заменой основного,
 * поэтому сбой во время сохранения не повреждает ранее сохранённые данные.
 */
public class FileDatabaseRepository implements DatabaseRepository {

    private final Path file;
    private final Path tempFile;

    /** Создаёт хранилище, работающее с указанным файлом. */
    public FileDatabaseRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Путь к файлу не задан");
        }
        this.file = file;
        this.tempFile = file.resolveSibling(file.getFileName() + ".tmp");
    }

    @Override
    public void save(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("База данных не может быть null");
        }

        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (ObjectOutputStream out =
                         new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                out.writeObject(database);
            }

            replaceMainFile();
        } catch (IOException e) {
            throw new StorageException("Не удалось сохранить базу данных в " + file, e);
        }
    }

    @Override
    public Database load() {
        if (!Files.exists(file)) {
            return new Database();
        }

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (Database) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new StorageException("Не удалось прочитать базу данных из " + file, e);
        }
    }

    /** Заменяет основной файл временным, по возможности неделимой операцией. */
    private void replaceMainFile() throws IOException {
        try {
            Files.move(tempFile, file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}