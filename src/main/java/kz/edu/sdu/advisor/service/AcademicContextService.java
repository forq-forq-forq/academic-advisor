package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Prerequisite;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AcademicStandingDto;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.model.dto.EnrolledCourseDto;
import kz.edu.sdu.advisor.repository.CourseCompletionRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.CurriculumCourseRepository;
import kz.edu.sdu.advisor.repository.PrerequisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a rich, structured academic context prompt for the authenticated student (US-41)
 * so the AI Advisor Copilot can give personalized, degree-aware guidance without requiring
 * the student to manually repeat their major, GPA, completed courses, or cart contents.
 */
@Service
@RequiredArgsConstructor
public class AcademicContextService {

    private final CourseCompletionRepository courseCompletionRepository;
    private final CumulativeGpaService cumulativeGpaService;
    private final CartService cartService;
    private final CourseRepository courseRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final PrerequisiteRepository prerequisiteRepository;

    @Transactional(readOnly = true)
    public String buildContextualPrompt(Student student, String userMessage, String currentPage) {
        if (student == null) {
            return userMessage;
        }

        List<CompletedCourseDto> completedCourses = courseCompletionRepository.findForStudent(student.getId())
                .stream()
                .map(CompletedCourseDto::of)
                .toList();

        Double calculatedGpa = cumulativeGpaService.calculate(completedCourses);
        Double effectiveGpa = calculatedGpa != null ? calculatedGpa : student.getGpa();
        AcademicStandingDto standing = AcademicStandingDto.of(student, effectiveGpa);

        CartDto cart = cartService.getCart(student.getStudentId());

        Set<String> satisfiedCodes = completedCourses.stream()
                .map(CompletedCourseDto::code)
                .collect(Collectors.toSet());
        cart.items().forEach(item -> satisfiedCodes.add(item.code()));

        Set<String> enrolledCodes = standing.enrolledCourses().stream()
                .map(EnrolledCourseDto::code)
                .collect(Collectors.toSet());

        Map<Long, CurriculumCourse> curriculumByCourseId = (student.getMajor() != null && student.getCatalogYear() != null)
                ? curriculumCourseRepository.findByMajorAndCatalogYearOrderBySemesterAsc(
                        student.getMajor(), student.getCatalogYear())
                .stream()
                .collect(Collectors.toMap(cc -> cc.getCourse().getId(), cc -> cc, (a, b) -> a))
                : Map.of();

        List<Course> allCourses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        Map<Long, List<String>> prereqsByCourseId = prerequisiteRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCourse().getId(),
                        Collectors.mapping(p -> p.getPrerequisiteCourse().getCode(), Collectors.toList())
                ));

        List<String> eligibleNextCourses = allCourses.stream()
                .filter(c -> !satisfiedCodes.contains(c.getCode()) && !enrolledCodes.contains(c.getCode()))
                .filter(c -> {
                    List<String> reqs = prereqsByCourseId.getOrDefault(c.getId(), List.of());
                    return satisfiedCodes.containsAll(reqs);
                })
                .map(c -> {
                    CurriculumCourse cc = curriculumByCourseId.get(c.getId());
                    String type = cc != null ? cc.getCourseType() : "ELECTIVE";
                    int diff = c.getDifficulty() != null ? c.getDifficulty() : 3;
                    return String.format("%s (%s, %d ECTS, %s, difficulty %d/5)",
                            c.getCode(), c.getName(), c.getCredits(), type, diff);
                })
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly, helpful AI Academic Advisor Copilot for SDU University (SITAM — Information Systems BSc.).\n");
        sb.append("Guidelines:\n");
        sb.append("1. Always reply in the SAME language the student used in their question (e.g. Russian if they wrote in Russian, English if in English).\n");
        sb.append("2. Answer the student's specific question directly and conversationally.\n");
        sb.append("3. Do NOT output a full semester plan or list of courses unless the student explicitly asks you to build a semester plan or recommend courses.\n");
        sb.append("4. Use the student's academic profile below as background reference when relevant.\n\n");
        sb.append("=== STUDENT ACADEMIC PROFILE ===\n");
        sb.append("- Name: ").append(student.getName()).append(" (ID: ").append(student.getStudentId()).append(")\n");
        sb.append("- Major: ").append(standing.majorName()).append(" (Faculty: ").append(standing.facultyName()).append(")\n");
        sb.append("- Catalog Year: ").append(standing.catalogYear() != null ? standing.catalogYear() : 2024)
          .append(" | Current Semester: ").append(standing.currentSemester() != null ? standing.currentSemester() : 1).append("\n");
        sb.append("- Career Goal / Track: ").append(StringUtils.hasText(student.getCareerGoal()) ? student.getCareerGoal() : "Not specified (General Software/CS track)").append("\n");
        sb.append("- Cumulative GPA: ").append(standing.formattedGpa())
          .append(" (Standing: ").append(standing.standingLabel()).append(")\n");
        sb.append("- Degree Progress: ").append(standing.completedCredits()).append(" / ").append(standing.totalDegreeCredits())
          .append(" ECTS completed (").append(standing.remainingCredits()).append(" ECTS remaining)\n");
        if (StringUtils.hasText(currentPage)) {
            sb.append("- Student's Active Page in App: ").append(currentPage.trim()).append("\n");
        }

        sb.append("\n=== COMPLETED COURSES (").append(completedCourses.size()).append(") ===\n");
        if (completedCourses.isEmpty()) {
            sb.append("None recorded yet.\n");
        } else {
            for (CompletedCourseDto c : completedCourses) {
                sb.append("- ").append(c.code()).append(" (").append(c.name()).append(", ")
                  .append(c.credits() != null ? c.credits() : 0).append(" ECTS): Grade=")
                  .append(c.letterGrade()).append(", Semester=").append(c.semesterTaken()).append("\n");
            }
        }

        sb.append("\n=== CURRENTLY ENROLLED COURSES (").append(standing.enrolledCredits()).append(" ECTS) ===\n");
        if (standing.enrolledCourses().isEmpty()) {
            sb.append("None enrolled this semester.\n");
        } else {
            for (EnrolledCourseDto e : standing.enrolledCourses()) {
                sb.append("- ").append(e.code()).append(" (").append(e.name()).append(", ").append(e.credits()).append(" ECTS)\n");
            }
        }

        sb.append("\n=== CURRENT SEMESTER PLANNER CART (").append(cart.totalCredits()).append(" / ").append(cart.maxCredits())
          .append(" ECTS, Workload: ").append(cart.workloadState()).append(") ===\n");
        if (cart.items().isEmpty()) {
            sb.append("Cart is currently empty.\n");
        } else {
            for (CartItemDto item : cart.items()) {
                sb.append("- ").append(item.code()).append(" (").append(item.name()).append(", ").append(item.credits()).append(" ECTS)\n");
            }
        }

        sb.append("\n=== ELIGIBLE NEXT COURSES (Prerequisites Met) ===\n");
        if (eligibleNextCourses.isEmpty()) {
            sb.append("No additional eligible courses found.\n");
        } else {
            eligibleNextCourses.forEach(line -> sb.append("- ").append(line).append("\n"));
        }

        sb.append("\n=== SDU KEY ACADEMIC REGULATIONS ===\n");
        sb.append("- Minimum passing grade: C (2.0) for core major courses, D (1.0) for general education.\n");
        sb.append("- Scholarship retention: GPA >= 3.00 with no failing grades.\n");
        sb.append("- Retakes: Up to 2 retakes per course; highest grade replaces previous in GPA.\n");
        sb.append("- Workload: Light <= 15 ECTS, Balanced 16-30 ECTS, Overload > 30 ECTS (max 40 ECTS).\n");

        sb.append("\n=== STUDENT QUESTION ===\n");
        sb.append(userMessage);

        return sb.toString();
    }
}
