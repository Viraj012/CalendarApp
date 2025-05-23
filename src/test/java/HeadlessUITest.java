import java.io.BufferedReader;
import java.io.FileReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.ExpectedException;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.security.Permission;
import java.lang.reflect.Field;

import view.HeadlessUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests for the HeadlessUI class.
 */
public class HeadlessUITest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private SecurityManager originalSecurityManager;

  @Before
  public void setUp() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
    originalSecurityManager = System.getSecurityManager();
    System.setSecurityManager(new NoExitSecurityManager());
  }

  @After
  public void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setSecurityManager(originalSecurityManager);
  }

  @Test
  public void testConstructorWithEmptyFile() throws IOException {
    File commandFile = tempFolder.newFile("empty_commands.txt");
    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    assertEquals("exit", ui.getCommand());
    ui.close();
  }

  @Test
  public void testConstructorWithNonEmptyFile() throws IOException {
    File commandFile = tempFolder.newFile("commands.txt");
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(commandFile))) {
      writer.write("command1\n");
      writer.write("command2\n");
      writer.write("command3\n");
    }

    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    assertEquals("command1", ui.getCommand());
    assertEquals("command2", ui.getCommand());
    assertEquals("command3", ui.getCommand());
    assertEquals("exit", ui.getCommand());
    ui.close();
  }

  @Test
  public void testConstructorWithWhitespaceLines() throws IOException {
    File commandFile = tempFolder.newFile("whitespace_commands.txt");
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(commandFile))) {
      writer.write("command1\n");
      writer.write("  \n");
      writer.write("\n");
      writer.write("command2\n");
    }

    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    assertEquals("command1", ui.getCommand());
    assertEquals("command2", ui.getCommand());
    assertEquals("exit", ui.getCommand());
    ui.close();
  }

  @Test
  public void testConstructorWithInvalidFile() throws IOException {
    exception.expect(IOException.class);
    HeadlessUI ui = new HeadlessUI("non_existent_file.txt");
  }

  @Test
  public void testDisplayMessage() throws IOException {
    File commandFile = tempFolder.newFile("display_message.txt");
    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    ui.displayMessage("Test message");
    assertEquals("Test message" + System.lineSeparator(), outContent.toString());
    ui.close();
  }

  @Test
  public void testDisplayError() throws IOException {
    File commandFile = tempFolder.newFile("display_error.txt");
    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    try {
      ui.displayError("Test error");
      fail("System.exit should have been called");
    } catch (ExitException e) {
      assertEquals(1, e.getStatus());
      assertEquals("Error: Test error" + System.lineSeparator(), errContent.toString());
    } finally {
      ui.close();
    }
  }

  @Test
  public void testGetCommandOutput() throws IOException {
    File commandFile = tempFolder.newFile("command_output.txt");
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(commandFile))) {
      writer.write("test command\n");
    }

    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());
    ui.getCommand();

    assertEquals("> test command" + System.lineSeparator(), outContent.toString());
    ui.close();
  }

  @Test
  public void testCloseWithException() throws Exception {
    File commandFile = tempFolder.newFile("close_exception.txt");
    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    Field readerField = HeadlessUI.class.getDeclaredField("reader");
    readerField.setAccessible(true);
    readerField.set(ui, null);

    try {
      ui.close();
      assertNotNull(ui);
    } catch (Exception e) {
      fail("Closing UI should not throw an exception even if reader is null");
    }
  }


  @Test
  public void testCloseWithIOException() throws Exception {
    File commandFile = tempFolder.newFile("close_io_exception.txt");
    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    BufferedReader mockReader = new BufferedReader(new FileReader(commandFile)) {
      @Override
      public void close() throws IOException {
        throw new IOException("Simulated IOException during close");
      }
    };

    Field readerField = HeadlessUI.class.getDeclaredField("reader");
    readerField.setAccessible(true);
    readerField.set(ui, mockReader);

    ui.close();

    assertTrue(
        errContent
            .toString()
            .contains("Error closing file: Simulated IOException during close"));
  }

  @Test
  public void testMultipleClose() throws IOException {
    File commandFile = tempFolder.newFile("multiple_close.txt");
    HeadlessUI ui = new HeadlessUI(commandFile.getAbsolutePath());

    ui.close();

    try {
      ui.close();
      assertNotNull(ui); // Ensure the object is still valid
    } catch (Exception e) {
      fail("Multiple close() calls should not throw an exception");
    }
  }

  private static class ExitException extends SecurityException {

    private final int status;

    public ExitException(int status) {
      super("System.exit(" + status + ")");
      this.status = status;
    }

    public int getStatus() {
      return status;
    }
  }

  private static class NoExitSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
      // Grants permission
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
      // Grants permission
    }

    @Override
    public void checkExit(int status) {
      super.checkExit(status);
      throw new ExitException(status);
    }
  }
}