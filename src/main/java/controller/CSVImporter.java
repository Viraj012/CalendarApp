package controller;

import model.Calendar;
import model.CalendarManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for importing events from CSV files.
 */
public class CSVImporter {

  /**
   * Imports events from a CSV file into a calendar.
   * The CSV file should be in Google Calendar CSV format.
   *
   * @param filename the path to the CSV file
   * @param calendarManager the calendar manager
   * @param calendarName the name of the calendar to import into
   * @return the number of events successfully imported, or -1 if an error occurred
   */
  public static int importFromCSV(String filename, CalendarManager calendarManager, String calendarName) {
    if (!calendarManager.calendarExists(calendarName)) {
      return -1;  // Calendar doesn't exist
    }

    Calendar calendar = calendarManager.getCalendar(calendarName);

    // Google Calendar CSV format has these headers:
    // Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private

    List<String[]> records = new ArrayList<>();
    int successCount = 0;

    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
      String line;
      boolean isHeader = true;

      while ((line = br.readLine()) != null) {
        if (isHeader) {
          isHeader = false;
          continue;  // Skip header row
        }

        String[] values = parseCSVLine(line);
        if (values.length < 9) {
          continue;  // Skip invalid rows
        }

        records.add(values);
      }

      // Process records
      for (String[] record : records) {
        try {
          if (importEvent(record, calendar)) {
            successCount++;
          }
        } catch (Exception e) {
          // Log the error but continue processing other records
          System.err.println("Error importing event: " + e.getMessage());
        }
      }

      return successCount;
    } catch (IOException e) {
      System.err.println("Error reading CSV file: " + e.getMessage());
      return -1;
    }
  }

  /**
   * Parses a CSV line, handling quoted values.
   */
  private static String[] parseCSVLine(String line) {
    List<String> result = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder currentValue = new StringBuilder();

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        // If this is a double quote within quotes, add a single quote
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          currentValue.append('"');
          i++; // Skip the next quote
        } else {
          // Toggle the inQuotes flag
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        // End of field, add to result
        result.add(currentValue.toString());
        currentValue.setLength(0); // Clear the builder
      } else {
        // Regular character, add to current value
        currentValue.append(c);
      }
    }

    // Add the last field
    result.add(currentValue.toString());

    return result.toArray(new String[0]);
  }

  /**
   * Imports a single event from a CSV record.
   */
  private static boolean importEvent(String[] record, Calendar calendar) {
    String subject = record[0].trim();
    String startDateStr = record[1].trim();
    String startTimeStr = record[2].trim();
    String endDateStr = record[3].trim();
    String endTimeStr = record[4].trim();
    String allDayStr = record[5].trim();
    String description = record[6].trim();
    String location = record[7].trim();
    String isPrivateStr = record[8].trim();

    boolean isAllDay = "True".equalsIgnoreCase(allDayStr);
    boolean isPrivate = "True".equalsIgnoreCase(isPrivateStr);

    try {
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

      if (isAllDay) {
        // All-day event
        LocalDate startDate = LocalDate.parse(startDateStr, dateFormatter);
        LocalDateTime dateTime = startDate.atStartOfDay();

        return calendar.createAllDayEvent(
            subject,
            dateTime,
            true, // autoDecline
            description,
            location,
            !isPrivate // isPublic is opposite of isPrivate
        );
      } else {
        // Regular event
        LocalDate startDate = LocalDate.parse(startDateStr, dateFormatter);
        LocalDate endDate = LocalDate.parse(endDateStr, dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter);
        LocalTime endTime = LocalTime.parse(endTimeStr, timeFormatter);

        LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

        return calendar.createEvent(
            subject,
            startDateTime,
            endDateTime,
            true, // autoDecline
            description,
            location,
            !isPrivate // isPublic is opposite of isPrivate
        );
      }
    } catch (DateTimeParseException e) {
      System.err.println("Error parsing date/time: " + e.getMessage());
      return false;
    }
  }
}