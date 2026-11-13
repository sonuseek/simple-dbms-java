package ru.dbms.index;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Индекс по одной колонке таблицы.
 * Хранит отображение «значение колонки — ключи строк с этим значением».
 * Значения хранятся в отсортированном виде, поэтому индекс пригоден
 * не только для поиска по равенству, но и для сравнений больше и меньше.
 */
public class Index implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String columnName;
    private final TreeMap<Comparable<Object>, Set<Object>> entries = new TreeMap<>();
    private final Set<Object> nullKeys = new LinkedHashSet<>();

    /** Создаёт пустой индекс с указанным именем по указанной колонке. */
    public Index(String name, String columnName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя индекса не может быть пустым");
        }
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("Имя колонки не может быть пустым");
        }
        this.name = name;
        this.columnName = columnName;
    }

    /** Возвращает имя индекса. */
    public String getName() {
        return name;
    }

    /** Возвращает имя колонки, по которой построен индекс. */
    public String getColumnName() {
        return columnName;
    }

    /** Возвращает количество различных значений в индексе. */
    public int size() {
        return entries.size() + (nullKeys.isEmpty() ? 0 : 1);
    }

    /** Добавляет в индекс строку: значение колонки и ключ строки. */
    public void add(Object value, Object rowKey) {
        if (value == null) {
            nullKeys.add(rowKey);
            return;
        }
        entries.computeIfAbsent(asComparable(value), key -> new LinkedHashSet<>()).add(rowKey);
    }

    /** Убирает строку из индекса. */
    public void remove(Object value, Object rowKey) {
        if (value == null) {
            nullKeys.remove(rowKey);
            return;
        }
        Comparable<Object> key = asComparable(value);
        Set<Object> keys = entries.get(key);
        if (keys != null) {
            keys.remove(rowKey);
            if (keys.isEmpty()) {
                entries.remove(key);
            }
        }
    }

    /** Полностью очищает индекс. */
    public void clear() {
        entries.clear();
        nullKeys.clear();
    }

    /** Возвращает ключи строк, у которых значение колонки равно указанному. */
    public Set<Object> findEqual(Object value) {
        if (value == null) {
            return Collections.unmodifiableSet(nullKeys);
        }
        Set<Object> keys = entries.get(asComparable(value));
        return keys == null ? Set.of() : Collections.unmodifiableSet(keys);
    }

    /**
     * Возвращает ключи строк со значением меньше указанного.
     *
     * @param inclusive включать ли строки со значением, равным указанному
     */
    public Set<Object> findLess(Object value, boolean inclusive) {
        return collect(entries.headMap(asComparable(value), inclusive));
    }

    /**
     * Возвращает ключи строк со значением больше указанного.
     *
     * @param inclusive включать ли строки со значением, равным указанному
     */
    public Set<Object> findGreater(Object value, boolean inclusive) {
        return collect(entries.tailMap(asComparable(value), inclusive));
    }

    /** Собирает ключи строк из части индекса. */
    private Set<Object> collect(Map<Comparable<Object>, Set<Object>> part) {
        Set<Object> result = new LinkedHashSet<>();
        for (Set<Object> keys : part.values()) {
            result.addAll(keys);
        }
        return result;
    }

    /** Приводит значение к сравнимому виду, проверяя, что сравнение возможно. */
    @SuppressWarnings("unchecked")
    private Comparable<Object> asComparable(Object value) {
        if (value instanceof Comparable<?>) {
            return (Comparable<Object>) value;
        }
        throw new IllegalArgumentException(
                "Значения колонки " + columnName + " нельзя упорядочить: " + value);
    }

    @Override
    public String toString() {
        return name + " по колонке " + columnName + ", значений: " + size();
    }
}
