package view;

import java.util.Scanner;

/**
 * Implementation of TextUI that uses the console for input/output.
 */
public class ConsoleUI implements TextUI {
  private Scanner scanner;

  /**
   * Creates a new console UI.
   */
  public ConsoleUI() {
    this.scanner = new Scanner(System.in);
  }

  @Override
  public void displayMessage(String message) {
    System.out.println(message);
  }

  @Override
  public void displayError(String error) {
    System.err.println("Error: " + error);
  }

  @Override
  public String getCommand() {
    System.out.print("> ");
    return scanner.nextLine();
  }

  @Override
  public void close() {
    if (scanner != null) {
      scanner.close();
      scanner = null;
    }
  }
}