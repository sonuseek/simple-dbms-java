package ru.dbms.sql;

/** Знак сравнения в условии WHERE. */
public enum Operator {

    /** Равно. */
    EQUALS("="),
    /** Не равно. */
    NOT_EQUALS("<>"),
    /** Меньше. */
    LESS("<"),
    /** Меньше или равно. */
    LESS_OR_EQUALS("<="),
    /** Больше. */
    GREATER(">"),
    /** Больше или равно. */
    GREATER_OR_EQUALS(">=");

    private final String text;

    Operator(String text) {
        this.text = text;
    }

    /** Возвращает знак так, как он пишется в запросе. */
    public String getText() {
        return text;
    }

    /** Находит знак по его написанию. Для != возвращает тот же знак, что для <>. */
    public static Operator fromText(String text) {
        if ("!=".equals(text)) {
            return NOT_EQUALS;
        }
        for (Operator operator : values()) {
            if (operator.text.equals(text)) {
                return operator;
            }
        }
        throw new SqlException("Неизвестный знак сравнения: " + text);
    }

    @Override
    public String toString() {
        return text;
    }
}