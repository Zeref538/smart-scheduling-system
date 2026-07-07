import java.io.Serializable;

public class ScheduleEntry implements Serializable {
    public SchedulingBackend.Section section;
    public SchedulingBackend.Course course;
    public SchedulingBackend.Professor professor;
    public SchedulingBackend.Room room;
    public String day;
    public int startHour;
    public int endHour;

    public ScheduleEntry(SchedulingBackend.Section section, SchedulingBackend.Course course,
                         SchedulingBackend.Professor professor, SchedulingBackend.Room room,
                         String day, int startHour, int endHour) {
        this.section = section;
        this.course = course;
        this.professor = professor;
        this.room = room;
        this.day = day;
        this.startHour = startHour;
        this.endHour = endHour;
    }
}
