import java.time.ZonedDateTime;
import model.Calendar;
import model.CalendarManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Tests for the CalendarManager class, focusing on multiple calendars
 * and timezone functionality.
 */
public class CalendarManagerTest {

  private CalendarManager manager;

  @Before
  public void setUp() {
    manager = new CalendarManager();
  }

  @Test
  public void testCreateCalendarWithUniqueName() {
    boolean result = manager.createCalendar("Work", ZoneId.of("America/New_York"));
    assertTrue("Creating calendar with unique name should succeed", result);
    assertNotNull("Calendar should be retrievable by name", manager.getCalendar("Work"));
  }

  @Test
  public void testCreateCalendarWithDuplicateName() {
    manager.createCalendar("Home", ZoneId.of("America/Los_Angeles"));
    boolean result = manager.createCalendar("Home", ZoneId.of("Europe/London"));
    assertFalse("Creating calendar with duplicate name should fail", result);
  }

  @Test
  public void testEditCalendarNameToUnique() {
    manager.createCalendar("Personal", ZoneId.of("America/Chicago"));
    boolean result = manager.editCalendar("Personal", "name", "Family");
    assertTrue("Editing calendar name to a unique value should succeed", result);
    assertNull("Old calendar name should no longer be valid", manager.getCalendar("Personal"));
    assertNotNull("New calendar name should be valid", manager.getCalendar("Family"));
  }

  @Test
  public void testEditCalendarNameToDuplicate() {
    manager.createCalendar("Work", ZoneId.of("America/New_York"));
    manager.createCalendar("Home", ZoneId.of("America/Los_Angeles"));
    boolean result = manager.editCalendar("Work", "name", "Home");
    assertFalse("Editing calendar name to existing name should fail", result);
  }

  @Test
  public void testEditCalendarTimezoneValid() {
    manager.createCalendar("Travel", ZoneId.of("America/New_York"));
    boolean result = manager.editCalendar("Travel", "timezone", "Europe/Paris");
    assertTrue("Editing calendar timezone to valid value should succeed", result);
    assertEquals("Calendar timezone should be updated",
        ZoneId.of("Europe/Paris"), manager.getCalendar("Travel").getTimezone());
  }

  @Test
  public void testEditCalendarTimezoneInvalid() {
    manager.createCalendar("Travel", ZoneId.of("America/New_York"));
    boolean result = manager.editCalendar("Travel", "timezone", "Invalid/Timezone");
    assertFalse("Editing calendar timezone to invalid value should fail", result);
  }

  @Test
  public void testUseCalendar() {
    manager.createCalendar("Work", ZoneId.of("America/New_York"));
    manager.createCalendar("Home", ZoneId.of("America/Los_Angeles"));

    boolean result = manager.useCalendar("Work");
    assertTrue("Using existing calendar should succeed", result);
    assertEquals("Current calendar should be set correctly",
        "Work", manager.getCurrentCalendar().getName());

    result = manager.useCalendar("NonExistent");
    assertFalse("Using non-existent calendar should fail", result);
  }

  @Test
  public void testEventsIsolatedBetweenCalendars() {
    manager.createCalendar("Work", ZoneId.of("America/New_York"));
    manager.createCalendar("Home", ZoneId.of("America/Los_Angeles"));

    manager.useCalendar("Work");
    Calendar workCal = manager.getCurrentCalendar();

    LocalDateTime startTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    LocalDateTime endTime = LocalDateTime.of(2023, 5, 15, 11, 0);

    boolean workResult = workCal.createEvent("Meeting", startTime, endTime,
        true, "Team meeting", "Conference room", true);
    assertTrue("Creating event in work calendar should succeed", workResult);

    manager.useCalendar("Home");
    Calendar homeCal = manager.getCurrentCalendar();

    // Same time slot should be available in home calendar
    boolean homeResult = homeCal.createEvent("Family time", startTime, endTime,
        true, "Family activity", "Living room", true);
    assertTrue("Creating overlapping event in different calendar should succeed", homeResult);

    // Verify both events exist in their respective calendars
    manager.useCalendar("Work");
    assertFalse("Work calendar's events should not be empty",
        manager.getCurrentCalendar().getEventsOn(startTime).isEmpty());

    manager.useCalendar("Home");
    assertFalse("Home calendar's events should not be empty",
        manager.getCurrentCalendar().getEventsOn(startTime).isEmpty());
  }

  @Test
  public void testTimezoneAffectsEventCreation() {
    // Create calendars in different timezones
    manager.createCalendar("NYC", ZoneId.of("America/New_York"));
    manager.createCalendar("Paris", ZoneId.of("Europe/Paris"));

    // Create event in NYC at 10 AM
    manager.useCalendar("NYC");
    LocalDateTime nycTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    boolean result1 = manager.getCurrentCalendar().createEvent("NYC Meeting", nycTime,
        nycTime.plusHours(1), true, "Description", "Location", true);
    assertTrue("Creating event in NYC calendar should succeed", result1);

    // Create event in Paris at the same wall-clock time (10 AM Paris time)
    manager.useCalendar("Paris");
    LocalDateTime parisTime = LocalDateTime.of(2023, 5, 15, 10, 0);
    boolean result2 = manager.getCurrentCalendar().createEvent("Paris Meeting", parisTime,
        parisTime.plusHours(1), true, "Description", "Location", true);
    assertTrue("Creating event in Paris calendar should succeed", result2);

    // Verify that these are different times in absolute terms
    manager.useCalendar("NYC");
    ZonedDateTime nycZonedTime = manager.getCurrentCalendar().toZonedDateTime(nycTime);

    manager.useCalendar("Paris");
    ZonedDateTime parisZonedTime = manager.getCurrentCalendar().toZonedDateTime(parisTime);

    assertNotEquals("10 AM in NYC and 10 AM in Paris should be different UTC times",
        nycZonedTime.toInstant(), parisZonedTime.toInstant());
  }
}