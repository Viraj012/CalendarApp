# Calendar Application - GUI Implementation

## Design Changes

### 1. Graphical User Interface Implementation
- **Added**: Complete GUI implementation using Java Swing
- **Added**: Intuitive visual calendar representation
- **Justification**: Provides a user-friendly alternative to command-line interaction

### 2. Multiple View Support
- **Added**: Month view showing calendar grid with events
- **Added**: Day view showing detailed timeline of events
- **Added**: Seamless switching between views
- **Justification**: Allows users to choose the most appropriate view for their needs

### 3. Interactive Event Management
- **Added**: Visual event creation through double-clicking on calendar
- **Added**: Event editing dialog with form-based interface
- **Added**: Visual representation of events with color coding
- **Justification**: Makes event management more intuitive and accessible

### 4. Calendar Management UI
- **Added**: Calendar selection dropdown
- **Added**: Calendar creation and editing dialogs
- **Justification**: Simplifies working with multiple calendars

### 5. Import/Export Integration
- **Added**: File chooser dialogs for import/export operations
- **Added**: Visual feedback for import/export operations
- **Justification**: Streamlines data exchange with other applications

## How to Run the Program

1. Navigate to the directory containing the JAR file
2. Run the program using one of the following commands:

```
# For GUI mode (default)
java -jar CalendarApp.jar

# You can also explicitly specify GUI mode
java -jar CalendarApp.jar --mode gui
```

Alternatively, simply double-click the JAR file to launch in GUI mode.

## Functional Status

### Working GUI Features
- Month view calendar with event display
- Day view with timeline visualization
- Event creation, editing, and viewing
- Calendar management (create, edit, switch)
- Import/export functionality with file dialogs
- Visual navigation between dates
- Event color coding by type
- Time zone support in the interface
- Background command processing

### Known Issues
- None

## Team Contributions

### Vishal Rajpurohit
- Implemented day view visualization
- Created event editing dialog
- Implemented event color coding
- Developed event detail display
- Created recurring event interface

### Sanskar Sharma
- Created overall GUI framework
- Implemented month view calendar grid
- Developed calendar management UI components
- Created import/export dialogs
- Integrated command processing with GUI
- Implemented navigation controls

## Additional Notes for Graders

- Please see the Instructions section in the help menu
- The GUI is fully integrated with the existing command structure through a bridge design pattern using SwingUI
- All existing command-line functionality remains accessible through the GUI
- The design follows proper MVC architecture with clear separation between the interface and model
- Added unit tests for verifying GUI command execution
- Used appropriate thread management for background command processing
