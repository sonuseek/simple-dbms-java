package ru.dbms.model;

import java.io.Serializable;

/** Описание одной колонки таблицы: имя и тип хранимых значений. */
public class Column implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;
    private final ColumnType type;

    /** Создаёт колонку с указанным именем и типом. */
    public Column(String name, ColumnType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя колонки не может быть пустым");
        }

        if (type == null) {
            throw new IllegalArgumentException("Тип колонки не может быть null");
        }

        this.name = name;
        this.type = type;
    }

    /** Возвращает имя колонки. */
    public String getName() {
        return name;
    }

    /** Возвращает тип колонки. */
    public ColumnType getType() {
        return type;
    }

    @Override
    public String toString() {
        return name + " " + type;
    }
}
