package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.CourseCompletion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseCompletionRepository extends Repository<CourseCompletion, CourseCompletion.Id> {

    @Query("select c from CourseCompletion c join fetch c.course where c.id.studentId = :studentId order by c.course.code")
    List<CourseCompletion> findForStudent(@Param("studentId") Long studentId);
}
