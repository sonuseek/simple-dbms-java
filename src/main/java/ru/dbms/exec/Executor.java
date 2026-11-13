package ru.dbms.exec;

import ru.dbms.model.Column;
import ru.dbms.model.Database;
import ru.dbms.model.Row;
import ru.dbms.model.Table;
import ru.dbms.sql.Condition;
import ru.dbms.sql.CreateIndexStatement;
import ru.dbms.sql.CreateTableStatement;
import ru.dbms.sql.DeleteStatement;
import ru.dbms.sql.DropTableStatement;
import ru.dbms.sql.InsertStatement;
import ru.dbms.sql.SelectStatement;
import ru.dbms.sql.SqlException;
import ru.dbms.sql.Statement;
import ru.dbms.sql.UpdateStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Выполняет разобранные запросы над базой данных. */
public class Executor {

    private final Database database;

    /** Создаёт исполнитель для указанной базы. */
    public Executor(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("База данных не задана");
        }
        this.database = database;
    }

    /** Выполняет запрос и возвращает результат. */
    public QueryResult execute(Statement statement) {
        if (statement == null) {
            throw new IllegalArgumentException("Запрос не задан");
        }

        if (statement instanceof CreateTableStatement create) {
            return executeCreateTable(create);
        }
        if (statement instanceof DropTableStatement drop) {
            return executeDropTable(drop);
        }
        if (statement instanceof InsertStatement insert) {
            return executeInsert(insert);
        }
        if (statement instanceof SelectStatement select) {
            return executeSelect(select);
        }
        if (statement instanceof UpdateStatement update) {
            return executeUpdate(update);
        }
        if (statement instanceof DeleteStatement delete) {
            return executeDelete(delete);
        }
        if (statement instanceof CreateIndexStatement) {
            throw new SqlException("Индексы пока не поддерживаются");
        }
        throw new SqlException("Неизвестный вид запроса: " + statement.getClass().getSimpleName());
    }

    /** CREATE TABLE: создаёт таблицу. */
    private QueryResult executeCreateTable(CreateTableStatement statement) {
        if (database.hasTable(statement.tableName())) {
            throw new SqlException("Таблица уже существует: " + statement.tableName());
        }

        Table table;
        try {
            table = new Table(statement.tableName(), statement.columns(), statement.primaryKey());
        } catch (IllegalArgumentException e) {
            throw new SqlException(e.getMessage());
        }

        database.createTable(table);
        return QueryResult.message("Таблица создана: " + statement.tableName());
    }

    /** DROP TABLE: удаляет таблицу. */
    private QueryResult executeDropTable(DropTableStatement statement) {
        if (!database.dropTable(statement.tableName())) {
            throw new SqlException("Таблица не найдена: " + statement.tableName());
        }
        return QueryResult.message("Таблица удалена: " + statement.tableName());
    }

    /** INSERT INTO: добавляет строку. */
    private QueryResult executeInsert(InsertStatement statement) {
        Table table = requireTable(statement.tableName());
        List<String> columnNames = statement.hasColumnNames()
                ? statement.columnNames()
                : defaultColumnNames(table);

        if (columnNames.size() != statement.values().size()) {
            throw new SqlException("В таблице " + table.getName() + " колонок "
                    + columnNames.size() + ", а значений " + statement.values().size());
        }

        Row row = new Row();
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            requireColumn(table, columnName);
            row.set(columnName, statement.values().get(i));
        }

        try {
            table.insert(row);
        } catch (IllegalArgumentException e) {
            throw new SqlException(e.getMessage());
        }
        return QueryResult.affected(1, "Добавлено строк: 1");
    }

    /** SELECT: выбирает строки. */
    private QueryResult executeSelect(SelectStatement statement) {
        Table table = requireTable(statement.tableName());

        List<String> columnNames = statement.isSelectAll()
                ? defaultColumnNames(table)
                : statement.columnNames();
        for (String columnName : columnNames) {
            requireColumn(table, columnName);
        }

        Condition condition = prepareCondition(table, statement.condition());

        List<List<Object>> rows = new ArrayList<>();
        for (Row row : table) {
            if (condition == null || condition.matches(row)) {
                List<Object> values = new ArrayList<>();
                for (String columnName : columnNames) {
                    values.add(row.get(columnName));
                }
                rows.add(values);
            }
        }

        return QueryResult.selection(columnNames, rows);
    }

    /** UPDATE: меняет значения в подходящих строках. */
    private QueryResult executeUpdate(UpdateStatement statement) {
        Table table = requireTable(statement.tableName());
        for (String columnName : statement.assignments().keySet()) {
            requireColumn(table, columnName);
        }

        Condition condition = prepareCondition(table, statement.condition());
        List<Object> keys = collectKeys(table, condition);

        Map<String, Object> assignments = statement.assignments();
        int changed = 0;
        for (Object key : keys) {
            try {
                if (table.updateRow(key, assignments)) {
                    changed++;
                }
            } catch (IllegalArgumentException e) {
                throw new SqlException(e.getMessage());
            }
        }

        return QueryResult.affected(changed, "Изменено строк: " + changed);
    }

    /** DELETE: удаляет подходящие строки. */
    private QueryResult executeDelete(DeleteStatement statement) {
        Table table = requireTable(statement.tableName());
        Condition condition = prepareCondition(table, statement.condition());

        List<Object> keys = collectKeys(table, condition);
        int removed = 0;
        for (Object key : keys) {
            if (table.deleteByKey(key)) {
                removed++;
            }
        }

        return QueryResult.affected(removed, "Удалено строк: " + removed);
    }

    /**
     * Собирает ключи строк, подходящих под условие.
     * Ключи собираются заранее, чтобы не менять таблицу во время перебора.
     */
    private List<Object> collectKeys(Table table, Condition condition) {
        List<Object> keys = new ArrayList<>();
        for (Row row : table) {
            if (condition == null || condition.matches(row)) {
                keys.add(row.get(table.getPrimaryKey()));
            }
        }
        return keys;
    }

    /** Приводит значение условия к типу колонки. Возвращает null, если условия нет. */
    private Condition prepareCondition(Table table, Condition condition) {
        if (condition == null) {
            return null;
        }

        Column column = requireColumn(table, condition.columnName());
        try {
            Object value = column.getType().convert(condition.value());
            return new Condition(condition.columnName(), condition.operator(), value);
        } catch (IllegalArgumentException e) {
            throw new SqlException("Условие по колонке " + column.getName() + ": " + e.getMessage());
        }
    }

    /** Возвращает имена всех колонок таблицы по порядку. */
    private List<String> defaultColumnNames(Table table) {
        List<String> names = new ArrayList<>();
        for (Column column : table.getColumns()) {
            names.add(column.getName());
        }
        return names;
    }

    /** Находит таблицу или сообщает, что её нет. */
    private Table requireTable(String name) {
        Table table = database.getTable(name);
        if (table == null) {
            throw new SqlException("Таблица не найдена: " + name);
        }
        return table;
    }

    /** Находит колонку или сообщает, что её нет. */
    private Column requireColumn(Table table, String columnName) {
        Column column = table.getColumn(columnName);
        if (column == null) {
            throw new SqlException("Колонка не найдена в таблице "
                    + table.getName() + ": " + columnName);
        }
        return column;
    }
}
