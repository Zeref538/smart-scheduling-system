import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;


public interface SchedulingBackend {
    void loadData();
    void saveData();
    
    List<Course> getCourses();
    List<Section> getSections();
    List<Professor> getProfessors();
    List<Room> getRooms();
    List<ScheduleEntry> getScheduleEntries();
    
    List<ScheduleEntry> getRecentScheduleEntries();
    
    // Course CRUD operations
    void addCourse(Course course);
    void updateCourse(Course course);
    void deleteCourse(String courseCode);
    
    // Section CRUD operations
    void addSection(Section section);
    void updateSection(Section section, String oldSectionName);
    void deleteSection(String sectionName);
    
    // Professor CRUD operations
    void addProfessor(Professor professor);
    void updateProfessor(Professor professor);
    void deleteProfessor(String professorId);
    
    // Room CRUD operations
    void addRoom(Room room);
    void updateRoom(Room room);
    void deleteRoom(String roomId);
    
    // Schedule entry CRUD operations
    void addScheduleEntry(ScheduleEntry entry);
    void updateScheduleEntry(ScheduleEntry oldEntry, ScheduleEntry newEntry);
    void deleteScheduleEntry(ScheduleEntry entry);
    
    // Schedule generation methods
List<ScheduleEntry> generateSchedule(JTextArea logArea, boolean avoidConflicts, boolean balanceLoad, boolean optimizeRooms, boolean considerLunch, boolean clearExisting, int earliestHour, int latestHour);

    List<ScheduleEntry> generateProfessorSchedule(Professor professor, JTextArea logArea, boolean avoidConflicts, boolean balanceLoad, boolean optimizeRooms,
            boolean considerLunch);
    String getLastGenerationInfo();

    void exportScheduleToCSV();

    public class Course implements Serializable {
        public String code;
        public String name;
        public int yearLevel;
        public int semester;
        public String programIntended; 
        public int durationHours;
        public String requiredRoomType; // e.g. "Lecture", "Lab", "Computer"
    
        public Course(String code, String name, int yearLevel, int semester, String programIntended, int durationHours, String requiredRoomType) {
            this.code = code;
            this.name = name;
            this.yearLevel = yearLevel;
            this.semester = semester;
            this.programIntended = programIntended;
            this.durationHours = durationHours;
            this.requiredRoomType = requiredRoomType;
        }
    }
    
    
    

    public class Section implements Serializable {
        public String courseName;
        public int yearLevel;
        public int semester;
        public String name;
        public List<String> students;
    
        public Section(String courseName, int yearLevel, int semester, String sectionNumber) {
            this.courseName = courseName;
            this.yearLevel = yearLevel;
            this.semester = semester;
            this.name = String.format("%s %d-Y%d-%s", courseName, yearLevel, semester, sectionNumber);
            this.students = new ArrayList<>();
        }
    }
    
    

    public class Professor implements Serializable {
        public String id;
        public String name;
        public String specialization; // e.g. "BSCS" or "ANY"
    
        public Professor(String id, String name, String specialization) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
        }
    }
    
    // Inside SchedulingBackend.java

    public class Room implements Serializable {
        public String id;
        public String building;
        public String floor;
        public String roomNumber;
        public int capacity;
        public String roomType; // e.g. "Lecture", "Lab", "Computer"
    
        public Room(String id, String building, String floor, String roomNumber, int capacity, String roomType) {
    this.id = id;
    this.building = building;
    this.floor = floor;
    this.roomNumber = roomNumber;
    this.capacity = capacity;
    this.roomType = roomType;
}

    
        @Override
        public String toString() {
            return id + " (" + roomType + ", Capacity: " + capacity + ")";
        }
    }
    
    
}