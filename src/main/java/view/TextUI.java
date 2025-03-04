package view;

/**
 * Interface for text-based user interface components.
 */
public interface TextUI {

  /**
   * Display a message to the user.
   *
   * @param message the message to display
   */
  void displayMessage(String message);

  /**
   * Display an error message to the user.
   *
   * @param error the error message to display
   */
  void displayError(String error);

  /**
   * Get a command from the user.
   *
   * @return the user's command
   */
  String getCommand();

  /**
   * Close any resources used by the UI.
   */
  void close();
}