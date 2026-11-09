package ru.dbms.model;

import java.time.LocalDate;

/** Типы данных, которые может хранить колонка таблицы. */
public enum ColumnType {

    INT(Integer.class),
    DOUBLE(Double.class),
    TEXT(String.class),
    DATE(LocalDate.class),
    BOOLEAN(Boolean.class);

    private final Class<?> javaType;

    ColumnType(Class<?> javaType) {
        this.javaType = javaType;
    }

    /** Возвращает Java-класс, в котором хранится значение этого типа. */
    public Class<?> getJavaType() {
        return javaType;
    }

    /** Проверяет, подходит ли значение этому типу. null допустим для любого типа. */
    public boolean accepts(Object value) {
        return value == null || javaType.isInstance(value);
    }
}