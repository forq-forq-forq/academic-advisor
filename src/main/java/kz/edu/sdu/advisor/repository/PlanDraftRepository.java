package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.PlanDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanDraftRepository extends JpaRepository<PlanDraft, Long> {

    List<PlanDraft> findByStudent_StudentIdOrderByUpdatedAtDesc(String studentId);

    Optional<PlanDraft> findByIdAndStudent_StudentId(Long id, String studentId);

    Optional<PlanDraft> findByStudent_StudentIdAndNameIgnoreCase(String studentId, String name);
}
