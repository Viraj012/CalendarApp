
import model.Calendar;
import model.CalendarImpl;
import model.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Enhanced test class for CalendarImpl with improved mutation coverage.
 */
public class CalendarImplTest {
  private Calendar calendar;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private LocalDateTime nextDayDateTime;
  private String exportFileName = "test_calendar_export.csv";

  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    startDateTime = LocalDateTime.of(2025, 3, 10, 14, 0);
    endDateTime = LocalDateTime.of(2025, 3, 10, 15, 30);
    nextDayDateTime = LocalDateTime.of(2025, 3, 11, 14, 0);
  }

  @After
  public void tearDown() {
    // Clean up any export files created during tests
    File exportFile = new File(exportFileName);
    if (exportFile.exists()) {
      exportFile.delete();
    }
  }

  @Test
  public void testCreateEventSuccess() {
    boolean result = calendar.createEvent("Team Meeting", startDateTime, endDateTime,
        false, "Weekly sync", "Conference Room A", true);

    assertTrue(result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Team Meeting", events.get(0).getSubject());
    assertEquals("Weekly sync", events.get(0).getDescription());
    assertEquals("Conference Room A", events.get(0).getLocation());
    assertTrue(events.get(0).isPublic());
    assertFalse(events.get(0).isAllDay());
    assertFalse(events.get(0).isRecurring());
  }

  @Test
  public void testCreateEventWithNullSubject() {
    boolean result = calendar.createEvent(null, startDateTime, endDateTime,
        false, "Description", "Location", true);

    assertFalse("Should reject null event name", result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateEventWithEmptySubject() {
    boolean result = calendar.createEvent("", startDateTime, endDateTime,
        false, "Description", "Location", true);

    assertFalse("Should reject empty event name", result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateEventWithNullStartTime() {
    boolean result = calendar.createEvent("Meeting", null, endDateTime,
        false, "Description", "Location", true);

    assertFalse("Should reject null start time", result);
  }

  @Test
  public void testCreateEventWithEndTimeBeforeStart() {
    LocalDateTime earlyEnd = startDateTime.minusMinutes(30);
    boolean result = calendar.createEvent("Meeting", startDateTime, earlyEnd,
        false, "Description", "Location", true);

    assertFalse("Should reject end time before start time", result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateEventWithEqualStartAndEndTimes() {
    boolean result = calendar.createEvent("Meeting", startDateTime, startDateTime,
        false, "Description", "Location", true);

    assertFalse("Should reject equal start and end times", result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateEventWithVariousParameterCombinations() {
    // Test with null description and location
    boolean result = calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, null, null, true);

    assertTrue(result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
    assertEquals("", events.get(0).getDescription()); // Should default to empty string
    assertEquals("", events.get(0).getLocation()); // Should default to empty string

    // Clear events
    calendar = new CalendarImpl();

    // Test with empty description and location
    result = calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, "", "", false);

    assertTrue(result);

    events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
    assertEquals("", events.get(0).getDescription());
    assertEquals("", events.get(0).getLocation());
    assertFalse(events.get(0).isPublic()); // Test private event
  }

  @Test
  public void testCreateAllDayEventSuccess() {
    boolean result = calendar.createAllDayEvent("Conference", startDateTime,
        false, "Annual tech conference", "Convention Center", true);

    assertTrue(result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Conference", events.get(0).getSubject());
    assertTrue(events.get(0).isAllDay());
    assertFalse(events.get(0).isRecurring());
    assertEquals("Annual tech conference", events.get(0).getDescription());
    assertEquals("Convention Center", events.get(0).getLocation());
    assertTrue(events.get(0).isPublic());
  }

  @Test
  public void testCreateAllDayEventWithNullSubject() {
    boolean result = calendar.createAllDayEvent(null, startDateTime,
        false, "Description", "Location", true);

    assertFalse("Should reject null event name", result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateAllDayEventWithEmptySubject() {
    boolean result = calendar.createAllDayEvent("", startDateTime,
        false, "Description", "Location", true);

    assertFalse("Should reject empty event name", result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(0, events.size());
  }

  @Test
  public void testCreateAllDayEventWithNullDateTime() {
    boolean result = calendar.createAllDayEvent("Conference", null,
        false, "Description", "Location", true);

    assertFalse("Should reject null date time", result);
  }

  @Test
  public void testCreateRecurringEventSuccess() {
    boolean result = calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", 4, null, false, "Team updates",
        "Meeting Room B", true);

    assertTrue(result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Weekly Status", events.get(0).getSubject());
    assertTrue(events.get(0).isRecurring());
    assertFalse(events.get(0).isAllDay());
    assertEquals("Team updates", events.get(0).getDescription());
    assertEquals("Meeting Room B", events.get(0).getLocation());
  }

  @Test
  public void testCreateRecurringEventWithUntilDate() {
    LocalDateTime untilDate = LocalDateTime.of(2025, 4, 15, 0, 0);
    boolean result = calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", -1, untilDate, false, "Team updates",
        "Meeting Room B", true);

    assertTrue(result);

    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Weekly Status", events.get(0).getSubject());
    assertTrue(events.get(0).isRecurring());
  }

  @Test
  public void testCreateRecurringEventSpanningMultipleDays() {
    // Create an event that spans multiple days
    LocalDateTime multiDayEnd = startDateTime.plusDays(1).withHour(10).withMinute(0);
    boolean result = calendar.createRecurringEvent("Multi-day Meeting", startDateTime, multiDayEnd,
        "M", 4, null, false, "Description",
        "Location", true);

    assertFalse("Should reject recurring events spanning multiple days", result);
  }

//  @Test
//  public void testCreateRecurringAllDayEventSuccess() {
//    boolean result = calendar.createRecurringAllDayEvent("Training Day", startDateTime,
//        "F", 3, null, false,
//        "Monthly training", "Training Room", false);
//
//    assertTrue(result);
//
//    List<Event> events = calendar.getEventsOn(startDateTime);
//    assertEquals(1, events.size());
//    assertEquals("Training Day", events.get(0).getSubject());
//    assertTrue(events.get(0).isRecurring());
//    assertTrue(events.get(0).isAllDay());
//    assertEquals("Monthly training", events.get(0).getDescription());
//    assertEquals("Training Room", events.get(0).getLocation());
//    assertFalse(events.get(0).isPublic());
//  }

//  @Test
//  public void testCreateRecurringAllDayEventWithUntilDate() {
//    LocalDateTime untilDate = LocalDateTime.of(2025, 6, 30, 0, 0);
//    boolean result = calendar.createRecurringAllDayEvent("Training Day", startDateTime,
//        "F", -1, untilDate, false,
//        "Monthly training", "Training Room", false);
//
//    assertTrue(result);
//
//    List<Event> events = calendar.getEventsOn(startDateTime);
//    assertEquals(1, events.size());
//    assertEquals("Training Day", events.get(0).getSubject());
//    assertTrue(events.get(0).isRecurring());
//    assertTrue(events.get(0).isAllDay());
//  }

  @Test
  public void testCreateEventWithConflictAndAutoDeclineTrue() {
    // Create first event
    calendar.createEvent("First Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);

    // Create second overlapping event with autoDecline=true
    boolean result = calendar.createEvent("Second Meeting",
        LocalDateTime.of(2025, 3, 10, 14, 45),
        LocalDateTime.of(2025, 3, 10, 16, 0),
        true, "Another meeting", "Another room", true);

    assertFalse(result);

    // Check only the first event was created
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("First Meeting", events.get(0).getSubject());
  }

  @Test
  public void testCreateEventWithConflictAndAutoDeclineFalse() {
    // Create first event
    calendar.createEvent("First Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);

    // Create second overlapping event with autoDecline=false
    boolean result = calendar.createEvent("Second Meeting",
        LocalDateTime.of(2025, 3, 10, 14, 45),
        LocalDateTime.of(2025, 3, 10, 16, 0),
        false, "Another meeting", "Another room", true);

    assertTrue("Should allow conflicting event with autoDecline=false", result);

    // Both events should exist
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(2, events.size());
  }

  @Test
  public void testCreateAllDayEventWithConflictAndAutoDeclineTrue() {
    // Create an all-day event
    calendar.createAllDayEvent("Conference", startDateTime,
        false, "Description", "Location", true);

    // Try to create a regular event on the same day with autoDecline=true
    boolean result = calendar.createEvent("Meeting",
        LocalDateTime.of(2025, 3, 10, 10, 0),
        LocalDateTime.of(2025, 3, 10, 11, 0),
        true, "Description", "Location", true);

    assertFalse(result);

    // Only the all-day event should exist
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Conference", events.get(0).getSubject());
    assertTrue(events.get(0).isAllDay());
  }

  @Test
  public void testCreateAllDayEventWithConflictAndAutoDeclineFalse() {
    // Create an all-day event
    calendar.createAllDayEvent("Conference", startDateTime,
        false, "Description", "Location", true);

    // Try to create a regular event on the same day with autoDecline=false
    boolean result = calendar.createEvent("Meeting",
        LocalDateTime.of(2025, 3, 10, 10, 0),
        LocalDateTime.of(2025, 3, 10, 11, 0),
        false, "Description", "Location", true);

    assertTrue("Should allow conflicting event with autoDecline=false", result);

    // Both events should exist
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(2, events.size());
  }

  @Test
  public void testCreateRecurringEventWithConflictsAndAutoDeclineTrue() {
    // Create a one-time event
    calendar.createEvent("One-time Meeting",
        LocalDateTime.of(2025, 3, 17, 14, 0),
        LocalDateTime.of(2025, 3, 17, 15, 30),
        false, "Description", "Location", true);

    // Try to create a recurring event that would conflict with the above
    boolean result = calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", 4, null, true, "Description",
        "Location", true);

    assertFalse("Should not create recurring event with conflicts and autoDecline=true", result);

    // Only the original one-time event should exist
    List<Event> events = calendar.getEventsOn(LocalDateTime.of(2025, 3, 17, 0, 0));
    assertEquals(1, events.size());
    assertEquals("One-time Meeting", events.get(0).getSubject());
  }

  @Test
  public void testCreateRecurringEventWithConflictsAndAutoDeclineFalse() {
    // Create a one-time event
    calendar.createEvent("One-time Meeting",
        LocalDateTime.of(2025, 3, 17, 14, 0),
        LocalDateTime.of(2025, 3, 17, 15, 30),
        false, "Description", "Location", true);

    // Create a recurring event that would conflict with the above but with autoDecline=false
    boolean result = calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", 4, null, false, "Description",
        "Location", true);

    assertTrue("Should allow conflicting recurring event with autoDecline=false", result);

    // Both events should exist on the conflict date
    List<Event> events = calendar.getEventsOn(LocalDateTime.of(2025, 3, 17, 0, 0));
    assertEquals(2, events.size());
  }

//  @Test
//  public void testIsBusyDuringEvent() {
//    // No events yet
//    assertFalse(calendar.isBusy(startDateTime));
//
//    // Add an event
//    calendar.createEvent("Meeting", startDateTime, endDateTime,
//        false, "Description", "Location", true);
//
//    // Check busy status during event
//    assertTrue(calendar.isBusy(startDateTime)); // At start time
//    assertTrue(calendar.isBusy(startDateTime.plusMinutes(30))); // During event
//    assertTrue(calendar.isBusy(endDateTime.minusMinutes(1))); // Right before end
//
//    // Check non-busy times
//    assertFalse(calendar.isBusy(startDateTime.minusMinutes(1))); // Right before start
//    assertFalse(calendar.isBusy(endDateTime)); // At end time (end is exclusive)
//    assertFalse(calendar.isBusy(endDateTime.plusMinutes(1))); // After end
//    assertFalse(calendar.isBusy(startDateTime.plusDays(1))); // Different day
//  }

  @Test
  public void testIsBusyDuringAllDayEvent() {
    // Add an all-day event
    calendar.createAllDayEvent("Conference", startDateTime,
        false, "Description", "Location", true);

    // Check busy status throughout the day
    assertTrue(calendar.isBusy(startDateTime.withHour(0).withMinute(0))); // Midnight
    assertTrue(calendar.isBusy(startDateTime.withHour(8).withMinute(0))); // Morning
    assertTrue(calendar.isBusy(startDateTime.withHour(12).withMinute(0))); // Noon
    assertTrue(calendar.isBusy(startDateTime.withHour(18).withMinute(0))); // Evening
    assertTrue(calendar.isBusy(startDateTime.withHour(23).withMinute(59))); // End of day

    // Check non-busy times
    assertFalse(calendar.isBusy(startDateTime.minusDays(1))); // Day before
    assertFalse(calendar.isBusy(startDateTime.plusDays(1))); // Day after
  }

//  @Test
//  public void testIsBusyWithRecurringEvent() {
//    // Add a recurring event (Mondays)
//    calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
//        "M", 4, null, false, "Description",
//        "Location", true);
//
//    // Check busy status for the recurrences
//    assertTrue(calendar.isBusy(startDateTime)); // First occurrence (March 10, 2025)
//    assertTrue(calendar.isBusy(startDateTime.plusDays(7))); // Second occurrence (March 17)
//    assertTrue(calendar.isBusy(startDateTime.plusDays(14))); // Third occurrence (March 24)
//    assertTrue(calendar.isBusy(startDateTime.plusDays(21))); // Fourth occurrence (March 31)
//
//    // Check non-busy times
//    assertFalse(calendar.isBusy(startDateTime.plusDays(1))); // Tuesday
//    assertFalse(calendar.isBusy(startDateTime.plusDays(28))); // Beyond occurrences (April 7)
//  }

  @Test
  public void testEditEventNameSuccess() {
    // Create an event
    calendar.createEvent("Original Name", startDateTime, endDateTime,
        false, "Original description", "Original location", true);

    // Edit the event
    boolean result = calendar.editEvent("name", "Original Name",
        startDateTime, endDateTime, "Updated Name");

    assertTrue(result);

    // Verify the event was updated
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Updated Name", events.get(0).getSubject());
    assertEquals("Original description", events.get(0).getDescription());
    assertEquals("Original location", events.get(0).getLocation());
  }

  @Test
  public void testEditEventDescriptionSuccess() {
    // Create an event
    calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, "Original description", "Original location", true);

    // Edit the event
    boolean result = calendar.editEvent("description", "Meeting",
        startDateTime, endDateTime, "Updated description");

    assertTrue(result);

    // Verify the event was updated
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
    assertEquals("Updated description", events.get(0).getDescription());
    assertEquals("Original location", events.get(0).getLocation());
  }

  @Test
  public void testEditEventLocationSuccess() {
    // Create an event
    calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, "Description", "Original location", true);

    // Edit the event
    boolean result = calendar.editEvent("location", "Meeting",
        startDateTime, endDateTime, "Updated location");

    assertTrue(result);

    // Verify the event was updated
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
    assertEquals("Description", events.get(0).getDescription());
    assertEquals("Updated location", events.get(0).getLocation());
  }

  @Test
  public void testEditEventPublicFlagSuccess() {
    // Create a public event
    calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);

    // Make it private
    boolean result = calendar.editEvent("public", "Meeting",
        startDateTime, endDateTime, "false");

    assertTrue(result);

    // Verify the event was updated
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertFalse(events.get(0).isPublic());

    // Make it public again
    result = calendar.editEvent("public", "Meeting",
        startDateTime, endDateTime, "true");

    assertTrue(result);

    // Verify the event was updated
    events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertTrue(events.get(0).isPublic());
  }

  @Test
  public void testEditEventInvalidProperty() {
    // Create an event
    calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);

    // Try to edit an invalid property
    boolean result = calendar.editEvent("invalidProperty", "Meeting",
        startDateTime, endDateTime, "New Value");

    assertFalse("Should reject invalid property name", result);
  }

  @Test
  public void testEditNonExistentEvent() {
    boolean result = calendar.editEvent("name", "Nonexistent Event",
        startDateTime, endDateTime, "New Name");

    assertFalse(result);
  }

  @Test
  public void testEditEventWithQuotedName() {
    // Create an event
    calendar.createEvent("Meeting with \"Quotes\"", startDateTime, endDateTime,
        false, "Description", "Location", true);

    // Edit using quoted name
    boolean result = calendar.editEvent("name", "\"Meeting with \"Quotes\"\"",
        startDateTime, endDateTime, "Updated Meeting");

    assertTrue("Should handle quoted event names", result);

    // Verify the event was updated
    List<Event> events = calendar.getEventsOn(startDateTime);
    assertEquals(1, events.size());
    assertEquals("Updated Meeting", events.get(0).getSubject());
  }

  @Test
  public void testEditAllEvents() {
    // Create two events with the same name
    calendar.createEvent("Recurring Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);
    calendar.createEvent("Recurring Meeting", nextDayDateTime,
        nextDayDateTime.plusHours(1),
        false, "Description", "Location", true);

    // Edit all events with this name
    boolean result = calendar.editAllEvents("location", "Recurring Meeting", "New Location");

    assertTrue(result);

    // Verify both events were updated
    List<Event> day1Events = calendar.getEventsOn(startDateTime);
    List<Event> day2Events = calendar.getEventsOn(nextDayDateTime);

    assertEquals(1, day1Events.size());
    assertEquals(1, day2Events.size());
    assertEquals("New Location", day1Events.get(0).getLocation());
    assertEquals("New Location", day2Events.get(0).getLocation());
  }

  @Test
  public void testEditAllEventsWithNoMatchingEvents() {
    boolean result = calendar.editAllEvents("name", "Nonexistent Event", "New Name");

    assertFalse("Should return false when no events match", result);
  }

  @Test
  public void testEditAllEventsWithInvalidProperty() {
    // Create an event
    calendar.createEvent("Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);

    // Try to edit an invalid property
    boolean result = calendar.editAllEvents("invalidProperty", "Meeting", "New Value");

    assertFalse("Should reject invalid property name", result);
  }

  @Test
  public void testEditEventsFrom() {
    // Create a recurring event
    calendar.createRecurringEvent("Weekly Meeting", startDateTime, endDateTime,
        "M", 4, null, false, "Description",
        "Old Location", true);

    // Define a date for editing from (after first occurrence)
    LocalDateTime editFromDate = LocalDateTime.of(2025, 3, 17, 0, 0);

    // Edit events from this date
    boolean result = calendar.editEventsFrom("location", "Weekly Meeting",
        editFromDate, "New Location");

    assertTrue(result);

    // Verify events after editFromDate have new location, and events before don't
    List<Event> allEvents = calendar.getEventsFrom(
        LocalDateTime.of(2025, 3, 1, 0, 0),
        LocalDateTime.of(2025, 4, 30, 0, 0));

    // Should have at least 2 events (possibly more due to implementation details)
    assertTrue(allEvents.size() >= 2);

    // Check that recurrence was split correctly
    boolean foundOldLocation = false;
    boolean foundNewLocation = false;

    for (Event event : allEvents) {
      if (event.getSubject().equals("Weekly Meeting")) {
        if (event.getLocation().equals("Old Location")) {
          foundOldLocation = true;
        } else if (event.getLocation().equals("New Location")) {
          foundNewLocation = true;
        }
      }
    }

    assertTrue("Should find event with old location", foundOldLocation);
    assertTrue("Should find event with new location", foundNewLocation);
  }

  @Test
  public void testEditEventsFromWithNoMatchingEvents() {
    boolean result = calendar.editEventsFrom("location", "Nonexistent Event",
        startDateTime, "New Location");

    assertFalse("Should return false when no events match", result);
  }

  @Test
  public void testEditEventsFromWithInvalidProperty() {
    // Create a recurring event
    calendar.createRecurringEvent("Weekly Meeting", startDateTime, endDateTime,
        "M", 4, null, false, "Description",
        "Location", true);

    // Try to edit an invalid property
    boolean result = calendar.editEventsFrom("invalidProperty", "Weekly Meeting",
        startDateTime.plusDays(7), "New Value");

    assertFalse("Should reject invalid property name", result);
  }

  @Test
  public void testGetEventsOnWithNoEvents() {
    List<Event> events = calendar.getEventsOn(startDateTime);

    assertNotNull("Should return empty list, not null", events);
    assertTrue("Should return empty list when no events exist", events.isEmpty());
  }

  @Test
  public void testGetEventsOnWithMultipleEvents() {
    // Create multiple events on the same day
    calendar.createEvent("Morning Meeting",
        startDateTime.withHour(9).withMinute(0),
        startDateTime.withHour(10).withMinute(0),
        false, "Description", "Location", true);
    calendar.createEvent("Afternoon Meeting",
        startDateTime.withHour(14).withMinute(0),
        startDateTime.withHour(15).withMinute(0),
        false, "Description", "Location", true);
    calendar.createAllDayEvent("Conference", startDateTime,
        false, "Description", "Location", true);

    // Get events for this day
    List<Event> events = calendar.getEventsOn(startDateTime);

    assertEquals(3, events.size());

    // Check each event is returned
    boolean foundMorning = false;
    boolean foundAfternoon = false;
    boolean foundConference = false;

    for (Event event : events) {
      if (event.getSubject().equals("Morning Meeting")) {
        foundMorning = true;
      } else if (event.getSubject().equals("Afternoon Meeting")) {
        foundAfternoon = true;
      } else if (event.getSubject().equals("Conference")) {
        foundConference = true;
      }
    }

    assertTrue(foundMorning);
    assertTrue(foundAfternoon);
    assertTrue(foundConference);
  }

  @Test
  public void testGetEventsOnWithRecurringEvent() {
    // Create a recurring event (Mondays)
    calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", 4, null, false, "Description",
        "Location", true);

    // First occurrence
    List<Event> firstWeekEvents = calendar.getEventsOn(startDateTime);
    assertEquals(1, firstWeekEvents.size());
    assertEquals("Weekly Status", firstWeekEvents.get(0).getSubject());

    // Second occurrence
    List<Event> secondWeekEvents = calendar.getEventsOn(startDateTime.plusDays(7));
    assertEquals(1, secondWeekEvents.size());
    assertEquals("Weekly Status", secondWeekEvents.get(0).getSubject());

    // No events on a different day (Tuesday)
    List<Event> tuesdayEvents = calendar.getEventsOn(startDateTime.plusDays(1));
    assertEquals(0, tuesdayEvents.size());
  }

  @Test
  public void testGetEventsFromWithDateRange() {
    // Create events on different days
    calendar.createEvent("Day 1 Meeting", startDateTime, endDateTime,
        false, "Description", "Location", true);
    calendar.createEvent("Day 2 Meeting", nextDayDateTime,
        nextDayDateTime.plusHours(1),
        false, "Description", "Location", true);
    calendar.createEvent("Day 3 Meeting", nextDayDateTime.plusDays(1),
        nextDayDateTime.plusDays(1).plusHours(1),
        false, "Description", "Location", true);

    // Get events for a date range that includes all three days
    List<Event> events = calendar.getEventsFrom(
        startDateTime.withHour(0).withMinute(0),
        nextDayDateTime.plusDays(1).withHour(23).withMinute(59));

    assertEquals(3, events.size());

    // Get events for a date range that includes only first two days
    events = calendar.getEventsFrom(
        startDateTime.withHour(0).withMinute(0),
        nextDayDateTime.withHour(23).withMinute(59));

    assertEquals(2, events.size());

    // Get events for a date range that includes only the middle day
    events = calendar.getEventsFrom(
        nextDayDateTime.withHour(0).withMinute(0),
        nextDayDateTime.withHour(23).withMinute(59));

    assertEquals(1, events.size());
    assertEquals("Day 2 Meeting", events.get(0).getSubject());
  }

  @Test
  public void testGetEventsFromWithNoEvents() {
    List<Event> events = calendar.getEventsFrom(
        startDateTime,
        startDateTime.plusDays(7));

    assertNotNull("Should return empty list, not null", events);
    assertTrue("Should return empty list when no events exist", events.isEmpty());
  }

  @Test
  public void testGetEventsFromWithRecurringEvent() {
    // Create a recurring event (Mondays for 4 weeks)
    calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", 4, null, false, "Description",
        "Location", true);

    // Get events for the full month
    List<Event> events = calendar.getEventsFrom(
        LocalDateTime.of(2025, 3, 1, 0, 0),
        LocalDateTime.of(2025, 3, 31, 23, 59));

    // Should only return the recurring event once, not once per occurrence
    assertEquals(1, events.size());
    assertEquals("Weekly Status", events.get(0).getSubject());
    assertTrue(events.get(0).isRecurring());
  }

  @Test
  public void testExportToCSVBasic() {
    // Create a few events
    calendar.createEvent("Regular Meeting", startDateTime, endDateTime,
        false, "Regular meeting desc", "Room A", true);
    calendar.createAllDayEvent("All Day Event", nextDayDateTime,
        false, "All day event desc", "Room B", false);

    // Export to CSV
    String filePath = calendar.exportToCSV(exportFileName);

    assertNotNull(filePath);

    // Verify file exists
    File exportFile = new File(filePath);
    assertTrue("Export file does not exist: " + filePath, exportFile.exists());

    try {
      // Read file content to verify structure
      List<String> lines = Files.readAllLines(exportFile.toPath(), StandardCharsets.UTF_8);
      assertTrue("Not enough lines in export file", lines.size() >= 3); // Header + 2 events

      // Check header
      String header = lines.get(0);
      assertTrue("Header missing Subject", header.contains("Subject"));
      assertTrue("Header missing Start Date", header.contains("Start Date"));
      assertTrue("Header missing Start Time", header.contains("Start Time"));
      assertTrue("Header missing End Date", header.contains("End Date"));
      assertTrue("Header missing End Time", header.contains("End Time"));
      assertTrue("Header missing All Day Event", header.contains("All Day Event"));
      assertTrue("Header missing Description", header.contains("Description"));
      assertTrue("Header missing Location", header.contains("Location"));
      assertTrue("Header missing Private", header.contains("Private"));

      // Check regular event
      boolean foundRegularEvent = false;
      boolean foundAllDayEvent = false;

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      String startDateStr = startDateTime.format(dateFormatter);
      String nextDayDateStr = nextDayDateTime.format(dateFormatter);

      for (int i = 1; i < lines.size(); i++) {
        String line = lines.get(i);

        if (line.contains("Regular Meeting")) {
          foundRegularEvent = true;
          assertTrue("Regular event missing date", line.contains(startDateStr));
          assertTrue("Regular event missing description", line.contains("Regular meeting desc"));
          assertTrue("Regular event missing location", line.contains("Room A"));
          assertTrue("Regular event should be public", line.contains("False")); // Private field
        } else if (line.contains("All Day Event")) {
          foundAllDayEvent = true;
          assertTrue("All day event missing date", line.contains(nextDayDateStr));
          assertTrue("All day event missing description", line.contains("All day event desc"));
          assertTrue("All day event missing location", line.contains("Room B"));
          assertTrue("All day event should be private", line.contains("True")); // Private field
        }
      }

      assertTrue("Regular event not found in export", foundRegularEvent);
      assertTrue("All day event not found in export", foundAllDayEvent);

    } catch (Exception e) {
      fail("Error reading export file: " + e.getMessage());
    }
  }

  @Test
  public void testExportToCSVWithRecurringEvents() {
    // Create a recurring event (Mondays for 4 weeks)
    calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
        "M", 4, null, false, "Weekly meeting",
        "Room A", true);

    // Export to CSV
    String filePath = calendar.exportToCSV(exportFileName);

    assertNotNull(filePath);
    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());

    try {
      // Read file content
      List<String> lines = Files.readAllLines(exportFile.toPath(), StandardCharsets.UTF_8);

      // Should have header + 4 occurrences of the recurring event
      assertTrue("Not enough occurrences in export file", lines.size() >= 5);

      // Count occurrences of "Weekly Status"
      int weeklyStatusCount = 0;
      for (int i = 1; i < lines.size(); i++) {
        if (lines.get(i).contains("Weekly Status")) {
          weeklyStatusCount++;
        }
      }

      assertEquals("Should have 4 occurrences of recurring event", 4, weeklyStatusCount);

    } catch (Exception e) {
      fail("Error reading export file: " + e.getMessage());
    }
  }

  @Test
  public void testExportToCSVWithSpecialCharacters() {
    // Create events with special characters in fields
    calendar.createEvent("Meeting with \"Quotes\"", startDateTime, endDateTime,
        false, "Description with, comma", "Room C", true);
    calendar.createEvent("Meeting with, Comma", nextDayDateTime, nextDayDateTime.plusHours(1),
        false, "Description with \"quotes\"", "Room D", true);

    // Export to CSV
    String filePath = calendar.exportToCSV(exportFileName);

    assertNotNull(filePath);
    File exportFile = new File(filePath);
    assertTrue(exportFile.exists());

    try {
      // Read raw file content
      String content = new String(Files.readAllBytes(exportFile.toPath()), StandardCharsets.UTF_8);

      // Verify quotes and commas are properly escaped
      assertTrue("Event with quotes not exported correctly",
          content.contains("\"Meeting with \"\"Quotes\"\"\"") ||
              content.contains("Meeting with \"Quotes\""));
      assertTrue("Event with comma not exported correctly",
          content.contains("\"Meeting with, Comma\""));
      assertTrue("Description with comma not exported correctly",
          content.contains("\"Description with, comma\""));
      assertTrue("Description with quotes not exported correctly",
          content.contains("\"Description with \"\"quotes\"\"\"") ||
              content.contains("Description with \"quotes\""));

    } catch (Exception e) {
      fail("Error reading export file: " + e.getMessage());
    }
  }

  @Test
  public void testExportToCSVInvalidFileName() {
    // Try to export to an invalid file path
    String result = calendar.exportToCSV("");

    // Implementation might handle this differently, but should not succeed
    if (result != null) {
      File file = new File(result);
      assertFalse("Should not create file with empty name", file.exists());
    }
  }
}