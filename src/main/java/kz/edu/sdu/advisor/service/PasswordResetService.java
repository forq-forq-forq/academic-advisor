package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.PasswordResetToken;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.repository.PasswordResetTokenRepository;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Handles password recovery token creation, validation, and BCrypt password updates (US-05).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    public static final int TOKEN_TTL_MINUTES = 30;
    private static final Pattern PASSWORD_POLICY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[\\x21-\\x7E]{12,64}$"
    );

    private final PasswordResetTokenRepository tokenRepository;
    private final StudentRepository studentRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Optional<String> requestPasswordReset(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();

        Optional<Student> studentOpt = findStudentByIdentifier(trimmed);
        Optional<StudentAccount> accountOpt = findAccountByIdentifier(trimmed);

        if (studentOpt.isEmpty() && accountOpt.isPresent() && accountOpt.get().getStudentId() != null) {
            studentOpt = studentRepository.findByStudentId(accountOpt.get().getStudentId());
        }
        if (accountOpt.isEmpty() && studentOpt.isPresent()) {
            accountOpt = studentOpt.get().getAccount() != null
                    ? Optional.of(studentOpt.get().getAccount())
                    : studentAccountRepository.findByStudentId(studentOpt.get().getStudentId());
        }

        if (studentOpt.isEmpty() && accountOpt.isEmpty()) {
            return Optional.empty();
        }

        final Optional<Student> resolvedStudentOpt = studentOpt;
        final Optional<StudentAccount> resolvedAccountOpt = accountOpt;
        String studentId = resolvedStudentOpt.map(Student::getStudentId)
                .orElseGet(() -> resolvedAccountOpt.map(StudentAccount::getStudentId).orElse(trimmed));
        String email = resolvedStudentOpt.map(Student::getEmail)
                .filter(StringUtils::hasText)
                .orElseGet(() -> resolvedAccountOpt.map(StudentAccount::getEmail).orElse(studentId + "@sdu.edu.kz"));

        List<PasswordResetToken> previousTokens = tokenRepository.findByStudentIdAndUsedFalse(studentId);
        for (PasswordResetToken previous : previousTokens) {
            previous.setUsed(true);
        }
        if (!previousTokens.isEmpty()) {
            tokenRepository.saveAll(previousTokens);
        }

        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setStudentId(studentId);
        resetToken.setEmail(email);
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plusMinutes(TOKEN_TTL_MINUTES));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        log.info("Password reset token prepared for student {}: /reset-password?token={}",
                studentId, resetToken.getToken());
        return Optional.of(resetToken.getToken());
    }

    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> validateResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return tokenRepository.findByToken(token.trim())
                .filter(PasswordResetToken::isValid);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        PasswordResetToken resetToken = validateResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Reset link is invalid or has expired."));

        if (!StringUtils.hasText(newPassword) || !Objects.equals(newPassword, confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (!PASSWORD_POLICY.matcher(newPassword).matches()) {
            throw new IllegalArgumentException(
                    "Password must be 12-64 characters and include uppercase, lowercase, number, and special character.");
        }

        String encoded = passwordEncoder.encode(newPassword);

        studentRepository.findByStudentId(resetToken.getStudentId()).ifPresent(student -> {
            student.setPasswordHash(encoded);
            if (student.getAccount() != null) {
                student.getAccount().setPasswordHash(encoded);
            }
            studentRepository.save(student);
        });

        studentAccountRepository.findByStudentId(resetToken.getStudentId()).ifPresent(account -> {
            account.setPasswordHash(encoded);
            studentAccountRepository.save(account);
        });

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private Optional<Student> findStudentByIdentifier(String identifier) {
        return identifier.contains("@")
                ? studentRepository.findByEmailIgnoreCase(identifier)
                : studentRepository.findByStudentId(identifier);
    }

    private Optional<StudentAccount> findAccountByIdentifier(String identifier) {
        return identifier.contains("@")
                ? studentAccountRepository.findByEmailIgnoreCase(identifier)
                : studentAccountRepository.findByStudentId(identifier);
    }
}
