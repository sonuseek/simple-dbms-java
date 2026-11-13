package ru.dbms.cli;

import ru.dbms.exec.Executor;
import ru.dbms.exec.QueryResult;
import ru.dbms.exec.ResultPrinter;
import ru.dbms.model.Column;
import ru.dbms.model.Database;
import ru.dbms.model.Table;
import ru.dbms.sql.Parser;
import ru.dbms.sql.SqlException;
import ru.dbms.sql.Statement;
import ru.dbms.storage.DatabaseRepository;
import ru.dbms.storage.StorageException;

import java.io.BufferedReader;
import java.io.PrintStream;

/**
 * Консольный интерфейс: читает запросы пользователя, выполняет их
 * и печатает результат. После каждого изменяющего запроса база сохраняется на диск.
 */
public class Console {

    private static final String PROMPT = "sql> ";

    private final DatabaseRepository repository;
    private final BufferedReader input;
    private final PrintStream output;
    private final ResultPrinter printer = new ResultPrinter();

    private Database database;
    private Executor executor;

    /** Создаёт консоль, работающую с указанным хранилищем и потоками ввода-вывода. */
    public Console(DatabaseRepository repository, BufferedReader input, PrintStream output) {
        this.repository = repository;
        this.input = input;
        this.output = output;
    }

    /** Запускает цикл чтения и выполнения запросов до команды EXIT или конца ввода. */
    public void run() {
        database = repository.load();
        executor = new Executor(database);

        output.println("Упрощённая СУБД. Введите HELP для справки, EXIT для выхода.");
        output.println("Загружено таблиц: " + database.size());

        while (true) {
            output.print(PROMPT);
            output.flush();

            String line = readLine();
            if (line == null) {
                break;
            }

            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (isCommand(line, "EXIT") || isCommand(line, "QUIT")) {
                break;
            }
            if (isCommand(line, "HELP")) {
                printHelp();
                continue;
            }
            if (isCommand(line, "TABLES")) {
                printTables();
                continue;
            }

            handleQuery(line);
        }

        output.println("До свидания.");
    }

    /** Выполняет один запрос и печатает результат или сообщение об ошибке. */
    private void handleQuery(String sql) {
        try {
            Statement statement = new Parser(sql).parse();
            QueryResult result = executor.execute(statement);
            output.println(printer.print(result));
            save();
        } catch (SqlException e) {
            output.println("Ошибка: " + e.getMessage());
        } catch (StorageException e) {
            output.println("Ошибка сохранения: " + e.getMessage());
        } catch (RuntimeException e) {
            output.println("Непредвиденная ошибка: " + e);
        }
    }

    /** Сохраняет базу на диск. */
    private void save() {
        try {
            repository.save(database);
        } catch (StorageException e) {
            output.println("Не удалось сохранить базу: " + e.getMessage());
        }
    }

    /** Читает строку ввода, возвращает null при конце потока или ошибке чтения. */
    private String readLine() {
        try {
            return input.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    /** Проверяет, что строка совпадает с командой, не считая регистра и точки с запятой. */
    private boolean isCommand(String line, String command) {
        String cleaned = line.endsWith(";") ? line.substring(0, line.length() - 1).trim() : line;
        return cleaned.equalsIgnoreCase(command);
    }

    /** Печатает список таблиц с их колонками. */
    private void printTables() {
        if (database.size() == 0) {
            output.println("Таблиц нет.");
            return;
        }
        for (String name : database.getTableNames()) {
            Table table = database.getTable(name);
            StringBuilder line = new StringBuilder(name + " (");
            for (int i = 0; i < table.getColumns().size(); i++) {
                Column column = table.getColumns().get(i);
                if (i > 0) {
                    line.append(", ");
                }
                line.append(column.getName()).append(' ').append(column.getType());
                if (column.getName().equals(table.getPrimaryKey())) {
                    line.append(" PRIMARY KEY");
                }
            }
            line.append("), строк: ").append(table.size());
            output.println(line);
        }
    }

    /** Печатает справку по поддерживаемым командам. */
    private void printHelp() {
        output.println("""
                Поддерживаемые запросы:
                  CREATE TABLE имя (колонка ТИП PRIMARY KEY, колонка ТИП, ...);
                  DROP TABLE имя;
                  INSERT INTO имя VALUES (значение, ...);
                  INSERT INTO имя (колонка, ...) VALUES (значение, ...);
                  SELECT * FROM имя [WHERE колонка знак значение];
                  SELECT колонка, ... FROM имя [WHERE колонка знак значение];
                  UPDATE имя SET колонка = значение, ... [WHERE колонка знак значение];
                  DELETE FROM имя [WHERE колонка знак значение];

                Типы колонок: INT, DOUBLE, TEXT, DATE, BOOLEAN
                Знаки сравнения: =, <>, !=, <, <=, >, >=
                Значения: 1, 2.5, 'текст', '2026-03-15', TRUE, FALSE, NULL

                Команды консоли:
                  TABLES — список таблиц
                  HELP   — эта справка
                  EXIT   — выход""");
    }
}
