import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

/**
 * Tests for the CalendarApp class.
 */
public class CalendarAppTest {

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final InputStream originalIn = System.in;

  private ByteArrayOutputStream testOut;
  private ByteArrayOutputStream testErr;

  private Path tempCommandFile;
  private Path tempExportFile;

  private SecurityManager originalSecurityManager;
  private int exitCode = 0;
  private boolean exitCalled = false;

  @Before
  public void setUp() throws IOException {

    testOut = new ByteArrayOutputStream();
    testErr = new ByteArrayOutputStream();
    System.setOut(new PrintStream(testOut));
    System.setErr(new PrintStream(testErr));

    tempCommandFile = Files.createTempFile("calendar-commands", ".txt");
    tempExportFile = Files.createTempFile("calendar-export", ".csv");

    Files.deleteIfExists(tempExportFile);

    originalSecurityManager = System.getSecurityManager();
    System.setSecurityManager(new SecurityManager() {
      @Override
      public void checkExit(int status) {
        exitCalled = true;
        exitCode = status;
        throw new SecurityException("System.exit intercepted");
      }

      @Override
      public void checkPermission(java.security.Permission perm) {

      }
    });
  }

  @After
  public void tearDown() throws IOException {

    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);

    Files.deleteIfExists(tempCommandFile);
    Files.deleteIfExists(tempExportFile);

    System.setSecurityManager(originalSecurityManager);
  }

  /**
   * Helper method to run CalendarApp with the given arguments and catch any System.exit calls.
   */
  private void runCalendarApp(String[] args) {
    exitCalled = false;
    exitCode = 0;

    try {
      CalendarApp.main(args);
    } catch (SecurityException e) {

      if (!e.getMessage().equals("System.exit intercepted")) {
        throw e;
      }
    }
  }

  /**
   * Helper method to create a command file with the given commands.
   */
  private void createCommandFile(String... commands) throws IOException {
    try (FileWriter writer = new FileWriter(tempCommandFile.toFile())) {
      for (String cmd : commands) {
        writer.write(cmd + "\n");
      }
    }
  }

  @Test
  public void testNoArguments() {

    runCalendarApp(new String[]{});

    assertTrue("System.exit should be called", exitCalled);
    assertEquals("Exit code should be 1", 1, exitCode);
    assertTrue("Error message should mention usage",
        testErr.toString().contains("Usage: java CalendarApp --mode"));
  }

  @Test
  public void testInvalidFirstArgument() {

    runCalendarApp(new String[]{"--invalid", "interactive"});

    assertTrue("System.exit should be called", exitCalled);
    assertEquals("Exit code should be 1", 1, exitCode);
    assertTrue("Error message should mention first argument",
        testErr.toString().contains("Expected '--mode' as first argument"));
  }

  @Test
  public void testInvalidMode() {

    runCalendarApp(new String[]{"--mode", "invalid"});

    assertTrue("System.exit should be called", exitCalled);
    assertEquals("Exit code should be 1", 1, exitCode);
    assertTrue("Error message should mention invalid mode",
        testErr.toString().contains("Invalid mode"));
  }

  @Test
  public void testHeadlessModeMissingFile() {

    runCalendarApp(new String[]{"--mode", "headless"});

    assertTrue("System.exit should be called", exitCalled);
    assertEquals("Exit code should be 1", 1, exitCode);
    assertTrue("Error message should mention invalid mode",
        testErr.toString().contains("Invalid mode"));
  }

  @Test
  public void testHeadlessModeFileNotFound() {

    runCalendarApp(new String[]{"--mode", "headless", "non-existent-file.txt"});

    assertTrue("System.exit should be called", exitCalled);
    assertEquals("Exit code should be 1", 1, exitCode);
    assertTrue("Error message should mention file error",
        testErr.toString().contains("Error opening commands file"));
  }

  @Test
  public void testHeadlessModeValidFile() throws IOException {

    createCommandFile("exit");

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    assertFalse("System.exit(1) should not be called", exitCalled && exitCode == 1);

    assertTrue("Command should be echoed", testOut.toString().contains("> exit"));
  }

  @Test
  public void testHeadlessModeWithCommands() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "show status on " + today,
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("Command should be echoed", output.contains("> show status on " + today));
    assertTrue("Status should be available", output.contains("available"));
    assertTrue("Exit command should be echoed", output.contains("> exit"));
  }

  @Test
  public void testHeadlessModeWithBadCommand() throws IOException {

    createCommandFile("invalid command");

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    assertTrue("System.exit should be called", exitCalled);
    assertEquals("Exit code should be 1", 1, exitCode);

    String output = testOut.toString();
    String error = testErr.toString();

    assertTrue("Command should be echoed", output.contains("> invalid command"));
    assertTrue("Error message should mention unknown command",
        error.contains("Error: Unknown command: invalid"));
  }

  @Test
  public void testInteractiveModeSimulation() {

    ByteArrayInputStream simulatedInput = new ByteArrayInputStream("exit\n".getBytes());
    System.setIn(simulatedInput);

    runCalendarApp(new String[]{"--mode", "interactive"});

    assertTrue("Prompt should be displayed", testOut.toString().contains("> "));
  }

  @Test
  public void testCreateEventAndPrint() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "create event Test Meeting from " + today + "T09:00 to " + today
            + "T10:00 --location \"Conference Room\" --description \"Important meeting\"",
        "print events on " + today,
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("Event should be created", output.contains("Event created successfully"));
    assertTrue("Event should be listed", output.contains("Test Meeting"));
    assertTrue("Location should be included", output.contains("Conference Room"));
  }

  @Test
  public void testCreateRecurringEvent() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
    String tomorrow = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "create event Daily Standup from " + today + "T09:00 to " + today
            + "T09:30 repeats MTWRF for 5 times",
        "print events from " + today + " to " + tomorrow,
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("Recurring event should be created",
        output.contains("Recurring event created successfully"));
    assertTrue("Event name should appear", output.contains("Daily Standup"));
  }

  @Test
  public void testEditEvent() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "create event Original Meeting from " + today + "T14:00 to " + today + "T15:00",
        "edit event name Original Meeting from " + today + "T14:00 to " + today
            + "T15:00 with \"Updated Meeting\"",
        "print events on " + today,
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("Event should be updated", output.contains("Event updated successfully"));
    assertTrue("New name should appear", output.contains("Updated Meeting"));
    assertFalse("Old name should not appear", output.contains("Original Meeting - "));
  }

  @Test
  public void testExportCalendar() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "create event Meeting 1 from " + today + "T10:00 to " + today + "T11:00",
        "create event Meeting 2 from " + today + "T13:00 to " + today + "T14:00",
        "export cal " + tempExportFile.toString(),
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("Export message should appear", output.contains("Calendar exported to:"));

    File exportFile = new File(tempExportFile.toString());
    assertTrue("Export file should exist", exportFile.exists());
    assertTrue("Export file should have content", exportFile.length() > 0);

    String csvContent = new String(Files.readAllBytes(tempExportFile));
    assertTrue("CSV should have header",
        csvContent.contains("Subject,Start Date,Start Time,End Date,End Time"));
    assertTrue("CSV should include first event", csvContent.contains("Meeting 1"));
    assertTrue("CSV should include second event", csvContent.contains("Meeting 2"));
  }

  @Test
  public void testShowStatus() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "create event Busy Time from " + today + "T15:00 to " + today + "T16:00",
        "show status on " + today + "T15:30",
        "show status on " + today + "T17:00",
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("Status should show busy during event", output.contains("busy"));
    assertTrue("Status should show available outside event", output.contains("available"));
  }

  @Test
  public void testAllDayEvents() throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String today = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

    createCommandFile(
        "create event Conference on " + today,
        "print events on " + today,
        "exit"
    );

    runCalendarApp(new String[]{"--mode", "headless", tempCommandFile.toString()});

    String output = testOut.toString();
    assertTrue("All-day event should be created",
        output.contains("All-day event created successfully"));
    assertTrue("Event name should appear", output.contains("Conference"));
    assertTrue("All Day marker should appear", output.contains("All Day"));
  }
}