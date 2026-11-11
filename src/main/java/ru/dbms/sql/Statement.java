package ru.dbms.sql;

/** Разобранный запрос. Общий тип для всех видов команд. */
public interface Statement {

    /** Возвращает имя таблицы, к которой относится запрос. */
    String tableName();
}