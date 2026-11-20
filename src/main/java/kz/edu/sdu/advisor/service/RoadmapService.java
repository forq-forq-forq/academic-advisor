package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.CartItem;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.CurriculumCourse;
import kz.edu.sdu.advisor.model.Prerequisite;
import kz.edu.sdu.advisor.model.RoadmapItem;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapCourseCardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapSemesterDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CurriculumCourseRepository;
import kz.edu.sdu.advisor.repository.PrerequisiteRepository;
import kz.edu.sdu.advisor.repository.RoadmapItemRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds and persists the student's 8-semester degree roadmap (US-18).
 */
@Service
@RequiredArgsConstructor
public class RoadmapService {

    public static final int TOTAL_SEMESTERS = 8;
    public static final int MAX_RECOMMENDED_CREDITS = 30;
    public static final String DEFAULT_PACING_STRATEGY = "BALANCED";

    private final RoadmapItemRepository roadmapItemRepository;
    private final StudentRepository studentRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public RoadmapBoardDto getRoadmap(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<RoadmapItem> items = roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);
        if (items.isEmpty()) {
            items = initializeDefaultRoadmap(student);
        }
        return buildBoard(student, items, DEFAULT_PACING_STRATEGY,
                "Balanced 4-year distribution aligned with your curriculum.");
    }

    protected List<RoadmapItem> initializeDefaultRoadmap(Student student) {
        String studentId = student.getStudentId();
        int currentSem = clampSemester(student.getCurrentSemester() != null ? student.getCurrentSemester() : 1);

        Map<Long, CurriculumCourse> curriculumByCourseId = loadCurriculumByCourseId(student);
        List<CurriculumCourse> curriculumList = loadCurriculumList(student);

        Map<Long, RoadmapItem> placedByCourseId = new LinkedHashMap<>();
        Map<Integer, Integer> semesterCredits = new HashMap<>();

        if (student.getCompletedCourses() != null) {
            for (Course c : student.getCompletedCourses()) {
                if (c == null || c.getId() == null) continue;
                CurriculumCourse cc = curriculumByCourseId.get(c.getId());
                int sem = cc != null ? clampSemester(cc.getSemester()) : Math.max(1, currentSem - 1);
                if (sem > currentSem && currentSem > 1) {
                    sem = currentSem - 1;
                }
                addPlacement(placedByCourseId, semesterCredits, student, c, sem);
            }
        }

        if (student.getEnrolledCourses() != null) {
            for (Course c : student.getEnrolledCourses()) {
                if (c == null || c.getId() == null || placedByCourseId.containsKey(c.getId())) continue;
                addPlacement(placedByCourseId, semesterCredits, student, c, currentSem);
            }
        }

        List<CartItem> cartItems = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(studentId);
        int nextSem = Math.min(TOTAL_SEMESTERS, currentSem + 1);
        for (CartItem cartItem : cartItems) {
            Course c = cartItem.getCourse();
            if (c == null || c.getId() == null || placedByCourseId.containsKey(c.getId())) continue;
            addPlacement(placedByCourseId, semesterCredits, student, c, nextSem);
        }

        // Place remaining CORE courses in their recommended curriculum semester
        for (CurriculumCourse cc : curriculumList) {
            Course c = cc.getCourse();
            if (c == null || c.getId() == null || placedByCourseId.containsKey(c.getId())) continue;
            if ("CORE".equalsIgnoreCase(cc.getCourseType())) {
                int targetSem = clampSemester(cc.getSemester());
                addPlacement(placedByCourseId, semesterCredits, student, c, targetSem);
            }
        }

        // Fill remaining capacity in each semester (up to 30 ECTS) with career-aligned electives
        String careerGoal = student.getCareerGoal();
        List<CurriculumCourse> remainingElectives = curriculumList.stream()
                .filter(cc -> cc.getCourse() != null && cc.getCourse().getId() != null)
                .filter(cc -> !placedByCourseId.containsKey(cc.getCourse().getId()))
                .sorted(Comparator
                        .comparingInt((CurriculumCourse cc) -> matchesCareerGoal(cc.getCourse(), careerGoal) ? 0 : 1)
                        .thenComparingInt(cc -> cc.getSemester() != null ? cc.getSemester() : 6)
                        .thenComparing(cc -> cc.getCourse().getCode()))
                .toList();

        for (CurriculumCourse cc : remainingElectives) {
            Course c = cc.getCourse();
            int credits = c.getCredits() != null ? c.getCredits() : 5;
            int targetSem = clampSemester(cc.getSemester());
            int currentLoad = semesterCredits.getOrDefault(targetSem, 0);
            if (currentLoad + credits <= MAX_RECOMMENDED_CREDITS) {
                addPlacement(placedByCourseId, semesterCredits, student, c, targetSem);
            }
        }

        // Ensure every semester 1..8 has at least one course if catalog courses exist
        for (int sem = 1; sem <= TOTAL_SEMESTERS; sem++) {
            final int s = sem;
            boolean emptySem = placedByCourseId.values().stream().noneMatch(item -> item.getSemester() == s);
            if (emptySem) {
                for (CurriculumCourse cc : remainingElectives) {
                    Course c = cc.getCourse();
                    if (!placedByCourseId.containsKey(c.getId())) {
                        addPlacement(placedByCourseId, semesterCredits, student, c, s);
                        break;
                    }
                }
            }
        }

        if (placedByCourseId.isEmpty()) {
            return List.of();
        }
        return roadmapItemRepository.saveAll(placedByCourseId.values());
    }

    protected RoadmapBoardDto buildBoard(
            Student student,
            List<RoadmapItem> items,
            String pacingStrategy,
            String pacingSummary) {

        Map<Long, CurriculumCourse> curriculumByCourseId = loadCurriculumByCourseId(student);
        Map<Long, List<Course>> prereqMap = loadPrerequisiteMap();

        Set<Long> completedIds = student.getCompletedCourses() == null
                ? Set.of()
                : student.getCompletedCourses().stream().map(Course::getId).collect(Collectors.toSet());
        Set<Long> enrolledIds = student.getEnrolledCourses() == null
                ? Set.of()
                : student.getEnrolledCourses().stream().map(Course::getId).collect(Collectors.toSet());

        Map<Long, Integer> placedSemesterByCourseId = new HashMap<>();
        for (RoadmapItem item : items) {
            if (item.getCourse() != null && item.getCourse().getId() != null) {
                placedSemesterByCourseId.put(item.getCourse().getId(), clampSemester(item.getSemester()));
            }
        }

        Map<Integer, List<RoadmapItem>> itemsBySemester = new HashMap<>();
        for (int s = 1; s <= TOTAL_SEMESTERS; s++) {
            itemsBySemester.put(s, new ArrayList<>());
        }
        for (RoadmapItem item : items) {
            int sem = clampSemester(item.getSemester());
            itemsBySemester.get(sem).add(item);
        }

        List<RoadmapSemesterDto> semesterDtos = new ArrayList<>();
        int totalCreditsAll = 0;
        int totalCoursesAll = 0;
        int totalPrereqConflicts = 0;
        int totalSeasonConflicts = 0;
        int overloadedCount = 0;

        for (int sem = 1; sem <= TOTAL_SEMESTERS; sem++) {
            String semesterSeason = (sem % 2 == 1) ? "FALL" : "SPRING";
            String seasonDisplay = (sem % 2 == 1) ? "Fall" : "Spring";
            List<RoadmapItem> semItems = itemsBySemester.get(sem).stream()
                    .filter(i -> i.getCourse() != null)
                    .sorted(Comparator.comparing(i -> i.getCourse().getCode()))
                    .toList();

            List<RoadmapCourseCardDto> cards = new ArrayList<>();
            int semCredits = 0;
            int semPrereqConflicts = 0;
            int semSeasonConflicts = 0;

            for (RoadmapItem item : semItems) {
                Course course = item.getCourse();
                int credits = course.getCredits() != null ? course.getCredits() : 0;
                int difficulty = course.getDifficulty() != null ? course.getDifficulty() : 3;
                CurriculumCourse cc = curriculumByCourseId.get(course.getId());
                String courseType = cc != null && cc.getCourseType() != null ? cc.getCourseType() : "ELECTIVE";
                String category = resolveCourseCategory(course.getCode(), courseType);
                String courseSeason = course.getSeason() != null && !course.getSeason().isBlank()
                        ? course.getSeason().toUpperCase(Locale.ROOT)
                        : "BOTH";
                String status = completedIds.contains(course.getId()) ? "COMPLETED"
                        : (enrolledIds.contains(course.getId()) ? "ENROLLED" : "PLANNED");

                List<Course> reqs = prereqMap.getOrDefault(course.getId(), List.of());
                List<String> reqCodes = reqs.stream().map(Course::getCode).toList();
                List<String> violatedPrereqs = new ArrayList<>();
                if (!"COMPLETED".equals(status)) {
                    for (Course req : reqs) {
                        if (req == null || req.getId() == null) continue;
                        if (completedIds.contains(req.getId())) {
                            Integer reqSem = placedSemesterByCourseId.get(req.getId());
                            if (reqSem != null && reqSem >= sem) {
                                violatedPrereqs.add(req.getCode());
                            }
                            continue;
                        }
                        Integer reqSem = placedSemesterByCourseId.get(req.getId());
                        if (reqSem == null || reqSem >= sem) {
                            violatedPrereqs.add(req.getCode());
                        }
                    }
                }
                boolean prereqViolation = !violatedPrereqs.isEmpty();
                if (prereqViolation) semPrereqConflicts++;

                boolean seasonMismatch = false;
                String seasonWarning = null;
                if (!"BOTH".equalsIgnoreCase(courseSeason) && !courseSeason.equalsIgnoreCase(semesterSeason)) {
                    seasonMismatch = true;
                    String offeringLabel = "FALL".equalsIgnoreCase(courseSeason) ? "Fall" : "Spring";
                    seasonWarning = course.getCode() + " is offered only in " + offeringLabel
                            + " (Semester " + sem + " is " + seasonDisplay + ")";
                    semSeasonConflicts++;
                }

                semCredits += credits;
                cards.add(new RoadmapCourseCardDto(
                        course.getId(),
                        course.getCode(),
                        course.getName(),
                        credits,
                        difficulty,
                        courseType,
                        category,
                        courseSeason,
                        status,
                        sem,
                        reqCodes,
                        prereqViolation,
                        violatedPrereqs,
                        seasonMismatch,
                        seasonWarning
                ));
            }

            boolean overloaded = semCredits > MAX_RECOMMENDED_CREDITS;
            String workloadState = overloaded ? "Overload" : (semCredits < 15 ? "Light" : "Balanced");
            if (overloaded) overloadedCount++;

            totalCreditsAll += semCredits;
            totalCoursesAll += cards.size();
            totalPrereqConflicts += semPrereqConflicts;
            totalSeasonConflicts += semSeasonConflicts;

            semesterDtos.add(new RoadmapSemesterDto(
                    sem,
                    "Semester " + sem + " (" + seasonDisplay + ")",
                    semesterSeason,
                    semCredits,
                    workloadState,
                    overloaded,
                    semPrereqConflicts,
                    semSeasonConflicts,
                    cards
            ));
        }

        return new RoadmapBoardDto(
                semesterDtos,
                totalCreditsAll,
                totalCoursesAll,
                totalPrereqConflicts,
                totalSeasonConflicts,
                overloadedCount,
                pacingStrategy,
                pacingSummary
        );
    }

    private void addPlacement(
            Map<Long, RoadmapItem> placedByCourseId,
            Map<Integer, Integer> semesterCredits,
            Student student,
            Course course,
            int semester) {
        int clamped = clampSemester(semester);
        placedByCourseId.put(course.getId(), new RoadmapItem(student, course, clamped));
        int credits = course.getCredits() != null ? course.getCredits() : 0;
        semesterCredits.merge(clamped, credits, Integer::sum);
    }

    protected int clampSemester(Integer semester) {
        if (semester == null || semester < 1) return 1;
        return Math.min(TOTAL_SEMESTERS, semester);
    }

    protected List<CurriculumCourse> loadCurriculumList(Student student) {
        if (student.getMajor() == null || student.getCatalogYear() == null) {
            return List.of();
        }
        return curriculumCourseRepository.findByMajor_CodeAndCatalogYearOrderBySemesterAsc(
                student.getMajor().getCode(), student.getCatalogYear());
    }

    protected Map<Long, CurriculumCourse> loadCurriculumByCourseId(Student student) {
        Map<Long, CurriculumCourse> map = new HashMap<>();
        for (CurriculumCourse cc : loadCurriculumList(student)) {
            if (cc.getCourse() != null && cc.getCourse().getId() != null) {
                map.put(cc.getCourse().getId(), cc);
            }
        }
        return map;
    }

    protected Map<Long, List<Course>> loadPrerequisiteMap() {
        return prerequisiteRepository.findAllWithCourses().stream()
                .filter(p -> p.getCourse() != null && p.getPrerequisiteCourse() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCourse().getId(),
                        Collectors.mapping(Prerequisite::getPrerequisiteCourse, Collectors.toList())
                ));
    }

    private String resolveCourseCategory(String code, String courseType) {
        if ("GENERAL".equalsIgnoreCase(courseType)) {
            return "GENERAL";
        }
        if (code != null && code.toUpperCase(Locale.ROOT).startsWith("MDE")) {
            return "GENERAL";
        }
        if ("CORE".equalsIgnoreCase(courseType)) {
            return "CORE";
        }
        return "ELECTIVE";
    }

    private boolean matchesCareerGoal(Course course, String careerGoal) {
        if (!StringUtils.hasText(careerGoal)) {
            return false;
        }
        String goalLower = careerGoal.toLowerCase(Locale.ROOT);
        String text = ((course.getName() != null ? course.getName() : "") + " "
                + (course.getDescription() != null ? course.getDescription() : "")).toLowerCase(Locale.ROOT);
        for (String token : goalLower.split("[\\s,/;&-]+")) {
            if (token.length() >= 3
                    && !Set.of("and", "the", "for", "with", "into", "from").contains(token)
                    && text.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
