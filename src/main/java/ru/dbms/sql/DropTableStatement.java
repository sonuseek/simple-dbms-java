package ru.dbms.sql;

/** Запрос DROP TABLE: удаление таблицы. */
public record DropTableStatement(String tableName) implements Statement {

    /** Проверяет, что имя таблицы задано. */
    public DropTableStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
    }
}