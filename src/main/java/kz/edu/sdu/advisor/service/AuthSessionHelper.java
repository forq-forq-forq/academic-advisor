package kz.edu.sdu.advisor.service;

import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Centralizes HTTP session and Spring Security context authentication checks
 * and student resolution across controllers.
 */
@Component
@RequiredArgsConstructor
public class AuthSessionHelper {

    public static final String SESSION_STUDENT_ID = "authenticatedStudentId";
    public static final String SESSION_STUDENT = "authenticatedStudent";

    private final StudentRepository studentRepository;

    public String resolveStudentId(HttpSession session) {
        if (session != null) {
            Object idAttr = session.getAttribute(SESSION_STUDENT_ID);
            if (idAttr instanceof String studentId && !studentId.isBlank()) {
                return studentId;
            }
            if (session.getAttribute(SESSION_STUDENT) instanceof Student student
                    && student.getStudentId() != null
                    && !student.getStudentId().isBlank()) {
                return student.getStudentId();
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.isAuthenticated()
                && auth.getName() != null
                && !auth.getName().isBlank()) {
            return auth.getName();
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
        if (studentOpt.isEmpty() && session != null) {
            session.invalidate();
            SecurityContextHolder.clearContext();
        }
        return studentOpt;
    }

    public void establishSession(HttpSession session, Student student) {
        session.setAttribute(SESSION_STUDENT, student);
        session.setAttribute(SESSION_STUDENT_ID, student.getStudentId());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                student.getStudentId(),
                null,
                List.of(new SimpleGrantedAuthority(student.getEffectiveRole().asAuthority()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    public void clearSession(HttpSession session) {
        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }
    }
}

