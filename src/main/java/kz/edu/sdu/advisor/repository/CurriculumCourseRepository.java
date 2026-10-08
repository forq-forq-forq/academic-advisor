package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumCourseRepository extends JpaRepository<CurriculumCourse, Long> {
    List<CurriculumCourse> findByMajorAndCatalogYearOrderBySemesterAsc(Major major, Integer catalogYear);
    List<CurriculumCourse> findByMajor_CodeAndCatalogYearOrderBySemesterAsc(String majorCode, Integer catalogYear);
    List<CurriculumCourse> findByMajor_CodeAndCatalogYearAndSemester(String majorCode, Integer catalogYear, Integer semester);
}

