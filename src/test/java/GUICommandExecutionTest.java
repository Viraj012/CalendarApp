import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import controller.CommandProcessor;
import model.CalendarManager;
import model.Event;
import view.gui.CalendarGUI;
import view.gui.SwingUI;

/**
 * Tests that GUI commands are properly sent to the controller and executed. This test verifies the
 * integration between SwingUI, CommandProcessor, and CalendarManager.
 */
public class GUICommandExecutionTest {

  private CalendarManager calendarManager;
  private SwingUI swingUI;
  private CommandProcessor processor;
  private Thread processorThread;
  private CountDownLatch commandProcessedLatch;

  @Before
  public void setUp() {

    calendarManager = new CalendarManager();
    calendarManager.createCalendar("TestCalendar", ZoneId.systemDefault());
    calendarManager.useCalendar("TestCalendar");

    CalendarGUI testGUI = new TestCalendarGUI(calendarManager);

    swingUI = new SwingUI(testGUI);
    testGUI.setSwingUI(swingUI);

    processor = new CommandProcessor(calendarManager, swingUI);

    commandProcessedLatch = new CountDownLatch(1);

    processorThread = new Thread(() -> {
      try {
        String command = swingUI.getCommand();
        processor.processCommand(command);

        commandProcessedLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    processorThread.setDaemon(true);
    processorThread.start();
  }

  @Test
  public void testCreateEventCommand() throws InterruptedException {

    int initialEventCount = calendarManager.getCurrentCalendar().getAllEvents().size();

    String createEventCommand = "create event \"Test Event\" on 2023-12-25T09:00";

    swingUI.addCommand(createEventCommand);

    boolean processed = commandProcessedLatch.await(5, TimeUnit.SECONDS);

    assertTrue("Command was not processed within timeout", processed);

    List<Event> events = calendarManager.getCurrentCalendar().getAllEvents();
    assertEquals("An event should have been added", initialEventCount + 1, events.size());

    Event createdEvent = events.get(events.size() - 1);
    assertEquals("\"Test Event\"", createdEvent.getSubject());
    assertEquals(LocalDate.of(2023, 12, 25).atTime(9, 0), createdEvent.getStartDateTime());
    assertTrue("Event should be an all-day event", createdEvent.isAllDay());
  }

  @Test
  public void testCreateMultipleEventsCommand() throws InterruptedException {

    String[] commands = {
        "create event \"Meeting 1\" from 2023-11-15T09:00 to 2023-11-15T10:00",
        "create event \"Meeting 2\" from 2023-11-16T11:00 to 2023-11-16T12:00",
        "create event \"All Day Event\" on 2023-11-17"
    };

    int initialEventCount = calendarManager.getCurrentCalendar().getAllEvents().size();

    for (int i = 0; i < commands.length; i++) {

      commandProcessedLatch = new CountDownLatch(1);

      processorThread = new Thread(() -> {
        try {
          String command = swingUI.getCommand();
          processor.processCommand(command);
          commandProcessedLatch.countDown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
      processorThread.setDaemon(true);
      processorThread.start();

      swingUI.addCommand(commands[i]);

      boolean processed = commandProcessedLatch.await(5, TimeUnit.SECONDS);
      assertTrue("Command was not processed within timeout", processed);
    }

    List<Event> events = calendarManager.getCurrentCalendar().getAllEvents();
    assertEquals("All events should have been added",
        initialEventCount + commands.length, events.size());
  }

  @Test
  public void testPrintEventsCommand() throws InterruptedException {

    setupTestEvent();

    commandProcessedLatch = new CountDownLatch(1);
    processorThread = new Thread(() -> {
      try {
        String command = swingUI.getCommand();
        processor.processCommand(command);
        commandProcessedLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    processorThread.setDaemon(true);
    processorThread.start();

    String printCommand = "print events on 2023-11-15";
    swingUI.addCommand(printCommand);

    boolean processed = commandProcessedLatch.await(5, TimeUnit.SECONDS);
    assertTrue("Command was not processed within timeout", processed);


  }

  @Test
  public void testEditEventCommand() throws InterruptedException {

    setupTestEvent();

    List<Event> events = calendarManager.getCurrentCalendar().getAllEvents();
    Event event = events.get(events.size() - 1);
    assertEquals("\"Test Event\"", event.getSubject());

    commandProcessedLatch = new CountDownLatch(1);
    processorThread = new Thread(() -> {
      try {
        String command = swingUI.getCommand();
        processor.processCommand(command);
        commandProcessedLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    processorThread.setDaemon(true);
    processorThread.start();

    String editCommand = "edit event subject \"Test Event\" from 2023-11-15T09:00 to 2023-11-15T10:00 with \"Updated Event\"";
    swingUI.addCommand(editCommand);

    boolean processed = commandProcessedLatch.await(5, TimeUnit.SECONDS);
    assertTrue("Command was not processed within timeout", processed);

    events = calendarManager.getCurrentCalendar().getAllEvents();
    event = events.get(events.size() - 1);
    assertEquals("Event subject should be updated", "Updated Event", event.getSubject());
  }

  @Test
  public void testCreateCalendarCommand() throws InterruptedException {

    int initialCalendarCount = calendarManager.getCalendarNames().size();

    commandProcessedLatch = new CountDownLatch(1);
    processorThread = new Thread(() -> {
      try {
        String command = swingUI.getCommand();
        processor.processCommand(command);
        commandProcessedLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    processorThread.setDaemon(true);
    processorThread.start();

    String createCalendarCommand = "create calendar --name WorkCalendar --timezone America/New_York";
    swingUI.addCommand(createCalendarCommand);

    boolean processed = commandProcessedLatch.await(5, TimeUnit.SECONDS);
    assertTrue("Command was not processed within timeout", processed);

    List<String> calendarNames = calendarManager.getCalendarNames();
    assertEquals("A calendar should have been added", initialCalendarCount + 1,
        calendarNames.size());
    assertTrue("WorkCalendar should exist", calendarNames.contains("WorkCalendar"));
  }

  private void setupTestEvent() throws InterruptedException {

    commandProcessedLatch = new CountDownLatch(1);
    processorThread = new Thread(() -> {
      try {
        String command = swingUI.getCommand();
        processor.processCommand(command);
        commandProcessedLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    processorThread.setDaemon(true);
    processorThread.start();

    String createCommand = "create event \"Test Event\" from 2023-11-15T09:00 to 2023-11-15T10:00";
    swingUI.addCommand(createCommand);

    boolean processed = commandProcessedLatch.await(5, TimeUnit.SECONDS);
    assertTrue("Command was not processed within timeout", processed);
  }

  private static class TestCalendarGUI extends CalendarGUI {

    public TestCalendarGUI(CalendarManager cm) {
      super(cm);
    }


    @Override
    public void setVisible(boolean visible) {
      //Do nothing to avoid Swing GUI rendering
    }

    @Override
    public void refreshView() {
      // Do nothing to avoid Swing GUI rendering
    }
  }
}