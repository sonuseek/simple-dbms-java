package ru.dbms.sql;

import ru.dbms.model.Column;

import java.util.List;

/** Запрос CREATE TABLE: создание таблицы с описанными колонками. */
public record CreateTableStatement(String tableName, List<Column> columns, String primaryKey)
        implements Statement {

    /** Проверяет, что описание таблицы задано полностью. */
    public CreateTableStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
        if (columns == null || columns.isEmpty()) {
            throw new SqlException("Не указаны колонки таблицы");
        }
        columns = List.copyOf(columns);
    }
}