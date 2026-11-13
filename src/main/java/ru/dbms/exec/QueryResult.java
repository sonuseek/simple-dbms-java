package ru.dbms.exec;

import java.util.List;

/**
 * Результат выполнения запроса.
 * Для SELECT содержит имена колонок и выбранные строки,
 * для изменяющих запросов — число затронутых строк,
 * для остальных — текстовое сообщение.
 */
public record QueryResult(List<String> columnNames, List<List<Object>> rows,
                          int affectedRows, String message) {

    /** Создаёт результат выборки. */
    public static QueryResult selection(List<String> columnNames, List<List<Object>> rows) {
        return new QueryResult(List.copyOf(columnNames), List.copyOf(rows), rows.size(), null);
    }

    /** Создаёт результат изменяющего запроса. */
    public static QueryResult affected(int count, String message) {
        return new QueryResult(null, null, count, message);
    }

    /** Создаёт результат без строк: сообщение об успешном выполнении. */
    public static QueryResult message(String message) {
        return new QueryResult(null, null, 0, message);
    }

    /** Сообщает, содержит ли результат таблицу с данными. */
    public boolean isSelection() {
        return columnNames != null;
    }
}
