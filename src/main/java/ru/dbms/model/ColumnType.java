package ru.dbms.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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

    /**
     * Приводит значение из запроса к этому типу.
     * Целое число становится дробным для DOUBLE, текст вида 2026-03-15 становится датой.
     * Если привести нельзя, выбрасывает IllegalArgumentException.
     */
    public Object convert(Object value) {
        if (value == null || javaType.isInstance(value)) {
            return value;
        }

        if (this == DOUBLE && value instanceof Integer number) {
            return number.doubleValue();
        }
        if (this == DATE && value instanceof String text) {
            return parseDate(text);
        }

        throw new IllegalArgumentException(
                "Значение " + describe(value) + " не подходит типу " + name());
    }

    /** Читает дату в формате ГГГГ-ММ-ДД. */
    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Дата должна быть в формате ГГГГ-ММ-ДД, а не '" + text + "'");
        }
    }

    /** Описывает значение для сообщения об ошибке. */
    private String describe(Object value) {
        if (value instanceof String text) {
            return "'" + text + "'";
        }
        return String.valueOf(value);
    }
}
