package ru.dbms.benchmark;

import ru.dbms.exec.Executor;
import ru.dbms.exec.QueryResult;
import ru.dbms.model.Database;
import ru.dbms.sql.Parser;

import java.util.Locale;
import java.util.Random;

/**
 * Измеряет, насколько индекс ускоряет выборку.
 * Создаёт две одинаковые таблицы, строит индекс только во второй
 * и сравнивает время выполнения одинаковых запросов.
 */
public class IndexBenchmark {

    private static final int ROWS = 100_000;
    private static final int WARMUP_RUNS = 20;
    private static final int MEASURED_RUNS = 50;
    private static final long SEED = 42;

    private final Database database = new Database();
    private final Executor executor = new Executor(database);

    /**
     * Запускает замеры и печатает результаты.
     *
     * @param args не используются
     */
    public static void main(String[] args) {
        new IndexBenchmark().run();
    }

    /** Готовит данные и выполняет все замеры. */
    private void run() {
        System.out.println("Строк в каждой таблице: " + ROWS);
        System.out.println("Прогревочных запусков: " + WARMUP_RUNS
                + ", измеряемых: " + MEASURED_RUNS);
        System.out.println();

        long filled = measureOnce(this::prepareData);
        System.out.printf(Locale.ROOT, "Заполнение таблиц: %.0f мс%n", filled / 1_000_000.0);

        long indexBuilt = measureOnce(() ->
                execute("CREATE INDEX idx_indexed_city ON indexed (city);"));
        System.out.printf(Locale.ROOT, "Построение индекса: %.0f мс%n%n", indexBuilt / 1_000_000.0);

        System.out.println("Запрос                        | без индекса | с индексом | выигрыш | строк");
        System.out.println("------------------------------+-------------+------------+---------+------");

        compare("WHERE city = 'Москва'",
                "SELECT id FROM plain WHERE city = 'Москва';",
                "SELECT id FROM indexed WHERE city = 'Москва';");

        compare("WHERE city = 'Калуга'",
                "SELECT id FROM plain WHERE city = 'Калуга';",
                "SELECT id FROM indexed WHERE city = 'Калуга';");

        compare("WHERE city = 'Нет такого'",
                "SELECT id FROM plain WHERE city = 'Нет такого';",
                "SELECT id FROM indexed WHERE city = 'Нет такого';");

        compare("WHERE city > 'Тверь'",
                "SELECT id FROM plain WHERE city > 'Тверь';",
                "SELECT id FROM indexed WHERE city > 'Тверь';");

        compare("WHERE city <> 'Москва'",
                "SELECT id FROM plain WHERE city <> 'Москва';",
                "SELECT id FROM indexed WHERE city <> 'Москва';");
    }

    /** Создаёт две одинаковые таблицы и заполняет их случайными данными. */
    private void prepareData() {
        String schema = " (id INT PRIMARY KEY, name TEXT, city TEXT, score DOUBLE);";
        execute("CREATE TABLE plain" + schema);
        execute("CREATE TABLE indexed" + schema);

        String[] cities = {"Москва", "Санкт-Петербург", "Казань", "Новосибирск", "Калуга",
                "Тверь", "Омск", "Самара", "Уфа", "Пермь"};
        Random random = new Random(SEED);

        for (int i = 1; i <= ROWS; i++) {
            String city = cities[random.nextInt(cities.length)];
            String values = "(" + i + ", 'Студент " + i + "', '" + city + "', "
                    + (random.nextInt(20000) / 10.0) + ");";
            execute("INSERT INTO plain VALUES " + values);
            execute("INSERT INTO indexed VALUES " + values);
        }
    }

    /** Выполняет оба запроса по многу раз и печатает строку таблицы с результатом. */
    private void compare(String title, String withoutIndex, String withIndex) {
        int found = execute(withIndex).affectedRows();

        long plainTime = measureMany(withoutIndex);
        long indexedTime = measureMany(withIndex);

        double plainMs = plainTime / 1_000_000.0;
        double indexedMs = indexedTime / 1_000_000.0;
        String speedup = indexedMs > 0
                ? String.format(Locale.ROOT, "%.1fx", plainMs / indexedMs)
                : "очень много";

        System.out.printf(Locale.ROOT, "%-29s | %8.3f мс | %7.3f мс | %7s | %5d%n",
                title, plainMs, indexedMs, speedup, found);
    }

    /** Возвращает среднее время выполнения запроса после прогрева. */
    private long measureMany(String sql) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            execute(sql);
        }

        long total = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            execute(sql);
            total += System.nanoTime() - start;
        }
        return total / MEASURED_RUNS;
    }

    /** Возвращает время выполнения одного действия в наносекундах. */
    private long measureOnce(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return System.nanoTime() - start;
    }

    /** Выполняет один запрос. */
    private QueryResult execute(String sql) {
        return executor.execute(new Parser(sql).parse());
    }
}
