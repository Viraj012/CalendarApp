package controller;

/**
 * Command object for Export Calendar commands.
 */
public class ExportCommand extends Command {
  private String fileName;

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}
