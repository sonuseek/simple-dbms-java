package ru.dbms.model;

import ru.dbms.index.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Таблица базы данных: схема колонок, хранимые строки и индексы.
 * Строки хранятся по значению первичного ключа, что даёт
 * быструю проверку уникальности при вставке.
 */
public class Table implements Serializable, Iterable<Row> {

    private static final long serialVersionUID = 2L;

    private final String name;
    private final List<Column> columns;
    private final String primaryKey;
    private final Map<Object, Row> rows = new LinkedHashMap<>();
    private final Map<String, Index> indexes = new LinkedHashMap<>();

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
        insertAll(List.of(row == null ? new Row() : row));
    }

    /**
     * Вставляет сразу несколько строк.
     * Сначала проверяются все строки, и только потом выполняется вставка,
     * поэтому при ошибке в любой из них таблица остаётся без изменений.
     *
     * @param newRows строки для вставки
     */
    public void insertAll(List<Row> newRows) {
        if (newRows == null || newRows.isEmpty()) {
            throw new IllegalArgumentException("Нет строк для вставки");
        }

        List<Row> prepared = new ArrayList<>();
        Set<Object> newKeys = new LinkedHashSet<>();

        for (Row row : newRows) {
            Row normalized = normalize(row);
            Object key = normalized.get(primaryKey);

            if (key == null) {
                throw new IllegalArgumentException("Первичный ключ не может быть null");
            }
            if (rows.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Строка с таким первичным ключом уже существует: " + key);
            }
            if (!newKeys.add(key)) {
                throw new IllegalArgumentException(
                        "Первичный ключ повторяется внутри запроса: " + key);
            }
            prepared.add(normalized);
        }

        for (Row row : prepared) {
            Object key = row.get(primaryKey);
            rows.put(key, row);
            addToIndexes(row, key);
        }
    }

    /** Проверяет колонки строки и приводит значения к типам колонок таблицы. */
    private Row normalize(Row row) {
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
        return normalized;
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

        removeFromIndexes(existing, key);
        rows.put(newKey, updated);
        addToIndexes(updated, newKey);
        return true;
    }

    /** Удаляет строку по первичному ключу. Возвращает true, если строка была удалена. */
    public boolean deleteByKey(Object key) {
        Row removed = rows.remove(key);
        if (removed == null) {
            return false;
        }
        removeFromIndexes(removed, key);
        return true;
    }

    /**
     * Создаёт индекс по колонке и заполняет его имеющимися строками.
     *
     * @param indexName имя индекса, должно быть свободно
     * @param columnName колонка таблицы, по которой строится индекс
     */
    public void createIndex(String indexName, String columnName) {
        if (indexes.containsKey(indexName)) {
            throw new IllegalArgumentException("Индекс уже существует: " + indexName);
        }
        if (getColumn(columnName) == null) {
            throw new IllegalArgumentException(
                    "Колонка не найдена в таблице " + name + ": " + columnName);
        }

        Index index = new Index(indexName, columnName);
        for (Map.Entry<Object, Row> entry : rows.entrySet()) {
            index.add(entry.getValue().get(columnName), entry.getKey());
        }
        indexes.put(indexName, index);
    }

    /** Удаляет индекс. Возвращает true, если индекс существовал. */
    public boolean dropIndex(String indexName) {
        return indexes.remove(indexName) != null;
    }

    /** Возвращает индекс по имени или null. */
    public Index getIndex(String indexName) {
        return indexes.get(indexName);
    }

    /** Возвращает имена всех индексов таблицы. */
    public Set<String> getIndexNames() {
        return Collections.unmodifiableSet(indexes.keySet());
    }

    /** Находит индекс по колонке или возвращает null, если индекса нет. */
    public Index findIndexForColumn(String columnName) {
        for (Index index : indexes.values()) {
            if (index.getColumnName().equals(columnName)) {
                return index;
            }
        }
        return null;
    }

    /** Добавляет строку во все индексы таблицы. */
    private void addToIndexes(Row row, Object key) {
        for (Index index : indexes.values()) {
            index.add(row.get(index.getColumnName()), key);
        }
    }

    /** Убирает строку из всех индексов таблицы. */
    private void removeFromIndexes(Row row, Object key) {
        for (Index index : indexes.values()) {
            index.remove(row.get(index.getColumnName()), key);
        }
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
