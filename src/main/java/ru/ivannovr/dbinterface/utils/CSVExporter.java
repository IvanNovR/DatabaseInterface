package ru.ivannovr.dbinterface.utils;

import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ivannovr.dbinterface.model.TableSchema;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CSVExporter {
    private static final Logger logger = LogManager.getLogger(CSVExporter.class);

    public static void exportToCSV(TableSchema table, ObservableList<ObservableList<String>> data, Path path) throws IOException {
        if (data.isEmpty()) {
            logger.warn("No data to export for table {}", table.getName());
            throw new IOException("The table " + table.getName() + " is empty, nothing to export");
        }

        StringBuilder csvContent = new StringBuilder();
        List<String> columns = table.getColumns();

        // Формируем заголовки столбцов
        csvContent.append(columns.stream()
                        .map(CSVExporter::escapeCSVValue)
                        .collect(Collectors.joining(",")))
                .append("\n");

        // Формируем данные
        for (ObservableList<String> row : data) {
            csvContent.append(row.stream()
                            .map(CSVExporter::escapeCSVValue)
                            .collect(Collectors.joining(",")))
                    .append("\n");
        }

        // Записываем файл
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(csvContent.toString());
            logger.info("Exported table {} to CSV file: {}", table.getName(), path);
        } catch (IOException e) {
            logger.error("Failed to write CSV file for table {}: {}", table.getName(), e.getMessage());
            throw e;
        }
    }

    private static String escapeCSVValue(String value) {
        if (value == null) {
            return "";
        }
        // Экранируем значения, содержащие запятые, кавычки или переносы строк
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}