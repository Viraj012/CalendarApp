import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controller.CommandHandler;
import controller.CreateCommand;
import controller.EditCommand;
import controller.ExportCommand;
import controller.PrintCommand;
import controller.ShowCommand;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.Calendar;
import model.Event;
import view.TextUI;

public class CommandHandlerTest {

  private CommandHandler commandHandler;
  private TestCalendar calendar;
  private TestTextUI view;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;

  @Before
  public void setUp() {
    calendar = new TestCalendar();
    view = new TestTextUI();
    commandHandler = new CommandHandler(calendar, view);
    startDateTime = LocalDateTime.of(2023, 1, 1, 9, 0);
    endDateTime = LocalDateTime.of(2023, 1, 1, 10, 0);
  }

  @Test
  public void testHandleCreateCommandRegularEventSuccess() {
    calendar.setCreateEventResult(true);

    CreateCommand cmd = new CreateCommandStub(
        "Test Event", startDateTime, endDateTime, false, false, false, null, -1, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("Event created successfully"));
    assert (view.getLastMessage().contains("Test Event"));
  }

  @Test
  public void testHandleCreateCommandRegularEventFailure() {
    calendar.setCreateEventResult(false);

    CreateCommand cmd = new CreateCommandStub(
        "Test Event", startDateTime, endDateTime, false, false, false, null, -1, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to create Event"));
  }

  @Test
  public void testHandleCreateCommandAllDayEventSuccess() {
    calendar.setCreateAllDayEventResult(true);

    CreateCommand cmd = new CreateCommandStub(
        "Test All-Day Event", startDateTime, null, false, true, false, null, -1, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("All-day event created successfully"));
  }

  @Test
  public void testHandleCreateCommandAllDayEventFailure() {
    calendar.setCreateAllDayEventResult(false);

    CreateCommand cmd = new CreateCommandStub(
        "Test All-Day Event", startDateTime, null, false, true, false, null, -1, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to create All-day event"));
  }

  @Test
  public void testHandleCreateCommandRecurringEventSuccess() {
    calendar.setCreateRecurringEventResult(true);

    CreateCommand cmd = new CreateCommandStub(
        "Test Recurring Event", startDateTime, endDateTime, false, false, true, "MWF", 10, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("Recurring event created successfully"));
  }

  @Test
  public void testHandleCreateCommandRecurringEventFailure() {
    calendar.setCreateRecurringEventResult(false);

    CreateCommand cmd = new CreateCommandStub(
        "Test Recurring Event", startDateTime, endDateTime, false, false, true, "MWF", 10, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to create recurring event"));
    assert (view.getLastError().contains("conflict detected or spans multiple days"));
  }

  @Test
  public void testHandleCreateCommandRecurringAllDayEventSuccess() {
    calendar.setCreateRecurringAllDayEventResult(true);

    CreateCommand cmd = new CreateCommandStub(
        "Test Recurring All-Day Event", startDateTime, null, false, true, true, "TR", 5, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("Recurring all-day event created successfully"));
  }

  @Test
  public void testHandleCreateCommandRecurringAllDayEventFailure() {
    calendar.setCreateRecurringAllDayEventResult(false);

    CreateCommand cmd = new CreateCommandStub(
        "Test Recurring All-Day Event", startDateTime, null, false, true, true, "TR", 5, null
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to create recurring all-day event"));
  }

  @Test
  public void testHandleEditCommandSingleEventSuccess() {
    calendar.setEditEventResult(true);

    EditCommand cmd = new EditCommandStub(
        EditCommand.EditType.SINGLE, "location", "Test Event",
        startDateTime, endDateTime, "New Location"
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().equals("Event updated successfully"));
  }

  @Test
  public void testHandleEditCommandSingleEventFailure() {
    calendar.setEditEventResult(false);

    EditCommand cmd = new EditCommandStub(
        EditCommand.EditType.SINGLE, "invalid", "Test Event",
        startDateTime, endDateTime, "New Value"
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to update event"));
  }

  @Test
  public void testHandleEditCommandFromDateSuccess() {
    calendar.setEditEventsFromResult(true);

    EditCommand cmd = new EditCommandStub(
        EditCommand.EditType.FROM_DATE, "description", "Test Event",
        startDateTime, null, "New Description"
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().equals("Events updated successfully"));
  }

  @Test
  public void testHandleEditCommandFromDateFailure() {
    calendar.setEditEventsFromResult(false);

    EditCommand cmd = new EditCommandStub(
        EditCommand.EditType.FROM_DATE, "invalid", "Test Event",
        startDateTime, null, "New Value"
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to update events"));
  }

  @Test
  public void testHandleEditCommandAllEventsSuccess() {
    calendar.setEditAllEventsResult(true);

    EditCommand cmd = new EditCommandStub(
        EditCommand.EditType.ALL, "name", "Test Event",
        null, null, "New Name"
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().equals("All events updated successfully"));
  }

  @Test
  public void testHandleEditCommandAllEventsFailure() {
    calendar.setEditAllEventsResult(false);

    EditCommand cmd = new EditCommandStub(
        EditCommand.EditType.ALL, "invalid", "Test Event",
        null, null, "New Value"
    );

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to update events"));
  }

  @Test
  public void testHandlePrintCommandSingleDateWithEvents() {
    List<Event> events = new ArrayList<>();
    events.add(new TestEvent("Test Event", startDateTime, endDateTime, false));

    calendar.setEventsOnResult(events);

    PrintCommand cmd = new PrintCommandStub(startDateTime, null, false);

    commandHandler.handleCommand(cmd);

    assert (view.getMessageHistory().get(0).contains("Events on 2023-01-01"));
  }

  @Test
  public void testHandlePrintCommandSingleDateNoEvents() {
    List<Event> events = new ArrayList<>();
    calendar.setEventsOnResult(events);

    PrintCommand cmd = new PrintCommandStub(startDateTime, null, false);

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("No events on 2023-01-01"));
  }

  @Test
  public void testHandlePrintCommandDateRangeWithEvents() {
    List<Event> events = new ArrayList<>();
    events.add(new TestEvent("Test Event", startDateTime, endDateTime, false));

    calendar.setEventsFromResult(events);

    PrintCommand cmd = new PrintCommandStub(startDateTime, endDateTime, true);

    commandHandler.handleCommand(cmd);

    assert (view.getMessageHistory().get(0).contains("Events from 2023-01-01 to 2023-01-01"));
  }

  @Test
  public void testHandlePrintCommandDateRangeNoEvents() {
    List<Event> events = new ArrayList<>();
    calendar.setEventsFromResult(events);

    PrintCommand cmd = new PrintCommandStub(startDateTime, endDateTime, true);

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("No events from 2023-01-01 to 2023-01-01"));
  }

  @Test
  public void testHandlePrintCommandEventSorting() {
    List<Event> events = new ArrayList<>();

    events.add(new TestEvent("Event 1", LocalDateTime.of(2023, 1, 1, 10, 0),
        LocalDateTime.of(2023, 1, 1, 11, 0), false));
    events.add(new TestEvent("Event 2", LocalDateTime.of(2023, 1, 1, 9, 0),
        LocalDateTime.of(2023, 1, 1, 10, 0), false));
    events.add(new TestEvent("All-day Event", LocalDateTime.of(2023, 1, 1, 0, 0), null, true));
    events.add(new TestEvent("Next Day Event", LocalDateTime.of(2023, 1, 2, 9, 0),
        LocalDateTime.of(2023, 1, 2, 10, 0), false));

    calendar.setEventsFromResult(events);

    PrintCommand cmd = new PrintCommandStub(startDateTime, endDateTime, true);

    commandHandler.handleCommand(cmd);

    List<String> messages = view.getMessageHistory();
    assert (messages.size() >= 5);
    assert (messages.get(0).contains("Events from 2023-01-01 to 2023-01-01"));

    assert (messages.get(1).contains("All-day Event"));
    assert (messages.get(2).contains("Event 2"));
    assert (messages.get(3).contains("Event 1"));
    assert (messages.get(4).contains("Next Day Event"));
  }

  @Test
  public void testHandleExportCommandSuccess() {
    calendar.setExportToCSVResult("/path/to/calendar.csv");

    ExportCommand cmd = new ExportCommandStub("calendar.csv");

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().contains("Calendar exported to:"));
  }

  @Test
  public void testHandleExportCommandFailure() {
    calendar.setExportToCSVResult(null);

    ExportCommand cmd = new ExportCommandStub("calendar.csv");

    commandHandler.handleCommand(cmd);

    assert (view.getLastError().contains("Failed to export calendar"));
  }

  @Test
  public void testHandleShowCommandBusy() {
    calendar.setIsBusyResult(true);

    ShowCommand cmd = new ShowCommandStub(startDateTime);

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().equals("busy"));
  }

  @Test
  public void testHandleShowCommandAvailable() {
    calendar.setIsBusyResult(false);

    ShowCommand cmd = new ShowCommandStub(startDateTime);

    commandHandler.handleCommand(cmd);

    assert (view.getLastMessage().equals("available"));
  }

  @Test
  public void testEventNumberingIncrements() {
    List<Event> events = new ArrayList<>();
    // Add multiple events to test the numbering increment
    events.add(new TestEvent("Event 1", startDateTime, endDateTime, false));
    events.add(new TestEvent("Event 2", startDateTime.plusHours(1), endDateTime.plusHours(1), false));
    events.add(new TestEvent("Event 3", startDateTime.plusHours(2), endDateTime.plusHours(2), false));

    calendar.setEventsFromResult(events);

    PrintCommand cmd = new PrintCommandStub(startDateTime, endDateTime.plusHours(2), true);
    commandHandler.handleCommand(cmd);

    List<String> messages = view.getMessageHistory();

    // Check that numbers are in ascending order (1, 2, 3) - would fail if eventNumber--
    assertTrue(messages.get(1).startsWith("1."));
    assertTrue(messages.get(2).startsWith("2."));
    assertTrue(messages.get(3).startsWith("3."));
  }

  @Test
  public void testDateComparisonInSorting() {
    List<Event> events = new ArrayList<>();

    // Create events on different dates, intentionally added in reverse order
    TestEvent day2Event = new TestEvent("Day 2 Event",
        LocalDateTime.of(2023, 1, 2, 9, 0),
        LocalDateTime.of(2023, 1, 2, 10, 0), false);

    TestEvent day1Event = new TestEvent("Day 1 Event",
        LocalDateTime.of(2023, 1, 1, 9, 0),
        LocalDateTime.of(2023, 1, 1, 10, 0), false);

    events.add(day2Event); // Add day 2 event first
    events.add(day1Event); // Add day 1 event second

    // Now sort using the same algorithm as in CommandHandler
    events.sort((e1, e2) -> {
      // This is the line where PIT mutates return dateCompare to return 0
      int dateCompare = e1.getStartDateTime().toLocalDate().compareTo(e2.getStartDateTime().toLocalDate());
      if (dateCompare != 0) {
        return dateCompare;
      }

      boolean e1AllDay = e1.isAllDay();
      boolean e2AllDay = e2.isAllDay();

      if (e1AllDay != e2AllDay) {
        return e1AllDay ? -1 : 1;
      }

      return e1.getStartDateTime().compareTo(e2.getStartDateTime());
    });

    // If date comparison works, day1Event should be first
    assertEquals("Day 1 Event", events.get(0).getSubject());
    assertEquals("Day 2 Event", events.get(1).getSubject());

    // Check with actual CommandHandler functionality
    calendar.setEventsFromResult(new ArrayList<>()); // Clear first
    events.clear(); // Reset events list
    events.add(day2Event);
    events.add(day1Event);
    calendar.setEventsFromResult(events);

    PrintCommand cmd = new PrintCommandStub(
        LocalDateTime.of(2023, 1, 1, 0, 0),
        LocalDateTime.of(2023, 1, 3, 0, 0),
        true);

    commandHandler.handleCommand(cmd);

    List<String> messages = view.getMessageHistory();
    // First event listed should be from day 1
    assertTrue(messages.get(1).contains("Day 1 Event"));
    // Second event listed should be from day 2
    assertTrue(messages.get(2).contains("Day 2 Event"));
  }

  private class TestCalendar implements Calendar {

    private boolean createEventResult;
    private boolean createAllDayEventResult;
    private boolean createRecurringEventResult;
    private boolean createRecurringAllDayEventResult;
    private boolean editEventResult;
    private boolean editEventsFromResult;
    private boolean editAllEventsResult;
    private List<Event> eventsOnResult = new ArrayList<>();
    private List<Event> eventsFromResult = new ArrayList<>();
    private boolean isBusyResult;
    private String exportToCSVResult;

    public void setCreateEventResult(boolean result) {
      this.createEventResult = result;
    }

    public void setCreateAllDayEventResult(boolean result) {
      this.createAllDayEventResult = result;
    }

    public void setCreateRecurringEventResult(boolean result) {
      this.createRecurringEventResult = result;
    }

    public void setCreateRecurringAllDayEventResult(boolean result) {
      this.createRecurringAllDayEventResult = result;
    }

    public void setEditEventResult(boolean result) {
      this.editEventResult = result;
    }

    public void setEditEventsFromResult(boolean result) {
      this.editEventsFromResult = result;
    }

    public void setEditAllEventsResult(boolean result) {
      this.editAllEventsResult = result;
    }

    public void setEventsOnResult(List<Event> events) {
      this.eventsOnResult = events;
    }

    public void setEventsFromResult(List<Event> events) {
      this.eventsFromResult = events;
    }

    public void setIsBusyResult(boolean result) {
      this.isBusyResult = result;
    }

    public void setExportToCSVResult(String result) {
      this.exportToCSVResult = result;
    }

    @Override
    public boolean createEvent(String eventName, LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        boolean autoDecline, String description, String location, boolean isPublic) {
      return createEventResult;
    }

    @Override
    public boolean createAllDayEvent(String eventName, LocalDateTime dateTime, boolean autoDecline,
        String description, String location, boolean isPublic) {
      return createAllDayEventResult;
    }

    @Override
    public boolean createRecurringEvent(String eventName, LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String weekdays, int occurrences, LocalDateTime untilDate, boolean autoDecline,
        String description,
        String location, boolean isPublic) {
      return createRecurringEventResult;
    }

    @Override
    public boolean createRecurringAllDayEvent(String eventName, LocalDateTime dateTime,
        String weekdays,
        int occurrences, LocalDateTime untilDate, boolean autoDecline, String description,
        String location,
        boolean isPublic) {
      return createRecurringAllDayEventResult;
    }

    @Override
    public boolean editEvent(String property, String eventName, LocalDateTime startDateTime,
        LocalDateTime endDateTime, String newValue) {
      return editEventResult;
    }

    @Override
    public boolean editEventsFrom(String property, String eventName, LocalDateTime startDateTime,
        String newValue) {
      return editEventsFromResult;
    }

    @Override
    public boolean editAllEvents(String property, String eventName, String newValue) {
      return editAllEventsResult;
    }

    @Override
    public List<Event> getEventsOn(LocalDateTime dateTime) {
      return eventsOnResult;
    }

    @Override
    public List<Event> getEventsFrom(LocalDateTime startDateTime, LocalDateTime endDateTime) {
      return eventsFromResult;
    }

    @Override
    public boolean isBusy(LocalDateTime dateTime) {
      return isBusyResult;
    }

    @Override
    public String exportToCSV(String fileName) {
      return exportToCSVResult;
    }
  }

  private class TestTextUI implements TextUI {

    private List<String> messageHistory = new ArrayList<>();
    private List<String> errorHistory = new ArrayList<>();

    public String getLastMessage() {
      return messageHistory.isEmpty() ? "" : messageHistory.get(messageHistory.size() - 1);
    }

    public String getLastError() {
      return errorHistory.isEmpty() ? "" : errorHistory.get(errorHistory.size() - 1);
    }

    public List<String> getMessageHistory() {
      return messageHistory;
    }

    public List<String> getErrorHistory() {
      return errorHistory;
    }

    @Override
    public void displayMessage(String message) {
      messageHistory.add(message);
    }

    @Override
    public void displayError(String error) {
      errorHistory.add(error);
    }

    @Override
    public String getCommand() {
      return "test command";
    }

    @Override
    public void close() {

    }
  }

  private class TestEvent implements Event {

    private String subject;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean isAllDay;

    public TestEvent(String subject, LocalDateTime startDateTime, LocalDateTime endDateTime,
        boolean isAllDay) {
      this.subject = subject;
      this.startDateTime = startDateTime;
      this.endDateTime = endDateTime;
      this.isAllDay = isAllDay;
    }

    @Override
    public String getSubject() {
      return subject;
    }

    @Override
    public LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    @Override
    public LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    @Override
    public String getDescription() {
      return "";
    }

    @Override
    public String getLocation() {
      return "";
    }

    @Override
    public boolean isPublic() {
      return true;
    }

    @Override
    public boolean isAllDay() {
      return isAllDay;
    }

    @Override
    public boolean isRecurring() {
      return false;
    }

    @Override
    public boolean conflictsWith(Event other) {
      return false;
    }

    @Override
    public String toString() {
      return subject;
    }
  }

  private class CreateCommandStub extends CreateCommand {

    private String eventName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean autoDecline;
    private boolean isAllDay;
    private boolean isRecurring;
    private String weekdays;
    private int occurrences;
    private LocalDateTime untilDate;

    public CreateCommandStub(String eventName, LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        boolean autoDecline, boolean isAllDay, boolean isRecurring, String weekdays,
        int occurrences, LocalDateTime untilDate) {
      this.eventName = eventName;
      this.startDateTime = startDateTime;
      this.endDateTime = endDateTime;
      this.autoDecline = autoDecline;
      this.isAllDay = isAllDay;
      this.isRecurring = isRecurring;
      this.weekdays = weekdays;
      this.occurrences = occurrences;
      this.untilDate = untilDate;
    }

    @Override
    public String getEventName() {
      return eventName;
    }

    @Override
    public LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    @Override
    public LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    @Override
    public boolean isAutoDecline() {
      return autoDecline;
    }

    @Override
    public String getDescription() {
      return "";
    }

    @Override
    public String getLocation() {
      return "";
    }

    @Override
    public boolean isPublic() {
      return true;
    }

    @Override
    public boolean isAllDay() {
      return isAllDay;
    }

    @Override
    public boolean isRecurring() {
      return isRecurring;
    }

    @Override
    public String getWeekdays() {
      return weekdays;
    }

    @Override
    public int getOccurrences() {
      return occurrences;
    }

    @Override
    public LocalDateTime getUntilDate() {
      return untilDate;
    }
  }

  private class EditCommandStub extends EditCommand {

    private EditType editType;
    private String property;
    private String eventName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String newValue;

    public EditCommandStub(EditType editType, String property, String eventName,
        LocalDateTime startDateTime, LocalDateTime endDateTime, String newValue) {
      super(editType);
      this.editType = editType;
      this.property = property;
      this.eventName = eventName;
      this.startDateTime = startDateTime;
      this.endDateTime = endDateTime;
      this.newValue = newValue;
    }

    @Override
    public EditType getEditType() {
      return editType;
    }

    @Override
    public String getProperty() {
      return property;
    }

    @Override
    public String getEventName() {
      return eventName;
    }

    @Override
    public LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    @Override
    public LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    @Override
    public String getNewValue() {
      return newValue;
    }
  }

  private class PrintCommandStub extends PrintCommand {

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean isDateRange;

    public PrintCommandStub(LocalDateTime startDateTime, LocalDateTime endDateTime,
        boolean isDateRange) {
      this.startDateTime = startDateTime;
      this.endDateTime = endDateTime;
      this.isDateRange = isDateRange;
    }

    @Override
    public LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    @Override
    public LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    @Override
    public boolean isDateRange() {
      return isDateRange;
    }
  }

  private class ExportCommandStub extends ExportCommand {

    private String fileName;

    public ExportCommandStub(String fileName) {
      this.fileName = fileName;
    }

    @Override
    public String getFileName() {
      return fileName;
    }
  }

  private class ShowCommandStub extends ShowCommand {

    private LocalDateTime dateTime;

    public ShowCommandStub(LocalDateTime dateTime) {
      this.dateTime = dateTime;
    }

    @Override
    public LocalDateTime getDateTime() {
      return dateTime;
    }
  }
}