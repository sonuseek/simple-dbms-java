package ru.dbms.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** База данных: набор таблиц, доступных по имени. */
public class Database implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Table> tables = new LinkedHashMap<>();

    /** Добавляет таблицу в базу. Таблица с таким именем не должна существовать. */
    public void createTable(Table table) {
        if (table == null) {
            throw new IllegalArgumentException("Таблица не может быть null");
        }
        if (tables.containsKey(table.getName())) {
            throw new IllegalArgumentException("Таблица уже существует: " + table.getName());
        }
        tables.put(table.getName(), table);
    }

    /** Удаляет таблицу. Возвращает true, если таблица существовала. */
    public boolean dropTable(String name) {
        return tables.remove(name) != null;
    }

    /** Возвращает таблицу по имени или null, если такой таблицы нет. */
    public Table getTable(String name) {
        return tables.get(name);
    }

    /** Проверяет, существует ли таблица с указанным именем. */
    public boolean hasTable(String name) {
        return tables.containsKey(name);
    }

    /** Возвращает имена всех таблиц в порядке создания. Изменению не подлежат. */
    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    /** Возвращает количество таблиц в базе. */
    public int size() {
        return tables.size();
    }

    @Override
    public String toString() {
        return "База данных, таблиц: " + tables.size() + " " + tables.keySet();
    }
}