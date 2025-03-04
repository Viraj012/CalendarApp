package view;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TextUI that reads commands from a file.
 */
public class FileUI implements TextUI {
  private BufferedReader reader;
  private List<String> commands;
  private int currentCommandIndex;

  /**
   * Creates a new file-based UI.
   *
   * @param filename the file to read commands from
   * @throws IOException if an I/O error occurs
   */
  public FileUI(String filename) throws IOException {
    this.reader = new BufferedReader(new FileReader(filename));
    this.commands = new ArrayList<>();
    this.currentCommandIndex = 0;

    String line;
    while ((line = reader.readLine()) != null) {
      if (!line.trim().isEmpty()) {
        commands.add(line);
      }
    }
  }

  @Override
  public void displayMessage(String message) {
    System.out.println(message);
  }

  @Override
  public void displayError(String error) {
    System.err.println("Error: " + error);
    // In headless mode, errors should terminate the program
    System.exit(1);
  }

  @Override
  public String getCommand() {
    if (currentCommandIndex < commands.size()) {
      String command = commands.get(currentCommandIndex);
      currentCommandIndex++;
      System.out.println("> " + command); // Echo the command
      return command;
    }
    return "exit"; // Return exit when no more commands
  }

  @Override
  public void close() {
    if (reader != null) {
      try {
        reader.close();
        reader = null;
      } catch (IOException e) {
        System.err.println("Error closing file: " + e.getMessage());
      }
    }
  }
}