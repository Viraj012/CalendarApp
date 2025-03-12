import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Scanner;
import view.InteractiveUI;

import static org.junit.Assert.*;

public class InteractiveUITest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final InputStream originalIn = System.in;

  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);
  }

  @Test
  public void testConstructor() {
    InteractiveUI ui = new InteractiveUI();
    assertNotNull(ui);
    ui.close();
  }

  @Test
  public void testDisplayMessage() {
    InteractiveUI ui = new InteractiveUI();
    ui.displayMessage("Test message");
    assertEquals("Test message" + System.lineSeparator(), outContent.toString());
    ui.close();
  }

  @Test
  public void testDisplayError() {
    InteractiveUI ui = new InteractiveUI();
    ui.displayError("Test error");
    assertEquals("Error: Test error" + System.lineSeparator(), errContent.toString());
    ui.close();
  }

  @Test
  public void testGetCommand() {
    String input = "test command" + System.lineSeparator();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
    System.setIn(inputStream);

    InteractiveUI ui = new InteractiveUI();
    String command = ui.getCommand();

    assertEquals("test command", command);
    assertEquals("> ", outContent.toString());
    ui.close();
  }

  @Test
  public void testMultipleCommands() {
    String input =
        "first command" + System.lineSeparator() + "second command" + System.lineSeparator();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
    System.setIn(inputStream);

    InteractiveUI ui = new InteractiveUI();
    String firstCommand = ui.getCommand();
    String secondCommand = ui.getCommand();

    assertEquals("first command", firstCommand);
    assertEquals("second command", secondCommand);
    assertEquals("> > ", outContent.toString());
    ui.close();
  }

  @Test
  public void testEmptyCommand() {
    String input = System.lineSeparator();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
    System.setIn(inputStream);

    InteractiveUI ui = new InteractiveUI();
    String command = ui.getCommand();

    assertEquals("", command);
    ui.close();
  }

  @Test
  public void testClose() throws Exception {

    final boolean[] inputStreamClosed = {false};
    InputStream monitoredInputStream = new ByteArrayInputStream("test".getBytes()) {
      @Override
      public void close() throws IOException {
        inputStreamClosed[0] = true;
        super.close();
      }
    };

    System.setIn(monitoredInputStream);

    InteractiveUI ui = new InteractiveUI();

    Field scannerField = InteractiveUI.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);

    ui.close();

    assertTrue("Input stream was not closed - Scanner.close() was likely not called",
        inputStreamClosed[0]);

    assertNull("Scanner was not set to null", scannerField.get(ui));
  }

  @Test
  public void testCloseWithNullScanner() throws Exception {
    InteractiveUI ui = new InteractiveUI();

    Field scannerField = InteractiveUI.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);
    scannerField.set(ui, null);

    ui.close();

    Scanner scanner = (Scanner) scannerField.get(ui);
    assertNull(scanner);
  }

  @Test
  public void testMultipleClose() {
    InteractiveUI ui = new InteractiveUI();
    ui.close();
    ui.close();
  }
}