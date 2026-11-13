package ru.dbms.cli;

import ru.dbms.storage.DatabaseRepository;
import ru.dbms.storage.FileDatabaseRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;

/** Точка входа в программу. */
public class Main {

    private static final String DEFAULT_FILE = "db.dat";

    /**
     * Запускает консольную СУБД.
     *
     * @param args первым аргументом можно указать путь к файлу базы
     */
    public static void main(String[] args) {
        Path file = Path.of(args.length > 0 ? args[0] : DEFAULT_FILE);
        DatabaseRepository repository = new FileDatabaseRepository(file);

        BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in, consoleCharset()));

        new Console(repository, input, System.out).run();
    }

    /**
     * Определяет кодировку консоли, чтобы русский ввод читался правильно.
     * В Windows консоль обычно работает не в UTF-8, поэтому кодировку
     * нужно брать у системы, а не задавать жёстко.
     */
    private static Charset consoleCharset() {
        String name = System.getProperty("stdin.encoding");
        if (name == null) {
            name = System.getProperty("native.encoding");
        }
        if (name == null) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(name);
        } catch (RuntimeException e) {
            return Charset.defaultCharset();
        }
    }
}
