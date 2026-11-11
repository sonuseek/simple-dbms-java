package ru.dbms.sql;

import java.util.List;

/**
 * Запрос SELECT: выборка строк.
 * Если список колонок не указан, выбираются все колонки (звёздочка).
 * Если условие не указано, выбираются все строки.
 */
public record SelectStatement(String tableName, List<String> columnNames, Condition condition)
        implements Statement {

    /** Проверяет, что имя таблицы задано. */
    public SelectStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
        columnNames = columnNames == null ? null : List.copyOf(columnNames);
    }

    /** Сообщает, выбираются ли все колонки. */
    public boolean isSelectAll() {
        return columnNames == null;
    }
}