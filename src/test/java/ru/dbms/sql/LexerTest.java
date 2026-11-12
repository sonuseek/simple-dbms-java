package ru.dbms.sql;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    private String dump(String sql) {
        StringBuilder result = new StringBuilder();
        for (Token token : new Lexer(sql).tokenize()) {
            if (token.getType() == TokenType.EOF) {
                break;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(token);
        }
        return result.toString();
    }

    @Test
    void readsSelectAll() {
        assertEquals("IDENTIFIER(SELECT) SYMBOL(*) IDENTIFIER(FROM) IDENTIFIER(students) SYMBOL(;)",
                dump("SELECT * FROM students;"));
    }

    @Test
    void readsInsertWithValues() {
        assertEquals("IDENTIFIER(INSERT) IDENTIFIER(INTO) IDENTIFIER(students) IDENTIFIER(VALUES) "
                        + "SYMBOL(() NUMBER(1) SYMBOL(,) STRING(Нейгум Софья) SYMBOL(,) "
                        + "NUMBER(8000.0) SYMBOL(,) IDENTIFIER(TRUE) SYMBOL()) SYMBOL(;)",
                dump("INSERT INTO students VALUES (1, 'Нейгум Софья', 8000.0, TRUE);"));
    }

    @Test
    void readsTwoCharOperators() {
        assertEquals("IDENTIFIER(a) SYMBOL(>=) NUMBER(1)", dump("a >= 1"));
        assertEquals("IDENTIFIER(a) SYMBOL(<=) NUMBER(1)", dump("a <= 1"));
        assertEquals("IDENTIFIER(a) SYMBOL(<>) NUMBER(1)", dump("a <> 1"));
        assertEquals("IDENTIFIER(a) SYMBOL(!=) NUMBER(1)", dump("a != 1"));
    }

    @Test
    void ignoresSpacesAndNewlines() {
        String expected = "IDENTIFIER(SELECT) SYMBOL(*) IDENTIFIER(FROM) IDENTIFIER(t) SYMBOL(;)";
        assertEquals(expected, dump("SELECT*FROM t;"));
        assertEquals(expected, dump("SELECT\n  *\tFROM   t ;"));
    }

    @Test
    void readsNamesWithUnderscoreAndDigits() {
        assertEquals("IDENTIFIER(group_id)", dump("group_id"));
        assertEquals("IDENTIFIER(col1)", dump("col1"));
    }

    @Test
    void readsStrings() {
        assertEquals("STRING()", dump("''"));
        assertEquals("STRING(  a b  )", dump("'  a b  '"));
        assertEquals("STRING(2026-03-15)", dump("'2026-03-15'"));
    }

    @Test
    void readsNumbers() {
        assertEquals("NUMBER(5000)", dump("5000"));
        assertEquals("NUMBER(25000.5)", dump("25000.5"));
    }

    @Test
    void alwaysEndsWithEof() {
        List<Token> tokens = new Lexer("a").tokenize();
        assertEquals(2, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(1).getType());
    }

    @Test
    void emptyInputGivesOnlyEof() {
        assertEquals(1, new Lexer("").tokenize().size());
    }

    @Test
    void remembersPosition() {
        List<Token> tokens = new Lexer("SELECT * FROM students").tokenize();
        assertEquals(7, tokens.get(1).getPosition());
    }

    @Test
    void checksKeywordsIgnoringCase() {
        List<Token> tokens = new Lexer("SELECT *").tokenize();
        assertTrue(tokens.get(0).isKeyword("select"));
        assertTrue(tokens.get(1).isSymbol("*"));
        assertFalse(tokens.get(1).isKeyword("*"));
    }

    @Test
    void failsOnUnclosedQuote() {
        assertThrows(SqlException.class, () -> new Lexer("SELECT 'abc").tokenize());
    }

    @Test
    void failsOnUnknownCharacter() {
        assertThrows(SqlException.class, () -> new Lexer("SELECT # FROM t").tokenize());
    }
}