package kz.edu.sdu.advisor.model.dto;

import kz.edu.sdu.advisor.model.Faculty;
import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcademicStandingDtoTest {

    @Test
    @DisplayName("AcademicStandingDto.of(null) should return safe default fallback")
    void of_nullStudent_shouldReturnSafeDefaults() {
        AcademicStandingDto dto = AcademicStandingDto.of(null);

        assertEquals(0.0, dto.gpa());
        assertEquals("0.00", dto.formattedGpa());
        assertEquals("Not Available", dto.standingLabel());
        assertEquals("standing-none", dto.standingClass());
        assertEquals(0, dto.completedCredits());
        assertEquals(240, dto.totalDegreeCredits());
        assertEquals(240, dto.remainingCredits());
        assertEquals(0.0, dto.completionPercent());
    }

    @Test
    @DisplayName("Honor Standing for GPA >= 3.50")
    void of_honorStanding() {
        Student student = createStudent(3.85, 120);
        AcademicStandingDto dto = AcademicStandingDto.of(student);

        assertEquals("Honor Standing", dto.standingLabel());
        assertEquals("standing-honor", dto.standingClass());
        assertEquals("3.85", dto.formattedGpa());
        assertEquals(120, dto.completedCredits());
        assertEquals(120, dto.remainingCredits());
        assertEquals(50.0, dto.completionPercent(), 0.001);
    }

    @Test
    @DisplayName("Good Standing for GPA between 2.67 and 3.49")
    void of_goodStanding() {
        Student student = createStudent(3.20, 60);
        AcademicStandingDto dto = AcademicStandingDto.of(student);

        assertEquals("Good Standing", dto.standingLabel());
        assertEquals("standing-good", dto.standingClass());
        assertEquals("3.20", dto.formattedGpa());
        assertEquals(25.0, dto.completionPercent(), 0.001);
    }

    @Test
    @DisplayName("Satisfactory Standing for GPA between 2.00 and 2.66")
    void of_satisfactoryStanding() {
        Student student = createStudent(2.15, 30);
        AcademicStandingDto dto = AcademicStandingDto.of(student);

        assertEquals("Satisfactory", dto.standingLabel());
        assertEquals("standing-satisfactory", dto.standingClass());
        assertEquals("2.15", dto.formattedGpa());
    }

    @Test
    @DisplayName("Academic Warning for GPA < 2.00")
    void of_academicWarning() {
        Student student = createStudent(1.75, 15);
        AcademicStandingDto dto = AcademicStandingDto.of(student);

        assertEquals("Academic Warning", dto.standingLabel());
        assertEquals("standing-warning", dto.standingClass());
        assertEquals("1.75", dto.formattedGpa());
    }

    private Student createStudent(Double gpa, int completedCredits) {
        Faculty faculty = new Faculty();
        faculty.setName("Engineering and Natural Sciences");

        Major major = new Major();
        major.setName("Computer Science");
        major.setTotalCredits(240);
        major.setFaculty(faculty);

        Student student = new Student();
        student.setStudentId("240103000");
        student.setName("Alice");
        student.setGpa(gpa);
        student.setMajor(major);
        student.setCatalogYear(2024);
        student.setCurrentSemester(3);

        // Add dummy completed courses
        for (int i = 0; i < completedCredits / 5; i++) {
            kz.edu.sdu.advisor.model.Course c = new kz.edu.sdu.advisor.model.Course();
            c.setCode("CS10" + i);
            c.setCredits(5);
            student.getCompletedCourses().add(c);
        }

        return student;
    }
}

