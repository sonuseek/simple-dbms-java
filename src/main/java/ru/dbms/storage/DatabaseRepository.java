package ru.dbms.storage;

import ru.dbms.model.Database;

/**
 * Хранилище базы данных (паттерн Repository).
 * Скрывает от остальной программы способ и место хранения данных.
 */
public interface DatabaseRepository {

    /** Сохраняет базу данных. */
    void save(Database database);

    /** Загружает базу данных. Если сохранённых данных нет, возвращает пустую базу. */
    Database load();
}