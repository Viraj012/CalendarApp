package controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for calendar commands. Extracts information from user input.
 */
class CommandParser {
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
    boolean autoDecline = command.contains("--autoDecline"); // Keep autoDecline flag handling
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
  private Command.CreateCommand parseAllDayCreateCommand(String command, boolean autoDecline,
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

    Command.CreateCommand createCmd = new Command.CreateCommand();
    createCmd.setEventName(eventName);
    createCmd.setDescription(description);
    createCmd.setLocation(location);
    createCmd.setPublic(isPublic);
    createCmd.setAllDay(true);
    createCmd.setAutoDecline(autoDecline);

    if (dateTimeStr.contains(" repeats ")) {
      return parseRecurringAllDayCreateCommand(createCmd, dateTimeStr);
    } else {
      try {
        LocalDateTime dateTime = CommandProcessor.parseDateTime(dateTimeStr);
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
  private Command.CreateCommand parseRecurringAllDayCreateCommand(Command.CreateCommand createCmd, String dateTimeStr) {
    String[] parts = dateTimeStr.split(" repeats ");
    if (parts.length != 2) {
      return null;
    }

    String dateStr = parts[0].trim();
    String recurrenceStr = parts[1].trim();

    try {
      LocalDateTime dateTime = CommandProcessor.parseDateTime(dateStr);
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
  private Command.CreateCommand parseRecurrencePattern(Command.CreateCommand createCmd, String recurrenceStr) {
    if (recurrenceStr.contains(" for ")) {
      String[] recParts = recurrenceStr.split(" for ");
      createCmd.setWeekdays(recParts[0].trim());
      createCmd.setOccurrences(Integer.parseInt(recParts[1].replace("times", "").trim()));
      return createCmd;
    } else if (recurrenceStr.contains(" until ")) {
      String[] recParts = recurrenceStr.split(" until ");
      createCmd.setWeekdays(recParts[0].trim());
      createCmd.setUntilDate(CommandProcessor.parseDateTime(recParts[1].trim()));
      return createCmd;
    }
    return null;
  }

  /**
   * Parse a create regular event command.
   */
  private Command.CreateCommand parseRegularCreateCommand(String command, boolean autoDecline,
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

    Command.CreateCommand createCmd = new Command.CreateCommand();
    createCmd.setEventName(eventName);
    createCmd.setDescription(description);
    createCmd.setLocation(location);
    createCmd.setPublic(isPublic);
    createCmd.setAllDay(false);
    createCmd.setAutoDecline(autoDecline);

    boolean isRecurring = endTimeOrRest.contains(" repeats ");

    if (isRecurring) {
      return parseRecurringRegularCreateCommand(createCmd, startTimeStr, endTimeOrRest);
    } else {
      try {
        LocalDateTime startDateTime = CommandProcessor.parseDateTime(startTimeStr);
        LocalDateTime endDateTime = CommandProcessor.parseDateTime(endTimeOrRest);

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
  private Command.CreateCommand parseRecurringRegularCreateCommand(Command.CreateCommand createCmd,
      String startTimeStr, String endTimeOrRest) {
    String[] endTimeParts = endTimeOrRest.split(" repeats ", 2);
    if (endTimeParts.length != 2) {
      return null;
    }

    String endTimeStr = endTimeParts[0].trim();
    String recurrenceStr = endTimeParts[1].trim();

    try {
      LocalDateTime startDateTime = CommandProcessor.parseDateTime(startTimeStr);
      LocalDateTime endDateTime = CommandProcessor.parseDateTime(endTimeStr);

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
  private Command.EditCommand parseEditSingleEventCommand(String property, String rest) {
    try {
      // Handle date/time property edits
      if (property.equalsIgnoreCase("starttime") ||
          property.equalsIgnoreCase("startdate") ||
          property.equalsIgnoreCase("endtime") ||
          property.equalsIgnoreCase("enddate")) {

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

        // Remove quotes from values if present
        if (eventName.startsWith("\"") && eventName.endsWith("\"")) {
          eventName = eventName.substring(1, eventName.length() - 1);
        }
        if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
          newValue = newValue.substring(1, newValue.length() - 1);
        }

        LocalDateTime startDateTime = CommandProcessor.parseDateTime(startTimeStr);
        LocalDateTime endDateTime = CommandProcessor.parseDateTime(endTimeStr);

        Command.EditCommand editCmd = new Command.EditCommand(Command.EditCommand.EditType.SINGLE);
        editCmd.setProperty(property);
        editCmd.setEventName(eventName);
        editCmd.setStartDateTime(startDateTime);
        editCmd.setEndDateTime(endDateTime);
        editCmd.setNewValue(newValue);

        return editCmd;
      } else {
        // Existing logic for other properties
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

        LocalDateTime startDateTime = CommandProcessor.parseDateTime(startTimeStr);
        LocalDateTime endDateTime = CommandProcessor.parseDateTime(endTimeStr);

        Command.EditCommand editCmd = new Command.EditCommand(Command.EditCommand.EditType.SINGLE);
        editCmd.setProperty(property);
        editCmd.setEventName(eventName);
        editCmd.setStartDateTime(startDateTime);
        editCmd.setEndDateTime(endDateTime);
        editCmd.setNewValue(newValue);

        return editCmd;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse an edit events from command.
   */
  private Command.EditCommand parseEditEventsFromCommand(String property, String rest) {
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

      LocalDateTime startDateTime = CommandProcessor.parseDateTime(startTimeStr);

      Command.EditCommand editCmd = new Command.EditCommand(Command.EditCommand.EditType.FROM_DATE);
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
  private Command.EditCommand parseEditAllEventsCommand(String property, String rest) {
    try {
      // Updated pattern to recognize "with" between quoted strings
      Pattern withPattern = Pattern.compile("\"([^\"]*)\"\\s+with\\s+\"([^\"]*)\"");
      Matcher withMatcher = withPattern.matcher(rest);

      if (withMatcher.find()) {
        String eventName = withMatcher.group(1);
        String newValue = withMatcher.group(2);

        Command.EditCommand editCmd = new Command.EditCommand(Command.EditCommand.EditType.ALL);
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

          Command.EditCommand editCmd = new Command.EditCommand(Command.EditCommand.EditType.ALL);
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
      Command.PrintCommand printCmd = new Command.PrintCommand();

      if (command.contains(" on ")) {
        String[] parts = command.split(" on ");
        if (parts.length != 2) {
          return null;
        }

        String dateTimeStr = parts[1].trim();
        LocalDateTime dateTime = CommandProcessor.parseDateTime(dateTimeStr);

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

        LocalDateTime startDateTime = CommandProcessor.parseDateTime(startDateStr);
        LocalDateTime endDateTime = CommandProcessor.parseDateTime(endDateStr);

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
    Command.ExportCommand exportCmd = new Command.ExportCommand();
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
      LocalDateTime dateTime = CommandProcessor.parseDateTime(dateTimeStr);

      Command.ShowCommand showCmd = new Command.ShowCommand();
      showCmd.setDateTime(dateTime);

      return showCmd;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse a create calendar command.
   *
   * @param command the original command string
   * @return a CreateCalendarCommand object or null if parsing fails
   */
  public Command parseCreateCalendarCommand(String command) {
    try {
      String[] parts = command.split("\\s+");
      String name = null;
      String timezoneStr = null;

      for (int i = 2; i < parts.length; i++) {
        if (parts[i].equals("--name") && i + 1 < parts.length) {
          name = parts[i + 1];
          i++;
        } else if (parts[i].equals("--timezone") && i + 1 < parts.length) {
          timezoneStr = parts[i + 1];
          i++;
        }
      }

      if (name == null || timezoneStr == null) {
        return null;
      }

      ZoneId timezone;
      try {
        timezone = ZoneId.of(timezoneStr);
      } catch (Exception e) {
        return null; // Invalid timezone
      }

      Command.CreateCalendarCommand cmd = new Command.CreateCalendarCommand();
      cmd.setName(name);
      cmd.setTimezone(timezone);
      return cmd;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse an edit calendar command.
   *
   * @param command the original command string
   * @return an EditCalendarCommand object or null if parsing fails
   */
  public Command parseEditCalendarCommand(String command) {
    try {
      String[] parts = command.split("\\s+");
      String name = null;
      String property = null;
      String newValue = null;

      for (int i = 2; i < parts.length; i++) {
        if (parts[i].equals("--name") && i + 1 < parts.length) {
          name = parts[i + 1];
          i++;
        } else if (parts[i].equals("--property") && i + 2 < parts.length) {
          property = parts[i + 1];
          newValue = parts[i + 2];
          i += 2;
        }
      }

      if (name == null || property == null || newValue == null) {
        return null;
      }

      Command.EditCalendarCommand cmd = new Command.EditCalendarCommand();
      cmd.setName(name);
      cmd.setProperty(property);
      cmd.setNewValue(newValue);
      return cmd;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse a use calendar command.
   *
   * @param command the original command string
   * @return a UseCalendarCommand object or null if parsing fails
   */
  public Command parseUseCalendarCommand(String command) {
    try {
      String[] parts = command.split("\\s+");
      String name = null;

      for (int i = 2; i < parts.length; i++) {
        if (parts[i].equals("--name") && i + 1 < parts.length) {
          name = parts[i + 1];
          break;
        }
      }

      if (name == null) {
        return null;
      }

      Command.UseCalendarCommand cmd = new Command.UseCalendarCommand();
      cmd.setName(name);
      return cmd;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse a copy event command.
   *
   * @param command the original command string
   * @return a CopyEventCommand object or null if parsing fails
   */
  public Command parseCopyEventCommand(String command) {
    try {
      // Example: copy event "Meeting" on 2023-05-15T14:00 --target WorkCal to 2023-06-15T14:00
      String eventNamePattern = "copy event ([^\\s]+|\"[^\"]+\")";
      Pattern pattern = Pattern.compile(eventNamePattern + " on ([^\\s]+) --target ([^\\s]+) to ([^\\s]+)");
      Matcher matcher = pattern.matcher(command);

      if (matcher.find()) {
        String eventName = matcher.group(1);
        String startTimeStr = matcher.group(2);
        String targetCalendar = matcher.group(3);
        String targetTimeStr = matcher.group(4);

        // Remove quotes if present
        if (eventName.startsWith("\"") && eventName.endsWith("\"")) {
          eventName = eventName.substring(1, eventName.length() - 1);
        }

        LocalDateTime startDateTime = CommandProcessor.parseDateTime(startTimeStr);
        LocalDateTime targetDateTime = CommandProcessor.parseDateTime(targetTimeStr);

        Command.CopyEventCommand cmd = new Command.CopyEventCommand();
        cmd.setEventName(eventName);
        cmd.setStartDateTime(startDateTime);
        cmd.setTargetCalendar(targetCalendar);
        cmd.setTargetDateTime(targetDateTime);

        return cmd;
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Parse a copy events command.
   *
   * @param command the original command string
   * @return a CopyEventsCommand object or null if parsing fails
   */
  public Command parseCopyEventsCommand(String command) {
    try {
      if (command.contains("copy events on")) {
        // Example: copy events on 2023-05-15 --target WorkCal to 2023-06-15
        Pattern pattern = Pattern.compile("copy events on ([^\\s]+) --target ([^\\s]+) to ([^\\s]+)");
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
          String dateStr = matcher.group(1);
          String targetCalendar = matcher.group(2);
          String targetDateStr = matcher.group(3);

          LocalDateTime date = CommandProcessor.parseDateTime(dateStr);
          LocalDateTime targetDate = CommandProcessor.parseDateTime(targetDateStr);

          Command.CopyEventsCommand cmd = new Command.CopyEventsCommand(Command.CopyEventsCommand.CopyType.DAY);
          cmd.setStartDate(date);
          cmd.setTargetCalendar(targetCalendar);
          cmd.setTargetDate(targetDate);

          return cmd;
        }
      } else if (command.contains("copy events between")) {
        // Example: copy events between 2023-05-15 and 2023-05-20 --target WorkCal to 2023-06-15
        Pattern pattern = Pattern.compile("copy events between ([^\\s]+) and ([^\\s]+) --target ([^\\s]+) to ([^\\s]+)");
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
          String startDateStr = matcher.group(1);
          String endDateStr = matcher.group(2);
          String targetCalendar = matcher.group(3);
          String targetDateStr = matcher.group(4);

          LocalDateTime startDate = CommandProcessor.parseDateTime(startDateStr);
          LocalDateTime endDate = CommandProcessor.parseDateTime(endDateStr);
          LocalDateTime targetDate = CommandProcessor.parseDateTime(targetDateStr);

          Command.CopyEventsCommand cmd = new Command.CopyEventsCommand(Command.CopyEventsCommand.CopyType.DATE_RANGE);
          cmd.setStartDate(startDate);
          cmd.setEndDate(endDate);
          cmd.setTargetCalendar(targetCalendar);
          cmd.setTargetDate(targetDate);

          return cmd;
        }
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }
}