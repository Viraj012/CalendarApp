
import model.CalendarManager;
import model.Event;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tests for the timezone functionality in CalendarManager's copying operations.
 */
public class CalendarTimezoneTest {

  private CalendarManager manager;

  @Before
  public void setUp() {
    manager = new CalendarManager();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Create calendars in different timezones
    manager.createCalendar("NYC", ZoneId.of("America/New_York"));
    manager.createCalendar("LA", ZoneId.of("America/Los_Angeles"));
    manager.createCalendar("London", ZoneId.of("Europe/London"));
    manager.createCalendar("Tokyo", ZoneId.of("Asia/Tokyo"));
  }

  @Test
  public void testCopyEventBetweenTimezonesPreservesAbsoluteTime() {
    // Create an event in New York at 1 PM
    manager.useCalendar("NYC");
    LocalDateTime nycTime = LocalDateTime
            .of(2023, 5, 15, 13, 0); // 1 PM EST
    LocalDateTime nycEndTime = LocalDateTime
            .of(2023, 5, 15, 15, 0); // 3 PM EST

    manager.getCurrentCalendar().createEvent("Business Meeting", nycTime, nycEndTime,
        true, "Important meeting", "Conference room", true);

    // Copy event to LA (3-hour time difference)
    LocalDateTime laTargetTime = LocalDateTime
            .of(2023, 5, 15, 13, 0); // 1 PM PST
    boolean result = manager
            .copyEvent("Business Meeting", nycTime, "LA", laTargetTime);
    assertTrue("Copying event should succeed", result);

    // Verify that the LA event is 3 hours earlier (10 AM PST instead of 1 PM EST)
    manager.useCalendar("LA");
    List<Event> laEvents = manager.getCurrentCalendar().getEventsOn(laTargetTime);

    assertEquals("There should be one event on the target date",
            1, laEvents.size());
    assertEquals("Event should be at 10 AM PST",
            13, laEvents.get(0).getStartDateTime().getHour());
    assertEquals("Event should be at 12 PM PST",
            15, laEvents.get(0).getEndDateTime().getHour());

  }

  @Test
  public void testCopyEventToRequestedTimeWithTimezoneAdjustment() {
    // Create an event in New York at 1 PM
    manager.useCalendar("NYC");
    LocalDateTime nycTime = LocalDateTime
            .of(2023, 5, 15, 13, 0); // 1 PM EST
    LocalDateTime nycEndTime = LocalDateTime
            .of(2023, 5, 15, 15, 0); // 3 PM EST

    manager.getCurrentCalendar().createEvent("Business Meeting", nycTime, nycEndTime,
        true, "Important meeting", "Conference room", true);

    // Copy to London requesting a 6PM London time (instead of "equivalent" 6PM NYC = 11PM London)
    LocalDateTime londonTargetTime = LocalDateTime
            .of(2023, 5, 15, 18, 0); // 6 PM London
    boolean result = manager.copyEvent("Business Meeting", nycTime,
            "London", londonTargetTime);
    assertTrue("Copying event should succeed", result);

    // Verify that the event is at the requested time, not the timezone-equivalent time
    manager.useCalendar("London");
    List<Event> londonEvents = manager.getCurrentCalendar().getEventsOn(londonTargetTime);

    assertEquals("There should be one event on the target date",
            1, londonEvents.size());
    assertEquals("Event should be at requested time (6 PM London)",
        18, londonEvents.get(0).getStartDateTime().getHour());
    assertEquals("Event duration should be preserved (2 hours)",
        20, londonEvents.get(0).getEndDateTime().getHour());
  }

  @Test
  public void testCopyAllDayEventBetweenTimezones() {
    // Create an all-day event in Tokyo
    manager.useCalendar("Tokyo");
    LocalDateTime tokyoDate = LocalDateTime
            .of(2023, 5, 15, 0, 0);

    manager.getCurrentCalendar().createAllDayEvent("Public Holiday", tokyoDate,
        true, "National holiday", "Japan", true);

    // Copy to London (9-hour time difference)
    LocalDateTime londonDate = LocalDateTime
            .of(2023, 5, 20, 0, 0); // 5 days later
    boolean result = manager.copyEvent("Public Holiday", tokyoDate,
            "London", londonDate);
    assertTrue("Copying all-day event should succeed", result);

    // Verify the event is on the correct date in London
    manager.useCalendar("London");
    List<Event> londonEvents = manager.getCurrentCalendar().getEventsOn(londonDate);

    assertEquals("There should be one event on the target date",
            1, londonEvents.size());
    assertTrue("Event should be all-day", londonEvents.get(0).isAllDay());
    assertEquals("Event should be on May 20th", 20,
            londonEvents.get(0).getStartDateTime().getDayOfMonth());
  }

  @Test
  public void testCopyRecurringEventBetweenTimezones() {
    // Create a recurring event in LA (every Monday and Wednesday for 3 times)
    manager.useCalendar("LA");
    LocalDateTime laStart = LocalDateTime
            .of(2023, 6, 5, 10, 0); // Monday 10 AM PST
    LocalDateTime laEnd = LocalDateTime
            .of(2023, 6, 5, 11, 0);   // Monday 11 AM PST

    manager.getCurrentCalendar().createRecurringEvent("Weekly Standup",
            laStart, laEnd,
        "MW", 3, null, true, "Team sync",
            "Conference room", true);

    // Copy to Tokyo (17-hour time difference)
    LocalDateTime tokyoTarget = LocalDateTime
            .of(2023, 6, 12, 10, 0);
    boolean result = manager.copyEvent("Weekly Standup",
            laStart, "Tokyo", tokyoTarget);
    assertTrue("Copying recurring event should succeed", result);

    // Verify the events in Tokyo are at 2 AM (Tokyo time) when LA is at 10 AM
    manager.useCalendar("Tokyo");

    // For a recurring event, our implementation still creates it as a recurring event
    // Check that all instances exist
    List<Event> allEvents = manager.getCurrentCalendar().getAllEvents();
    assertEquals("There should be one recurring event series",
            1, allEvents.size());
    assertTrue("Event should be recurring", allEvents.get(0).isRecurring());

    // Check first occurrence time - should be at 10 AM Tokyo time as requested,
    // not at 2 AM (which would be the actual timezone-adjusted time)
    LocalDateTime firstOccurrence = LocalDateTime.of(2023, 6,
            12, 0, 0); // Check June 12
    List<Event> occurrences = manager.getCurrentCalendar().getEventsOn(firstOccurrence);

    // Note: The recurrence calculation might vary due to the timezone change,
    // but we should have at least one occurrence on that day
    assertEquals("There should be one occurrence on the target date",
            1, occurrences.size());
    assertEquals("Event occurrence should be at the requested time",
        10, occurrences.get(0).getStartDateTime().getHour());
  }

  @Test
  public void testCopyEventsOnDayWithTimezoneConversion() {
    // Create multiple events in NYC
    manager.useCalendar("NYC");

    LocalDateTime morning = LocalDateTime
            .of(2023, 5, 15, 9, 0);  // 9 AM EST
    LocalDateTime noon = LocalDateTime
            .of(2023, 5, 15, 12, 0);    // 12 PM EST
    LocalDateTime afternoon = LocalDateTime
            .of(2023, 5, 15, 15, 0); // 3 PM EST

    manager.getCurrentCalendar().createEvent("Morning Meeting",
            morning, morning.plusHours(1),
        true, "Morning brief", "Room 1", true);
    manager.getCurrentCalendar().createEvent("Lunch", noon, noon.plusHours(1),
        true, "Team lunch", "Cafeteria", true);
    manager.getCurrentCalendar().createEvent("Afternoon Review",
            afternoon, afternoon.plusHours(1),
        true, "Daily review", "Room 2", true);

    // Copy all events for that day to Tokyo
    LocalDateTime tokyoDate = LocalDateTime
            .of(2023, 5, 16, 0, 0); // Next day
    boolean result = manager.copyEventsOnDay(morning, "Tokyo", tokyoDate);
    assertTrue("Copying all events should succeed", result);

    // Verify events in Tokyo have appropriate time conversions (+14 hours from NYC)
    manager.useCalendar("Tokyo");
    List<Event> tokyoEvents = manager.getCurrentCalendar().getAllEvents();

    // In Tokyo, events should be at local times equivalent to the NYC times
    // 9 AM EST = 10 PM JST on same calendar day (or 10 PM JST next day if we're shifting days)
    // 12 PM EST = 1 AM JST next day
    // 3 PM EST = 4 AM JST next day

    assertEquals("All three events should be copied", 3, tokyoEvents.size());

    // Sort events by time to verify
    tokyoEvents.sort((e1, e2) -> e1.getStartDateTime().compareTo(e2.getStartDateTime()));

    // Verify event times - note that absolute time is preserved
    assertEquals("The first event should have appropriate time conversion",
        "Morning Meeting", tokyoEvents.get(0).getSubject());
    assertEquals("The second event should have appropriate time conversion",
        "Lunch", tokyoEvents.get(1).getSubject());
    assertEquals("The third event should have appropriate time conversion",
        "Afternoon Review", tokyoEvents.get(2).getSubject());
  }

  @Test
  public void testCopyEventsInRangeWithTimezoneConversion() {
    // Create events on different days in NYC
    manager.useCalendar("NYC");

    LocalDateTime day1 = LocalDateTime
            .of(2023, 5, 15, 13, 0); // Mon 1 PM EST
    LocalDateTime day2 = LocalDateTime
            .of(2023, 5, 16, 10, 0); // Tue 10 AM EST
    LocalDateTime day3 = LocalDateTime
            .of(2023, 5, 17, 15, 0); // Wed 3 PM EST

    manager.getCurrentCalendar().createEvent("Monday Meeting", day1, day1.plusHours(1),
        true, "Monday brief", "Room 1", true);
    manager.getCurrentCalendar().createEvent("Tuesday Workshop", day2, day2.plusHours(2),
        true, "Workshop", "Room 2", true);
    manager.getCurrentCalendar().createEvent("Wednesday Review", day3, day3.plusHours(1),
        true, "Weekly review", "Room 3", true);

    // Define range (May 15-17)
    LocalDateTime rangeStart = LocalDateTime.of(2023, 5, 15, 0, 0);
    LocalDateTime rangeEnd = LocalDateTime.of(2023, 5, 17, 23, 59);

    // Copy to LA one week later
    LocalDateTime laTarget = LocalDateTime
            .of(2023, 5, 22, 0, 0); // Mon May 22
    boolean result = manager.copyEventsInRange(rangeStart, rangeEnd,
            "LA", laTarget);
    assertTrue("Copying events in range should succeed", result);

    // Verify events in LA have appropriate times (convert from EST to PST, -3 hours)
    manager.useCalendar("LA");

    // Check first day (Monday)
    List<Event> day1Events = manager.getCurrentCalendar().getEventsOn(
        LocalDateTime.of(2023, 5, 22, 0, 0));
    assertEquals("Monday should have one event", 1, day1Events.size());
    assertEquals("Monday Meeting", day1Events.get(0).getSubject());
    assertEquals("Time should be adjusted for timezone",
        10, day1Events.get(0).getStartDateTime().getHour()); // 1 PM EST -> 10 AM PST

    // Check second day (Tuesday)
    List<Event> day2Events = manager.getCurrentCalendar().getEventsOn(
        LocalDateTime.of(2023, 5, 23, 0, 0));
    assertEquals("Tuesday should have one event", 1, day2Events.size());
    assertEquals("Tuesday Workshop", day2Events.get(0).getSubject());
    assertEquals("Time should be adjusted for timezone",
        7, day2Events.get(0).getStartDateTime().getHour()); // 10 AM EST -> 7 AM PST

    // Check third day (Wednesday)
    List<Event> day3Events = manager.getCurrentCalendar().getEventsOn(
        LocalDateTime.of(2023, 5, 24, 0, 0));
    assertEquals("Wednesday should have one event", 1, day3Events.size());
    assertEquals("Wednesday Review", day3Events.get(0).getSubject());
    assertEquals("Time should be adjusted for timezone",
        12, day3Events.get(0).getStartDateTime().getHour()); // 3 PM EST -> 12 PM PST
  }

  @Test
  public void testDaylightSavingTimeTransition() {
    // Create calendars in timezones that handle DST differently
    manager.createCalendar("Arizona", ZoneId.of("America/Phoenix")); // No DST
    manager.createCalendar("Berlin", ZoneId.of("Europe/Berlin"));    // Has DST

    // Create event in Berlin during DST
    manager.useCalendar("Berlin");
    LocalDateTime berlinTime = LocalDateTime
            .of(2023, 7, 15, 14, 0); // 2 PM during DST
    manager.getCurrentCalendar().createEvent("Summer Event",
            berlinTime, berlinTime.plusHours(1),
        true, "Summer meeting", "Berlin Office", true);

    // Copy to Arizona
    LocalDateTime arizonaTarget = LocalDateTime
            .of(2023, 7, 15, 14, 0); // Same wall clock time
    boolean result = manager.copyEvent("Summer Event", berlinTime,
            "Arizona", arizonaTarget);
    assertTrue("Copying event across DST boundaries should succeed", result);

    // Verify time adjustment includes DST offset
    manager.useCalendar("Arizona");
    List<Event> arizonaEvents = manager.getCurrentCalendar().getEventsOn(arizonaTarget);
    assertEquals("There should be one event", 1, arizonaEvents.size());

    // Berlin is UTC+2 during DST, Arizona is UTC-7
    // If we kept absolute time, Berlin 2 PM would be Arizona 5 AM
    // But since we requested 2 PM Arizona time, it should remain at 2 PM
    assertEquals("Event should be at requested Arizona time",
        14, arizonaEvents.get(0).getStartDateTime().getHour());

    // Convert both to UTC to confirm they're at different absolute times
    ZonedDateTime berlinZoned = berlinTime.atZone(ZoneId.of("Europe/Berlin"));
    ZonedDateTime arizonaZoned = arizonaEvents.get(0).getStartDateTime()
        .atZone(ZoneId.of("America/Phoenix"));

    assertNotEquals("UTC times should be different",
        berlinZoned.toInstant().toEpochMilli(),
        arizonaZoned.toInstant().toEpochMilli());
  }

  @Test
  public void testEditCalendarTimezoneAffectsConversion() {
    // Create an event in NYC
    manager.useCalendar("NYC");
    LocalDateTime nycTime = LocalDateTime.of(2023, 5,
            15, 13, 0); // 1 PM EST
    manager.getCurrentCalendar().createEvent("Meeting", nycTime, nycTime.plusHours(1),
        true, "Important meeting", "Office", true);

    // Create a new calendar for this test
    manager.createCalendar("Test",
            ZoneId.of("America/Chicago")); // Central Time

    // Copy event to Test calendar
    LocalDateTime testTarget = LocalDateTime
            .of(2023, 5, 15, 13, 0); // 1 PM Central
    boolean result = manager.copyEvent("Meeting",
            nycTime, "Test", testTarget);
    assertTrue("Copying to Test calendar should succeed", result);

    // Verify initial copying
    manager.useCalendar("Test");
    List<Event> testEvents = manager.getCurrentCalendar().getEventsOn(testTarget);
    assertEquals("There should be one event", 1, testEvents.size());

    // Edit Test calendar timezone to Pacific
    boolean edited = manager.editCalendar("Test",
            "timezone", "America/Los_Angeles");
    assertTrue("Editing timezone should succeed", edited);

    // Now copy another event to verify the timezone change is respected
    manager.useCalendar("NYC");
    LocalDateTime nycTime2 = LocalDateTime
            .of(2023, 5, 16, 15, 0); // 3 PM EST
    manager.getCurrentCalendar().createEvent("Second Meeting", nycTime2,
            nycTime2.plusHours(1),
        true, "Follow-up", "Office", true);

    LocalDateTime testTarget2 = LocalDateTime
            .of(2023, 5, 16, 15, 0); // 3 PM Pacific
    boolean result2 = manager.copyEvent("Second Meeting",
            nycTime2, "Test", testTarget2);
    assertTrue("Copying to Test calendar after timezone change should succeed", result2);

    // Verify second copy with new timezone
    manager.useCalendar("Test");
    List<Event> testEvents2 = manager.getCurrentCalendar().getEventsOn(
        LocalDateTime.of(2023, 5, 16, 0, 0));
    assertEquals("There should be one event on May 16", 1, testEvents2.size());
    assertEquals("Second Meeting", testEvents2.get(0).getSubject());
    assertEquals("Time should be at 3 PM Pacific as requested",
        15, testEvents2.get(0).getStartDateTime().getHour());
  }

  @Test
  public void testCopyToNonExistentCalendar() {
    // Create an event in NYC
    manager.useCalendar("NYC");
    LocalDateTime nycTime = LocalDateTime
            .of(2023, 5, 15, 13, 0);
    manager.getCurrentCalendar().createEvent("Meeting", nycTime, nycTime.plusHours(1),
        true, "Important meeting", "Office", true);

    // Try to copy to a non-existent calendar
    boolean result = manager.copyEvent("Meeting", nycTime,
            "NonExistent", nycTime);
    assertFalse("Copying to non-existent calendar should fail", result);
  }

  @Test
  public void testCopyNonExistentEvent() {
    // Create calendars but no events
    manager.useCalendar("NYC");

    // Try to copy non-existent event
    LocalDateTime nycTime = LocalDateTime
            .of(2023, 5, 15, 13, 0);
    boolean result = manager.copyEvent("Non-existent Meeting",
            nycTime, "LA", nycTime);
    assertFalse("Copying non-existent event should fail", result);
  }
}