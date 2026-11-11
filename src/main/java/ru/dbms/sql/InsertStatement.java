package ru.dbms.sql;

import java.util.List;

/**
 * Запрос INSERT INTO: добавление строки.
 * Если список колонок не указан, значения идут в порядке схемы таблицы.
 */
public record InsertStatement(String tableName, List<String> columnNames, List<Object> values)
        implements Statement {

    /** Проверяет, что значения заданы и их число совпадает с числом колонок. */
    public InsertStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
        if (values == null || values.isEmpty()) {
            throw new SqlException("Не указаны значения для вставки");
        }
        if (columnNames != null && columnNames.size() != values.size()) {
            throw new SqlException("Число колонок (" + columnNames.size()
                    + ") не совпадает с числом значений (" + values.size() + ")");
        }
        columnNames = columnNames == null ? null : List.copyOf(columnNames);
        values = values.stream().toList();
    }

    /** Сообщает, перечислены ли колонки явно. */
    public boolean hasColumnNames() {
        return columnNames != null;
    }
}