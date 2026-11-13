package ru.dbms.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Таблица базы данных: схема колонок и хранимые строки.
 * Строки индексируются по значению первичного ключа, что даёт
 * быструю проверку уникальности при вставке.
 */
public class Table implements Serializable, Iterable<Row> {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<Column> columns;
    private final String primaryKey;
    private final Map<Object, Row> rows = new LinkedHashMap<>();

    /** Создаёт таблицу с указанной схемой и первичным ключом. */
    public Table(String name, List<Column> columns, String primaryKey) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя таблицы не может быть пустым");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Таблица должна содержать хотя бы одну колонку");
        }
        if (primaryKey == null || primaryKey.isBlank()) {
            throw new IllegalArgumentException("Первичный ключ не задан");
        }

        boolean keyFound = false;
        for (Column column : columns) {
            if (column.getName().equals(primaryKey)) {
                keyFound = true;
                break;
            }
        }
        if (!keyFound) {
            throw new IllegalArgumentException("Колонка первичного ключа не найдена: " + primaryKey);
        }

        this.name = name;
        this.columns = List.copyOf(columns);
        this.primaryKey = primaryKey;
    }

    /** Возвращает имя таблицы. */
    public String getName() {
        return name;
    }

    /** Возвращает схему таблицы. Изменению не подлежит. */
    public List<Column> getColumns() {
        return columns;
    }

    /** Возвращает имя колонки первичного ключа. */
    public String getPrimaryKey() {
        return primaryKey;
    }

    /** Находит колонку по имени или возвращает null, если такой колонки нет. */
    public Column getColumn(String columnName) {
        for (Column column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }

    /** Возвращает количество строк в таблице. */
    public int size() {
        return rows.size();
    }

    /** Возвращает строку по значению первичного ключа или null. */
    public Row findByKey(Object key) {
        return rows.get(key);
    }

    /**
     * Вставляет строку в таблицу.
     * Значения приводятся к типам колонок, первичный ключ должен быть задан и свободен.
     * Колонки, не указанные в строке, получают значение null.
     */
    public void insert(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Строка не может быть null");
        }

        for (String columnName : row.getValues().keySet()) {
            if (getColumn(columnName) == null) {
                throw new IllegalArgumentException(
                        "Колонка не найдена в таблице " + name + ": " + columnName);
            }
        }

        Row normalized = new Row();
        for (Column column : columns) {
            normalized.set(column.getName(), convert(column, row.get(column.getName())));
        }

        Object key = normalized.get(primaryKey);
        if (key == null) {
            throw new IllegalArgumentException("Первичный ключ не может быть null");
        }
        if (rows.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Строка с таким первичным ключом уже существует: " + key);
        }

        rows.put(key, normalized);
    }

    /**
     * Меняет значения в строке с указанным первичным ключом.
     * Возвращает false, если строки с таким ключом нет.
     * Если меняется сам первичный ключ, новое значение должно быть свободно.
     */
    public boolean updateRow(Object key, Map<String, Object> newValues) {
        Row existing = rows.get(key);
        if (existing == null) {
            return false;
        }
        if (newValues == null || newValues.isEmpty()) {
            throw new IllegalArgumentException("Не указано, какие значения менять");
        }

        for (String columnName : newValues.keySet()) {
            if (getColumn(columnName) == null) {
                throw new IllegalArgumentException(
                        "Колонка не найдена в таблице " + name + ": " + columnName);
            }
        }

        Row updated = new Row();
        for (Column column : columns) {
            String columnName = column.getName();
            Object value = newValues.containsKey(columnName)
                    ? convert(column, newValues.get(columnName))
                    : existing.get(columnName);
            updated.set(columnName, value);
        }

        Object newKey = updated.get(primaryKey);
        if (newKey == null) {
            throw new IllegalArgumentException("Первичный ключ не может быть null");
        }

        if (!newKey.equals(key)) {
            if (rows.containsKey(newKey)) {
                throw new IllegalArgumentException(
                        "Строка с таким первичным ключом уже существует: " + newKey);
            }
            rows.remove(key);
        }
        rows.put(newKey, updated);
        return true;
    }

    /** Приводит значение к типу колонки, добавляя имя колонки в сообщение об ошибке. */
    private Object convert(Column column, Object value) {
        try {
            return column.getType().convert(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Колонка " + column.getName() + ": " + e.getMessage());
        }
    }

    /** Удаляет строку по первичному ключу. Возвращает true, если строка была удалена. */
    public boolean deleteByKey(Object key) {
        return rows.remove(key) != null;
    }

    /** Перебирает строки таблицы в порядке добавления. */
    @Override
    public Iterator<Row> iterator() {
        return Collections.unmodifiableCollection(rows.values()).iterator();
    }

    @Override
    public String toString() {
        return name + columns + " строк: " + rows.size();
    }
}
