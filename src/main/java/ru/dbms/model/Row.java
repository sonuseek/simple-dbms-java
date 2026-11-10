package ru.dbms.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Одна строка таблицы: значения, сопоставленные именам колонок. */
public class Row implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> values = new LinkedHashMap<>();

    /** Возвращает значение колонки или null, если значения нет. */
    public Object get(String columnName) {
        return values.get(columnName);
    }

    /** Записывает значение в указанную колонку. */
    public void set(String columnName, Object value) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("Имя колонки не может быть пустым");
        }
        values.put(columnName, value);
    }

    /** Проверяет, задано ли значение для колонки. */
    public boolean has(String columnName) {
        return values.containsKey(columnName);
    }

    /** Возвращает все значения строки в порядке добавления. Изменению не подлежат. */
    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}