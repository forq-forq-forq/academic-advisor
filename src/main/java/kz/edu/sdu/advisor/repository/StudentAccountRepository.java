package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.StudentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentAccountRepository extends JpaRepository<StudentAccount, Long> {

    Optional<StudentAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}