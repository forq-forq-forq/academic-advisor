package kz.edu.sdu.advisor.service;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Centralizes HTTP session authentication checks and student resolution
 * across controllers so session handling is consistent and never uses stale state.
 */
@Component
@RequiredArgsConstructor
public class AuthSessionHelper {

    public static final String SESSION_STUDENT_ID = "authenticatedStudentId";
    public static final String SESSION_STUDENT = "authenticatedStudent";

    private final StudentRepository studentRepository;

    public String resolveStudentId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object idAttr = session.getAttribute(SESSION_STUDENT_ID);
        if (idAttr instanceof String studentId && !studentId.isBlank()) {
            return studentId;
        }
        if (session.getAttribute(SESSION_STUDENT) instanceof Student student
                && student.getStudentId() != null
                && !student.getStudentId().isBlank()) {
            return student.getStudentId();
        }
        return null;
    }

    public boolean isAuthenticated(HttpSession session) {
        return resolveStudentId(session) != null;
    }

    public Optional<Student> resolveAuthenticatedStudent(HttpSession session) {
        String studentId = resolveStudentId(session);
        if (studentId == null) {
            return Optional.empty();
        }
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            session.invalidate();
        }
        return studentOpt;
    }

    public void establishSession(HttpSession session, Student student) {
        session.setAttribute(SESSION_STUDENT, student);
        session.setAttribute(SESSION_STUDENT_ID, student.getStudentId());
    }
}

