package controller;

import model.Calendar;
import model.Event;
import view.TextUI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Processes user commands and interacts with the Calendar model.
 */
public class CommandProcessor {
  private Calendar calendar;
  private TextUI view;

  /**
   * Creates a new command processor.
   *
   * @param calendar the calendar model
   * @param view the text UI view
   */
  public CommandProcessor(Calendar calendar, TextUI view) {
    this.calendar = calendar;
    this.view = view;
  }

  /**
   * Process a command from the user.
   *
   * @param command the command to process
   * @return true if the application should continue running, false to exit
   */
  public boolean processCommand(String command) {
    String[] parts = command.trim().split("\\s+");

    if (parts.length == 0 || (parts.length == 1 && parts[0].isEmpty())) {
      view.displayError("Empty command");
      return true;
    }

    try {
      switch (parts[0].toLowerCase()) {
        case "create":
          if (parts.length < 2 || !parts[1].equals("event")) {
            view.displayError("Invalid create command. Expected 'create event'");
            return true;
          }
          processCreateCommand(command);
          return true;

        case "edit":
          if (parts.length < 2 || (!parts[1].equals("event") && !parts[1].equals("events"))) {
            view.displayError("Invalid edit command. Expected 'edit event' or 'edit events'");
            return true;
          }
          processEditCommand(command);
          return true;

        case "print":
          if (parts.length < 2 || !parts[1].equals("events")) {
            view.displayError("Invalid print command. Expected 'print events'");
            return true;
          }
          processPrintCommand(command);
          return true;

        case "export":
          if (parts.length < 2 || !parts[1].equals("cal")) {
            view.displayError("Invalid export command. Expected 'export cal'");
            return true;
          }
          processExportCommand(command);
          return true;

        case "show":
          if (parts.length < 2 || !parts[1].equals("status")) {
            view.displayError("Invalid show command. Expected 'show status'");
            return true;
          }
          processShowCommand(command);
          return true;

        case "exit":
          return false;

        default:
          view.displayError("Unknown command: " + parts[0]);
          return true;
      }
    } catch (Exception e) {
      view.displayError("Error processing command: " + e.getMessage());
      return true;
    }
  }

  /**
   * Process a create event command.
   */
  private void processCreateCommand(String command) {
    boolean autoDecline = command.contains("--autoDecline");
    String cmdWithoutAutoDecline = command.replace("--autoDecline", "").trim();

    if (cmdWithoutAutoDecline.contains(" on ")) {
      // All-day event
      processCreateAllDayCommand(cmdWithoutAutoDecline, autoDecline);
    } else if (cmdWithoutAutoDecline.contains(" from ")) {
      // Regular event with start and end time
      processCreateRegularCommand(cmdWithoutAutoDecline, autoDecline);
    } else {
      view.displayError("Invalid create event command format");
    }
  }

  /**
   * Process a create all-day event command.
   */
  private void processCreateAllDayCommand(String command, boolean autoDecline) {
    // Parse command for all-day event
    String[] parts = command.split(" on ");
    if (parts.length != 2) {
      view.displayError("Invalid all-day event format");
      return;
    }

    String eventName = parts[0].substring("create event".length()).trim();
    String dateTimeStr = parts[1].trim();

    // Check if it's a recurring event
    if (dateTimeStr.contains(" repeats ")) {
      processCreateRecurringAllDayCommand(eventName, dateTimeStr, autoDecline);
    } else {
      // Single all-day event
      try {
        LocalDateTime dateTime = parseDateTime(dateTimeStr);
        boolean success = calendar.createAllDayEvent(eventName, dateTime, autoDecline);

        if (success) {
          view.displayMessage("All-day event created successfully: " + eventName);
        } else {
          view.displayError("Failed to create all-day event (conflict detected)");
        }
      } catch (DateTimeParseException e) {
        view.displayError("Invalid date/time format: " + dateTimeStr);
      }
    }
  }

  /**
   * Process a create recurring all-day event command.
   */
  private void processCreateRecurringAllDayCommand(String eventName, String dateTimeStr, boolean autoDecline) {
    String[] parts = dateTimeStr.split(" repeats ");
    if (parts.length != 2) {
      view.displayError("Invalid recurring event format");
      return;
    }

    String dateStr = parts[0].trim();
    String recurrenceStr = parts[1].trim();

    try {
      LocalDateTime dateTime = parseDateTime(dateStr);
      String weekdays;
      int occurrences = -1;
      LocalDateTime untilDate = null;

      if (recurrenceStr.contains(" for ")) {
        // Recurs for N times
        String[] recParts = recurrenceStr.split(" for ");
        weekdays = recParts[0].trim();
        occurrences = Integer.parseInt(recParts[1].replace("times", "").trim());
      } else if (recurrenceStr.contains(" until ")) {
        // Recurs until a date
        String[] recParts = recurrenceStr.split(" until ");
        weekdays = recParts[0].trim();
        untilDate = parseDateTime(recParts[1].trim());
      } else {
        view.displayError("Invalid recurrence pattern");
        return;
      }

      boolean success = calendar.createRecurringAllDayEvent(
          eventName, dateTime, weekdays, occurrences, untilDate, autoDecline);

      if (success) {
        view.displayMessage("Recurring all-day event created successfully: " + eventName);
      } else {
        view.displayError("Failed to create recurring all-day event (conflict detected)");
      }
    } catch (DateTimeParseException e) {
      view.displayError("Invalid date/time format: " + dateStr);
    } catch (NumberFormatException e) {
      view.displayError("Invalid number of occurrences");
    }
  }

  /**
   * Process a create regular event command.
   */
  private void processCreateRegularCommand(String command, boolean autoDecline) {
    String[] parts = command.split(" from ");
    if (parts.length != 2) {
      view.displayError("Invalid event format");
      return;
    }

    String eventName = parts[0].substring("create event".length()).trim();
    String timeRangeStr = parts[1].trim();

    String[] timeRangeParts = timeRangeStr.split(" to ");
    if (timeRangeParts.length != 2) {
      view.displayError("Invalid time range format");
      return;
    }

    String startTimeStr = timeRangeParts[0].trim();
    String endTimeOrRest = timeRangeParts[1].trim();

    // Check if it's a recurring event
    boolean isRecurring = endTimeOrRest.contains(" repeats ");

    if (isRecurring) {
      String[] endTimeParts = endTimeOrRest.split(" repeats ", 2);
      String endTimeStr = endTimeParts[0].trim();
      String recurrenceStr = endTimeParts[1].trim();

      processCreateRecurringRegularCommand(eventName, startTimeStr, endTimeStr, recurrenceStr, autoDecline);
    } else {
      // Single regular event
      try {
        LocalDateTime startDateTime = parseDateTime(startTimeStr);
        LocalDateTime endDateTime = parseDateTime(endTimeOrRest);

        boolean success = calendar.createEvent(eventName, startDateTime, endDateTime, autoDecline);

        if (success) {
          view.displayMessage("Event created successfully: " + eventName);
        } else {
          view.displayError("Failed to create event (conflict detected)");
        }
      } catch (DateTimeParseException e) {
        view.displayError("Invalid date/time format");
      }
    }
  }

  /**
   * Process a create recurring regular event command.
   */
  private void processCreateRecurringRegularCommand(String eventName, String startTimeStr,
      String endTimeStr, String recurrenceStr,
      boolean autoDecline) {
    try {
      LocalDateTime startDateTime = parseDateTime(startTimeStr);
      LocalDateTime endDateTime = parseDateTime(endTimeStr);

      String weekdays;
      int occurrences = -1;
      LocalDateTime untilDate = null;

      if (recurrenceStr.contains(" for ")) {
        // Recurs for N times
        String[] recParts = recurrenceStr.split(" for ");
        weekdays = recParts[0].trim();
        occurrences = Integer.parseInt(recParts[1].replace("times", "").trim());
      } else if (recurrenceStr.contains(" until ")) {
        // Recurs until a date
        String[] recParts = recurrenceStr.split(" until ");
        weekdays = recParts[0].trim();
        untilDate = parseDateTime(recParts[1].trim());
      } else {
        view.displayError("Invalid recurrence pattern");
        return;
      }

      boolean success = calendar.createRecurringEvent(
          eventName, startDateTime, endDateTime, weekdays, occurrences, untilDate, autoDecline);

      if (success) {
        view.displayMessage("Recurring event created successfully: " + eventName);
      } else {
        view.displayError("Failed to create recurring event (conflict detected or spans multiple days)");
      }
    } catch (DateTimeParseException e) {
      view.displayError("Invalid date/time format");
    } catch (NumberFormatException e) {
      view.displayError("Invalid number of occurrences");
    }
  }

  /**
   * Process an edit event command.
   */
  private void processEditCommand(String command) {
    // Example: edit event name "Meeting" from 2023-04-01T10:00 to 2023-04-01T11:00 with "Team Meeting"
    // Example: edit events name "Meeting" from 2023-04-01T10:00 with "Team Meeting"
    // Example: edit events name "Meeting" "Team Meeting"

    String[] parts = command.split("\\s+", 4);
    if (parts.length < 4) {
      view.displayError("Invalid edit command format");
      return;
    }

    String editType = parts[1]; // "event" or "events"
    String property = parts[2]; // property to edit
    String rest = parts[3]; // rest of the command

    if ("event".equals(editType)) {
      processEditSingleEvent(property, rest);
    } else if ("events".equals(editType)) {
      if (rest.contains(" from ")) {
        processEditEventsFrom(property, rest);
      } else {
        processEditAllEvents(property, rest);
      }
    } else {
      view.displayError("Invalid edit type: " + editType);
    }
  }

  /**
   * Process an edit single event command.
   */
  private void processEditSingleEvent(String property, String rest) {
    // Example: edit event name "Meeting" from 2023-04-01T10:00 to 2023-04-01T11:00 with "Team Meeting"

    try {
      // Extract event name correctly
      int fromIdx = rest.indexOf(" from ");

      if (fromIdx == -1) {
        view.displayError("Invalid edit event format");
        return;
      }

      String eventName = rest.substring(0, fromIdx).trim();

      // Remove quotes if present
      if (eventName.startsWith("\"") && eventName.endsWith("\"")) {
        eventName = eventName.substring(1, eventName.length() - 1);
      }
      // Extract time range
      int toIdx = rest.indexOf(" to ", fromIdx);
      int withIdx = rest.indexOf(" with ", toIdx);

      if (toIdx == -1 || withIdx == -1) {
        view.displayError("Invalid edit event format");
        return;
      }

      String startTimeStr = rest.substring(fromIdx + 6, toIdx).trim();
      String endTimeStr = rest.substring(toIdx + 4, withIdx).trim();
      String newValue = rest.substring(withIdx + 6).trim();

      // Remove quotes if present
      if (eventName.startsWith("\"") && eventName.endsWith("\"")) {
        eventName = eventName.substring(1, eventName.length() - 1);
      }
      if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
        newValue = newValue.substring(1, newValue.length() - 1);
      }

      LocalDateTime startDateTime = parseDateTime(startTimeStr);
      LocalDateTime endDateTime = parseDateTime(endTimeStr);

      boolean success = calendar.editEvent(property, eventName, startDateTime, endDateTime, newValue);

      if (success) {
        view.displayMessage("Event updated successfully");
      } else {
        view.displayError("Failed to update event (not found or invalid property)");
      }
    } catch (Exception e) {
      view.displayError("Error processing edit event command: " + e.getMessage());
    }
  }

  /**
   * Process an edit events from command.
   */
  private void processEditEventsFrom(String property, String rest) {
    try {
      // Extract event name correctly
      int fromIdx = rest.indexOf(" from ");

      if (fromIdx == -1) {
        view.displayError("Invalid edit events format");
        return;
      }

      String eventName = rest.substring(0, fromIdx).trim();

      // Extract from time and new value
      int withIdx = rest.indexOf(" with ", fromIdx);

      if (withIdx == -1) {
        view.displayError("Invalid edit events format");
        return;
      }

      String startTimeStr = rest.substring(fromIdx + 6, withIdx).trim();
      String newValue = rest.substring(withIdx + 6).trim();

      // Remove quotes if present
      if (eventName.startsWith("\"") && eventName.endsWith("\"")) {
        eventName = eventName.substring(1, eventName.length() - 1);
      }
      if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
        newValue = newValue.substring(1, newValue.length() - 1);
      }

      LocalDateTime startDateTime = parseDateTime(startTimeStr);

      boolean success = calendar.editEventsFrom(property, eventName, startDateTime, newValue);

      if (success) {
        view.displayMessage("Events updated successfully");
      } else {
        view.displayError("Failed to update events (not found or invalid property)");
      }
    } catch (Exception e) {
      view.displayError("Error processing edit events command: " + e.getMessage());
    }
  }
  /**
   * Process an edit all events command.
   */
  private void processEditAllEvents(String property, String rest) {
    try {
      // More robust parsing using regular expressions
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]*)\"\\s+\"([^\"]*)\"");
      java.util.regex.Matcher matcher = pattern.matcher(rest);

      if (matcher.find()) {
        String eventName = matcher.group(1);
        String newValue = matcher.group(2);

        boolean success = calendar.editAllEvents(property, eventName, newValue);

        if (success) {
          view.displayMessage("All events updated successfully");
        } else {
          view.displayError("Failed to update events (not found or invalid property)");
        }
      } else {
        view.displayError("Invalid edit all events format");
      }
    } catch (Exception e) {
      view.displayError("Error processing edit all events command: " + e.getMessage());
    }
  }
  /**
   * Process a print events command.
   */
  private void processPrintCommand(String command) {
    // Handle: print events on <date>
    // Handle: print events from <startDate> to <endDate>

    if (command.contains(" on ")) {
      String[] parts = command.split(" on ");
      if (parts.length != 2) {
        view.displayError("Invalid print events on command");
        return;
      }

      String dateTimeStr = parts[1].trim();

      try {
        LocalDateTime dateTime = parseDateTime(dateTimeStr);
        List<Event> events = calendar.getEventsOn(dateTime);

        if (events.isEmpty()) {
          view.displayMessage("No events on " + dateTime.toLocalDate());
        } else {
          view.displayMessage("Events on " + dateTime.toLocalDate() + ":");
          for (Event event : events) {
            view.displayMessage(" • " + event.toString());
          }
        }
      } catch (DateTimeParseException e) {
        view.displayError("Invalid date format: " + dateTimeStr);
      }
    } else if (command.contains(" from ")) {
      String[] parts = command.split(" from ");
      if (parts.length != 2) {
        view.displayError("Invalid print events from command");
        return;
      }

      String rangeStr = parts[1].trim();
      String[] rangeParts = rangeStr.split(" to ");

      if (rangeParts.length != 2) {
        view.displayError("Invalid date range format");
        return;
      }

      String startDateStr = rangeParts[0].trim();
      String endDateStr = rangeParts[1].trim();

      try {
        LocalDateTime startDateTime = parseDateTime(startDateStr);
        LocalDateTime endDateTime = parseDateTime(endDateStr);

        List<Event> events = calendar.getEventsFrom(startDateTime, endDateTime);

        if (events.isEmpty()) {
          view.displayMessage("No events from " + startDateTime.toLocalDate() +
              " to " + endDateTime.toLocalDate());
        } else {
          view.displayMessage("Events from " + startDateTime.toLocalDate() +
              " to " + endDateTime.toLocalDate() + ":");
          for (Event event : events) {
            view.displayMessage(" • " + event.toString());
          }
        }
      } catch (DateTimeParseException e) {
        view.displayError("Invalid date format");
      }
    } else {
      view.displayError("Invalid print command format");
    }
  }

  /**
   * Process an export calendar command.
   */
  private void processExportCommand(String command) {
    // Handle: export cal filename.csv

    String[] parts = command.split("\\s+");
    if (parts.length != 3) {
      view.displayError("Invalid export command format");
      return;
    }

    String fileName = parts[2];

    String path = calendar.exportToCSV(fileName);

    if (path != null) {
      view.displayMessage("Calendar exported to: " + path);
    } else {
      view.displayError("Failed to export calendar");
    }
  }

  /**
   * Process a show status command.
   */
  private void processShowCommand(String command) {
    // Handle: show status on <dateTime>

    String[] parts = command.split(" on ");
    if (parts.length != 2) {
      view.displayError("Invalid show status command");
      return;
    }

    String dateTimeStr = parts[1].trim();

    try {
      LocalDateTime dateTime = parseDateTime(dateTimeStr);
      boolean isBusy = calendar.isBusy(dateTime);

      if (isBusy) {
        view.displayMessage("busy");
      } else {
        view.displayMessage("available");
      }
    } catch (DateTimeParseException e) {
      view.displayError("Invalid date/time format: " + dateTimeStr);
    }
  }

  /**
   * Parse a date-time string in the format "yyyy-MM-ddThh:mm".
   *
   * @param dateTimeStr the date-time string
   * @return the parsed LocalDateTime
   * @throws DateTimeParseException if the string cannot be parsed
   */
  private LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr.contains("T")) {
      // Format: yyyy-MM-ddThh:mm
      String[] parts = dateTimeStr.split("T");
      if (parts.length != 2) {
        throw new DateTimeParseException("Invalid date-time format", dateTimeStr, 0);
      }

      LocalDate date = LocalDate.parse(parts[0]);
      LocalTime time = LocalTime.parse(parts[1]);

      return LocalDateTime.of(date, time);
    } else {
      // Format: yyyy-MM-dd (without time)
      LocalDate date = LocalDate.parse(dateTimeStr);
      return LocalDateTime.of(date, LocalTime.MIDNIGHT);
    }
  }
}