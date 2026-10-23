package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.AcademicStandingDto;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import kz.edu.sdu.advisor.model.dto.CourseRecommendationDto;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates personalized, prerequisite-valid, workload-balanced semester course plans,
 * "Why this course?" explanations (US-43), and context-aware course/academic explanations
 * when external AI is unavailable or unconfigured.
 */
@Service
@RequiredArgsConstructor
public class SemesterPlanService {

    public static final int TARGET_BALANCED_CREDITS = 30;

    private static final List<String> PLAN_PHRASES = List.of(
            "build my next semester",
            "build semester",
            "build plan",
            "semester plan",
            "what electives fit my career",
            "what courses should i take",
            "what should i take",
            "recommend courses",
            "recommend electives",
            "suggest courses",
            "suggest electives",
            "собери семестр",
            "составь план",
            "план на семестр",
            "порекомендуй курсы",
            "посоветуй курсы",
            "порекомендуй предметы",
            "посоветуй предметы",
            "какие курсы взять",
            "какие предметы взять",
            "какие элективы"
    );

    private final CourseCompletionRepository courseCompletionRepository;
    private final CumulativeGpaService cumulativeGpaService;
    private final CartService cartService;
    private final CourseRepository courseRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final PrerequisiteRepository prerequisiteRepository;

    public boolean shouldIncludeRecommendations(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return false;
        }
        String lower = userMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("explain") || lower.contains("объясни") || lower.contains("расскажи про")) {
            return false;
        }
        return PLAN_PHRASES.stream().anyMatch(lower::contains);
    }

    @Transactional(readOnly = true)
    public List<CourseRecommendationDto> buildRecommendedPlan(Student student, String userMessage) {
        if (student == null) {
            return List.of();
        }

        List<CompletedCourseDto> completedCourses = courseCompletionRepository.findForStudent(student.getId())
                .stream()
                .map(CompletedCourseDto::of)
                .toList();

        CartDto cart = cartService.getCart(student.getStudentId());
        Set<Long> inCartCourseIds = cart.items().stream()
                .map(CartItemDto::courseId)
                .collect(Collectors.toSet());

        Set<String> satisfiedCodes = completedCourses.stream()
                .map(CompletedCourseDto::code)
                .collect(Collectors.toSet());
        cart.items().forEach(item -> satisfiedCodes.add(item.code()));

        Set<String> enrolledCodes = student.getEnrolledCourses() != null
                ? student.getEnrolledCourses().stream().map(Course::getCode).collect(Collectors.toSet())
                : Set.of();

        Map<Long, CurriculumCourse> curriculumByCourseId = loadCurriculumMap(student);
        Map<Long, List<String>> prereqsByCourseId = loadPrereqsMap();

        boolean electivesFocus = userMessage != null && userMessage.toLowerCase(Locale.ROOT).contains("elective");
        String careerGoal = StringUtils.hasText(student.getCareerGoal()) ? student.getCareerGoal().trim() : null;

        List<Course> eligible = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"))
                .stream()
                .filter(c -> !satisfiedCodes.contains(c.getCode()) && !enrolledCodes.contains(c.getCode()))
                .filter(c -> {
                    List<String> reqs = prereqsByCourseId.getOrDefault(c.getId(), List.of());
                    return satisfiedCodes.containsAll(reqs);
                })
                .sorted(Comparator.comparingInt((Course c) -> scoreCoursePriority(
                        c, curriculumByCourseId.get(c.getId()), electivesFocus, careerGoal))
                        .thenComparing(Course::getCode))
                .toList();

        List<CourseRecommendationDto> recommendations = new ArrayList<>();
        int runningCredits = cart.totalCredits();

        for (Course course : eligible) {
            if (recommendations.size() >= 5) {
                break;
            }
            int credits = course.getCredits() != null ? course.getCredits() : 3;
            if (runningCredits + credits > TARGET_BALANCED_CREDITS && !recommendations.isEmpty()) {
                continue;
            }

            CurriculumCourse cc = curriculumByCourseId.get(course.getId());
            String courseType = cc != null ? cc.getCourseType() : "ELECTIVE";
            int difficulty = course.getDifficulty() != null ? course.getDifficulty() : 3;
            List<String> prereqs = prereqsByCourseId.getOrDefault(course.getId(), List.of());
            String reason = buildReason(course, cc, prereqs, careerGoal);

            recommendations.add(new CourseRecommendationDto(
                    course.getId(),
                    course.getCode(),
                    course.getName(),
                    credits,
                    difficulty,
                    courseType,
                    reason,
                    inCartCourseIds.contains(course.getId())
            ));
            runningCredits += credits;
        }

        return recommendations;
    }

    public String buildFallbackAdvisorReply(Student student, List<CourseRecommendationDto> recommendations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Based on your academic record for ")
          .append(student.getMajor() != null ? student.getMajor().getName() : "your major");
        if (StringUtils.hasText(student.getCareerGoal())) {
            sb.append(" and your career goal (").append(student.getCareerGoal()).append(")");
        }
        sb.append(", here is a balanced semester plan (≤ 30 ECTS) with all prerequisites satisfied:");
        if (recommendations.isEmpty()) {
            sb.append(" All eligible courses are already in your cart or completed.");
        }
        return sb.toString();
    }

    /**
     * Generates a smart, context-aware reply for any student question (course explanations,
     * GPA/scholarship/retake rules, cart balance, or general standing) using real DB records.
     */
    @Transactional(readOnly = true)
    public String buildSmartFallbackReply(
            Student student,
            String userMessage,
            String currentPage,
            List<CourseRecommendationDto> recommendations) {

        if (student == null) {
            return "Please log in to receive personalized academic advice.";
        }

        String msg = userMessage != null ? userMessage.trim() : "";
        String lower = msg.toLowerCase(Locale.ROOT);

        List<CompletedCourseDto> completedCourses = courseCompletionRepository.findForStudent(student.getId())
                .stream()
                .map(CompletedCourseDto::of)
                .toList();
        Double calculatedGpa = cumulativeGpaService.calculate(completedCourses);
        AcademicStandingDto standing = AcademicStandingDto.of(student, calculatedGpa);
        CartDto cart = cartService.getCart(student.getStudentId());
        Map<Long, CurriculumCourse> curriculumByCourseId = loadCurriculumMap(student);
        Map<Long, List<String>> prereqsByCourseId = loadPrereqsMap();
        List<Course> allCourses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));

        // 1. Check if a specific course code or title is mentioned in the message
        Optional<Course> mentionedCourse = findMentionedCourse(msg, allCourses);
        if (mentionedCourse.isPresent() || lower.contains("explain") || lower.contains("объясни")) {
            if (mentionedCourse.isPresent()) {
                return formatSingleCourseExplanation(
                        mentionedCourse.get(), student, completedCourses, standing, cart, curriculumByCourseId, prereqsByCourseId);
            }
            return formatContextualCoursesExplanation(
                    student, completedCourses, standing, cart, allCourses, curriculumByCourseId, prereqsByCourseId);
        }

        // 2. Semester plan / recommendations
        if (shouldIncludeRecommendations(msg)) {
            return buildFallbackAdvisorReply(student, recommendations);
        }

        // 3. Cart / workload balance questions
        if (lower.contains("cart") || lower.contains("balance") || lower.contains("workload") || lower.contains("overload") || lower.contains("корзин")) {
            StringBuilder sb = new StringBuilder();
            sb.append("📊 Your Semester Cart currently has ").append(cart.totalCredits())
              .append(" / ").append(cart.maxCredits()).append(" ECTS (Workload: ").append(cart.workloadState()).append(").\n");
            if (cart.items().isEmpty()) {
                sb.append("Your cart is currently empty. Click 'Build my next semester' to get personalized course recommendations.");
            } else {
                sb.append("Courses in cart: ")
                  .append(cart.items().stream().map(i -> i.code() + " (" + i.credits() + " ECTS)").collect(Collectors.joining(", ")))
                  .append(".\n");
                if (cart.exceedsLimit()) {
                    sb.append("⚠️ Warning: You exceed the maximum limit by ").append(cart.excessCredits())
                      .append(" ECTS. Consider removing 1–2 courses.");
                } else if (cart.totalCredits() > TARGET_BALANCED_CREDITS) {
                    sb.append("ℹ️ Your load is above 30 ECTS (Overload). A balanced semester is typically 16–30 ECTS.");
                } else {
                    sb.append("✅ Your credit load is within the recommended SDU limit (≤ 30 ECTS).");
                }
            }
            return sb.toString();
        }

        // 4. GPA, Scholarship, Retake, Summer School, Registration & FAQ questions
        if (lower.contains("gpa") || lower.contains("scholarship") || lower.contains("retake")
                || lower.contains("passing") || lower.contains("summer") || lower.contains("registration")
                || lower.contains("standing") || lower.contains("faq")) {
            StringBuilder sb = new StringBuilder();
            sb.append("📌 Your Academic Standing Summary:\n")
              .append("• Cumulative GPA: ").append(standing.formattedGpa())
              .append(" (").append(standing.standingLabel()).append(")\n")
              .append("• Degree Progress: ").append(standing.completedCredits()).append(" / ")
              .append(standing.totalDegreeCredits()).append(" ECTS completed (")
              .append(standing.enrolledCredits()).append(" ECTS currently enrolled)\n\n");

            if (lower.contains("scholarship")) {
                sb.append("🎓 Scholarship Rule: State scholarship requires a GPA ≥ 3.00 with no failing grades (F/FX/AW). ");
                if (calculatedGpa == null) {
                    sb.append("Your transcript does not have graded courses recorded yet — add your grades on the Transcript (/profile) page to see your exact GPA.");
                } else if (calculatedGpa >= 3.0) {
                    sb.append("Your current GPA (").append(standing.formattedGpa()).append(") meets the ≥ 3.00 scholarship threshold!");
                } else {
                    sb.append("Your current GPA (").append(standing.formattedGpa()).append(") is below 3.00. Prioritizing A/B+ grades in upcoming high-credit courses will help raise it.");
                }
            } else if (lower.contains("retake")) {
                sb.append("🔄 Retake Policy: At SDU, you may retake a course up to 2 times. The highest grade achieved replaces the previous grade in your cumulative GPA calculation.");
            } else if (lower.contains("summer")) {
                sb.append("☀️ Summer School Policy: You can enroll in up to 12 ECTS during the summer term to catch up on prerequisites or lighten your regular semester workload.");
            } else if (lower.contains("passing")) {
                sb.append("✅ Minimum Passing Grade: Core major courses require at least 'C' (2.00), while general education courses require at least 'D' (1.00).");
            } else {
                sb.append("📐 GPA Calculation: Cumulative GPA is weighted by ECTS credits using the SDU Handbook scale (A=4.00, A-=3.67, B+=3.33, B=3.00, B-=2.67, C+=2.33, C=2.00, C-=1.67, D+=1.33, D=1.00, F/FX/AW=0.00). You can update your grades anytime on the Transcript (/profile) page.");
            }
            return sb.toString();
        }

        // 5. General personalized advisor summary
        return String.format(
                "👋 Hello %s! You are in %s (Semester %d) with %d / %d ECTS completed (GPA: %s — %s) and %d ECTS in your Planner cart.%n%n"
                        + "You can ask me to:%n"
                        + "• \"Build my next semester\" — generate a prerequisite-valid, balanced course plan%n"
                        + "• \"What electives fit my career?\" — match electives to your career goal (%s)%n"
                        + "• \"Explain INF202\" (or \"Explain this course\") — inspect course difficulty, credits, and prerequisites.",
                student.getName(),
                standing.majorName(),
                standing.currentSemester() != null ? standing.currentSemester() : 1,
                standing.completedCredits(),
                standing.totalDegreeCredits(),
                standing.formattedGpa(),
                standing.standingLabel(),
                cart.totalCredits(),
                StringUtils.hasText(student.getCareerGoal()) ? student.getCareerGoal() : "Information Systems / Software Engineering"
        );
    }

    private Optional<Course> findMentionedCourse(String message, List<Course> allCourses) {
        if (!StringUtils.hasText(message)) {
            return Optional.empty();
        }
        String upper = message.toUpperCase(Locale.ROOT);
        String upperCompact = upper.replaceAll("\\s+", "");
        for (Course c : allCourses) {
            String codeUpper = c.getCode().toUpperCase(Locale.ROOT);
            String codeCompact = codeUpper.replaceAll("\\s+", "");
            if (upper.contains(codeUpper) || upperCompact.contains(codeCompact)) {
                return Optional.of(c);
            }
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (Course c : allCourses) {
            if (c.getName() != null && c.getName().length() >= 5
                    && lower.contains(c.getName().toLowerCase(Locale.ROOT))) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private String formatSingleCourseExplanation(
            Course course,
            Student student,
            List<CompletedCourseDto> completedCourses,
            AcademicStandingDto standing,
            CartDto cart,
            Map<Long, CurriculumCourse> curriculumByCourseId,
            Map<Long, List<String>> prereqsByCourseId) {

        CurriculumCourse cc = curriculumByCourseId.get(course.getId());
        String type = cc != null ? cc.getCourseType() : "ELECTIVE";
        Integer recSem = cc != null ? cc.getSemester() : null;
        int diff = course.getDifficulty() != null ? course.getDifficulty() : 3;
        List<String> prereqs = prereqsByCourseId.getOrDefault(course.getId(), List.of());

        Set<String> completedCodes = completedCourses.stream().map(CompletedCourseDto::code).collect(Collectors.toSet());
        Set<String> enrolledCodes = standing.enrolledCourses().stream().map(EnrolledCourseDto::code).collect(Collectors.toSet());
        Set<String> cartCodes = cart.items().stream().map(CartItemDto::code).collect(Collectors.toSet());

        String status;
        if (completedCodes.contains(course.getCode())) {
            status = "Completed ✓";
        } else if (enrolledCodes.contains(course.getCode())) {
            status = "Currently Enrolled 📚";
        } else if (cartCodes.contains(course.getCode())) {
            status = "In Your Semester Cart 🛒";
        } else {
            Set<String> satisfied = completedCourses.stream().map(CompletedCourseDto::code).collect(Collectors.toSet());
            satisfied.addAll(cartCodes);
            boolean prereqsMet = satisfied.containsAll(prereqs);
            status = prereqsMet ? "Eligible to take next semester ✅" : "Missing prerequisites ⚠️";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📘 ").append(course.getCode()).append(" — ").append(course.getName()).append("\n");
        sb.append("• Credits: ").append(course.getCredits()).append(" ECTS | Type: ").append(type);
        if (recSem != null) {
            sb.append(" (Recommended Semester ").append(recSem).append(")");
        }
        sb.append("\n• Difficulty: ").append(diff).append(" / 5\n");
        sb.append("• Prerequisites: ").append(prereqs.isEmpty() ? "None" : String.join(", ", prereqs)).append("\n");
        sb.append("• Your Status: ").append(status).append("\n");
        if (StringUtils.hasText(course.getDescription())) {
            sb.append("• Overview: ").append(course.getDescription());
        }
        return sb.toString();
    }

    private String formatContextualCoursesExplanation(
            Student student,
            List<CompletedCourseDto> completedCourses,
            AcademicStandingDto standing,
            CartDto cart,
            List<Course> allCourses,
            Map<Long, CurriculumCourse> curriculumByCourseId,
            Map<Long, List<String>> prereqsByCourseId) {

        Map<String, Course> courseByCode = allCourses.stream()
                .collect(Collectors.toMap(Course::getCode, c -> c, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        if (!cart.items().isEmpty()) {
            sb.append("🛒 Courses currently in your Semester Cart (").append(cart.totalCredits()).append(" ECTS):\n\n");
            for (CartItemDto item : cart.items()) {
                Course c = courseByCode.get(item.code());
                appendCourseBrief(sb, c, item.code(), item.name(), item.credits(), curriculumByCourseId, prereqsByCourseId);
            }
        } else if (!standing.enrolledCourses().isEmpty()) {
            sb.append("📚 Your Currently Enrolled Courses (").append(standing.enrolledCredits()).append(" ECTS):\n\n");
            for (EnrolledCourseDto enrolled : standing.enrolledCourses()) {
                Course c = courseByCode.get(enrolled.code());
                appendCourseBrief(sb, c, enrolled.code(), enrolled.name(), enrolled.credits(), curriculumByCourseId, prereqsByCourseId);
            }
        } else {
            sb.append("📘 Top Eligible Courses for ").append(standing.majorName()).append(":\n\n");
            List<CourseRecommendationDto> recs = buildRecommendedPlan(student, "Build my next semester");
            for (CourseRecommendationDto rec : recs) {
                Course c = courseByCode.get(rec.code());
                appendCourseBrief(sb, c, rec.code(), rec.name(), rec.credits(), curriculumByCourseId, prereqsByCourseId);
            }
        }
        sb.append("\n💡 Tip: Ask about any specific course code (e.g., \"Explain INF202\" or \"Explain CSS217\") or click \"ℹ️ Explain\" on any course in the Planner.");
        return sb.toString();
    }

    private void appendCourseBrief(
            StringBuilder sb,
            Course c,
            String code,
            String name,
            int credits,
            Map<Long, CurriculumCourse> curriculumByCourseId,
            Map<Long, List<String>> prereqsByCourseId) {

        int diff = (c != null && c.getDifficulty() != null) ? c.getDifficulty() : 3;
        String desc = (c != null && StringUtils.hasText(c.getDescription()))
                ? c.getDescription()
                : "University curriculum course.";
        List<String> prereqs = c != null ? prereqsByCourseId.getOrDefault(c.getId(), List.of()) : List.of();
        CurriculumCourse cc = c != null ? curriculumByCourseId.get(c.getId()) : null;
        String type = cc != null ? cc.getCourseType() : "COURSE";

        sb.append("• ").append(code).append(" — ").append(name)
          .append(" (").append(credits).append(" ECTS, ").append(type)
          .append(", Difficulty ").append(diff).append("/5)\n")
          .append("  ").append(desc);
        if (!prereqs.isEmpty()) {
            sb.append(" [Prereq: ").append(String.join(", ", prereqs)).append("]");
        }
        sb.append("\n\n");
    }

    private Map<Long, CurriculumCourse> loadCurriculumMap(Student student) {
        if (student.getMajor() == null || student.getCatalogYear() == null) {
            return Map.of();
        }
        return curriculumCourseRepository.findByMajorAndCatalogYearOrderBySemesterAsc(
                        student.getMajor(), student.getCatalogYear())
                .stream()
                .collect(Collectors.toMap(cc -> cc.getCourse().getId(), cc -> cc, (a, b) -> a));
    }

    private Map<Long, List<String>> loadPrereqsMap() {
        return prerequisiteRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCourse().getId(),
                        Collectors.mapping(p -> p.getPrerequisiteCourse().getCode(), Collectors.toList())
                ));
    }

    private int scoreCoursePriority(
            Course course,
            CurriculumCourse cc,
            boolean electivesFocus,
            String careerGoal) {

        String type = cc != null ? cc.getCourseType() : "ELECTIVE";
        int semester = cc != null && cc.getSemester() != null ? cc.getSemester() : 6;
        boolean matchesCareer = matchesCareerGoal(course, careerGoal);
        boolean isGeneralMde = course.getCode() != null && course.getCode().startsWith("MDE");
        int credits = course.getCredits() != null ? course.getCredits() : 3;

        if (electivesFocus) {
            if ("ELECTIVE".equalsIgnoreCase(type) && matchesCareer) return 10 + semester;
            if ("ELECTIVE".equalsIgnoreCase(type) && !isGeneralMde) return 20 + semester;
            if ("ELECTIVE".equalsIgnoreCase(type)) return 30 + semester;
            return 40 + semester;
        } else {
            if ("CORE".equalsIgnoreCase(type) && !isGeneralMde && credits >= 5) return 10 + semester;
            if (matchesCareer) return 20 + semester;
            if ("CORE".equalsIgnoreCase(type) && credits >= 5) return 25 + semester;
            if ("CORE".equalsIgnoreCase(type)) return 35 + semester;
            return 45 + semester;
        }
    }

    private boolean matchesCareerGoal(Course course, String careerGoal) {
        if (!StringUtils.hasText(careerGoal)) {
            return false;
        }
        String goalLower = careerGoal.toLowerCase(Locale.ROOT);
        String text = ((course.getName() != null ? course.getName() : "") + " "
                + (course.getDescription() != null ? course.getDescription() : "")).toLowerCase(Locale.ROOT);

        for (String token : goalLower.split("[\\s,/;-]+")) {
            if (token.length() >= 3 && text.contains(token)) {
                return true;
            }
        }
        if ((goalLower.contains("ai") || goalLower.contains("data") || goalLower.contains("ml"))
                && (text.contains("intelligence") || text.contains("machine learning") || text.contains("data") || text.contains("neural"))) {
            return true;
        }
        if ((goalLower.contains("backend") || goalLower.contains("software") || goalLower.contains("web") || goalLower.contains("cloud"))
                && (text.contains("web") || text.contains("cloud") || text.contains("distributed") || text.contains("database") || text.contains("software"))) {
            return true;
        }
        return false;
    }

    private String buildReason(Course course, CurriculumCourse cc, List<String> prereqs, String careerGoal) {
        StringBuilder reason = new StringBuilder();
        if (cc != null && "CORE".equalsIgnoreCase(cc.getCourseType())) {
            reason.append("Core curriculum requirement (Semester ").append(cc.getSemester()).append(")");
        } else if (matchesCareerGoal(course, careerGoal)) {
            reason.append("Elective aligned with your career goal (").append(careerGoal).append(")");
        } else {
            reason.append("Recommended degree course to maintain a balanced semester workload");
        }

        if (!prereqs.isEmpty()) {
            reason.append("; prerequisites satisfied (").append(String.join(", ", prereqs)).append(")");
        }
        int diff = course.getDifficulty() != null ? course.getDifficulty() : 3;
        reason.append(" · Difficulty ").append(diff).append("/5.");
        return reason.toString();
    }
}
