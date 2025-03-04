
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import view.ConsoleUI;
import view.FileUI;
import view.TextUI;

import static org.junit.Assert.*;

/**
 * Test cases for the TextUI implementations.
 */
public class TextUITest {

  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;

  @Before
  public void setUpStreams() {
    // Save original streams
    originalOut = System.out;
    originalErr = System.err;

    // Set up new streams
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @After
  public void restoreStreams() {
    // Restore original streams
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testConsoleUIDisplayMessage() {
    TextUI ui = new ConsoleUI();
    ui.displayMessage("Test message");

    assertEquals("Test message" + System.lineSeparator(), outContent.toString());
  }

  @Test
  public void testConsoleUIDisplayError() {
    TextUI ui = new ConsoleUI();
    ui.displayError("Test error");

    assertEquals("Error: Test error" + System.lineSeparator(), errContent.toString());
  }

  @Test
  public void testConsoleUIGetCommand() throws IOException {
    // Set up a ByteArrayInputStream with a test command
    String testInput = "test command" + System.lineSeparator();
    InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());

    // Save original System.in
    InputStream originalIn = System.in;

    try {
      // Set our test InputStream as System.in
      System.setIn(inputStream);

      // Create a new ConsoleUI that will use our test InputStream
      TextUI ui = new ConsoleUI();

      // Get the command
      String command = ui.getCommand();

      // Verify the prompt was displayed and the command was read correctly
      assertTrue("Should display prompt", outContent.toString().contains(">"));
      assertEquals("Should read command correctly", "test command", command);

    } finally {
      // Restore original System.in
      System.setIn(originalIn);
    }
  }

  @Test
  public void testFileUIDisplayMessage() throws IOException {
    // Create a temporary file for testing
    File tempFile = File.createTempFile("commands", ".txt");
    tempFile.deleteOnExit();

    // Write test commands to the file
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("command1" + System.lineSeparator());
      writer.write("command2" + System.lineSeparator());
    }

    // Create FileUI with the temp file
    TextUI ui = new FileUI(tempFile.getAbsolutePath());

    // Test displayMessage
    ui.displayMessage("Test message");

    assertEquals("Test message" + System.lineSeparator(), outContent.toString());
  }

  @Test
  public void testFileUIDisplayError() throws IOException {
    // Create a temporary file for testing
    File tempFile = File.createTempFile("commands", ".txt");
    tempFile.deleteOnExit();

    // Write a test command to the file
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("test command" + System.lineSeparator());
    }

    // Create FileUI with the temp file
    TextUI ui = new FileUI(tempFile.getAbsolutePath());

    // We can't directly test System.exit(), but we can check if the error message is displayed
    try {
      ui.displayError("Test error");
      fail("Should have exited with System.exit(1)");
    } catch (SecurityException e) {
      // This will only happen if we've set up a SecurityManager to intercept System.exit()
      // In most test environments, this won't happen, and the test will fail
    }

    // We can still check that the error was printed
    assertEquals("Error: Test error" + System.lineSeparator(), errContent.toString());
  }

  @Test
  public void testFileUIGetCommand() throws IOException {
    // Create a temporary file for testing
    File tempFile = File.createTempFile("commands", ".txt");
    tempFile.deleteOnExit();

    // Write test commands to the file
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("command1" + System.lineSeparator());
      writer.write("command2" + System.lineSeparator());
    }

    // Create FileUI with the temp file
    TextUI ui = new FileUI(tempFile.getAbsolutePath());

    // Get the first command
    String command1 = ui.getCommand();
    assertEquals("Should read first command correctly", "command1", command1);

    // Get the second command
    String command2 = ui.getCommand();
    assertEquals("Should read second command correctly", "command2", command2);

    // When no more commands, should return "exit"
    String command3 = ui.getCommand();
    assertEquals("Should return exit when no more commands", "exit", command3);
  }

  @Test
  public void testFileUIClose() throws IOException {
    // Create a temporary file for testing
    File tempFile = File.createTempFile("commands", ".txt");
    tempFile.deleteOnExit();

    // Write a test command to the file
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("test command" + System.lineSeparator());
    }

    // Create FileUI with the temp file
    TextUI ui = new FileUI(tempFile.getAbsolutePath());

    // Test that close works without exceptions
    ui.close();

    // Try to close again (should handle null reader gracefully)
    ui.close();
  }

  @Test(expected = IOException.class)
  public void testFileUIWithNonexistentFile() throws IOException {
    // Attempt to create FileUI with a non-existent file
    new FileUI("nonexistent_file.txt");
  }
}