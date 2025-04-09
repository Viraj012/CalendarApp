package view.gui;

import view.TextUI;

import javax.swing.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Implementation of TextUI for Swing applications.
 * Provides a bridge between the command-based model and the GUI.
 */
public class SwingUI implements TextUI {
  private CalendarGUI gui;
  private final BlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
  private final JTextArea outputArea = new JTextArea();

  /**
   * Creates a new Swing UI.
   *
   * @param gui the main GUI frame
   */
  public SwingUI(CalendarGUI gui) {
    this.gui = gui;
    outputArea.setEditable(false);
  }

  @Override
  public void displayMessage(String message) {
    // For GUI, messages are typically displayed in the UI directly
    // We'll log them to our output area for debugging
    SwingUtilities.invokeLater(() -> {
      outputArea.append(message + "\n");
      // Also could be displayed in status bar or popup
    });
  }

  @Override
  public void displayError(String error) {
    // For GUI, errors are typically displayed in the UI directly
    // Show a dialog for errors
    SwingUtilities.invokeLater(() -> {
      JOptionPane.showMessageDialog(
          gui,
          error,
          "Error",
          JOptionPane.ERROR_MESSAGE);
    });
  }

  @Override
  public String getCommand() {
    // In the GUI, commands are triggered by user actions
    // This blocks until a command is available
    try {
      return commandQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return "exit";
    }
  }

  /**
   * Adds a command to the queue.
   * This is called by the GUI when a user action should trigger a command.
   *
   * @param command the command to add
   */
  public void addCommand(String command) {
    try {
      commandQueue.put(command);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      displayError("Command interrupted: " + e.getMessage());
    }
  }

  @Override
  public void close() {
    // Clean up any resources
    // For now, just add an exit command to the queue
    addCommand("exit");
  }

  /**
   * Gets the output area for this UI.
   *
   * @return the output text area
   */
  public JTextArea getOutputArea() {
    return outputArea;
  }

  /**
   * Refreshes the calendar view to show newly added events.
   * This method is called after importing events.
   */
  public void refreshCalendarView() {
    if (gui != null) {
      SwingUtilities.invokeLater(() -> {
        gui.refreshView();
      });
    }
  }
}