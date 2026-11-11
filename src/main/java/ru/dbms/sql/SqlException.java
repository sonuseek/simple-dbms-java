package ru.dbms.sql;

/** Ошибка в тексте запроса: непонятный символ, пропущенное слово и подобное. */
public class SqlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Создаёт исключение с сообщением об ошибке. */
    public SqlException(String message) {
        super(message);
    }
}