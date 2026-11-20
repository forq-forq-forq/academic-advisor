package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.RoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapItemRepository extends JpaRepository<RoadmapItem, Long> {

    List<RoadmapItem> findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(String studentId);

    Optional<RoadmapItem> findByStudent_StudentIdAndCourse_Id(String studentId, Long courseId);

    void deleteByStudent_StudentIdAndCourse_Id(String studentId, Long courseId);

    void deleteByStudent_StudentId(String studentId);
}
