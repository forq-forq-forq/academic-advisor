package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Major;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.StudentAccount;
import kz.edu.sdu.advisor.model.dto.RegistrationForm;
import kz.edu.sdu.advisor.repository.StudentAccountRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StudentRegistrationService {

    private final StudentAccountRepository studentAccountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentAccount register(RegistrationForm form, Major major) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        String studentId = extractStudentIdFromEmail(email);

        StudentAccount account = new StudentAccount();
        account.setEmail(email);
        account.setStudentId(studentId);
        account.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        account.setMajor(major);
        account.setCatalogYear(form.getCatalogYear());
        account = studentAccountRepository.save(account);

        Student student = new Student();
        student.setStudentId(studentId);
        student.setEmail(email);
        student.setName("Student " + studentId);
        student.setPasswordHash(account.getPasswordHash());
        student.setMajor(major);
        student.setCatalogYear(form.getCatalogYear());
        student.setCurrentSemester(1);
        student.setGpa(0.0);
        student.setAccount(account);
        studentRepository.save(student);

        account.setStudent(student);
        return account;
    }

    public static String extractStudentIdFromEmail(String email) {
        if (email == null) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return null;
        }
        return email.substring(0, atIndex).trim().toLowerCase(Locale.ROOT);
    }
}