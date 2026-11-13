package ru.dbms.exec;

import java.time.LocalDate;
import java.util.List;

/** Превращает результат запроса в текст для вывода на экран. */
public class ResultPrinter {

    private static final String NULL_TEXT = "NULL";

    /** Возвращает готовый для печати текст результата. */
    public String print(QueryResult result) {
        if (!result.isSelection()) {
            return result.message();
        }
        return drawTable(result) + "\nСтрок: " + result.rows().size();
    }

    /** Рисует таблицу с рамкой. */
    private String drawTable(QueryResult result) {
        List<String> columns = result.columnNames();
        int[] widths = measure(columns, result.rows());

        StringBuilder text = new StringBuilder();
        String separator = buildSeparator(widths);

        text.append(separator).append('\n');
        text.append(buildRow(columns, widths)).append('\n');
        text.append(separator);

        for (List<Object> row : result.rows()) {
            text.append('\n').append(buildRow(format(row), widths));
        }
        text.append('\n').append(separator);

        return text.toString();
    }

    /** Считает ширину каждой колонки по самому длинному значению. */
    private int[] measure(List<String> columns, List<List<Object>> rows) {
        int[] widths = new int[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }
        for (List<Object> row : rows) {
            List<String> texts = format(row);
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], texts.get(i).length());
            }
        }
        return widths;
    }

    /** Превращает значения строки в текст. */
    private List<String> format(List<Object> row) {
        return row.stream().map(this::formatValue).toList();
    }

    /** Превращает одно значение в текст. */
    private String formatValue(Object value) {
        if (value == null) {
            return NULL_TEXT;
        }
        if (value instanceof Boolean flag) {
            return flag ? "TRUE" : "FALSE";
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return value.toString();
    }

    /** Строит горизонтальную линию рамки. */
    private String buildSeparator(int[] widths) {
        StringBuilder line = new StringBuilder("+");
        for (int width : widths) {
            line.append("-".repeat(width + 2)).append('+');
        }
        return line.toString();
    }

    /** Строит строку таблицы с выравниванием по ширине колонок. */
    private String buildRow(List<String> values, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String value = values.get(i);
            line.append(' ').append(value)
                    .append(" ".repeat(widths[i] - value.length()))
                    .append(" |");
        }
        return line.toString();
    }
}
