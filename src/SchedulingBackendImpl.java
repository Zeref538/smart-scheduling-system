import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

public class SchedulingBackendImpl implements SchedulingBackend {
    private final List<Course> courses = new ArrayList<>();
    private List<Section> sections = new ArrayList<>();
    private List<Professor> professors = new ArrayList<>();
    private List<Room> rooms = new ArrayList<>();
    private List<ScheduleEntry> scheduleEntries = new ArrayList<>();
    private List<ScheduleEntry> recentScheduleEntries = new ArrayList<>();
    private String lastGenerationInfo = "";
    
    // Format for console logs
    private static final SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    
    // Constructor to validate database on instantiation
    public SchedulingBackendImpl() {
        logHeader("SYSTEM INITIALIZATION");
        validateDatabase();
    }

    /**
     * Logs a formatted message to the console
     * @param message The message to log
     */
    private void log(String message) {
        String timestamp = LOG_TIME_FORMAT.format(new java.util.Date());
        System.out.println("[" + timestamp + "] " + message);
    }
    
    /**
     * Logs an error message to the console
     * @param message The error message
     */
    private void logError(String message) {
        String timestamp = LOG_TIME_FORMAT.format(new java.util.Date());
        System.err.println("[" + timestamp + "] ERROR: " + message);
    }
    
    /**
     * Logs a section header to visually separate different operations
     * @param header The header text
     */
    private void logHeader(String header) {
        String timestamp = LOG_TIME_FORMAT.format(new java.util.Date());
        String line = "=".repeat(80);
        System.out.println("\n" + line);
        System.out.println("[" + timestamp + "] " + header);
        System.out.println(line);
    }
    
    /**
     * Logs a sub-header for sections within operations
     * @param subHeader The sub-header text
     */
    private void logSubHeader(String subHeader) {
        String timestamp = LOG_TIME_FORMAT.format(new java.util.Date());
        String line = "-".repeat(60);
        System.out.println("\n" + line);
        System.out.println("[" + timestamp + "] " + subHeader);
        System.out.println(line);
    }
    
    /**
     * Logs the current data counts
     */
    private void logDataCounts() {
        log("Current data counts:");
        log("  - Courses:         " + courses.size());
        log("  - Sections:        " + sections.size());
        log("  - Professors:      " + professors.size());
        log("  - Rooms:           " + rooms.size());
        log("  - Schedule Entries: " + scheduleEntries.size());
    }

    /**
     * Creates sample data for demonstration if database is empty
    
    public void createSampleDataIfEmpty() {
        logHeader("CREATING SAMPLE DATA");
        System.out.println("Checking if sample data needs to be created...");
        
        // Only create sample data if all collections are empty
        if (courses.isEmpty() && sections.isEmpty() && professors.isEmpty() && rooms.isEmpty()) {
            log("No data found. Creating sample demonstration data...");
            
            try {
                // Sample Courses
                Course cs101 = new Course("CS101", "Introduction to Programming", 1, 1, "BSCS", 2, "Lecture");
                Course cs102 = new Course("CS102", "Data Structures", 1, 2, "BSCS", 3, "Computer");
                Course math101 = new Course("MATH101", "Calculus", 1, 1, "BSCS", 2, "Lecture");
                Course eng101 = new Course("ENG101", "English Composition", 1, 1, "ANY", 1, "Lecture");
                
                addCourse(cs101);
                addCourse(cs102);
                addCourse(math101);
                addCourse(eng101);
                
                // Sample Professors
                Professor prof1 = new Professor("P001", "Dr. Smith", "BSCS");
                Professor prof2 = new Professor("P002", "Dr. Johnson", "BSCS");
                Professor prof3 = new Professor("P003", "Prof. Williams", "ANY");
                
                addProfessor(prof1);
                addProfessor(prof2);
                addProfessor(prof3);
                
                // Sample Rooms
                Room room1 = new Room("Main", "1", "101", 40, "Lecture");
                Room room2 = new Room("Main", "1", "102", 30, "Computer");
                Room room3 = new Room("Main", "2", "201", 25, "Lecture");
                
                addRoom(room1);
                addRoom(room2);
                addRoom(room3);
                
                // Sample Sections
                Section section1 = new Section("Introduction to Programming", 1, 1, "A");
                Section section2 = new Section("Data Structures", 1, 2, "A");
                Section section3 = new Section("Calculus", 1, 1, "B");
                Section section4 = new Section("English Composition", 1, 1, "C");
                
                addSection(section1);
                addSection(section2);
                addSection(section3);
                addSection(section4);
                
                log("Sample data created successfully!");
                
            } catch (Exception e) {
                logError("Error creating sample data: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            log("Existing data found, not creating sample data.");
        }
    } */

    // Validate database tables and create them if they don't exist
    private void validateDatabase() {
        logSubHeader("DATABASE VALIDATION");
        try {
            Connection conn = DBConnection.getConnection();
            
            // Create tables if they don't exist
            log("Creating/validating database tables...");
            Statement stmt = conn.createStatement();
            
            // Courses table
            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                "course_code VARCHAR(20) PRIMARY KEY, " +
                "course_name VARCHAR(100) NOT NULL, " +
                "year_level INT NOT NULL, " +
                "semester INT NOT NULL, " +
                "program_intended VARCHAR(50) NOT NULL, " +
                "duration_hours INT NOT NULL, " +
                "required_room_type VARCHAR(50) NOT NULL" +
                ") ENGINE=InnoDB");
                
            // Sections table
            stmt.execute("CREATE TABLE IF NOT EXISTS sections (" +
                "section_name VARCHAR(100) PRIMARY KEY, " +
                "course_name VARCHAR(100) NOT NULL, " +
                "year_level INT NOT NULL, " +
                "semester INT NOT NULL" +
                ") ENGINE=InnoDB");
                
            // Professors table
            stmt.execute("CREATE TABLE IF NOT EXISTS professors (" +
                "professor_id VARCHAR(50) PRIMARY KEY, " +
                "professor_name VARCHAR(100) NOT NULL, " +
                "specialization VARCHAR(50) NOT NULL" +
                ") ENGINE=InnoDB");
                
            // Rooms table
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                "room_id VARCHAR(50) PRIMARY KEY, " +
                "building VARCHAR(50) NOT NULL, " +
                "floor VARCHAR(10) NOT NULL, " +
                "room_number VARCHAR(20) NOT NULL, " +
                "capacity INT NOT NULL, " +
                "room_type VARCHAR(50) NOT NULL" +
                ") ENGINE=InnoDB");
                
            // Schedule table - only create if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS schedule (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "section_name VARCHAR(100) NOT NULL, " +
                "course_code VARCHAR(20) NOT NULL, " +
                "professor_id VARCHAR(50) NOT NULL, " +
                "room_id VARCHAR(50) NOT NULL, " +
                "day VARCHAR(20) NOT NULL, " +
                "start_hour INT NOT NULL, " +
                "end_hour INT NOT NULL, " +
                "FOREIGN KEY (section_name) REFERENCES sections(section_name) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (professor_id) REFERENCES professors(professor_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE ON UPDATE CASCADE" +
                ") ENGINE=InnoDB");
                
            log("Database tables validated successfully");
            
            // Check counts in each table
            log("Current table counts:");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM courses")) {
                rs.next();
                log("  - Courses:       " + rs.getInt(1));
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM sections")) {
                rs.next();
                log("  - Sections:      " + rs.getInt(1));
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM professors")) {
                rs.next();
                log("  - Professors:    " + rs.getInt(1));
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
                rs.next();
                log("  - Rooms:         " + rs.getInt(1));
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schedule")) {
                rs.next();
                log("  - Schedule Entries: " + rs.getInt(1));
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            logError("Database validation failed: " + e.getMessage());
            e.printStackTrace();
            
            // Notify user of database error
            JOptionPane.showMessageDialog(null, 
                "Error validating database: " + e.getMessage() + 
                "\nPlease check your database connection.", 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void loadData() {
        logHeader("LOADING DATA");
        
        // Always load from database
        loadDataFromDatabase();
        
        // Fix section naming issues
        fixSectionNames();
        
        // Verify and fix course relationships
        verifyAndFixCourseRelationships();
        
        // Create sample data if database is empty
        // createSampleDataIfEmpty();
        
        logSubHeader("DATA LOADING COMPLETE");
        logDataCounts();
    }

    private void loadDataFromDatabase() {
        logSubHeader("LOADING FROM DATABASE");
        Connection conn = null;
        try {
            log("Connecting to database...");
            conn = DBConnection.getConnection();

            // Load courses
            log("Loading courses...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT course_code, course_name, year_level, semester, program_intended AS program, duration_hours AS duration, required_room_type FROM courses")) {
                int count = 0;
                while (rs.next()) {
                    Course course = new Course(
                            rs.getString("course_code"),
                            rs.getString("course_name"),
                            rs.getInt("year_level"),
                            rs.getInt("semester"),
                            rs.getString("program"),  // alias from program_intended
                            rs.getInt("duration"),
                            rs.getString("required_room_type"));
                    
                    // Only add if it doesn't exist
                    if (!courses.stream().anyMatch(c -> c.code.equals(course.code))) {
                        courses.add(course);
                        count++;
                    }
                }
                log("Loaded " + count + " new courses from database");
            }

            // Load sections
            log("Loading sections...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT section_name, course_name, year_level, semester FROM sections")) {
                int count = 0;
                int parseWarnings = 0;
                while (rs.next()) {
                    String sectionName = rs.getString("section_name");
                    String sectionNumber;
                    
                    // Safely extract section number from section_name with different possible formats
                    try {
                        String[] parts = sectionName.split("-");
                        // If format is like "CourseName-YearLevel-Semester-SectionNumber"
                        if (parts.length >= 4) {
                            sectionNumber = parts[3];
                        }
                        // If format is like "CourseName-SectionNumber" 
                        else if (parts.length >= 2) {
                            sectionNumber = parts[parts.length - 1]; // Use last segment
                        }
                        // Otherwise just use the whole name as section number
                        else {
                            sectionNumber = sectionName;
                        }
                    } catch (Exception e) {
                        // If any error in parsing, use a default
                        log("Warning: Error parsing section name: " + sectionName);
                        sectionNumber = "A";
                        parseWarnings++;
                    }
                    
                    Section section = new Section(
                            rs.getString("course_name"),
                            rs.getInt("year_level"),
                            rs.getInt("semester"),
                            sectionNumber
                    );
                    
                    // Only add if it doesn't exist
                    if (!sections.stream().anyMatch(s -> s.name.equals(section.name))) {
                        sections.add(section);
                        count++;
                    }
                }
                if (parseWarnings > 0) {
                    log("Warning: " + parseWarnings + " section names had parsing issues");
                }
                log("Loaded " + count + " new sections from database");
            }

            // Load professors
            log("Loading professors...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT professor_id, professor_name, specialization FROM professors")) {
                int count = 0;
                while (rs.next()) {
                    Professor professor = new Professor(
                            rs.getString("professor_id"),
                            rs.getString("professor_name"),
                            rs.getString("specialization"));
                    
                    // Only add if it doesn't exist
                    if (!professors.stream().anyMatch(p -> p.id.equals(professor.id))) {
                        professors.add(professor);
                        count++;
                    }
                }
                log("Loaded " + count + " new professors from database");
            }

            // Load rooms
            log("Loading rooms...");

        String sql = "SELECT * FROM rooms";

        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;

            while (rs.next()) {
                String roomId = rs.getString("room_id");
                String building = rs.getString("building");
                String floor = rs.getString("floor");
                String roomNumber = rs.getString("room_number");
                int capacity = rs.getInt("capacity");
                String roomType = rs.getString("room_type");

                SchedulingBackend.Room room = new SchedulingBackend.Room(
                    roomId, building, floor, roomNumber, capacity, roomType
                );

                // Only add if it doesn't already exist
                if (rooms.stream().noneMatch(r -> r.id.equals(roomId))) {
                    rooms.add(room);
                    count++;
                }
            }

            log("Loaded " + count + " rooms from database");

        } catch (SQLException e) {
            logError("Failed to load rooms: " + e.getMessage());
            e.printStackTrace();
        }

            // Load schedule entries
            log("Loading schedule entries...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                        "SELECT s.section_name, c.course_code, p.professor_id, r.room_id, " +
                                "sch.day, sch.start_hour, sch.end_hour " +
                                "FROM schedule sch " +
                                "JOIN sections s ON sch.section_name = s.section_name " +
                                "JOIN courses c ON sch.course_code = c.course_code " +
                                "JOIN professors p ON sch.professor_id = p.professor_id " +
                                "JOIN rooms r ON sch.room_id = r.room_id")) {
                int count = 0;
                int failedCount = 0;
                while (rs.next()) {
                    String sectionName = rs.getString("section_name");
                    String courseCode = rs.getString("course_code");
                    String professorId = rs.getString("professor_id");
                    String roomId = rs.getString("room_id");
                    String day = rs.getString("day");
                    int startHour = rs.getInt("start_hour");
                    int endHour = rs.getInt("end_hour");

                    Section section = findSectionByName(sectionName);
                    Course course = findCourseByCode(courseCode);
                    Professor professor = findProfessorById(professorId);
                    Room room = findRoomById(roomId);

                    if (section != null && course != null && professor != null && room != null) {
                        ScheduleEntry entry = new ScheduleEntry(
                                section, course, professor, room, day, startHour, endHour);
                                
                        // Only add if it doesn't exist
                        if (!scheduleEntries.stream().anyMatch(e -> 
                            e.section.name.equals(entry.section.name) &&
                            e.course.code.equals(entry.course.code) &&
                            e.professor.id.equals(entry.professor.id) &&
                            e.room.id.equals(entry.room.id) &&
                            e.day.equals(entry.day) &&
                            e.startHour == entry.startHour &&
                            e.endHour == entry.endHour)) {
                            scheduleEntries.add(entry);
                            count++;
                        }
                    } else {
                        failedCount++;
                        log("Warning: Could not load schedule entry due to missing reference: " +
                                "Section=" + sectionName + " (" + (section==null ? "not found" : "found") + "), " +
                                "Course=" + courseCode + " (" + (course==null ? "not found" : "found") + "), " +
                                "Professor=" + professorId + " (" + (professor==null ? "not found" : "found") + "), " +
                                "Room=" + roomId + " (" + (room==null ? "not found" : "found") + ")");
                    }
                }
                
                if (failedCount > 0) {
                    log("Warning: Skipped " + failedCount + " schedule entries due to missing references");
                }
                log("Loaded " + count + " new schedule entries from database");
            }

            log("Database data loading complete");
            logDataCounts();

        } catch (ClassNotFoundException | SQLException e) {
            logError("Failed to load data from database: " + e.getMessage());
            e.printStackTrace();
            
            // Show detailed error and initialize empty collections
            JOptionPane.showMessageDialog(null, 
                "Failed to load data from database: " + e.getMessage() + 
                "\nMake sure your database server is running and properly configured in DBConnection.java." +
                "\nThe application will continue with existing data.", 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Course findCourseByCode(String code) {
        return courses.stream()
            .filter(c -> c.code.equals(code))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void saveData() {
    
        saveToDatabase();
    }

    private void saveToDatabase() {
        Connection conn = null;
        
        try {
            conn = DBConnection.getConnection();
            
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // First delete existing data to prevent duplicates
                try (Statement stmt = conn.createStatement()) {
                    System.out.println("Clearing existing data from database...");
                    // Delete in reverse order of dependency
                    stmt.executeUpdate("DELETE FROM schedule");
                    stmt.executeUpdate("DELETE FROM students");
                    stmt.executeUpdate("DELETE FROM sections");
                    stmt.executeUpdate("DELETE FROM courses");
                    stmt.executeUpdate("DELETE FROM professors");
                    stmt.executeUpdate("DELETE FROM rooms");
                }
                
                // Save courses
                int coursesInserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO courses (course_code, course_name, year_level, semester, program_intended, duration_hours, required_room_type) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    for (Course course : courses) {
                        ps.setString(1, course.code);
                        ps.setString(2, course.name);
                        ps.setInt(3, course.yearLevel);
                        ps.setInt(4, course.semester);
                        ps.setString(5, course.programIntended);
                        ps.setInt(6, course.durationHours);
                        ps.setString(7, course.requiredRoomType);
                        ps.addBatch();
                        coursesInserted++;
                    }
                    ps.executeBatch();
                }
                System.out.println("Inserted " + coursesInserted + " courses into database");
                
                // Save sections
                int sectionsInserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sections (section_name, course_name, year_level, semester) VALUES (?, ?, ?, ?)")) {
                    for (Section section : sections) {
                        ps.setString(1, section.name);
                        ps.setString(2, section.courseName);
                        ps.setInt(3, section.yearLevel);
                        ps.setInt(4, section.semester);
                        ps.addBatch();
                        sectionsInserted++;
                    }
                    ps.executeBatch();
                }
                System.out.println("Inserted " + sectionsInserted + " sections into database");
                
                // Save professors
                int professorsInserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO professors (professor_id, professor_name, specialization) VALUES (?, ?, ?)")) {
                    for (Professor prof : professors) {
                        ps.setString(1, prof.id);
                        ps.setString(2, prof.name);
                        ps.setString(3, prof.specialization);
                        ps.addBatch();
                        professorsInserted++;
                    }
                    ps.executeBatch();
                }
                System.out.println("Inserted " + professorsInserted + " professors into database");
                
                // Save rooms
                int roomsInserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rooms (room_id, building, floor, room_number, capacity, room_type) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (Room room : rooms) {
                        ps.setString(1, room.id);
                        ps.setString(2, room.building);
                        ps.setString(3, room.floor);
                        ps.setString(4, room.roomNumber);
                        ps.setInt(5, room.capacity);
                        ps.setString(6, room.roomType);
                        ps.addBatch();
                        roomsInserted++;
                    }
                    ps.executeBatch();
                }
                System.out.println("Inserted " + roomsInserted + " rooms into database");
                
                // Save schedule entries
                int scheduleEntriesInserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO schedule (section_name, course_code, professor_id, room_id, day, start_hour, end_hour) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    for (ScheduleEntry entry : scheduleEntries) {
                        ps.setString(1, entry.section.name);
                        ps.setString(2, entry.course.code);
                        ps.setString(3, entry.professor.id);
                        ps.setString(4, entry.room.id);
                        ps.setString(5, entry.day);
                        ps.setInt(6, entry.startHour);
                        ps.setInt(7, entry.endHour);
                        ps.addBatch();
                        scheduleEntriesInserted++;
                    }
                    ps.executeBatch();
                }
                System.out.println("Inserted " + scheduleEntriesInserted + " schedule entries into database");
                
                // Verify data saved correctly
                try (Statement stmt = conn.createStatement()) {
                    // Verify courses
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM courses");
                    if (rs.next() && rs.getInt(1) != courses.size()) {
                        System.out.println("Warning: Database course count (" + rs.getInt(1) + 
                            ") does not match expected (" + courses.size() + ")");
                    }
                    
                    // Verify schedule entries
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM schedule");
                    if (rs.next() && rs.getInt(1) != scheduleEntries.size()) {
                        System.out.println("Warning: Database schedule entry count (" + rs.getInt(1) + 
                            ") does not match expected (" + scheduleEntries.size() + ")");
                    }
                }
                
                // Commit all changes
                conn.commit();
                System.out.println("Data successfully committed to database");
                
            } catch (SQLException e) {
                // Rollback transaction in case of error
                if (conn != null) {
                    try {
                        conn.rollback();
                        System.out.println("Transaction rolled back due to error: " + e.getMessage());
                    } catch (SQLException rollbackEx) {
                        System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                    }
                }
                throw e;
            } finally {
                // Restore auto-commit mode
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException e) {
                        System.err.println("Error restoring auto-commit: " + e.getMessage());
                    }
                }
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error saving to database: " + e.getMessage());
            e.printStackTrace();
            
            // Report error instead of falling back to files
            JOptionPane.showMessageDialog(null, 
                "Failed to save data to database: " + e.getMessage() + 
                "\nPlease check your database connection.", 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public List<Course> getCourses() {
        return courses;
    }

    @Override
    public List<Section> getSections() {
        return sections;
    }

    @Override
    public List<Professor> getProfessors() {
        return professors;
    }

    @Override
    public List<Room> getRooms() {
        return rooms;
    }

    @Override
    public List<ScheduleEntry> getScheduleEntries() {
        return scheduleEntries;
    }


    // Do NOT use @Override here

// NOTE: This is the updated logic integrated from the fixed canvas version
// Make sure import java.util.stream.Collectors; is at the top

public List<ScheduleEntry> generateSchedule(JTextArea logArea, boolean avoidConflicts, boolean balanceLoad, boolean optimizeRooms, boolean considerLunch, boolean clearExisting, int earliestHour, int latestHour){
    recentScheduleEntries.clear();
    List<Integer> hours = new ArrayList<>();
for (int h = earliestHour; h <= latestHour - 1; h++) hours.add(h);

    lastGenerationInfo = String.format("Full Schedule - Generated on %s",
        new SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(new java.util.Date()));

    if (clearExisting) {
        scheduleEntries.clear();
    }

    if (courses.isEmpty() || sections.isEmpty() || professors.isEmpty() || rooms.isEmpty()) {
        logError("Missing required data for schedule generation");
        return new ArrayList<>();
    }

    Map<String, Integer> professorLoad = new HashMap<>();
    Map<String, List<ScheduleEntry>> professorSchedules = new HashMap<>();
    Map<String, List<ScheduleEntry>> roomSchedules = new HashMap<>();
    List<ScheduleEntry> newEntries = new ArrayList<>();

    for (Professor p : professors) professorLoad.put(p.id, 0);

    String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};


    int totalAttempts = 0;
    int failedMatches = 0;
    int profMissing = 0;
    int roomMissing = 0;
    int timeConflict = 0;

    for (Section section : sections) {
        for (Course course : courses) {
            if (course.yearLevel != section.yearLevel || course.semester != section.semester) continue;

            totalAttempts++;

            
            boolean isBSCS = course.programIntended.equalsIgnoreCase("BSCS");
boolean isBSIT = course.programIntended.equalsIgnoreCase("BSIT");
boolean isBSCpE = course.programIntended.equalsIgnoreCase("BSCpE");
boolean isANY = course.programIntended.equalsIgnoreCase("ANY");

boolean sectionMatches = isANY 
    || section.courseName.startsWith(course.programIntended); // Matches prefix like "BSCS"

if (!sectionMatches) continue;


            Professor professor = professors.stream()
                .filter(p -> isANY || p.specialization.equalsIgnoreCase(course.programIntended))
                .min(Comparator.comparingInt(p -> professorLoad.getOrDefault(p.id, 0)))
                .orElse(null);
            if (professor == null) {
                profMissing++;
                logError("No professor match for course: " + course.name);
                continue;
            }

            List<Room> matchingRooms = rooms.stream()
                .filter(r -> r.roomType.equalsIgnoreCase(course.requiredRoomType))
                .collect(Collectors.toList());
            if (matchingRooms.isEmpty()) {
                roomMissing++;
                logError("No rooms of type " + course.requiredRoomType + " for course: " + course.name);
                continue;
            }

            boolean scheduled = false;
            outer:
            for (String day : days) {
                for (int hour : hours) {
                    for (Room room : matchingRooms) {
                        String roomId = room.building + "-" + room.floor + "-" + room.roomNumber;

                        List<ScheduleEntry> profSchedule = professorSchedules.getOrDefault(professor.id, new ArrayList<>());
                        List<ScheduleEntry> roomSchedule = roomSchedules.getOrDefault(roomId, new ArrayList<>());

int proposedEndHour = hour + course.durationHours;

boolean profConflict = profSchedule.stream().anyMatch(e ->
    e.day.equals(day) && hour < e.endHour && proposedEndHour > e.startHour
);

boolean roomConflict = roomSchedule.stream().anyMatch(e ->
    e.day.equals(day) && hour < e.endHour && proposedEndHour > e.startHour
);


                        if (profConflict || roomConflict) {
                            timeConflict++;
                            continue;
                        }
int endHour = hour + course.durationHours;
ScheduleEntry entry = new ScheduleEntry(section, course, professor, room, day, hour, endHour);

                        newEntries.add(entry);
                        profSchedule.add(entry);
                        roomSchedule.add(entry);

                        professorSchedules.put(professor.id, profSchedule);
                        roomSchedules.put(roomId, roomSchedule);
                        professorLoad.put(professor.id, professorLoad.get(professor.id) + 1);
                        scheduled = true;
                        break outer;
                    }
                }
            }

            if (!scheduled) {
                failedMatches++;
                logError("Could not find a valid day/time for: " + section.name + " - " + course.name);
            }
        }
    }

    log("Data Summary:");
    log("- Professors available: " + professors.size());
    log("- Rooms available: " + rooms.size());
    log("- Courses loaded: " + courses.size());
    log("- Sections loaded: " + sections.size());
    log("\nSchedule Generation Report:");
    log("- Total course-section attempts: " + totalAttempts);
    log("- Successfully scheduled: " + newEntries.size());
    log("- No professor available: " + profMissing);
    log("- No room available: " + roomMissing);
    log("- Time/room/professor conflicts: " + timeConflict);

    scheduleEntries.addAll(newEntries);
    return newEntries;
}




     private Professor findOptimalProfessor(Course course, Map<String, Integer> professorAssignmentCount, boolean balanceLoad, JTextArea logArea) {
        if (logArea != null) {
            logArea.append("Finding professor for course: " + course.name + " (Program: " + course.programIntended + ")\n");
        }
        
        // First try to find professors with exact specialization match to the course's program
        List<Professor> exactMatchProfessors = professors.stream()
            .filter(p -> p.specialization.equalsIgnoreCase(course.programIntended))
            .collect(Collectors.toList());
            
        if (logArea != null) {
            logArea.append("Found " + exactMatchProfessors.size() + " professors with exact program match\n");
        }
        
        // If we have exact matches, use them
        if (!exactMatchProfessors.isEmpty()) {
            if (balanceLoad) {
                // Sort by current load to balance teaching assignments
                exactMatchProfessors.sort((p1, p2) -> {
                    int p1Load = professorAssignmentCount.getOrDefault(p1.id, 0);
                    int p2Load = professorAssignmentCount.getOrDefault(p2.id, 0);
                    return Integer.compare(p1Load, p2Load);
                });
            }
            Professor selected = exactMatchProfessors.get(0);
            if (logArea != null) {
                logArea.append("Selected professor with exact match: " + selected.name + " (Specialization: " + selected.specialization + ")\n");
            }
            return selected;
        }
        
        // If no exact matches, try professors with "ANY" specialization
        List<Professor> anySpecializationProfessors = professors.stream()
            .filter(p -> p.specialization.equalsIgnoreCase("ANY"))
            .collect(Collectors.toList());
            
        if (logArea != null) {
            logArea.append("Found " + anySpecializationProfessors.size() + " professors with ANY specialization\n");
        }
        
        if (!anySpecializationProfessors.isEmpty()) {
            if (balanceLoad) {
                // Sort by current load to balance teaching assignments
                anySpecializationProfessors.sort((p1, p2) -> {
                    int p1Load = professorAssignmentCount.getOrDefault(p1.id, 0);
                    int p2Load = professorAssignmentCount.getOrDefault(p2.id, 0);
                    return Integer.compare(p1Load, p2Load);
                });
            }
            Professor selected = anySpecializationProfessors.get(0);
            if (logArea != null) {
                logArea.append("Selected professor with ANY specialization: " + selected.name + "\n");
            }
            return selected;
        }
        
        // If still no matches, try to find professors with similar specializations
        List<Professor> similarSpecializationProfessors = professors.stream()
            .filter(p -> !p.specialization.equalsIgnoreCase("ANY") && 
                        (p.specialization.contains(course.programIntended) || 
                         course.programIntended.contains(p.specialization)))
            .collect(Collectors.toList());
            
        if (logArea != null) {
            logArea.append("Found " + similarSpecializationProfessors.size() + " professors with similar specializations\n");
        }
        
        if (!similarSpecializationProfessors.isEmpty()) {
            if (balanceLoad) {
                // Sort by current load to balance teaching assignments
                similarSpecializationProfessors.sort((p1, p2) -> {
                    int p1Load = professorAssignmentCount.getOrDefault(p1.id, 0);
                    int p2Load = professorAssignmentCount.getOrDefault(p2.id, 0);
                    return Integer.compare(p1Load, p2Load);
                });
            }
            Professor selected = similarSpecializationProfessors.get(0);
            if (logArea != null) {
                logArea.append("Selected professor with similar specialization: " + selected.name + " (Specialization: " + selected.specialization + ")\n");
            }
            return selected;
        }
        
        // If no suitable professor found, return null
        if (logArea != null) {
            logArea.append("No suitable professor found for course: " + course.name + "\n");
        }
        return null;
    }
    
    private Room findOptimalRoom(Course course, boolean optimizeRooms, JTextArea logArea) {
        if (logArea != null) {
            logArea.append("Finding room for course: " + course.name + " (Required type: " + course.requiredRoomType + ")\n");
            logArea.append("Available rooms: " + rooms.size() + "\n");
        }
        
        List<Room> eligibleRooms = rooms.stream()
            .filter(r -> r.roomType.equals(course.requiredRoomType) || r.roomType.equals("ANY"))
            .collect(Collectors.toList());
            
        System.out.println("Eligible rooms (matching type): " + eligibleRooms.size());
        
        if (eligibleRooms.isEmpty()) {
            System.out.println("No eligible rooms with matching type, using any available room");
            // If no eligible rooms, take any room
            return rooms.isEmpty() ? null : rooms.get(0);
        }
        
        if (optimizeRooms) {
            // Sort rooms by suitability (considering capacity, type, etc.)
            eligibleRooms.sort((r1, r2) -> {
                // First prefer exact room type matches
                boolean r1ExactMatch = r1.roomType.equals(course.requiredRoomType);
                boolean r2ExactMatch = r2.roomType.equals(course.requiredRoomType);
                if (r1ExactMatch != r2ExactMatch) {
                    return r1ExactMatch ? -1 : 1;
                }
                
                // Then prefer rooms with adequate but not excessive capacity
                // (assuming average class size of 30 students)
                int idealSize = 30;
                int r1Diff = Math.abs(r1.capacity - idealSize);
                int r2Diff = Math.abs(r2.capacity - idealSize);
                return Integer.compare(r1Diff, r2Diff);
            });
        }
        
        Room selected = eligibleRooms.get(0);
        System.out.println("Selected room: " + selected.id + " (Type: " + selected.roomType + ", Capacity: " + selected.capacity + ")");
        return selected;
    }

    @Override
    public void exportScheduleToCSV() {
        // Implementation of export to CSV
    }

    
    // Helper methods for finding objects by name/id
    private Section findSectionByName(String name) {
        return sections.stream()
                .filter(s -> s.name.equals(name))
                .findFirst()
                .orElse(null);
    }
    
    private Course findCourseByName(String name) {
        return courses.stream()
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElse(null);
    }
    
    private Professor findProfessorByName(String name) {
        return professors.stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .orElse(null);
    }
    
    private Room findRoomById(String id) {
        return rooms.stream()
                .filter(r -> r.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    // Course CRUD operations
    @Override
    public void addCourse(Course course) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO courses (course_code, course_name, year_level, semester, program_intended, duration_hours, required_room_type) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, course.code);
            ps.setString(2, course.name);
            ps.setInt(3, course.yearLevel);
            ps.setInt(4, course.semester);
            ps.setString(5, course.programIntended);
            ps.setInt(6, course.durationHours);
            ps.setString(7, course.requiredRoomType);
            ps.executeUpdate();
            
            // Add to in-memory collection only after successful DB insert
            courses.add(course);
            System.out.println("Course added successfully: " + course.code);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error adding course to database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to add course: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void updateCourse(Course course) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE courses SET course_name=?, year_level=?, semester=?, program_intended=?, duration_hours=?, required_room_type=? WHERE course_code=?");
            ps.setString(1, course.name);
            ps.setInt(2, course.yearLevel);
            ps.setInt(3, course.semester);
            ps.setString(4, course.programIntended);
            ps.setInt(5, course.durationHours);
            ps.setString(6, course.requiredRoomType);
            ps.setString(7, course.code);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                // Update in-memory collection
                for (int i = 0; i < courses.size(); i++) {
                    if (courses.get(i).code.equals(course.code)) {
                        courses.set(i, course);
                        break;
                    }
                }
                System.out.println("Course updated successfully: " + course.code);
            } else {
                System.out.println("No course found with code: " + course.code);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error updating course in database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to update course: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void deleteCourse(String courseCode) {
        try {
            Connection conn = DBConnection.getConnection();
            
            // First delete related schedule entries
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM schedule WHERE course_code = ?");
            ps1.setString(1, courseCode);
            ps1.executeUpdate();
            
            // Then delete the course
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM courses WHERE course_code = ?");
            ps2.setString(1, courseCode);
            int affected = ps2.executeUpdate();
            
            if (affected > 0) {
                // Remove from in-memory collection
                courses.removeIf(c -> c.code.equals(courseCode));
                
                // Also remove related schedule entries from memory
                scheduleEntries.removeIf(entry -> entry.course.code.equals(courseCode));
                
                System.out.println("Course deleted successfully: " + courseCode);
            } else {
                System.out.println("No course found with code: " + courseCode);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error deleting course from database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to delete course: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Section CRUD operations
    @Override
    public void addSection(Section section) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sections (section_name, course_name, year_level, semester) VALUES (?, ?, ?, ?)");
            ps.setString(1, section.name);
            ps.setString(2, section.courseName);
            ps.setInt(3, section.yearLevel);
            ps.setInt(4, section.semester);
            ps.executeUpdate();
            sections.add(section);
            System.out.println("Section added successfully: " + section.name);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error adding section to database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to add section: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void updateSection(Section section, String oldSectionName) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE sections SET section_name=?, course_name=?, year_level=?, semester=? WHERE section_name=?");
            ps.setString(1, section.name);
            ps.setString(2, section.courseName);
            ps.setInt(3, section.yearLevel);
            ps.setInt(4, section.semester);
            ps.setString(5, oldSectionName);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                for (int i = 0; i < sections.size(); i++) {
                    if (sections.get(i).name.equals(oldSectionName)) {
                        sections.set(i, section);
                        break;
                    }
                }
                if (!oldSectionName.equals(section.name)) {
                    PreparedStatement updateSchedule = conn.prepareStatement(
                        "UPDATE schedule SET section_name = ? WHERE section_name = ?");
                    updateSchedule.setString(1, section.name);
                    updateSchedule.setString(2, oldSectionName);
                    updateSchedule.executeUpdate();
                    for (ScheduleEntry entry : scheduleEntries) {
                        if (entry.section.name.equals(oldSectionName)) {
                            entry.section = section;
                        }
                    }
                }
            } else {
                System.out.println("No section found with name: " + oldSectionName);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error updating section in database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to update section: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void deleteSection(String sectionName) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement deleteStudents = conn.prepareStatement("DELETE FROM students WHERE section_name = ?");
            deleteStudents.setString(1, sectionName);
            deleteStudents.executeUpdate();
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM schedule WHERE section_name = ?");
            ps1.setString(1, sectionName);
            ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM sections WHERE section_name = ?");
            ps2.setString(1, sectionName);
            int affected = ps2.executeUpdate();
            if (affected > 0) {
                sections.removeIf(s -> s.name.equals(sectionName));
                scheduleEntries.removeIf(entry -> entry.section.name.equals(sectionName));
                System.out.println("Section deleted successfully: " + sectionName);
            } else {
                System.out.println("No section found with name: " + sectionName);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error deleting section from database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to delete section: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Professor CRUD operations
    @Override
    public void addProfessor(Professor professor) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO professors (professor_id, professor_name, specialization) VALUES (?, ?, ?)");
            ps.setString(1, professor.id);
            ps.setString(2, professor.name);
            ps.setString(3, professor.specialization);
            ps.executeUpdate();
            professors.add(professor);
            System.out.println("Professor added successfully: " + professor.name);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error adding professor to database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to add professor: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void updateProfessor(Professor professor) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE professors SET professor_name=?, specialization=? WHERE professor_id=?");
            ps.setString(1, professor.name);
            ps.setString(2, professor.specialization);
            ps.setString(3, professor.id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                for (int i = 0; i < professors.size(); i++) {
                    if (professors.get(i).id.equals(professor.id)) {
                        professors.set(i, professor);
                        break;
                    }
                }
                for (ScheduleEntry entry : scheduleEntries) {
                    if (entry.professor.id.equals(professor.id)) {
                        entry.professor = professor;
                    }
                }
                System.out.println("Professor updated successfully: " + professor.name);
            } else {
                System.out.println("No professor found with ID: " + professor.id);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error updating professor in database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to update professor: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void deleteProfessor(String professorId) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM schedule WHERE professor_id = ?");
            ps1.setString(1, professorId);
            ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM professors WHERE professor_id = ?");
            ps2.setString(1, professorId);
            int affected = ps2.executeUpdate();
            if (affected > 0) {
                professors.removeIf(p -> p.id.equals(professorId));
                scheduleEntries.removeIf(entry -> entry.professor.id.equals(professorId));
                System.out.println("Professor deleted successfully: " + professorId);
            } else {
                System.out.println("No professor found with ID: " + professorId);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error deleting professor from database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to delete professor: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Room CRUD operations
    @Override
    public void addRoom(Room room) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO rooms (room_id, building, floor, room_number, capacity, room_type) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, room.id);
            ps.setString(2, room.building);
            ps.setString(3, room.floor);
            ps.setString(4, room.roomNumber);
            ps.setInt(5, room.capacity);
            ps.setString(6, room.roomType);
            ps.executeUpdate();
            rooms.add(room);
            System.out.println("Room added successfully: " + room.id);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error adding room to database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to add room: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void updateRoom(Room room) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE rooms SET building=?, floor=?, room_number=?, capacity=?, room_type=? WHERE room_id=?");
            ps.setString(1, room.building);
            ps.setString(2, room.floor);
            ps.setString(3, room.roomNumber);
            ps.setInt(4, room.capacity);
            ps.setString(5, room.roomType);
            ps.setString(6, room.id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                for (int i = 0; i < rooms.size(); i++) {
                    if (rooms.get(i).id.equals(room.id)) {
                        rooms.set(i, room);
                        break;
                    }
                }
                for (ScheduleEntry entry : scheduleEntries) {
                    if (entry.room.id.equals(room.id)) {
                        entry.room = room;
                    }
                }
                System.out.println("Room updated successfully: " + room.id);
            } else {
                System.out.println("No room found with ID: " + room.id);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error updating room in database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to update room: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void deleteRoom(String roomId) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM schedule WHERE room_id = ?");
            ps1.setString(1, roomId);
            ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM rooms WHERE room_id = ?");
            ps2.setString(1, roomId);
            int affected = ps2.executeUpdate();
            if (affected > 0) {
                rooms.removeIf(r -> r.id.equals(roomId));
                scheduleEntries.removeIf(entry -> entry.room.id.equals(roomId));
                System.out.println("Room deleted successfully: " + roomId);
            } else {
                System.out.println("No room found with ID: " + roomId);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error deleting room from database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to delete room: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Schedule entry CRUD operations
    @Override
    public void addScheduleEntry(ScheduleEntry entry) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO schedule (section_name, course_code, professor_id, room_id, day, start_hour, end_hour) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, entry.section.name);
            ps.setString(2, entry.course.code);
            ps.setString(3, entry.professor.id);
            ps.setString(4, entry.room.id);
            ps.setString(5, entry.day);
            ps.setInt(6, entry.startHour);
            ps.setInt(7, entry.endHour);
            ps.executeUpdate();
            scheduleEntries.add(entry);
            System.out.println("Schedule entry added successfully");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error adding schedule entry to database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to add schedule entry: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void deleteScheduleEntry(ScheduleEntry entry) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM schedule WHERE section_name=? AND course_code=? AND professor_id=? AND room_id=? AND day=? AND start_hour=? AND end_hour=?");
            ps.setString(1, entry.section.name);
            ps.setString(2, entry.course.code);
            ps.setString(3, entry.professor.id);
            ps.setString(4, entry.room.id);
            ps.setString(5, entry.day);
            ps.setInt(6, entry.startHour);
            ps.setInt(7, entry.endHour);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                scheduleEntries.removeIf(e ->
                    e.section.name.equals(entry.section.name) &&
                    e.course.code.equals(entry.course.code) &&
                    e.professor.id.equals(entry.professor.id) &&
                    e.room.id.equals(entry.room.id) &&
                    e.day.equals(entry.day) &&
                    e.startHour == entry.startHour &&
                    e.endHour == entry.endHour);
                System.out.println("Schedule entry deleted successfully");
            } else {
                System.out.println("No matching schedule entry found for deletion");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error deleting schedule entry from database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to delete schedule entry: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void updateScheduleEntry(ScheduleEntry oldEntry, ScheduleEntry newEntry) {
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE schedule SET section_name=?, course_code=?, professor_id=?, room_id=?, day=?, start_hour=?, end_hour=? " +
                "WHERE section_name=? AND course_code=? AND professor_id=? AND room_id=? AND day=? AND start_hour=? AND end_hour=?");
            // Set new values
            ps.setString(1, newEntry.section.name);
            ps.setString(2, newEntry.course.code);
            ps.setString(3, newEntry.professor.id);
            ps.setString(4, newEntry.room.id);
            ps.setString(5, newEntry.day);
            ps.setInt(6, newEntry.startHour);
            ps.setInt(7, newEntry.endHour);
            // Set old values for WHERE clause
            ps.setString(8, oldEntry.section.name);
            ps.setString(9, oldEntry.course.code);
            ps.setString(10, oldEntry.professor.id);
            ps.setString(11, oldEntry.room.id);
            ps.setString(12, oldEntry.day);
            ps.setInt(13, oldEntry.startHour);
            ps.setInt(14, oldEntry.endHour);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                for (int i = 0; i < scheduleEntries.size(); i++) {
                    ScheduleEntry entry = scheduleEntries.get(i);
                    if (entry.equals(oldEntry) ||
                        (entry.section.name.equals(oldEntry.section.name) &&
                         entry.course.code.equals(oldEntry.course.code) &&
                         entry.day.equals(oldEntry.day) &&
                         entry.startHour == oldEntry.startHour)) {
                        scheduleEntries.set(i, newEntry);
                        break;
                    }
                }
                System.out.println("Schedule entry updated successfully");
            } else {
                System.out.println("No matching schedule entry found for update");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error updating schedule entry in database: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to update schedule entry: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ...remaining methods...

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public void setProfessors(List<Professor> professors) {
        this.professors = professors;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public void setScheduleEntries(List<ScheduleEntry> scheduleEntries) {
        this.scheduleEntries = scheduleEntries;
    }

    /**
     * Fixes the section naming issue by standardizing section names
     * This should be called when loading data to ensure consistency
     */
    private void fixSectionNames() {
        logSubHeader("STANDARDIZING SECTION NAMES");
        log("Checking for inconsistent section names...");
        
        try {
            Connection conn = DBConnection.getConnection();
            
            // First check if there are any sections that need fixing
            boolean hasInconsistentNames = false;
            for (Section section : sections) {
                String expectedName = String.format("%s %d-Y%d-%s", 
                    section.courseName, section.yearLevel, section.semester, 
                    section.name.substring(section.name.lastIndexOf('-') + 1));
                
                if (!section.name.equals(expectedName)) {
                    hasInconsistentNames = true;
                    log("Inconsistent section name found: '" + section.name + 
                                      "' (expected: '" + expectedName + "')");
                    break;
                }
            }
            
            if (!hasInconsistentNames) {
                log("All section names are already in standard format - no changes needed");
                return;
            }
            
            // Start a transaction for the update
            conn.setAutoCommit(false);
            
            try {
                // Update all sections to have consistent naming
                List<Section> updatedSections = new ArrayList<>();
                int fixedCount = 0;
                
                for (Section section : sections) {
                    String sectionNumber = section.name.contains("-") ? 
                        section.name.substring(section.name.lastIndexOf('-') + 1) : "A";
                    
                    String oldName = section.name;
                    String newName = String.format("%s %d-Y%d-%s", 
                        section.courseName, section.yearLevel, section.semester, sectionNumber);
                    
                    // If the name has changed, update it in the database
                    if (!oldName.equals(newName)) {
                        // First update any references in schedule
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE schedule SET section_name = ? WHERE section_name = ?")) {
                            ps.setString(1, newName);
                            ps.setString(2, oldName);
                            int updated = ps.executeUpdate();
                            if (updated > 0) {
                                log("Updated " + updated + " schedule entries from '" + oldName + "' to '" + newName + "'");
                            }
                        }
                        
                        // Then update any references in students
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE students SET section_name = ? WHERE section_name = ?")) {
                            ps.setString(1, newName);
                            ps.setString(2, oldName);
                            int updated = ps.executeUpdate();
                            if (updated > 0) {
                                log("Updated " + updated + " student references from '" + oldName + "' to '" + newName + "'");
                            }
                        }
                        
                        // Finally update the section itself
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE sections SET section_name = ? WHERE section_name = ?")) {
                            ps.setString(1, newName);
                            ps.setString(2, oldName);
                            int updated = ps.executeUpdate();
                            if (updated > 0) {
                                log("Updated section '" + oldName + "' to '" + newName + "'");
                                fixedCount++;
                            }
                        }
                        
                        // Create an updated section object
                        Section updatedSection = new Section(
                            section.courseName, section.yearLevel, section.semester, sectionNumber);
                        updatedSection.students = new ArrayList<>(section.students);
                        updatedSections.add(updatedSection);
                    } else {
                        updatedSections.add(section);
                    }
                }
                
                // Replace the sections list with the updated one
                sections = updatedSections;
                
                // Commit the changes
                conn.commit();
                log("Section name standardization complete - fixed " + fixedCount + " section names");
                
            } catch (SQLException e) {
                // Roll back in case of error
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logError("Failed to roll back transaction: " + rollbackEx.getMessage());
                }
                logError("Failed to standardize section names: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Restore auto-commit
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logError("Failed to restore auto-commit: " + e.getMessage());
                }
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            logError("Database error when fixing section names: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fixes potential issues with course and section data relationships
     * This ensures course codes and names map correctly for scheduling
     */
    private void verifyAndFixCourseRelationships() {
        logSubHeader("VERIFYING COURSE RELATIONSHIPS");
        log("Checking section-to-course mappings...");
        
        try {
            Connection conn = DBConnection.getConnection();
            
            // Check if all sections have a corresponding course
            List<String> sectionsWithoutCourses = new ArrayList<>();
            for (Section section : sections) {
                boolean hasMatchingCourse = false;
                for (Course course : courses) {
                    if (course.name.equals(section.courseName)) {
                        hasMatchingCourse = true;
                        break;
                    }
                }
                
                if (!hasMatchingCourse) {
                    sectionsWithoutCourses.add(section.name);
                    log("Warning: Section '" + section.name + "' has no matching course with name: '" + section.courseName + "'");
                }
            }
            
            if (sectionsWithoutCourses.isEmpty()) {
                log("All sections have a matching course - no fixes needed");
                return;
            }
            
            log("Found " + sectionsWithoutCourses.size() + " sections without matching courses - will attempt to fix");
            
            // Start transaction for potential fixes
            conn.setAutoCommit(false);
            
            try {
                int fixedByCaseCount = 0;
                int createdCourseCount = 0;
                
                // Option 1: Check if there are courses with the same name but different case
                for (String sectionName : sectionsWithoutCourses) {
                    Section section = findSectionByName(sectionName);
                    if (section == null) continue;
                    
                    // Find a course with similar name (case insensitive)
                    Course matchingCourse = null;
                    for (Course course : courses) {
                        if (course.name.equalsIgnoreCase(section.courseName)) {
                            matchingCourse = course;
                            break;
                        }
                    }
                    
                    if (matchingCourse != null) {
                        // Update the section in the database
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE sections SET course_name = ? WHERE section_name = ?")) {
                            ps.setString(1, matchingCourse.name);
                            ps.setString(2, section.name);
                            int updated = ps.executeUpdate();
                            if (updated > 0) {
                                log("Fixed section '" + section.name + "': updated course name from '" + 
                                        section.courseName + "' to '" + matchingCourse.name + "'");
                                
                                // Update the in-memory object
                                section.courseName = matchingCourse.name;
                                fixedByCaseCount++;
                            }
                        }
                    } else {
                        log("No case-insensitive match found for section " + section.name + 
                                " with course name: " + section.courseName);
                    }
                }
                
                log("Fixed " + fixedByCaseCount + " sections by adjusting course name case");
                
                // Option 2: For any remaining unmatched sections, create placeholder courses
                for (String sectionName : sectionsWithoutCourses) {
                    Section section = findSectionByName(sectionName);
                    if (section == null) continue;
                    
                    // Check if the section has been fixed in the previous step
                    boolean isFixed = false;
                    for (Course course : courses) {
                        if (course.name.equals(section.courseName)) {
                            isFixed = true;
                            break;
                        }
                    }
                    
                    if (!isFixed) {
                        // Create a placeholder course
                        String courseCode = "PL" + Math.abs(section.courseName.hashCode()) % 1000;
                        Course placeholderCourse = new Course(
                                courseCode,
                                section.courseName, 
                                section.yearLevel,
                                section.semester,
                                "ANY", // Generic program
                                2, // Default 2-hour duration
                                "Lecture" // Default room type
                        );
                        
                        // Add to database
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO courses (course_code, course_name, year_level, semester, program_intended, duration_hours, required_room_type) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                            ps.setString(1, placeholderCourse.code);
                            ps.setString(2, placeholderCourse.name);
                            ps.setInt(3, placeholderCourse.yearLevel);
                            ps.setInt(4, placeholderCourse.semester);
                            ps.setString(5, placeholderCourse.programIntended);
                            ps.setInt(6, placeholderCourse.durationHours);
                            ps.setString(7, placeholderCourse.requiredRoomType);
                            ps.executeUpdate();
                            
                            // Add to in-memory collection
                            courses.add(placeholderCourse);
                            log("Created placeholder course '" + placeholderCourse.code + 
                                    "' for section '" + section.name + "'");
                            createdCourseCount++;
                        } catch (SQLException e) {
                            // If insertion fails (possibly due to duplicate code), try with a different code
                            log("Failed to create placeholder course, using alternative code");
                            String altCode = "PL" + System.currentTimeMillis() % 10000;
                            placeholderCourse.code = altCode;
                            
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "INSERT INTO courses (course_code, course_name, year_level, semester, program_intended, duration_hours, required_room_type) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                                ps.setString(1, placeholderCourse.code);
                                ps.setString(2, placeholderCourse.name);
                                ps.setInt(3, placeholderCourse.yearLevel);
                                ps.setInt(4, placeholderCourse.semester);
                                ps.setString(5, placeholderCourse.programIntended);
                                ps.setInt(6, placeholderCourse.durationHours);
                                ps.setString(7, placeholderCourse.requiredRoomType);
                                ps.executeUpdate();
                                
                                // Add to in-memory collection
                                courses.add(placeholderCourse);
                                log("Created placeholder course with alternative code '" + 
                                        placeholderCourse.code + "' for section '" + section.name + "'");
                                createdCourseCount++;
                            }
                        }
                    }
                }
                
                // Commit the changes
                conn.commit();
                log("Course relationship fixes complete: " + fixedByCaseCount + " case fixes, " + 
                    createdCourseCount + " placeholder courses created");
                
            } catch (SQLException e) {
                // Roll back in case of error
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logError("Failed to roll back transaction: " + rollbackEx.getMessage());
                }
                logError("Failed to fix course relationships: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Restore auto-commit
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logError("Failed to restore auto-commit: " + e.getMessage());
                }
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            logError("Database error when fixing course relationships: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<ScheduleEntry> generateProfessorSchedule(Professor professor, JTextArea logArea, boolean avoidConflicts, boolean balanceLoad, boolean optimizeRooms,
            boolean considerLunch) {
        // Clear recent schedule entries
        recentScheduleEntries.clear();
        
        // Update generation info
        lastGenerationInfo = String.format("Professor Schedule (%s) - Generated on %s", 
            professor.name,
            new SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(new java.util.Date()));

        // Get all courses that match the professor's specialization
        List<Course> eligibleCourses = courses.stream()
            .filter(course -> professor.specialization.equals("ANY") || 
                            course.programIntended.equals(professor.specialization))
            .collect(Collectors.toList());

        if (eligibleCourses.isEmpty()) {
            throw new IllegalStateException("No eligible courses found for professor's specialization: " + professor.specialization);
        }

        logArea.append("Found " + eligibleCourses.size() + " eligible courses for professor's specialization.\n");

        // Get all sections that need scheduling
        List<Section> sections = getSections();
        if (sections.isEmpty()) {
            throw new IllegalStateException("No sections available for scheduling");
        }

        logArea.append("Processing " + sections.size() + " sections...\n");
        
        // Generate schedule entries for this professor
        List<ScheduleEntry> generatedEntries = new ArrayList<>();
        
        for (Section section : sections) {
            for (Course course : eligibleCourses) {
                // Skip if course doesn't match section's year level
                if (course.yearLevel != section.yearLevel) continue;
                
                // Check if this course is already scheduled for this section
                boolean alreadyScheduled = scheduleEntries.stream()
                    .anyMatch(entry -> entry.section.equals(section) && entry.course.equals(course));
                
                if (alreadyScheduled) continue;
                
                // Find optimal room
                Room room = findOptimalRoom(course, optimizeRooms, logArea);
                if (room == null) {
                    logArea.append("Warning: No suitable room found for course " + course.name + "\n");
                    continue;
                }
                
                // Try to find a suitable time slot
                String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
                for (String day : days) {
                    // Skip lunch break if enabled
                    final int startHour = considerLunch ? 7 : 7;
                    final int endHour = considerLunch ? 21 : 21;
                    
                    for (int hour = startHour; hour <= endHour - course.durationHours; hour++) {
                        final int currentHour = hour;
                        // Skip lunch hour if enabled
                        if (considerLunch && currentHour == 12) continue;
                        
                        // Check if professor is available at this time
                        boolean professorAvailable = !scheduleEntries.stream()
                            .anyMatch(entry -> entry.professor.equals(professor) && 
                                             entry.day.equals(day) &&
                                             ((currentHour >= entry.startHour && currentHour < entry.endHour) ||
                                              (currentHour + course.durationHours > entry.startHour && currentHour + course.durationHours <= entry.endHour)));
                        
                        if (!professorAvailable) continue;
                        
                        // Check if section is available at this time
                        boolean sectionAvailable = !scheduleEntries.stream()
                            .anyMatch(entry -> entry.section.equals(section) && 
                                             entry.day.equals(day) &&
                                             ((currentHour >= entry.startHour && currentHour < entry.endHour) ||
                                              (currentHour + course.durationHours > entry.startHour && currentHour + course.durationHours <= entry.endHour)));
                        
                        if (!sectionAvailable) continue;
                        
                        // Check if room is available at this time
                        boolean roomAvailable = !scheduleEntries.stream()
                            .anyMatch(entry -> entry.room.equals(room) && 
                                             entry.day.equals(day) &&
                                             ((currentHour >= entry.startHour && currentHour < entry.endHour) ||
                                              (currentHour + course.durationHours > entry.startHour && currentHour + course.durationHours <= entry.endHour)));
                        
                        if (roomAvailable) {
                            // Create new schedule entry
                            ScheduleEntry entry = new ScheduleEntry(
                                section, course, professor, room, day, currentHour, currentHour + course.durationHours);
                                    generatedEntries.add(entry);
                            scheduleEntries.add(entry);
                            logArea.append("Scheduled " + course.name + " for " + section.name + 
                                         " on " + day + " at " + currentHour + ":00 in " + room.id + "\n");
                                    break;
                        }
                    }
                    if (!generatedEntries.isEmpty()) break;
                }
            }
        }

        // Update recent schedule entries with the generated entries
        recentScheduleEntries.addAll(generatedEntries);
        
        // Save the generated schedule
        saveData();
        
        return generatedEntries;
    }

    private Professor findProfessorById(String id) {
        return professors.stream()
            .filter(p -> p.id.equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<ScheduleEntry> getRecentScheduleEntries() {
        return new ArrayList<>(recentScheduleEntries);
    }

    @Override
    public String getLastGenerationInfo() {
        return lastGenerationInfo;
    }

    public void refreshPanel() {
        // Implementation of refreshPanel method
    }
}