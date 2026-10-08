package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.model.dto.RegistrationForm;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StudentRegistrationService {

    private final StudentAccountRepository studentAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentAccount register(RegistrationForm form, Major major) {
        StudentAccount account = new StudentAccount();
        account.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        account.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        account.setMajor(major);
        account.setCatalogYear(form.getCatalogYear());
        return studentAccountRepository.save(account);
    }
}