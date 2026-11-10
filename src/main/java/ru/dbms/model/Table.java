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
     * Проверяет, что все колонки существуют, значения подходят по типу,
     * первичный ключ задан и ещё не занят.
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
            Object value = row.get(column.getName());
            if (!column.getType().accepts(value)) {
                throw new IllegalArgumentException(
                        "Значение не подходит типу колонки " + column.getName()
                                + " (" + column.getType() + "): " + value);
            }
            normalized.set(column.getName(), value);
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