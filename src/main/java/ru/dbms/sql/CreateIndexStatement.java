package ru.dbms.sql;

/** Запрос CREATE INDEX: создание индекса по колонке таблицы. */
public record CreateIndexStatement(String indexName, String tableName, String columnName)
        implements Statement {

    /** Проверяет, что все части запроса заданы. */
    public CreateIndexStatement {
        if (indexName == null || indexName.isBlank()) {
            throw new SqlException("Не указано имя индекса");
        }
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
        if (columnName == null || columnName.isBlank()) {
            throw new SqlException("Не указана колонка индекса");
        }
    }
}