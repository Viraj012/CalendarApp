
import model.CalendarManager;
import model.Event;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Tests for the event copying functionality between calendars.
 */
public class CopyEventTest {

  private CalendarManager manager;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;

  @Before
  public void setUp() {
    manager = new CalendarManager();
    manager.createCalendar("Source", ZoneId.of("America/New_York"));
    manager.createCalendar("Target", ZoneId.of("Europe/London"));

    // May 15, 2023 at 10:00 AM
    startDateTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    // May 15, 2023 at 11:00 AM
    endDateTime = LocalDateTime.of(2023, 5, 15, 11, 0);

    // Create an event in the source calendar
    manager.useCalendar("Source");
    manager.getCurrentCalendar().createEvent("Test Event", startDateTime, endDateTime,
        true, "Test Description", "Test Location", true);
  }

  @Test
  public void testCopySingleEventToSameCalendar() {
    // Target date is one week later
    LocalDateTime targetDateTime = startDateTime.plusDays(7);

    boolean result = manager.copyEvent("Test Event", startDateTime, "Source", targetDateTime);
    assertTrue("Copying event to same calendar should succeed", result);

    // Verify that the event was copied
    List<Event> events = manager.getCurrentCalendar().getEventsOn(targetDateTime);
    assertEquals("There should be one event on the target date", 1, events.size());
    assertEquals("Copied event should have the same name", "Test Event", events.get(0).getSubject());
  }

  @Test
  public void testCopySingleEventToDifferentCalendar() {
    // Target date is one week later
    LocalDateTime targetDateTime = startDateTime.plusDays(7);

    boolean result = manager.copyEvent("Test Event", startDateTime, "Target", targetDateTime);
    assertTrue("Copying event to different calendar should succeed", result);

    // Verify that the event was copied to the target calendar
    manager.useCalendar("Target");
    List<Event> events = manager.getCurrentCalendar().getEventsOn(targetDateTime);
    assertEquals("There should be one event on the target date", 1, events.size());
    assertEquals("Copied event should have the same name", "Test Event", events.get(0).getSubject());
  }

  @Test
  public void testCopyEventWithConflict() {
    // Create conflicting event in target calendar
    manager.useCalendar("Target");
    manager.getCurrentCalendar().createEvent("Existing Event", startDateTime, endDateTime,
        true, "Existing Description", "Existing Location", true);

    // Switch back to source
    manager.useCalendar("Source");

    // Try to copy to the same time slot (conflict)
    boolean result = manager.copyEvent("Test Event", startDateTime, "Target", startDateTime);
    assertFalse("Copying event with conflict should fail", result);
  }

  @Test
  public void testCopyRecurringEvent() {
    // Create a recurring event in the source calendar
    manager.useCalendar("Source");

    // Clear previous events
    LocalDateTime recurrStartTime = LocalDateTime.of(2023, 6, 5, 10, 0);
    LocalDateTime recurrEndTime = LocalDateTime.of(2023, 6, 5, 11, 0);

    // Create recurring event: every Monday and Wednesday for 3 occurrences
    boolean created = manager.getCurrentCalendar().createRecurringEvent(
        "Recurring Meeting", recurrStartTime, recurrEndTime, "MW", 3, null,
        true, "Recurring Description", "Recurring Location", true);
    assertTrue("Creating recurring event should succeed", created);

    // Copy to target calendar
    LocalDateTime targetDateTime = recurrStartTime.plusDays(14); // Two weeks later
    boolean result = manager.copyEvent("Recurring Meeting", recurrStartTime, "Target", targetDateTime);
    assertTrue("Copying recurring event should succeed", result);

    // Verify that the event was copied with its recurrence pattern
    manager.useCalendar("Target");
    List<Event> targetEvents = manager.getCurrentCalendar().getAllEvents();
    assertEquals("There should be one event series in the target calendar", 1, targetEvents.size());
    assertTrue("Copied event should be recurring", targetEvents.get(0).isRecurring());
  }

  @Test
  public void testCopyEventsOnDay() {
    // Create multiple events on the same day in source calendar
    manager.useCalendar("Source");

    LocalDateTime morningStart = LocalDateTime.of(2023, 5, 15, 9, 0);
    LocalDateTime morningEnd = LocalDateTime.of(2023, 5, 15, 10, 0);
    LocalDateTime afternoonStart = LocalDateTime.of(2023, 5, 15, 14, 0);
    LocalDateTime afternoonEnd = LocalDateTime.of(2023, 5, 15, 15, 0);

    manager.getCurrentCalendar().createEvent("Morning Meeting", morningStart, morningEnd,
        true, "Morning Description", "Morning Location", true);
    manager.getCurrentCalendar().createEvent("Afternoon Meeting", afternoonStart, afternoonEnd,
        true, "Afternoon Description", "Afternoon Location", true);

    // Copy all events to the next day
    LocalDateTime targetDate = LocalDateTime.of(2023, 5, 16, 0, 0);
    boolean result = manager.copyEventsOnDay(startDateTime, "Target", targetDate);
    assertTrue("Copying all events on a day should succeed", result);

    // Verify that both events were copied
    manager.useCalendar("Target");
    List<Event> targetEvents = manager.getCurrentCalendar().getAllEvents();
    assertEquals("There should be three events in the target calendar", 3, targetEvents.size());
  }

  @Test
  public void testCopyEventsInRange() {
    // Set up multiple events in source calendar on different days
    manager.useCalendar("Source");

    LocalDateTime day1Start = LocalDateTime.of(2023, 5, 15, 10, 0);
    LocalDateTime day1End = LocalDateTime.of(2023, 5, 15, 11, 0);
    LocalDateTime day2Start = LocalDateTime.of(2023, 5, 16, 10, 0);
    LocalDateTime day2End = LocalDateTime.of(2023, 5, 16, 11, 0);
    LocalDateTime day3Start = LocalDateTime.of(2023, 5, 17, 10, 0);
    LocalDateTime day3End = LocalDateTime.of(2023, 5, 17, 11, 0);

    // Create events on day 2 and 3 (day 1 already has an event from setUp)
    manager.getCurrentCalendar().createEvent("Day 2 Event", day2Start, day2End,
        true, "Day 2 Description", "Day 2 Location", true);
    manager.getCurrentCalendar().createEvent("Day 3 Event", day3Start, day3End,
        true, "Day 3 Description", "Day 3 Location", true);

    // Define date range to copy (day 1 to day 3)
    LocalDateTime rangeStart = LocalDateTime.of(2023, 5, 15, 0, 0);
    LocalDateTime rangeEnd = LocalDateTime.of(2023, 5, 17, 23, 59);

    // Target date is one week later
    LocalDateTime targetDate = LocalDateTime.of(2023, 5, 22, 0, 0);

    boolean result = manager.copyEventsInRange(rangeStart, rangeEnd, "Target", targetDate);
    assertTrue("Copying events in date range should succeed", result);

    // Verify that all three events were copied with the same relative positions
    manager.useCalendar("Target");
    List<Event> day1Events = manager.getCurrentCalendar().getEventsOn(LocalDateTime.of(2023, 5, 22, 0, 0));
    List<Event> day2Events = manager.getCurrentCalendar().getEventsOn(LocalDateTime.of(2023, 5, 23, 0, 0));
    List<Event> day3Events = manager.getCurrentCalendar().getEventsOn(LocalDateTime.of(2023, 5, 24, 0, 0));

    assertEquals("Day 1 should have one copied event", 1, day1Events.size());
    assertEquals("Day 2 should have one copied event", 1, day2Events.size());
    assertEquals("Day 3 should have one copied event", 1, day3Events.size());
  }

  @Test
  public void testCopyToNonExistentCalendar() {
    boolean result = manager.copyEvent("Test Event", startDateTime, "NonExistent", startDateTime.plusDays(1));
    assertFalse("Copying to non-existent calendar should fail", result);
  }

  @Test
  public void testCopyNonExistentEvent() {
    boolean result = manager.copyEvent("Non-Existent Event", startDateTime, "Target", startDateTime.plusDays(1));
    assertFalse("Copying non-existent event should fail", result);
  }

  @Test
  public void testAllDayEventCopy() {
    // Create an all-day event in the source calendar
    manager.useCalendar("Source");

    LocalDateTime allDayDate = LocalDateTime.of(2023, 5, 20, 0, 0);
    boolean created = manager.getCurrentCalendar().createAllDayEvent(
        "All-Day Event", allDayDate, true, "All-Day Description", "All-Day Location", true);
    assertTrue("Creating all-day event should succeed", created);

    // Copy to target calendar
    LocalDateTime targetDate = LocalDateTime.of(2023, 5, 27, 0, 0); // One week later
    boolean result = manager.copyEvent("All-Day Event", allDayDate, "Target", targetDate);
    assertTrue("Copying all-day event should succeed", result);

    // Verify that the event was copied and is still an all-day event
    manager.useCalendar("Target");
    List<Event> targetEvents = manager.getCurrentCalendar().getEventsOn(targetDate);
    assertEquals("There should be one event on the target date", 1, targetEvents.size());
    assertTrue("Copied event should be an all-day event", targetEvents.get(0).isAllDay());
  }
}