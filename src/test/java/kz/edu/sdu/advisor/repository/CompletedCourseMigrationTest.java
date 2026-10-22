package kz.edu.sdu.advisor.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class CompletedCourseMigrationTest {
    @Test
    void upgradesExistingSqliteCompletionsWithoutLosingData() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("create table student_courses (student_id bigint not null, course_id bigint not null, primary key (student_id, course_id))");
            statement.execute("insert into student_courses values (1, 2)");

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V1__completed_course_history.sql"));

            try (var result = statement.executeQuery("select * from student_courses")) {
                assertTrue(result.next());
                assertEquals(1, result.getLong("student_id"));
                assertEquals(2, result.getLong("course_id"));
                assertNull(result.getString("letter_grade"));
                assertNull(result.getString("semester_taken"));
                assertFalse(result.next());
            }
        }
    }
}
