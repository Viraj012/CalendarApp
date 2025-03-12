package controller;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for calendar commands. Extracts information from user input.
 */
public class CommandParser {
  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("--description\\s+\"([^\"]*)\"");
  private static final Pattern LOCATION_PATTERN = Pattern.compile("--location\\s+\"([^\"]*)\"");
  private static final Pattern EDIT_ALL_PATTERN = Pattern.compile("\"([^\"]*)\"\\s+\"([^\"]*)\"");

  /**
   * Parse a create event command.
   *
   * @param command the original command string
   * @return a CreateCommand object or null if parsing fails
   */
  public Command parseCreateCommand(String command) {
    boolean autoDecline = command.contains("--autoDecline");
    String cmdWithoutAutoDecline = command.replace("--autoDecline", "").trim();

    String description = extractQuotedParameter(cmdWithoutAutoDecline, DESCRIPTION_PATTERN);
    cmdWithoutAutoDecline = cmdWithoutAutoDecline.replace(
        description.isEmpty() ? "" : "--description \"" + description + "\"", "").trim();

    String location = extractQuotedParameter(cmdWithoutAutoDecline, LOCATION_PATTERN);
    cmdWithoutAutoDecline = cmdWithoutAutoDecline.replace(
        location.isEmpty() ? "" : "--location \"" + location + "\"", "").trim();

    boolean isPrivate = command.contains("--private");
    cmdWithoutAutoDecline = cmdWithoutAutoDecline.replace("--private", "").trim();

    try {
      if (cmdWithoutAutoDecline.contains(" on ")) {
        return parseAllDayCreateCommand(cmdWithoutAutoDecline, autoDecline, description,
            location, !isPrivate);
      } else if (cmdWithoutAutoDecline.contains(" from ")) {
        return parseRegularCreateCommand(cmdWithoutAutoDecline, autoDecline, description,
            location, !isPrivate);
      }
    } catch (Exception e) {
      // Return null if parsing fails
    }

    return null;
  }

  /**
   * Extract quoted parameters from command strings.
   *
   * @param command the command string
   * @param pattern the regex pattern to use
   * @return the extracted parameter or empty string if not found
   */
  private String extractQuotedParameter(String command, Pattern pattern) {
    Matcher matcher = pattern.matcher(command);
    return matcher.find() ? matcher.group(1) : "";
  }

  /**
   * Parse a create all-day event command.
   */
  private CreateCommand parseAllDayCreateCommand(String command, boolean autoDecline,
      String description, String location, boolean isPublic) {
    String[] parts = command.split(" on ");
    if (parts.length != 2) {
      return null;
    }

    String eventName = parts[0].substring("create event".length()).trim();
    String dateTimeStr = parts[1].trim();

    if (eventName.isEmpty()) {
      return null;
    }

    CreateCommand createCmd = new CreateCommand();
    createCmd.setEventName(eventName);
    createCmd.setAutoDecline(autoDecline);
    createCmd.setDescription(description);
    createCmd.setLocation(location);
    createCmd.setPublic(isPublic);
    createCmd.setAllDay(true);

    if (dateTimeStr.contains(" repeats ")) {
      return parseRecurringAllDayCreateCommand(createCmd, dateTimeStr);
    } else {
      try {
        LocalDateTime dateTime = DateTimeUtil.parseDateTime(dateTimeStr);
        createCmd.setStartDateTime(dateTime);
        return createCmd;
      } catch (Exception e) {
        return null;
      }
    }
  }

  /**
   * Parse a recurring all-day event command.
   */
  private CreateCommand parseRecurringAllDayCreateCommand(CreateCommand createCmd, String dateTimeStr) {
    String[] parts = dateTimeStr.split(" repeats ");
    if (parts.length != 2) {
      return null;
    }

    String dateStr = parts[0].trim();
    String recurrenceStr = parts[1].trim();

    try {
      LocalDateTime dateTime = DateTimeUtil.parseDateTime(dateStr);
      createCmd.setStartDateTime(dateTime);
      createCmd.setRecurring(true);

      return parseRecurrencePattern(createCmd, recurrenceStr);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse a recurrence pattern.
   */
  private CreateCommand parseRecurrencePattern(CreateCommand createCmd, String recurrenceStr) {
    if (recurrenceStr.contains(" for ")) {
      String[] recParts = recurrenceStr.split(" for ");
      createCmd.setWeekdays(recParts[0].trim());
      createCmd.setOccurrences(Integer.parseInt(recParts[1].replace("times", "").trim()));
      return createCmd;
    } else if (recurrenceStr.contains(" until ")) {
      String[] recParts = recurrenceStr.split(" until ");
      createCmd.setWeekdays(recParts[0].trim());
      createCmd.setUntilDate(DateTimeUtil.parseDateTime(recParts[1].trim()));
      return createCmd;
    }
    return null;
  }

  /**
   * Parse a create regular event command.
   */
  private CreateCommand parseRegularCreateCommand(String command, boolean autoDecline,
      String description, String location, boolean isPublic) {
    String[] parts = command.split(" from ");
    if (parts.length != 2) {
      return null;
    }

    String eventName = parts[0].substring("create event".length()).trim();
    String timeRangeStr = parts[1].trim();

    if (eventName.isEmpty()) {
      return null;
    }

    String[] timeRangeParts = timeRangeStr.split(" to ");
    if (timeRangeParts.length != 2) {
      return null;
    }

    String startTimeStr = timeRangeParts[0].trim();
    String endTimeOrRest = timeRangeParts[1].trim();

    CreateCommand createCmd = new CreateCommand();
    createCmd.setEventName(eventName);
    createCmd.setAutoDecline(autoDecline);
    createCmd.setDescription(description);
    createCmd.setLocation(location);
    createCmd.setPublic(isPublic);
    createCmd.setAllDay(false);

    boolean isRecurring = endTimeOrRest.contains(" repeats ");

    if (isRecurring) {
      return parseRecurringRegularCreateCommand(createCmd, startTimeStr, endTimeOrRest);
    } else {
      try {
        LocalDateTime startDateTime = DateTimeUtil.parseDateTime(startTimeStr);
        LocalDateTime endDateTime = DateTimeUtil.parseDateTime(endTimeOrRest);

        if (endDateTime.isBefore(startDateTime)) {
          return null;
        }

        createCmd.setStartDateTime(startDateTime);
        createCmd.setEndDateTime(endDateTime);
        return createCmd;
      } catch (Exception e) {
        return null;
      }
    }
  }

  /**
   * Parse a recurring regular event command.
   */
  private CreateCommand parseRecurringRegularCreateCommand(CreateCommand createCmd,
      String startTimeStr, String endTimeOrRest) {
    String[] endTimeParts = endTimeOrRest.split(" repeats ", 2);
    if (endTimeParts.length != 2) {
      return null;
    }

    String endTimeStr = endTimeParts[0].trim();
    String recurrenceStr = endTimeParts[1].trim();

    try {
      LocalDateTime startDateTime = DateTimeUtil.parseDateTime(startTimeStr);
      LocalDateTime endDateTime = DateTimeUtil.parseDateTime(endTimeStr);

      if (endDateTime.isBefore(startDateTime)) {
        return null;
      }

      createCmd.setStartDateTime(startDateTime);
      createCmd.setEndDateTime(endDateTime);
      createCmd.setRecurring(true);

      return parseRecurrencePattern(createCmd, recurrenceStr);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse an edit command.
   *
   * @param command the original command string
   * @return an EditCommand object or null if parsing fails
   */
  public Command parseEditCommand(String command) {
    String[] parts = command.split("\\s+", 4);
    if (parts.length < 4) {
      return null;
    }

    String editType = parts[1];
    String property = parts[2];
    String rest = parts[3];

    if ("event".equals(editType)) {
      return parseEditSingleEventCommand(property, rest);
    } else if ("events".equals(editType)) {
      if (rest.contains(" from ")) {
        return parseEditEventsFromCommand(property, rest);
      } else {
        return parseEditAllEventsCommand(property, rest);
      }
    }

    return null;
  }

  /**
   * Parse an edit single event command.
   */
  private EditCommand parseEditSingleEventCommand(String property, String rest) {
    try {
      int fromIdx = rest.indexOf(" from ");
      if (fromIdx == -1) {
        return null;
      }

      String eventName = rest.substring(0, fromIdx).trim();
      int toIdx = rest.indexOf(" to ", fromIdx);
      int withIdx = rest.indexOf(" with ", toIdx);

      if (toIdx == -1 || withIdx == -1) {
        return null;
      }

      String startTimeStr = rest.substring(fromIdx + 6, toIdx).trim();
      String endTimeStr = rest.substring(toIdx + 4, withIdx).trim();
      String newValue = rest.substring(withIdx + 6).trim();

      // Remove quotes from newValue if present
      if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
        newValue = newValue.substring(1, newValue.length() - 1);
      }

      LocalDateTime startDateTime = DateTimeUtil.parseDateTime(startTimeStr);
      LocalDateTime endDateTime = DateTimeUtil.parseDateTime(endTimeStr);

      EditCommand editCmd = new EditCommand(EditCommand.EditType.SINGLE);
      editCmd.setProperty(property);
      editCmd.setEventName(eventName);
      editCmd.setStartDateTime(startDateTime);
      editCmd.setEndDateTime(endDateTime);
      editCmd.setNewValue(newValue);

      return editCmd;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse an edit events from command.
   */
  private EditCommand parseEditEventsFromCommand(String property, String rest) {
    try {
      int fromIdx = rest.indexOf(" from ");
      if (fromIdx == -1) {
        return null;
      }

      String eventName = rest.substring(0, fromIdx).trim();
      int withIdx = rest.indexOf(" with ", fromIdx);

      if (withIdx == -1) {
        return null;
      }

      String startTimeStr = rest.substring(fromIdx + 6, withIdx).trim();
      String newValue = rest.substring(withIdx + 6).trim();

      if (eventName.startsWith("\"") && eventName.endsWith("\"")) {
        eventName = eventName.substring(1, eventName.length() - 1);
      }
      if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
        newValue = newValue.substring(1, newValue.length() - 1);
      }

      LocalDateTime startDateTime = DateTimeUtil.parseDateTime(startTimeStr);

      EditCommand editCmd = new EditCommand(EditCommand.EditType.FROM_DATE);
      editCmd.setProperty(property);
      editCmd.setEventName(eventName);
      editCmd.setStartDateTime(startDateTime);
      editCmd.setNewValue(newValue);

      return editCmd;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse an edit all events command.
   */
  private EditCommand parseEditAllEventsCommand(String property, String rest) {
    try {
      // Updated pattern to recognize "with" between quoted strings
      Pattern withPattern = Pattern.compile("\"([^\"]*)\"\\s+with\\s+\"([^\"]*)\"");
      Matcher withMatcher = withPattern.matcher(rest);

      if (withMatcher.find()) {
        String eventName = withMatcher.group(1);
        String newValue = withMatcher.group(2);

        EditCommand editCmd = new EditCommand(EditCommand.EditType.ALL);
        editCmd.setProperty(property);
        editCmd.setEventName(eventName);
        editCmd.setNewValue(newValue);

        return editCmd;
      } else {
        // Fallback to the original pattern for backward compatibility
        Matcher matcher = EDIT_ALL_PATTERN.matcher(rest);
        if (matcher.find()) {
          String eventName = matcher.group(1);
          String newValue = matcher.group(2);

          EditCommand editCmd = new EditCommand(EditCommand.EditType.ALL);
          editCmd.setProperty(property);
          editCmd.setEventName(eventName);
          editCmd.setNewValue(newValue);

          return editCmd;
        }
      }
    } catch (Exception e) {
      // Handle exceptions
    }

    return null;
  }

  /**
   * Parse a print command.
   *
   * @param command the original command string
   * @return a PrintCommand object or null if parsing fails
   */
  public Command parsePrintCommand(String command) {
    try {
      PrintCommand printCmd = new PrintCommand();

      if (command.contains(" on ")) {
        String[] parts = command.split(" on ");
        if (parts.length != 2) {
          return null;
        }

        String dateTimeStr = parts[1].trim();
        LocalDateTime dateTime = DateTimeUtil.parseDateTime(dateTimeStr);

        printCmd.setStartDateTime(dateTime);
        printCmd.setDateRange(false);

        return printCmd;
      } else if (command.contains(" from ")) {
        String[] parts = command.split(" from ");
        if (parts.length != 2) {
          return null;
        }

        String rangeStr = parts[1].trim();
        String[] rangeParts = rangeStr.split(" to ");

        if (rangeParts.length != 2) {
          return null;
        }

        String startDateStr = rangeParts[0].trim();
        String endDateStr = rangeParts[1].trim();

        LocalDateTime startDateTime = DateTimeUtil.parseDateTime(startDateStr);
        LocalDateTime endDateTime = DateTimeUtil.parseDateTime(endDateStr);

        printCmd.setStartDateTime(startDateTime);
        printCmd.setEndDateTime(endDateTime);
        printCmd.setDateRange(true);

        return printCmd;
      }
    } catch (Exception e) {
      // Return null if parsing fails
    }

    return null;
  }

  /**
   * Parse an export command.
   *
   * @param command the original command string
   * @return an ExportCommand object or null if parsing fails
   */
  public Command parseExportCommand(String command) {
    String[] parts = command.split("\\s+");
    if (parts.length != 3) {
      return null;
    }

    String fileName = parts[2];
    ExportCommand exportCmd = new ExportCommand();
    exportCmd.setFileName(fileName);

    return exportCmd;
  }

  /**
   * Parse a show status command.
   *
   * @param command the original command string
   * @return a ShowCommand object or null if parsing fails
   */
  public Command parseShowCommand(String command) {
    String[] parts = command.split(" on ");
    if (parts.length != 2) {
      return null;
    }

    String dateTimeStr = parts[1].trim();

    try {
      LocalDateTime dateTime = DateTimeUtil.parseDateTime(dateTimeStr);

      ShowCommand showCmd = new ShowCommand();
      showCmd.setDateTime(dateTime);

      return showCmd;
    } catch (Exception e) {
      return null;
    }
  }
}