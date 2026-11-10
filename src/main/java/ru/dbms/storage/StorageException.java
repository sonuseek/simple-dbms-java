package ru.dbms.storage;

/** Ошибка чтения или записи файла базы данных. */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Создаёт исключение с сообщением и исходной причиной. */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Создаёт исключение с сообщением. */
    public StorageException(String message) {
        super(message);
    }
}