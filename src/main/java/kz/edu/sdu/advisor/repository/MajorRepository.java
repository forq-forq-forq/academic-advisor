package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    Optional<Major> findByCode(String code);
    List<Major> findByFaculty_Code(String facultyCode);
}

