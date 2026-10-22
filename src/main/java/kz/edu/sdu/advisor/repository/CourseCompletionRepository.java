package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.CourseCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, CourseCompletion.Id> {

    @Query("select c from CourseCompletion c join fetch c.course where c.id.studentId = :studentId order by c.course.code")
    List<CourseCompletion> findForStudent(@Param("studentId") Long studentId);
}
