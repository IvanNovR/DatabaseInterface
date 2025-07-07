package ru.ivannovr.dbinterface.utils;

import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteConfirmationFormatter {
    private static final Logger logger = LogManager.getLogger(DeleteConfirmationFormatter.class);

    public static String formatConfirmationMessage(ObservableList<String> mainRecord,
                                                   Map<String, List<ObservableList<String>>> relatedRecords) {
        StringBuilder message = new StringBuilder("Are you sure you want to delete this record?\n");
        message.append("Main record: ").append(mainRecord.stream().collect(Collectors.joining(", "))).append("\n");

        if (!relatedRecords.isEmpty()) {
            message.append("The following related records will also be deleted:\n");
            for (Map.Entry<String, List<ObservableList<String>>> entry : relatedRecords.entrySet()) {
                String childTable = entry.getKey();
                List<ObservableList<String>> records = entry.getValue();
                message.append("- Table ").append(childTable).append(": ").append(records.size()).append(" record(s)\n");
                for (ObservableList<String> record : records) {
                    message.append("  - ").append(record.stream().collect(Collectors.joining(", "))).append("\n");
                }
            }
        } else {
            message.append("No related records found.");
        }

        logger.debug("Formatted confirmation message for deletion: {}", message);
        return message.toString();
    }
}