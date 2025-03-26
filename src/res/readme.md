# Calendar Application

## Design Changes

### 1. Multiple Calendar Support
- **Added**: `CalendarManager` class to manage multiple calendars
- **Added**: Calendar creation, editing, and switching functionality
- **Justification**: Allows users to maintain separate calendars for different purposes (work, personal, etc.)

### 2. Cross-Calendar Operations
- **Added**: Ability to copy events between calendars
- **Added**: Support for copying single events, day events, and date ranges
- **Added**: Automatic timezone conversion during copying
- **Justification**: Enhances productivity by allowing events to be reused across calendars

### 3. Timezone Management
- **Added**: Support for creating calendars with different timezones
- **Added**: Automatic event time adjustment when changing calendar timezone
- **Justification**: Improves usability for users who work across multiple timezones

### 4. Improved Event Conflict Management
- **Modified**: Enhanced conflict detection for recurring events
- **Modified**: Added conflict checking when editing event date/time properties
- **Justification**: Prevents scheduling errors and improves reliability



## How to Run the Program

1. Navigate to the directory containing the JAR file
2. Run the program using one of the following commands:

```
# For interactive mode
java -jar calendar.jar --mode interactive

# For headless mode with a commands file
java -jar calendar.jar --mode headless commands.txt
```

## Functional Status

### Working Features
- All calendar management functions (create, edit, use)
- Event copying between calendars (single events, days, date ranges)
- Automatic timezone conversion for events
- All previously implemented features (event creation, editing, viewing, etc.)

### Known Issues
- None

## Team Contributions

### Vishal Rajpurohit
- Implemented event copying functionality
- Enhanced conflict detection and resolution for events
- Implemented testing for event copying functionality

### Sanskar Sharma
- Developed calendar management system
- Implemented timezone handling and conversion
- Created inter-calendar operations framework
- Integrated new features with existing codebase

## Additional Notes for Graders

- The application maintains backward compatibility with all commands from the previous version
- We've extensively tested timezone conversion to ensure accuracy across different time zones
- The design follows the MVC architecture established in the previous version
