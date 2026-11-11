package ru.dbms.sql;

/**
 * Запрос DELETE: удаление строк.
 * Если условие не указано, удаляются все строки таблицы.
 */
public record DeleteStatement(String tableName, Condition condition) implements Statement {

    /** Проверяет, что имя таблицы задано. */
    public DeleteStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
    }
}