package ru.dbms.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * Запрос INSERT INTO: добавление одной или нескольких строк.
 * Если список колонок не указан, значения идут в порядке схемы таблицы.
 */
public record InsertStatement(String tableName, List<String> columnNames, List<List<Object>> rows)
        implements Statement {

    /** Проверяет, что значения заданы и их число совпадает с числом колонок. */
    public InsertStatement {
        if (tableName == null || tableName.isBlank()) {
            throw new SqlException("Не указано имя таблицы");
        }
        if (rows == null || rows.isEmpty()) {
            throw new SqlException("Не указаны значения для вставки");
        }

        List<List<Object>> copy = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row == null || row.isEmpty()) {
                throw new SqlException("Пустой список значений");
            }
            if (columnNames != null && columnNames.size() != row.size()) {
                throw new SqlException("Число колонок (" + columnNames.size()
                        + ") не совпадает с числом значений (" + row.size() + ")");
            }
            if (row.size() != rows.get(0).size()) {
                throw new SqlException("Строки содержат разное число значений");
            }
            copy.add(new ArrayList<>(row));
        }

        columnNames = columnNames == null ? null : List.copyOf(columnNames);
        rows = List.copyOf(copy);
    }

    /** Сообщает, перечислены ли колонки явно. */
    public boolean hasColumnNames() {
        return columnNames != null;
    }

    /** Возвращает количество вставляемых строк. */
    public int rowCount() {
        return rows.size();
    }
}
