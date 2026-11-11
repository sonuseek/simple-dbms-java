package ru.dbms.sql;

import ru.dbms.model.Row;

import java.time.LocalDate;

/** Условие WHERE: сравнение значения колонки с заданным значением. */
public record Condition(String columnName, Operator operator, Object value) {

    /** Проверяет, что условие задано полностью. */
    public Condition {
        if (columnName == null || columnName.isBlank()) {
            throw new SqlException("В условии не указана колонка");
        }
        if (operator == null) {
            throw new SqlException("В условии не указан знак сравнения");
        }
    }

    /** Проверяет, подходит ли строка под это условие. */
    public boolean matches(Row row) {
        Object actual = row.get(columnName);

        if (actual == null || value == null) {
            boolean bothNull = actual == null && value == null;
            return operator == Operator.NOT_EQUALS ? !bothNull : bothNull && operator == Operator.EQUALS;
        }

        int result = compare(actual, value);

        return switch (operator) {
            case EQUALS -> result == 0;
            case NOT_EQUALS -> result != 0;
            case LESS -> result < 0;
            case LESS_OR_EQUALS -> result <= 0;
            case GREATER -> result > 0;
            case GREATER_OR_EQUALS -> result >= 0;
        };
    }

    /** Сравнивает два значения. Числа сравниваются между собой независимо от типа. */
    private int compare(Object actual, Object expected) {
        if (actual instanceof Number first && expected instanceof Number second) {
            return Double.compare(first.doubleValue(), second.doubleValue());
        }
        if (actual instanceof String first && expected instanceof String second) {
            return first.compareTo(second);
        }
        if (actual instanceof LocalDate first && expected instanceof LocalDate second) {
            return first.compareTo(second);
        }
        if (actual instanceof Boolean first && expected instanceof Boolean second) {
            return Boolean.compare(first, second);
        }
        throw new SqlException("Нельзя сравнить " + actual.getClass().getSimpleName()
                + " и " + expected.getClass().getSimpleName() + " в колонке " + columnName);
    }

    @Override
    public String toString() {
        return columnName + " " + operator + " " + value;
    }
}