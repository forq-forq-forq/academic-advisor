package kz.edu.sdu.advisor.security;

import kz.edu.sdu.advisor.model.Role;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Loads Spring Security UserDetails by student ID or SDU email (T-06).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final StudentAccountRepository studentAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Empty student identifier");
        }
        String trimmed = identifier.trim();

        Optional<Student> studentOpt = trimmed.contains("@")
                ? studentRepository.findByEmailIgnoreCase(trimmed)
                : studentRepository.findByStudentId(trimmed);

        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            String passwordHash = student.getEffectivePasswordHash();
            if (passwordHash == null || passwordHash.isBlank()) {
                throw new UsernameNotFoundException("No password hash configured for: " + trimmed);
            }
            Role role = student.getEffectiveRole();
            return User.withUsername(student.getStudentId())
                    .password(passwordHash)
                    .authorities(List.of(new SimpleGrantedAuthority(role.asAuthority())))
                    .build();
        }

        Optional<StudentAccount> accountOpt = trimmed.contains("@")
                ? studentAccountRepository.findByEmailIgnoreCase(trimmed)
                : studentAccountRepository.findByStudentId(trimmed);

        if (accountOpt.isPresent()) {
            StudentAccount account = accountOpt.get();
            Role role = account.getRole() != null ? account.getRole() : Role.STUDENT;
            String principal = account.getStudentId() != null ? account.getStudentId() : account.getEmail();
            return User.withUsername(principal)
                    .password(account.getPasswordHash())
                    .authorities(List.of(new SimpleGrantedAuthority(role.asAuthority())))
                    .build();
        }

        throw new UsernameNotFoundException("User not found: " + trimmed);
    }
}
