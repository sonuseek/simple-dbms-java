package ru.dbms.sql;

/** Один кусочек запроса: его вид, текст и место в исходной строке. */
public class Token {

    private final TokenType type;
    private final String text;
    private final int position;

    /** Создаёт кусочек запроса. */
    public Token(TokenType type, String text, int position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }

    /** Возвращает вид кусочка. */
    public TokenType getType() {
        return type;
    }

    /** Возвращает текст кусочка. У строки кавычки уже убраны. */
    public String getText() {
        return text;
    }

    /** Возвращает позицию первого символа в исходной строке запроса. */
    public int getPosition() {
        return position;
    }

    /** Проверяет, что это слово, совпадающее с указанным. Регистр не важен. */
    public boolean isKeyword(String keyword) {
        return type == TokenType.IDENTIFIER && text.equalsIgnoreCase(keyword);
    }

    /** Проверяет, что это указанный знак. */
    public boolean isSymbol(String symbol) {
        return type == TokenType.SYMBOL && text.equals(symbol);
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}