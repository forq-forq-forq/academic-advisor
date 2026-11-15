package kz.edu.sdu.advisor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.repository.StudentRepository;
import kz.edu.sdu.advisor.service.AuthSessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Bridges legacy/session-based authentication attributes (authenticatedStudentId)
 * into Spring Security's SecurityContextHolder so RBAC and CSRF filters see a real principal.
 */
@Component
@RequiredArgsConstructor
public class SessionSecurityAuthenticationFilter extends OncePerRequestFilter {

    private final StudentRepository studentRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        boolean needsAuth = existing == null
                || existing instanceof AnonymousAuthenticationToken
                || !existing.isAuthenticated();

        if (needsAuth) {
            HttpSession session = request.getSession(false);
            String studentId = resolveSessionStudentId(session);
            if (studentId != null) {
                Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
                if (studentOpt.isPresent()) {
                    Student student = studentOpt.get();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            student.getStudentId(),
                            null,
                            List.of(new SimpleGrantedAuthority(student.getEffectiveRole().asAuthority()))
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                } else {
                    session.invalidate();
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveSessionStudentId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object idAttr = session.getAttribute(AuthSessionHelper.SESSION_STUDENT_ID);
        if (idAttr instanceof String id && !id.isBlank()) {
            return id;
        }
        if (session.getAttribute(AuthSessionHelper.SESSION_STUDENT) instanceof Student student
                && student.getStudentId() != null
                && !student.getStudentId().isBlank()) {
            return student.getStudentId();
        }
        return null;
    }
}
