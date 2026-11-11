package ru.dbms.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * Режет текст запроса на отдельные кусочки (токены).
 * Пробелы и переводы строк пропускаются, текст в кавычках остаётся одним кусочком.
 */
public class Lexer {

    private static final String[] TWO_CHAR_SYMBOLS = {">=", "<=", "<>", "!="};
    private static final String ONE_CHAR_SYMBOLS = ",;()*=><";

    private final String sql;
    private int position;

    /** Создаёт лексер для указанного текста запроса. */
    public Lexer(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("Текст запроса не задан");
        }
        this.sql = sql;
    }

    /** Разбирает весь запрос и возвращает список кусочков. Последний всегда EOF. */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        position = 0;

        while (true) {
            skipSpaces();
            if (position >= sql.length()) {
                break;
            }
            tokens.add(readToken());
        }

        tokens.add(new Token(TokenType.EOF, "", sql.length()));
        return tokens;
    }

    /** Пропускает пробелы, табуляции и переводы строк. */
    private void skipSpaces() {
        while (position < sql.length() && Character.isWhitespace(sql.charAt(position))) {
            position++;
        }
    }

    /** Читает один кусочек, начиная с текущей позиции. */
    private Token readToken() {
        char current = sql.charAt(position);

        if (current == '\'') {
            return readString();
        }
        if (Character.isDigit(current)) {
            return readNumber();
        }
        if (Character.isLetter(current) || current == '_') {
            return readIdentifier();
        }
        return readSymbol();
    }

    /** Читает текст в одинарных кавычках. Кавычки в результат не попадают. */
    private Token readString() {
        int start = position;
        position++;

        StringBuilder text = new StringBuilder();
        while (position < sql.length() && sql.charAt(position) != '\'') {
            text.append(sql.charAt(position));
            position++;
        }

        if (position >= sql.length()) {
            throw new SqlException("Не закрыта кавычка в позиции " + start);
        }
        position++;

        return new Token(TokenType.STRING, text.toString(), start);
    }

    /** Читает число: целое или с одной точкой. */
    private Token readNumber() {
        int start = position;
        boolean dotSeen = false;

        while (position < sql.length()) {
            char current = sql.charAt(position);
            if (Character.isDigit(current)) {
                position++;
            } else if (current == '.' && !dotSeen) {
                dotSeen = true;
                position++;
            } else {
                break;
            }
        }

        return new Token(TokenType.NUMBER, sql.substring(start, position), start);
    }

    /** Читает слово: буквы, цифры и подчёркивания. */
    private Token readIdentifier() {
        int start = position;

        while (position < sql.length()) {
            char current = sql.charAt(position);
            if (Character.isLetterOrDigit(current) || current == '_') {
                position++;
            } else {
                break;
            }
        }

        return new Token(TokenType.IDENTIFIER, sql.substring(start, position), start);
    }

    /** Читает знак, сначала проверяя двухсимвольные варианты. */
    private Token readSymbol() {
        int start = position;

        if (position + 1 < sql.length()) {
            String pair = sql.substring(position, position + 2);
            for (String symbol : TWO_CHAR_SYMBOLS) {
                if (symbol.equals(pair)) {
                    position += 2;
                    return new Token(TokenType.SYMBOL, symbol, start);
                }
            }
        }

        char current = sql.charAt(position);
        if (ONE_CHAR_SYMBOLS.indexOf(current) >= 0) {
            position++;
            return new Token(TokenType.SYMBOL, String.valueOf(current), start);
        }

        throw new SqlException("Непонятный символ '" + current + "' в позиции " + start);
    }
}