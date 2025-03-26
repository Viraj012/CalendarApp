import controller.CommandProcessor;
import model.Calendar;
import model.CalendarManager;
import model.Event;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import view.TextUI;

/**
 * Tests for calendar timezone editing functionality.
 * These tests verify that when a calendar's timezone is changed,
 * all events in that calendar have their times adjusted accordingly.
 */
public class EditTimezoneTest {

  private CalendarManager manager;
  private CommandProcessor processor;
  private MockTextUI mockUI;

  @Before
  public void setUp() {
    manager = new CalendarManager();
    mockUI = new MockTextUI();
    processor = new CommandProcessor(manager, mockUI);

    // Create a default calendar in New York timezone
    manager.createCalendar("Default", ZoneId.of("America/New_York"));
    manager.useCalendar("Default");
  }

  /**
   * Test that regular events are adjusted correctly when timezone changes.
   */
  @Test
  public void testEditTimezoneRegularEvents() {
    // Create a few events in New York timezone
    Calendar calendar = manager.getCurrentCalendar();
    LocalDateTime morning = LocalDateTime
            .of(2023, 7, 10, 9, 0);  // 9 AM ET
    LocalDateTime noon = LocalDateTime
            .of(2023, 7, 10, 12, 0);    // 12 PM ET

    calendar.createEvent("Morning Meeting", morning, morning.plusHours(1),
            true, "Morning brief", "Room 1", true);
    calendar.createEvent("Lunch", noon, noon.plusHours(1),
            true, "Team lunch", "Cafeteria", true);

    // Verify events are created correctly in NY timezone
    List<Event> eventsBeforeChange = calendar.getEventsOn(morning);
    assertEquals("Should find all events on the date",
            2, eventsBeforeChange.size());

    // Find events by name
    Event morningEvent = findEventByName(eventsBeforeChange, "Morning Meeting");
    Event noonEvent = findEventByName(eventsBeforeChange, "Lunch");

    assertNotNull("Morning event should exist", morningEvent);
    assertNotNull("Noon event should exist", noonEvent);

    // Verify original times
    assertEquals(9, morningEvent.getStartDateTime().getHour());
    assertEquals(12, noonEvent.getStartDateTime().getHour());

    // Change the timezone to Los Angeles (3 hours behind NY)
    boolean success = manager.editCalendar("Default",
            "timezone", "America/Los_Angeles");
    assertTrue("Timezone change should succeed", success);

    // Verify calendar timezone changed
    assertEquals(ZoneId.of("America/Los_Angeles"), calendar.getTimezone());

    // Get events after timezone change - should maintain same wall clock time
    List<Event> eventsAfterChange = calendar.getEventsOn(morning.toLocalDate().atStartOfDay());
    assertEquals("Should still find all events on the date",
            2, eventsAfterChange.size());

    // Find events by name again
    morningEvent = findEventByName(eventsAfterChange, "Morning Meeting");
    noonEvent = findEventByName(eventsAfterChange, "Lunch");

    assertNotNull("Morning event should exist after timezone change", morningEvent);
    assertNotNull("Noon event should exist after timezone change", noonEvent);

    // Verify that times are adjusted to maintain the same "wall clock time" in new timezone
    // 9 AM ET = 6 AM PT
    // 12 PM ET = 9 AM PT
    assertEquals(6, morningEvent.getStartDateTime().getHour());
    assertEquals(9, noonEvent.getStartDateTime().getHour());
  }

  /**
   * Test that all-day events remain as all-day events when timezone changes.
   */
  @Test
  public void testEditTimezoneAllDayEvents() {
    // Create an all-day event in New York timezone
    Calendar calendar = manager.getCurrentCalendar();
    LocalDateTime date = LocalDateTime.of(2023, 7, 10, 0, 0);

    calendar.createAllDayEvent("Company Holiday", date,
            true, "Independence Day Observed",
            "Office Closed", true);

    // Verify event is created correctly
    List<Event> eventsBeforeChange = calendar.getEventsOn(date);
    assertEquals(1, eventsBeforeChange.size());
    Event holidayEvent = eventsBeforeChange.get(0);
    assertTrue("Event should be an all-day event", holidayEvent.isAllDay());
    assertEquals(0, holidayEvent.getStartDateTime().getHour());

    // Change the timezone to Tokyo (13-14 hours ahead of NY)
    boolean success = manager.editCalendar("Default",
            "timezone", "Asia/Tokyo");
    assertTrue("Timezone change should succeed", success);

    // Verify calendar timezone changed
    assertEquals(ZoneId.of("Asia/Tokyo"), calendar.getTimezone());

    // Get events after timezone change - all-day events should still be on the same date
    List<Event> eventsAfterChange = calendar.getEventsOn(date);
    assertEquals("All-day events should remain on the same date",
            1, eventsAfterChange.size());

    Event eventAfterChange = eventsAfterChange.get(0);
    assertTrue("Event should still be an all-day event after timezone change",
            eventAfterChange.isAllDay());

    // For all-day events, the date should remain the same
    assertEquals(date.toLocalDate(), eventAfterChange.getStartDateTime().toLocalDate());
    assertEquals(0, eventAfterChange.getStartDateTime().getHour());
  }

  /**
   * Test that recurring events are adjusted correctly when timezone changes.
   */
  @Test
  public void testEditTimezoneRecurringEvents() {
    // Create a recurring event in New York timezone (weekly on Mondays)
    Calendar calendar = manager.getCurrentCalendar();
    LocalDateTime startDateTime = LocalDateTime
            .of(2023, 7, 10, 14, 0); // Monday, 2 PM ET
    LocalDateTime endDateTime = LocalDateTime
            .of(2023, 7, 10, 15, 0);   // Monday, 3 PM ET

    calendar.createRecurringEvent("Weekly Status", startDateTime, endDateTime,
            "M", 4, null, true,
            "Weekly team status", "Conference Room", true);

    // Verify the recurring event is created correctly
    List<Event> eventsBeforeChange = calendar.getEventsOn(startDateTime);
    assertEquals(1, eventsBeforeChange.size());
    Event recurringEvent = eventsBeforeChange.get(0);
    //assertTrue("Event should be recurring", recurringEvent.isRecurring());
    assertEquals(14, recurringEvent.getStartDateTime().getHour());

    // Change the timezone to London (5 hours ahead of NY)
    boolean success = manager.editCalendar("Default",
            "timezone", "Europe/London");
    assertTrue("Timezone change should succeed", success);

    // Verify calendar timezone changed
    assertEquals(ZoneId.of("Europe/London"), calendar.getTimezone());

    // Get events after timezone change - find the recurring event occurrences
    List<Event> allEvents = calendar.getAllEvents();
    assertEquals(1, allEvents.size()); // One recurring event series

    // Get first occurrence in London time
    List<Event> firstOccurrence = calendar.getEventsOn(startDateTime.toLocalDate().atStartOfDay());
    assertEquals(1, firstOccurrence.size());
    Event firstEvent = firstOccurrence.get(0);

    // 2 PM ET = 7 PM London time
    assertEquals(19, firstEvent.getStartDateTime().getHour());

    // Check a future occurrence (second Monday)
    LocalDateTime secondMonday = startDateTime.toLocalDate().plusDays(7).atStartOfDay();
    List<Event> secondOccurrence = calendar.getEventsOn(secondMonday);
    assertEquals(1, secondOccurrence.size());

    Event secondEvent = secondOccurrence.get(0);
    // Still should be at 7 PM London time on second Monday
    assertEquals(19, secondEvent.getStartDateTime().getHour());
  }

  /**
   * Test timezone changes across international date line.
   */
  @Test
  public void testEditTimezoneAcrossDateLine() {
    // Create an event at 8 PM in New York
    Calendar calendar = manager.getCurrentCalendar();
    LocalDateTime eveningTime = LocalDateTime
            .of(2023, 7, 10, 20, 0);  // 8 PM ET

    calendar.createEvent("Evening Call", eveningTime, eveningTime.plusHours(1),
            true, "International call", "Phone", true);

    // Verify event is created correctly
    List<Event> eventsBeforeChange = calendar.getEventsOn(eveningTime);
    assertEquals(1, eventsBeforeChange.size());
    Event eveningEvent = eventsBeforeChange.get(0);
    assertEquals(20, eveningEvent.getStartDateTime().getHour());
    assertEquals(7, eveningEvent.getStartDateTime().getMonthValue());
    assertEquals(10, eveningEvent.getStartDateTime().getDayOfMonth());

    // Change to Tokyo timezone (13-14 hours ahead of NY)
    boolean success = manager.editCalendar("Default", "timezone",
            "Asia/Tokyo");
    assertTrue("Timezone change should succeed", success);

    // 8 PM ET = 9 AM next day in Tokyo
    // The event now should be on July 11 at 9 AM in Tokyo
    LocalDateTime nextDayMorning = LocalDateTime
            .of(2023, 7, 11, 9, 0);
    List<Event> eventsAfterChange = calendar.getEventsOn(nextDayMorning);

    assertEquals("Event should be found on the next day", 1,
            eventsAfterChange.size());
    Event eventAfterChange = eventsAfterChange.get(0);
    assertEquals("Evening Call", eventAfterChange.getSubject());
    assertEquals(9, eventAfterChange.getStartDateTime().getHour());
    assertEquals(7, eventAfterChange.getStartDateTime().getMonthValue());
    assertEquals(11, eventAfterChange.getStartDateTime().getDayOfMonth());
  }

  /**
   * Test timezone changes with DST transitions.
   */
  @Test
  public void testEditTimezoneDSTTransition() {
    // Create a calendar during DST in New York
    ZoneId nyZone = ZoneId.of("America/New_York");
    manager.createCalendar("DST-Test", nyZone);
    manager.useCalendar("DST-Test");

    Calendar calendar = manager.getCurrentCalendar();

    // Create an event during DST (summer) - July 15, 2023 at 10 AM ET
    LocalDateTime summerTime = LocalDateTime.of(2023, 7, 15, 10, 0);
    calendar.createEvent("Summer Meeting", summerTime, summerTime.plusHours(1),
            true, "Summer planning", "Conference Room", true);

    // Create an event outside of DST (winter) - January 15, 2023 at 10 AM ET
    LocalDateTime winterTime = LocalDateTime
            .of(2023, 1, 15, 10, 0);
    calendar.createEvent("Winter Meeting", winterTime, winterTime.plusHours(1),
            true, "Winter planning", "Conference Room", true);

    // Change timezone to a region without DST (e.g., Phoenix, Arizona)
    boolean success = manager.editCalendar("DST-Test",
            "timezone", "America/Phoenix");
    assertTrue("Timezone change should succeed", success);

    // Verify calendar timezone changed
    assertEquals(ZoneId.of("America/Phoenix"), calendar.getTimezone());

    // During summer, NY is on EDT (UTC-4), Phoenix is on MST (UTC-7) = 3 hour difference
    // During winter, NY is on EST (UTC-5), Phoenix is on MST (UTC-7) = 2 hour difference

    // Check the summer event (should be at 7 AM in Phoenix)
    List<Event> summerEvents = calendar.getEventsOn(summerTime);
    Event summerEvent = findEventByName(summerEvents, "Summer Meeting");
    assertEquals(7, summerEvent.getStartDateTime().getHour()); // 10 AM EDT = 7 AM MST

    // Check the winter event (should be at 8 AM in Phoenix)
    List<Event> winterEvents = calendar.getEventsOn(winterTime);
    Event winterEvent = findEventByName(winterEvents, "Winter Meeting");
    assertEquals(8, winterEvent.getStartDateTime().getHour()); // 10 AM EST = 8 AM MST
  }

  /**
   * Test using command processor for timezone editing.
   */
  @Test
  public void testEditTimezoneViaCommand() {
    // Create events via the processor
    mockUI.setNextCommand("create event \"Board Meeting\" from 2023-08-15T15:00 " +
            "to 2023-08-15T16:30");
    processor.processCommand(mockUI.getCommand());

    mockUI.setNextCommand("create event \"Team Lunch\" from 2023-08-15T12:00 " +
            "to 2023-08-15T13:00");
    processor.processCommand(mockUI.getCommand());

    // Verify events were created with correct times in NY timezone
    LocalDateTime checkDate = LocalDateTime
            .of(2023, 8, 15, 0, 0);
    List<Event> eventsBeforeChange = manager.getCurrentCalendar().getEventsOn(checkDate);
    assertEquals(2, eventsBeforeChange.size());

    Event lunchEvent = findEventByName(eventsBeforeChange, "Team Lunch");
    Event boardEvent = findEventByName(eventsBeforeChange, "Board Meeting");

    assertEquals(12, lunchEvent.getStartDateTime().getHour());
    assertEquals(15, boardEvent.getStartDateTime().getHour());

    // Edit timezone via command processor
    mockUI.setNextCommand("edit calendar --name Default --property timezone Europe/Paris");
    processor.processCommand(mockUI.getCommand());

    assertEquals("Calendar updated successfully", mockUI.getLastMessage());

    // Verify events adjusted for Paris timezone (+6 hours from NY)
    List<Event> eventsAfterChange = manager.getCurrentCalendar().getEventsOn(checkDate);
    assertEquals(2, eventsAfterChange.size());

    lunchEvent = findEventByName(eventsAfterChange, "Team Lunch");
    boardEvent = findEventByName(eventsAfterChange, "Board Meeting");

    assertEquals(18, lunchEvent.getStartDateTime().getHour());  // 12 PM ET = 6 PM Paris
    assertEquals(21, boardEvent.getStartDateTime().getHour());  // 3 PM ET = 9 PM Paris
  }

  /**
   * Helper method to find an event by name in a list of events.
   */
  private Event findEventByName(List<Event> events, String name) {
    for (Event event : events) {
      if (event.getSubject().equals(name) ||
              event.getSubject().contains(name) ||
              event.getSubject().equals(name)) {
        return event;
      }
    }
    return null;
  }

  /**
   * Mock TextUI implementation for testing with the command processor.
   */
  private static class MockTextUI implements TextUI {
    private String lastMessage;
    private String lastError;
    private String nextCommand;

    public void setNextCommand(String command) {
      this.nextCommand = command;
    }

    public String getLastMessage() {
      return lastMessage;
    }

    public void reset() {
      lastMessage = null;
      lastError = null;
    }

    @Override
    public void displayMessage(String message) {
      lastMessage = message;
    }

    @Override
    public void displayError(String error) {
      lastError = error;
    }

    @Override
    public String getCommand() {
      return nextCommand;
    }

    @Override
    public void close() {
      // Do nothing for mock
    }
  }
}