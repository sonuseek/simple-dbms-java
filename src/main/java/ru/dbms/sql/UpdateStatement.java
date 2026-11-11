package ru.dbms.sql;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Запрос UPDATE: изменение значений в строках.
 * Если условие не указано, изменяются все строки таблицы.
 */
public record UpdateStatement(String tableName, Map<String, Object> assignments, Condition condition)
        implements Statement {

    /** Проверяет, что указано, какие колонки менять. */
    public UpdateStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
        if (assignments == null || assignments.isEmpty()) {
            throw new SqlException("Не указано, какие значения менять");
        }
        assignments = new LinkedHashMap<>(assignments);
    }
}