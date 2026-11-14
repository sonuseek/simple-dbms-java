package ru.dbms.sql;

import ru.dbms.model.Column;
import ru.dbms.model.ColumnType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Собирает разобранный запрос из кусочков, выданных лексером.
 * Поддерживает CREATE TABLE, DROP TABLE, CREATE INDEX,
 * INSERT INTO, SELECT, UPDATE и DELETE.
 */
public class Parser {

    private final List<Token> tokens;
    private int position;

    /** Создаёт парсер для текста запроса. */
    public Parser(String sql) {
        this.tokens = new Lexer(sql).tokenize();
    }

    /** Разбирает запрос целиком. */
    public Statement parse() {
        if (current().getType() == TokenType.EOF) {
            throw new SqlException("Пустой запрос");
        }

        Statement statement = parseStatement();

        if (current().isSymbol(";")) {
            next();
        }
        if (current().getType() != TokenType.EOF) {
            throw new SqlException("Лишний текст после запроса: " + current().getText());
        }
        return statement;
    }

    /** Определяет вид запроса по первому слову. */
    private Statement parseStatement() {
        if (current().isKeyword("SELECT")) {
            return parseSelect();
        }
        if (current().isKeyword("INSERT")) {
            return parseInsert();
        }
        if (current().isKeyword("UPDATE")) {
            return parseUpdate();
        }
        if (current().isKeyword("DELETE")) {
            return parseDelete();
        }
        if (current().isKeyword("CREATE")) {
            return parseCreate();
        }
        if (current().isKeyword("DROP")) {
            return parseDrop();
        }
        throw new SqlException("Неизвестная команда: " + current().getText());
    }

    /** SELECT * FROM t [WHERE ...] или SELECT a, b FROM t [WHERE ...] */
    private Statement parseSelect() {
        expectKeyword("SELECT");

        List<String> columns = null;
        if (current().isSymbol("*")) {
            next();
        } else {
            columns = parseNameList();
        }

        expectKeyword("FROM");
        String table = expectIdentifier("имя таблицы");
        Condition condition = parseOptionalWhere();

        return new SelectStatement(table, columns, condition);
    }

    /** INSERT INTO t [(a, b)] VALUES (1, 'x') */
    private Statement parseInsert() {
        expectKeyword("INSERT");
        expectKeyword("INTO");
        String table = expectIdentifier("имя таблицы");

        List<String> columns = null;
        if (current().isSymbol("(")) {
            next();
            columns = parseNameList();
            expectSymbol(")");
        }

        expectKeyword("VALUES");

        List<List<Object>> rows = new ArrayList<>();
        while (true) {
            expectSymbol("(");
            rows.add(parseValueList());
            expectSymbol(")");

            if (current().isSymbol(",")) {
                next();
            } else {
                break;
            }
        }

        return new InsertStatement(table, columns, rows);
    }

    /** UPDATE t SET a = 1, b = 'x' [WHERE ...] */
    private Statement parseUpdate() {
        expectKeyword("UPDATE");
        String table = expectIdentifier("имя таблицы");
        expectKeyword("SET");

        Map<String, Object> assignments = new LinkedHashMap<>();
        while (true) {
            String column = expectIdentifier("имя колонки");
            expectSymbol("=");
            assignments.put(column, parseValue());

            if (current().isSymbol(",")) {
                next();
            } else {
                break;
            }
        }

        Condition condition = parseOptionalWhere();
        return new UpdateStatement(table, assignments, condition);
    }

    /** DELETE FROM t [WHERE ...] */
    private Statement parseDelete() {
        expectKeyword("DELETE");
        expectKeyword("FROM");
        String table = expectIdentifier("имя таблицы");
        Condition condition = parseOptionalWhere();

        return new DeleteStatement(table, condition);
    }

    /** CREATE TABLE ... или CREATE INDEX ... */
    private Statement parseCreate() {
        expectKeyword("CREATE");

        if (current().isKeyword("TABLE")) {
            return parseCreateTable();
        }
        if (current().isKeyword("INDEX")) {
            return parseCreateIndex();
        }
        throw new SqlException("После CREATE ожидается TABLE или INDEX, а не " + current().getText());
    }

    /** CREATE TABLE t (id INT PRIMARY KEY, name TEXT) */
    private Statement parseCreateTable() {
        expectKeyword("TABLE");
        String table = expectIdentifier("имя таблицы");
        expectSymbol("(");

        List<Column> columns = new ArrayList<>();
        String primaryKey = null;

        while (true) {
            String columnName = expectIdentifier("имя колонки");
            ColumnType type = parseColumnType();
            columns.add(new Column(columnName, type));

            if (current().isKeyword("PRIMARY")) {
                next();
                expectKeyword("KEY");
                if (primaryKey != null) {
                    throw new SqlException("Первичный ключ указан дважды");
                }
                primaryKey = columnName;
            }

            if (current().isSymbol(",")) {
                next();
            } else {
                break;
            }
        }

        expectSymbol(")");

        if (primaryKey == null) {
            throw new SqlException("Не указан первичный ключ (PRIMARY KEY)");
        }
        return new CreateTableStatement(table, columns, primaryKey);
    }

    /** CREATE INDEX idx ON t (column) */
    private Statement parseCreateIndex() {
        expectKeyword("INDEX");
        String indexName = expectIdentifier("имя индекса");
        expectKeyword("ON");
        String table = expectIdentifier("имя таблицы");
        expectSymbol("(");
        String column = expectIdentifier("имя колонки");
        expectSymbol(")");

        return new CreateIndexStatement(indexName, table, column);
    }

    /** DROP TABLE t */
    private Statement parseDrop() {
        expectKeyword("DROP");
        expectKeyword("TABLE");
        String table = expectIdentifier("имя таблицы");

        return new DropTableStatement(table);
    }

    /** Читает тип колонки: INT, DOUBLE, TEXT, DATE или BOOLEAN. */
    private ColumnType parseColumnType() {
        String text = expectIdentifier("тип колонки");
        for (ColumnType type : ColumnType.values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new SqlException("Неизвестный тип колонки: " + text);
    }

    /** Читает WHERE, если он есть. Иначе возвращает null. */
    private Condition parseOptionalWhere() {
        if (!current().isKeyword("WHERE")) {
            return null;
        }
        next();

        String column = expectIdentifier("имя колонки");
        Token operatorToken = current();
        if (operatorToken.getType() != TokenType.SYMBOL) {
            throw new SqlException("Ожидается знак сравнения, а не " + operatorToken.getText());
        }
        next();

        Operator operator = Operator.fromText(operatorToken.getText());
        return new Condition(column, operator, parseValue());
    }

    /** Читает список имён через запятую. */
    private List<String> parseNameList() {
        List<String> names = new ArrayList<>();
        while (true) {
            names.add(expectIdentifier("имя колонки"));
            if (current().isSymbol(",")) {
                next();
            } else {
                return names;
            }
        }
    }

    /** Читает список значений через запятую. */
    private List<Object> parseValueList() {
        List<Object> values = new ArrayList<>();
        while (true) {
            values.add(parseValue());
            if (current().isSymbol(",")) {
                next();
            } else {
                return values;
            }
        }
    }

    /**
     * Читает одно значение: число, текст, TRUE, FALSE или NULL.
     * Текст остаётся текстом: превращение в дату выполняется позже,
     * когда известен тип колонки.
     */
    private Object parseValue() {
        Token token = current();

        if (token.getType() == TokenType.NUMBER) {
            next();
            return parseNumber(token.getText());
        }
        if (token.getType() == TokenType.STRING) {
            next();
            return token.getText();
        }
        if (token.isKeyword("TRUE")) {
            next();
            return Boolean.TRUE;
        }
        if (token.isKeyword("FALSE")) {
            next();
            return Boolean.FALSE;
        }
        if (token.isKeyword("NULL")) {
            next();
            return null;
        }
        throw new SqlException("Ожидается значение, а не " + describe(token));
    }

    /** Превращает текст числа в целое или дробное. */
    private Object parseNumber(String text) {
        try {
            if (text.contains(".")) {
                return Double.valueOf(text);
            }
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new SqlException("Неверное число: " + text);
        }
    }

    /** Возвращает текущий кусочек. */
    private Token current() {
        return tokens.get(position);
    }

    /** Переходит к следующему кусочку. */
    private void next() {
        if (position < tokens.size() - 1) {
            position++;
        }
    }

    /** Требует указанное слово и пропускает его. */
    private void expectKeyword(String keyword) {
        if (!current().isKeyword(keyword)) {
            throw new SqlException("Ожидается " + keyword + ", а не " + describe(current()));
        }
        next();
    }

    /** Требует указанный знак и пропускает его. */
    private void expectSymbol(String symbol) {
        if (!current().isSymbol(symbol)) {
            throw new SqlException("Ожидается " + symbol + ", а не " + describe(current()));
        }
        next();
    }

    /** Требует слово и возвращает его текст. */
    private String expectIdentifier(String what) {
        if (current().getType() != TokenType.IDENTIFIER) {
            throw new SqlException("Ожидается " + what + ", а не " + describe(current()));
        }
        String text = current().getText();
        next();
        return text;
    }

    /** Описывает кусочек для сообщения об ошибке. */
    private String describe(Token token) {
        if (token.getType() == TokenType.EOF) {
            return "конец запроса";
        }
        return "'" + token.getText() + "' в позиции " + token.getPosition();
    }
}