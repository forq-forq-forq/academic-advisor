package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.Prerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Long> {

    List<Prerequisite> findByCourse_Id(Long courseId);

    List<Prerequisite> findByCourse_Code(String courseCode);

    @Query("SELECT p FROM Prerequisite p JOIN FETCH p.prerequisiteCourse WHERE p.course.id = :courseId")
    List<Prerequisite> findByCourseIdWithDetails(@Param("courseId") Long courseId);

    @Query("SELECT p FROM Prerequisite p JOIN FETCH p.course JOIN FETCH p.prerequisiteCourse")
    List<Prerequisite> findAllWithCourses();
}
