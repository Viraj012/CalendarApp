import model.Calendar;
import model.CalendarManager;
import model.Event;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Tests for the CalendarManager class, focusing on multiple calendars
 * and timezone functionality.
 */
public class CalendarManagerTest {

  private CalendarManager calendarManager;

  @Before
  public void setUp() {
    calendarManager = new CalendarManager();
  }


  @Test
  public void testCreateCalendar() {

    boolean result = calendarManager.createCalendar("Test", ZoneId.systemDefault());
    assertTrue("Should successfully create a calendar with a unique name", result);

    result = calendarManager.createCalendar("Test", ZoneId.systemDefault());
    assertFalse("Should fail to create a calendar with a duplicate name", result);
  }

  @Test
  public void testUseCalendar() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());

    boolean result = calendarManager.useCalendar("Test");
    assertTrue("Should successfully use an existing calendar", result);

    result = calendarManager.useCalendar("NonExistent");
    assertFalse("Should fail to use a non-existent calendar", result);
  }

  @Test
  public void testGetCurrentCalendar() {

    assertNull("Initial current calendar should be null", calendarManager.getCurrentCalendar());

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar currentCalendar = calendarManager.getCurrentCalendar();
    assertNotNull("Current calendar should not be null after setting", currentCalendar);
    assertEquals("Current calendar should have the correct name", "Test",
            currentCalendar.getName());
  }

  @Test
  public void testCalendarExists() {

    assertFalse("Should return false for non-existent calendar",
            calendarManager.calendarExists("Test"));

    calendarManager.createCalendar("Test", ZoneId.systemDefault());

    assertTrue("Should return true for existing calendar", calendarManager.calendarExists("Test"));
  }

  @Test
  public void testGetCalendar() {

    assertNull("Should return null for non-existent calendar", calendarManager.getCalendar("Test"));

    calendarManager.createCalendar("Test", ZoneId.systemDefault());

    Calendar calendar = calendarManager.getCalendar("Test");
    assertNotNull("Should return non-null for existing calendar", calendar);
    assertEquals("Should return calendar with correct name", "Test", calendar.getName());
  }


  @Test
  public void testEditCalendarName() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());

    boolean result = calendarManager.editCalendar("Test", "name", "NewName");
    assertTrue("Should successfully rename calendar", result);

    assertFalse("Old name should no longer exist", calendarManager.calendarExists("Test"));
    assertTrue("New name should exist", calendarManager.calendarExists("NewName"));

    calendarManager.createCalendar("Test2", ZoneId.systemDefault());

    result = calendarManager.editCalendar("Test2", "name", "NewName");
    assertFalse("Should fail to rename to existing name", result);
  }

  @Test
  public void testEditCalendarNameWithCurrentCalendar() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar currentBefore = calendarManager.getCurrentCalendar();

    boolean result = calendarManager.editCalendar("Test", "name", "NewName");
    assertTrue("Should successfully rename current calendar", result);

    Calendar currentAfter = calendarManager.getCurrentCalendar();
    assertEquals("Current calendar name should be updated", "NewName", currentAfter.getName());
    assertNotNull("Current calendar should still be set", currentAfter);
  }

  @Test
  public void testEditCalendarTimezone() {

    ZoneId originalZone = ZoneId.of("America/New_York");
    calendarManager.createCalendar("Test", originalZone);
    calendarManager.useCalendar("Test");

    ZoneId newZone = ZoneId.of("Europe/London");
    boolean result = calendarManager.editCalendar("Test", "timezone", "Europe/London");
    assertTrue("Should successfully change timezone", result);

    Calendar calendar = calendarManager.getCalendar("Test");
    assertEquals("Timezone should be updated", newZone, calendar.getTimezone());

    result = calendarManager.editCalendar("Test", "timezone", "InvalidZone");
    assertFalse("Should fail with invalid timezone", result);
  }

  @Test
  public void testEditCalendarInvalidProperty() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());

    boolean result = calendarManager.editCalendar("Test", "invalidProperty", "value");
    assertFalse("Should fail with invalid property", result);
  }

  @Test
  public void testEditNonExistentCalendar() {

    boolean result = calendarManager.editCalendar("NonExistent", "name", "NewName");
    assertFalse("Should fail for non-existent calendar", result);
  }


  @Test
  public void testTimezoneChangeWithEvents() {

    ZoneId originalZone = ZoneId.of("America/New_York");
    calendarManager.createCalendar("Test", originalZone);
    calendarManager.useCalendar("Test");

    Calendar calendar = calendarManager.getCurrentCalendar();

    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean eventCreated = calendar.createEvent("Regular Event", startTime, endTime, true,
            "Description", "Location", true);
    assertTrue("Should create regular event", eventCreated);

    LocalDateTime allDayDate = LocalDateTime.of(2023, 1, 2, 0, 0);
    eventCreated = calendar.createAllDayEvent("All-day Event", allDayDate, true, "Description",
            "Location", true);
    assertTrue("Should create all-day event", eventCreated);

    LocalDateTime recurringStart = LocalDateTime.of(2023, 1, 3, 14, 0);
    LocalDateTime recurringEnd = LocalDateTime.of(2023, 1, 3, 15, 0);
    LocalDateTime untilDate = LocalDateTime.of(2023, 1, 31, 0, 0);
    eventCreated = calendar.createRecurringEvent("Recurring Event", recurringStart, recurringEnd,
            "MWF", -1, untilDate, true, "Description", "Location", true);
    assertTrue("Should create recurring event", eventCreated);

    LocalDateTime recurringAllDayDate = LocalDateTime.of(2023, 1, 4, 0, 0);
    eventCreated = calendar.createRecurringAllDayEvent("Recurring All-day Event",
            recurringAllDayDate, "TR", 4, null, true, "Description", "Location", true);
    assertTrue("Should create recurring all-day event", eventCreated);

    ZoneId newZone = ZoneId.of("Europe/London");
    boolean result = calendarManager.editCalendar("Test", "timezone", "Europe/London");
    assertTrue("Should successfully change timezone", result);

    calendar = calendarManager.getCurrentCalendar();
    assertEquals("Timezone should be updated", newZone, calendar.getTimezone());
    assertEquals("All events should be preserved", 4, calendar.getAllEvents().size());
  }

  @Test
  public void testTimezoneChangeEventTimesAdjusted() {

    ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
    calendarManager.createCalendar("Tokyo", tokyoZone);
    calendarManager.useCalendar("Tokyo");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime tokyoTime = LocalDateTime.of(2023, 1, 1, 10, 0);
    LocalDateTime tokyoEndTime = LocalDateTime.of(2023, 1, 1, 11, 0);
    boolean eventCreated = calendar.createEvent("Tokyo Event", tokyoTime, tokyoEndTime, true,
            "Description", "Location", true);
    assertTrue("Should create event in Tokyo timezone", eventCreated);

    ZoneId londonZone = ZoneId.of("Europe/London");
    boolean result = calendarManager.editCalendar("Tokyo", "timezone", "Europe/London");
    assertTrue("Should successfully change timezone", result);

    calendar = calendarManager.getCurrentCalendar();
    List<Event> events = calendar.getAllEvents();
    assertEquals("Should still have one event", 1, events.size());
    Event adjustedEvent = events.get(0);

    assertEquals("Hour should be adjusted to 1:00 (London time)", 1,
            adjustedEvent.getStartDateTime().getHour());
    assertEquals("End hour should be adjusted to 2:00 (London time)", 2,
            adjustedEvent.getEndDateTime().getHour());
  }

  @Test
  public void testTimezoneChangeRecurringEventUntilDateAdjusted() {

    ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
    calendarManager.createCalendar("Tokyo", tokyoZone);
    calendarManager.useCalendar("Tokyo");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime tokyoTime = LocalDateTime.of(2023, 1, 1, 10, 0);
    LocalDateTime tokyoEndTime = LocalDateTime.of(2023, 1, 1, 11, 0);
    LocalDateTime tokyoUntilDate = LocalDateTime.of(2023, 1, 31, 10, 0);

    boolean eventCreated = calendar.createRecurringEvent(
            "Tokyo Recurring Event",
            tokyoTime,
            tokyoEndTime,
            "MWF",
            -1,
            tokyoUntilDate,
            true,
            "Description",
            "Location",
            true);
    assertTrue("Should create recurring event in Tokyo timezone", eventCreated);

    ZoneId londonZone = ZoneId.of("Europe/London");
    boolean result = calendarManager.editCalendar("Tokyo", "timezone", "Europe/London");
    assertTrue("Should successfully change timezone", result);

    calendar = calendarManager.getCurrentCalendar();
    List<Event> events = calendar.getAllEvents();
    assertEquals("Should still have one event", 1, events.size());
    Event adjustedEvent = events.get(0);

    assertTrue("Event should still be recurring", adjustedEvent.isRecurring());

    List<Event> occurrences = calendar.getEventsFrom(
            adjustedEvent.getStartDateTime(),
            LocalDateTime.of(2023, 1, 31, 23, 59));

    assertTrue("Should have multiple occurrences", occurrences.size() > 1);
  }


  @Test
  public void testCopyEvent() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean eventCreated = sourceCalendar.createEvent("Test Event", startTime, endTime, true,
            "Description", "Location", true);
    assertTrue("Should create event", eventCreated);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Test Event", startTime, "Target", targetTime);
    assertTrue("Should copy event successfully", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have one event", 1, targetEvents.size());
    Event copiedEvent = targetEvents.get(0);
    assertEquals("Event subject should match", "Test Event", copiedEvent.getSubject());
    assertEquals("Event start time should match target time", targetTime,
            copiedEvent.getStartDateTime());
    assertEquals("Event description should be preserved", "Description",
            copiedEvent.getDescription());
    assertEquals("Event location should be preserved", "Location", copiedEvent.getLocation());
    assertTrue("Event should maintain public status", copiedEvent.isPublic());
  }

  @Test
  public void testCopyAllDayEvent() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime dateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
    boolean eventCreated = sourceCalendar.createAllDayEvent("All-day Event", dateTime, true,
            "Description", "Location", true);
    assertTrue("Should create all-day event", eventCreated);

    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEvent("All-day Event", dateTime, "Target", targetDate);
    assertTrue("Should copy all-day event successfully", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have one event", 1, targetEvents.size());
    Event copiedEvent = targetEvents.get(0);
    assertTrue("Copied event should be all-day", copiedEvent.isAllDay());
    assertEquals("Event date should match target date", targetDate.toLocalDate(),
            copiedEvent.getStartDateTime().toLocalDate());
  }


  @Test
  public void testCopyEventFailIfTargetCalendarDoesNotExist() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean eventCreated = sourceCalendar.createEvent("Test Event", startTime, endTime, true,
            "Description", "Location", true);
    assertTrue("Should create event", eventCreated);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Test Event", startTime, "NonExistent", targetTime);
    assertFalse("Should fail to copy to non-existent calendar", copied);
  }

  @Test
  public void testCopyEventFailIfNoCurrentCalendar() {

    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Test Event", startTime, "Target", targetTime);
    assertFalse("Should fail if no current calendar is set", copied);
  }

  @Test
  public void testCopyEventFailIfEventDoesNotExist() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("NonExistent", startTime, "Target", targetTime);
    assertFalse("Should fail if event does not exist", copied);
  }


  @Test
  public void testEventNameMatchWithDirectMatch() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean created = calendar.createEvent("Test Event", startTime, endTime, true, "Description",
            "Location", true);
    assertTrue("Should create event", created);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Test Event", startTime, "Test", targetTime);
    assertTrue("Should match event name directly", copied);
  }

  @Test
  public void testEventNameMatchWithQuotedName() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean created = calendar.createEvent("\"Quoted Event\"", startTime, endTime, true,
            "Description", "Location", true);
    assertTrue("Should create event with quoted name", created);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Quoted Event", startTime, "Test", targetTime);
    assertTrue("Should match event name without quotes", copied);
  }

  @Test
  public void testEventNameMatchWithQuotedSearchAndStoredUnquoted() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean created = calendar.createEvent("Unquoted Event", startTime, endTime, true,
            "Description", "Location", true);
    assertTrue("Should create event with unquoted name", created);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("\"Unquoted Event\"", startTime, "Test", targetTime);
    assertTrue("Should match event when search name has quotes but stored name doesn't", copied);
  }

  @Test
  public void testEventNameMatchWithRecurringSuffix() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    LocalDateTime untilDate = LocalDateTime.of(2023, 1, 31, 0, 0);
    boolean created = calendar.createRecurringEvent("Recurring Event", startTime, endTime, "MWF",
            -1, untilDate, true, "Description", "Location", true);
    assertTrue("Should create recurring event", created);

    List<Event> events = calendar.getEventsOn(startTime);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Recurring Event", startTime, "Test", targetTime);

  }

  @Test
  public void testEventNameMatchWithRecurringSuffixAndQuotes() {

    calendarManager.createCalendar("Test", ZoneId.systemDefault());
    calendarManager.useCalendar("Test");

    Calendar calendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    LocalDateTime untilDate = LocalDateTime.of(2023, 1, 31, 0, 0);
    boolean created = calendar.createRecurringEvent("\"Complex Recurring Event\"", startTime,
            endTime, "MWF", -1, untilDate, true, "Description", "Location", true);
    assertTrue("Should create recurring event with quoted name", created);

    List<Event> events = calendar.getEventsOn(startTime);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);

    boolean copied = calendarManager.copyEvent("Complex Recurring Event", startTime, "Test",
            targetTime);

  }


  @Test
  public void testCopyEventsOnDay() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime date = LocalDateTime.of(2023, 1, 1, 0, 0);

    boolean created = sourceCalendar.createEvent("Event 1", date.withHour(9), date.withHour(10),
            true, "Description", "Location", true);
    assertTrue("Should create event 1", created);

    created = sourceCalendar.createEvent("Event 2", date.withHour(14), date.withHour(15), true,
            "Description", "Location", true);
    assertTrue("Should create event 2", created);

    created = sourceCalendar.createAllDayEvent("All-day Event", date, true, "Description",
            "Location", true);

    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(date, "Target", targetDate);
    assertTrue("Should copy events successfully", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getEventsOn(targetDate);

  }

  @Test
  public void testCopyEventsOnDayFailIfNoneExist() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    LocalDateTime sourceDate = LocalDateTime.of(2023, 1, 1, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(sourceDate, "Target", targetDate);
    assertFalse("Should fail if no events exist on the source day", copied);
  }

  @Test
  public void testCopyEventsOnDayFailIfTargetCalendarDoesNotExist() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime date = LocalDateTime.of(2023, 1, 1, 0, 0);
    boolean created = sourceCalendar.createEvent("Event", date.withHour(9), date.withHour(10), true,
            "Description", "Location", true);
    assertTrue("Should create event", created);

    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(date, "NonExistent", targetDate);
    assertFalse("Should fail if target calendar does not exist", copied);
  }

  @Test
  public void testCopyEventsOnDayFailIfNoCurrentCalendar() {

    LocalDateTime sourceDate = LocalDateTime.of(2023, 1, 1, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(sourceDate, "Target", targetDate);
    assertFalse("Should fail if no current calendar is set", copied);
  }

  @Test
  public void testCopyEventsWithTimezoneConversion() {

    ZoneId sourceZone = ZoneId.of("America/New_York");
    calendarManager.createCalendar("Source", sourceZone);

    ZoneId targetZone = ZoneId.of("Europe/London");
    calendarManager.createCalendar("Target", targetZone);

    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime sourceDateTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime sourceEndTime = LocalDateTime.of(2023, 1, 1, 13, 0);
    boolean created = sourceCalendar.createEvent("Timezone Test", sourceDateTime, sourceEndTime,
            true, "Description", "Location", true);
    assertTrue("Should create event", created);

    LocalDateTime targetDateTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("Timezone Test", sourceDateTime, "Target",
            targetDateTime);
    assertTrue("Should copy event with timezone conversion", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have one event", 1, targetEvents.size());
  }


  @Test
  public void testCopyEventsWithDayRolloverDueToTimezone() {

    ZoneId sourceZone = ZoneId.of("America/New_York");
    calendarManager.createCalendar("Source", sourceZone);

    ZoneId targetZone = ZoneId.of("Asia/Tokyo");
    calendarManager.createCalendar("Target", targetZone);

    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime sourceLateNight = LocalDateTime.of(2023, 1, 1, 23, 0);
    LocalDateTime sourceEndTime = LocalDateTime.of(2023, 1, 2, 0, 0);
    boolean created = sourceCalendar.createEvent("Late Night Event", sourceLateNight, sourceEndTime,
            true, "Description", "Location", true);
    assertTrue("Should create late night event", created);

    LocalDateTime sourceEarlyMorning = LocalDateTime.of(2023, 1, 2, 7, 0);
    LocalDateTime sourceEarlyEnd = LocalDateTime.of(2023, 1, 2, 8, 0);
    created = sourceCalendar.createEvent("Early Morning Event", sourceEarlyMorning, sourceEarlyEnd,
            true, "Description", "Location", true);
    assertTrue("Should create early morning event", created);

    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(sourceLateNight, "Target", targetDate);
    assertTrue("Should copy events with day rollover", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();

  }

  @Test
  public void testCopyEventsWithDayRollbackDueToTimezone() {

    ZoneId sourceZone = ZoneId.of("Asia/Tokyo");
    calendarManager.createCalendar("Source", sourceZone);

    ZoneId targetZone = ZoneId.of("America/New_York");
    calendarManager.createCalendar("Target", targetZone);

    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime sourceEarlyMorning = LocalDateTime.of(2023, 1, 2, 7, 0);
    LocalDateTime sourceEndTime = LocalDateTime.of(2023, 1, 2, 8, 0);
    boolean created = sourceCalendar.createEvent("Early Morning Event", sourceEarlyMorning,
            sourceEndTime, true, "Description", "Location", true);
    assertTrue("Should create early morning event", created);

    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(sourceEarlyMorning, "Target", targetDate);
    assertTrue("Should copy events with day rollback", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have the event", 1, targetEvents.size());
  }

  @Test
  public void testCopyEventsInRange() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();

    LocalDateTime day1 = LocalDateTime.of(2023, 1, 1, 0, 0);
    boolean created = sourceCalendar.createEvent("Day 1 Event", day1.withHour(9), day1.withHour(10),
            true, "Description", "Location", true);
    assertTrue("Should create day 1 event", created);

    LocalDateTime day2 = LocalDateTime.of(2023, 1, 2, 0, 0);
    created = sourceCalendar.createEvent("Day 2 Event", day2.withHour(14), day2.withHour(15), true,
            "Description", "Location", true);
    assertTrue("Should create day 2 event", created);

    LocalDateTime day3 = LocalDateTime.of(2023, 1, 3, 0, 0);
    created = sourceCalendar.createAllDayEvent("Day 3 All-day Event", day3, true, "Description",
            "Location", true);
    assertTrue("Should create day 3 all-day event", created);

    LocalDateTime targetStart = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsInRange(day1, day3, "Target", targetStart);
    assertTrue("Should copy events in range successfully", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");

    List<Event> day1Events = targetCalendar.getEventsOn(targetStart);
    assertEquals("Target should have 1 event on day 1", 1, day1Events.size());

    List<Event> day2Events = targetCalendar.getEventsOn(targetStart.plusDays(1));
    assertEquals("Target should have 1 event on day 2", 1, day2Events.size());

    List<Event> day3Events = targetCalendar.getEventsOn(targetStart.plusDays(2));
    assertEquals("Target should have 1 event on day 3", 1, day3Events.size());
  }

  @Test
  public void testCopyEventsInRangeFailIfNoneExist() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2023, 1, 7, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsInRange(startDate, endDate, "Target", targetDate);
    assertFalse("Should fail if no events exist in the source range", copied);
  }

  @Test
  public void testCopyEventsInRangeFailIfTargetCalendarDoesNotExist() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime date = LocalDateTime.of(2023, 1, 1, 0, 0);
    boolean created = sourceCalendar.createEvent("Event", date.withHour(9), date.withHour(10), true,
            "Description", "Location", true);
    assertTrue("Should create event", created);

    LocalDateTime endDate = LocalDateTime.of(2023, 1, 7, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsInRange(date, endDate, "NonExistent", targetDate);
    assertFalse("Should fail if target calendar does not exist", copied);
  }

  @Test
  public void testCopyEventsInRangeFailIfNoCurrentCalendar() {

    LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2023, 1, 7, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsInRange(startDate, endDate, "Target", targetDate);
    assertFalse("Should fail if no current calendar is set", copied);
  }


  @Test
  public void testWeekdaysToStringViaRecurringEvents() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 12, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 13, 0);

    boolean created = sourceCalendar.createRecurringEvent("All Weekdays Event", startTime, endTime,
            "MTWRFSU", 7, null, true, "Description", "Location", true);
    assertTrue("Should create recurring event with all weekdays", created);

    LocalDateTime targetTime = LocalDateTime.of(2023, 2, 1, 12, 0);
    boolean copied = calendarManager.copyEvent("All Weekdays Event", startTime, "Target",
            targetTime);
    assertTrue("Should copy recurring event with all weekdays", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have one event", 1, targetEvents.size());
    Event copiedEvent = targetEvents.get(0);
    assertTrue("Copied event should be recurring", copiedEvent.isRecurring());

    for (String weekdays : new String[]{"M", "W", "F", "MWF", "TR", "MTWRF", "SU"}) {
      created = sourceCalendar.createRecurringEvent("Weekdays " + weekdays, startTime.plusHours(1),
              endTime.plusHours(1), weekdays, 4, null, true, "Description", "Location", true);

      copied = calendarManager.copyEvent("Weekdays " + weekdays, startTime.plusHours(1), "Target",
              targetTime.plusHours(1));

    }
  }


  @Test
  public void testCopyEventsOnDayWithExactBoundaries() {

    ZoneId nyZone = ZoneId.of("America/New_York");
    ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
    calendarManager.createCalendar("NY", nyZone);
    calendarManager.createCalendar("Tokyo", tokyoZone);
    calendarManager.useCalendar("NY");

    Calendar nyCalendar = calendarManager.getCurrentCalendar();

    LocalDateTime midnight = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 1, 1, 1, 0);
    boolean created = nyCalendar.createEvent("Midnight Event", midnight, endTime, true, "", "",
            true);
    assertTrue(created);

    LocalDateTime almostMidnight = LocalDateTime.of(2023, 1, 1, 23, 59, 59);
    LocalDateTime endTime2 = LocalDateTime.of(2023, 1, 2, 0, 30);
    created = nyCalendar.createEvent("Almost Midnight Event", almostMidnight, endTime2, true, "",
            "", true);
    assertTrue(created);

    boolean copied = calendarManager.copyEventsOnDay(midnight, "Tokyo", midnight.plusMonths(1));
    assertTrue("Should copy events on day boundaries", copied);

    Calendar tokyoCalendar = calendarManager.getCalendar("Tokyo");
    List<Event> tokyoEvents = tokyoCalendar.getAllEvents();
    assertEquals("Should have 2 events in Tokyo calendar", 2, tokyoEvents.size());
  }

  @Test
  public void testCopyEventsOnDayWithNoEvents() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    LocalDateTime emptyDay = LocalDateTime.of(2023, 3, 15, 0, 0);
    boolean copied = calendarManager.copyEventsOnDay(emptyDay, "Target", emptyDay);
    assertFalse("Should return false when no events exist on source day", copied);
  }

  @Test
  public void testCopyEventsInRangeEdgeCases() {

    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    Calendar sourceCalendar = calendarManager.getCurrentCalendar();

    LocalDateTime rangeStart = LocalDateTime.of(2023, 1, 1, 0, 0);
    LocalDateTime rangeEnd = LocalDateTime.of(2023, 1, 7, 23, 59, 59);

    boolean created = sourceCalendar.createEvent("Start Event", rangeStart, rangeStart.plusHours(1),
            true, "", "", true);
    assertTrue(created);

    created = sourceCalendar.createEvent("End Event", rangeEnd.minusHours(1), rangeEnd, true, "",
            "", true);
    assertTrue(created);

    LocalDateTime targetStart = LocalDateTime.of(2023, 2, 1, 0, 0);
    boolean copied = calendarManager.copyEventsInRange(rangeStart, rangeEnd, "Target", targetStart);
    assertTrue("Should copy events at range boundaries", copied);

    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have 2 events", 2, targetEvents.size());

    LocalDateTime singleDay = LocalDateTime.of(2023, 3, 15, 12, 0);
    created = sourceCalendar.createEvent("Single Day Event", singleDay, singleDay.plusHours(1),
            true, "", "", true);
    assertTrue(created);

    copied = calendarManager.copyEventsInRange(singleDay, singleDay, "Target",
            targetStart.plusMonths(1));
    assertTrue("Should copy events when range is a single point in time", copied);
  }

  @Test
  public void testCopyEventsWithConditionBoundaries() {

    ZoneId hawaiiZone = ZoneId.of("Pacific/Honolulu");
    ZoneId aucklandZone = ZoneId.of("Pacific/Auckland");

    calendarManager.createCalendar("Hawaii", hawaiiZone);
    calendarManager.createCalendar("Auckland", aucklandZone);
    calendarManager.useCalendar("Hawaii");

    Calendar hawaiiCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime lateHawaii = LocalDateTime.of(2023, 1, 1, 23, 0);
    LocalDateTime endHawaii = LocalDateTime.of(2023, 1, 2, 1, 0);
    boolean created = hawaiiCalendar.createEvent("Late Night Event", lateHawaii, endHawaii, true,
            "", "", true);
    assertTrue(created);

    LocalDateTime targetDay = LocalDateTime.of(2023, 2, 1, 0, 0);

    boolean copied = calendarManager.copyEventsOnDay(lateHawaii, "Auckland", targetDay);
    assertTrue("Should handle boundary condition for timezone day crossing", copied);

    Calendar aucklandCalendar = calendarManager.getCalendar("Auckland");
    List<Event> aucklandEvents = aucklandCalendar.getAllEvents();
    assertTrue("Should have copied events accounting for day boundaries",
            aucklandEvents.size() > 0);
  }

  @Test
  public void testCopyNonRecurringAllDayEvent() {
    // Create source and target calendars
    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    // Create a non-recurring all-day event
    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 0, 0);
    boolean eventCreated = sourceCalendar.createAllDayEvent(
            "Birthday Party",
            dateTime,
            false,
            "Annual celebration",
            "Home",
            true);
    assertTrue("Should create all-day event", eventCreated);

    // Copy the event to the target calendar
    LocalDateTime targetDate = LocalDateTime.of(2023, 6, 15, 0, 0);
    boolean copied = calendarManager.copyEvent(
            "Birthday Party",
            dateTime,
            "Target",
            targetDate);

    assertTrue("Non-recurring all-day event should be copied successfully", copied);

    // Verify the event exists in target calendar
    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have one event", 1, targetEvents.size());

    Event copiedEvent = targetEvents.get(0);
    assertEquals("Event subject should match", "Birthday Party", copiedEvent.getSubject());
    assertTrue("Copied event should be all-day", copiedEvent.isAllDay());
    assertFalse("Copied event should not be recurring", copiedEvent.isRecurring());
    assertEquals("Event date should match target date", targetDate.toLocalDate(),
            copiedEvent.getStartDateTime().toLocalDate());
  }

  @Test
  public void testCopyRecurringAllDayEvent() {
    // Create source and target calendars
    calendarManager.createCalendar("Source", ZoneId.systemDefault());
    calendarManager.createCalendar("Target", ZoneId.systemDefault());
    calendarManager.useCalendar("Source");

    // Create a recurring all-day event with until date
    Calendar sourceCalendar = calendarManager.getCurrentCalendar();
    LocalDateTime startDate = LocalDateTime.of(2023, 5, 15, 0, 0);
    LocalDateTime untilDate = LocalDateTime.of(2023, 8, 15, 0, 0);

    boolean eventCreated = sourceCalendar.createRecurringAllDayEvent(
            "Monthly Review",
            startDate,
            "MWF", // Monday, Wednesday, Friday
            -1, // Occurrences not used
            untilDate, // Until date specified
            false,
            "Performance review",
            "Meeting Room",
            true);

    assertTrue("Should create recurring all-day event with until date", eventCreated);

    // Copy the event to target calendar
    LocalDateTime targetDate = LocalDateTime.of(2023, 6, 15, 0, 0);
    boolean copied = calendarManager.copyEvent(
            "Monthly Review",
            startDate,
            "Target",
            targetDate);

    assertTrue("Recurring all-day event with until date should be copied", copied);

    // Verify event exists in target calendar
    Calendar targetCalendar = calendarManager.getCalendar("Target");
    List<Event> targetEvents = targetCalendar.getAllEvents();
    assertEquals("Target should have one event", 1, targetEvents.size());

    Event copiedEvent = targetEvents.get(0);
    assertEquals("Event subject should match", "Monthly Review", copiedEvent.getSubject());
    assertTrue("Copied event should be all-day", copiedEvent.isAllDay());
    assertTrue("Copied event should be recurring", copiedEvent.isRecurring());

    // Verify occurrences exist across a time range
    // Original event spans May 15 to Aug 15 (92 days)
    // New event should span June 15 to Sept 15 (same duration)
    LocalDateTime targetEndDate = targetDate.plusMonths(3);
    List<Event> occurrences = targetCalendar.getEventsFrom(targetDate, targetEndDate);
    assertTrue("Should have multiple occurrences", occurrences.size() > 1);

    // Verify the first occurrence is on the target date
    LocalDateTime firstOccurrenceDate = targetDate.toLocalDate().atStartOfDay();
    List<Event> firstDayEvents = targetCalendar.getEventsOn(firstOccurrenceDate);
    // assertTrue("Should have event on the first day", !firstDayEvents.isEmpty());
  }
}