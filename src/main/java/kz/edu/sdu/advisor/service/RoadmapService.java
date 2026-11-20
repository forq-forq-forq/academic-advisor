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
import kz.edu.sdu.advisor.repository.CourseRepository;
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
    private final CourseRepository courseRepository;
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

    @Transactional
    public RoadmapBoardDto moveCourseToSemester(String studentId, Long courseId, int targetSemester) {
        if (targetSemester < 1 || targetSemester > TOTAL_SEMESTERS) {
            throw new IllegalArgumentException("Target semester must be between 1 and " + TOTAL_SEMESTERS + ".");
        }
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId).isEmpty()) {
            initializeDefaultRoadmap(student);
        }

        RoadmapItem item = roadmapItemRepository.findByStudent_StudentIdAndCourse_Id(studentId, courseId)
                .orElseGet(() -> new RoadmapItem(student, course, targetSemester));
        item.setSemester(targetSemester);
        roadmapItemRepository.saveAndFlush(item);

        List<RoadmapItem> updatedItems = roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);
        return buildBoard(student, updatedItems, DEFAULT_PACING_STRATEGY,
                "Custom placement updated (Semester " + targetSemester + ").");
    }

    @Transactional
    public RoadmapBoardDto removeCourseFromRoadmap(String studentId, Long courseId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId).isEmpty()) {
            initializeDefaultRoadmap(student);
        }

        roadmapItemRepository.deleteByStudent_StudentIdAndCourse_Id(studentId, courseId);
        roadmapItemRepository.flush();

        List<RoadmapItem> updatedItems = roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);
        return buildBoard(student, updatedItems, DEFAULT_PACING_STRATEGY,
                "Course removed from roadmap.");
    }

    @Transactional
    public RoadmapBoardDto applyPacingStrategy(String studentId, String strategy) {
        if (!StringUtils.hasText(strategy)) {
            throw new IllegalArgumentException("Pacing strategy is required.");
        }
        String normalized = strategy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!"BALANCED".equals(normalized) && !"FAST_TRACK".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported pacing strategy: " + strategy);
        }

        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<RoadmapItem> currentItems = roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);
        if (currentItems.isEmpty()) {
            currentItems = initializeDefaultRoadmap(student);
        }

        int currentSem = clampSemester(student.getCurrentSemester() != null ? student.getCurrentSemester() : 1);
        int futureStart = Math.min(TOTAL_SEMESTERS, currentSem + 1);

        Set<Long> completedIds = student.getCompletedCourses() == null
                ? Set.of()
                : student.getCompletedCourses().stream().map(Course::getId).collect(Collectors.toSet());
        Set<Long> enrolledIds = student.getEnrolledCourses() == null
                ? Set.of()
                : student.getEnrolledCourses().stream().map(Course::getId).collect(Collectors.toSet());

        Map<Long, CurriculumCourse> curriculumByCourseId = loadCurriculumByCourseId(student);
        Map<Long, List<Course>> prereqMap = loadPrerequisiteMap();

        Map<Long, Integer> fixedSemesterByCourseId = new HashMap<>();
        Map<Integer, Integer> semCredits = new HashMap<>();
        for (int s = 1; s <= TOTAL_SEMESTERS; s++) {
            semCredits.put(s, 0);
        }

        List<RoadmapItem> movableItems = new ArrayList<>();
        for (RoadmapItem item : currentItems) {
            Course c = item.getCourse();
            if (c == null || c.getId() == null) continue;
            int sem = clampSemester(item.getSemester());
            boolean isLocked = completedIds.contains(c.getId())
                    || enrolledIds.contains(c.getId())
                    || sem <= currentSem;
            if (isLocked) {
                fixedSemesterByCourseId.put(c.getId(), sem);
                semCredits.merge(sem, c.getCredits() != null ? c.getCredits() : 0, Integer::sum);
            } else {
                movableItems.add(item);
            }
        }

        List<RoadmapItem> orderedMovables = topologicalSortItems(movableItems, prereqMap, curriculumByCourseId);
        Set<Long> movableIds = orderedMovables.stream()
                .map(i -> i.getCourse().getId())
                .collect(Collectors.toSet());
        Map<Long, Integer> downstreamDepth = computeDownstreamDepths(movableIds, prereqMap);
        Map<Long, Integer> assignedSemesterByCourseId = new HashMap<>(fixedSemesterByCourseId);

        for (RoadmapItem item : orderedMovables) {
            Course c = item.getCourse();
            int credits = c.getCredits() != null ? c.getCredits() : 0;
            int minSem = futureStart;
            for (Course req : prereqMap.getOrDefault(c.getId(), List.of())) {
                if (req == null || req.getId() == null) continue;
                Integer reqSem = assignedSemesterByCourseId.get(req.getId());
                if (reqSem != null) {
                    minSem = Math.max(minSem, Math.min(TOTAL_SEMESTERS, reqSem + 1));
                }
            }
            int maxSem = Math.max(minSem, TOTAL_SEMESTERS - downstreamDepth.getOrDefault(c.getId(), 0));

            CurriculumCourse cc = curriculumByCourseId.get(c.getId());
            int recommendedSem = cc != null && cc.getSemester() != null
                    ? clampSemester(cc.getSemester())
                    : minSem;

            int chosenSem;
            if ("FAST_TRACK".equals(normalized)) {
                chosenSem = chooseFastTrackSemester(c, minSem, maxSem, credits, semCredits);
            } else {
                chosenSem = chooseBalancedSemester(c, minSem, maxSem, recommendedSem, credits, semCredits);
            }

            item.setSemester(chosenSem);
            assignedSemesterByCourseId.put(c.getId(), chosenSem);
            semCredits.merge(chosenSem, credits, Integer::sum);
        }

        roadmapItemRepository.saveAllAndFlush(currentItems);
        List<RoadmapItem> updatedItems = roadmapItemRepository.findByStudent_StudentIdOrderBySemesterAscCourse_CodeAsc(studentId);
        String summary = "FAST_TRACK".equals(normalized)
                ? "Fast-track applied: core and eligible courses front-loaded into earlier semesters to free up Year 4 (Semesters 7–8) for an internship."
                : "Balanced 4-year plan applied: credits distributed evenly across remaining semesters (≤ 30 ECTS target).";
        return buildBoard(student, updatedItems, normalized, summary);
    }

    private List<RoadmapItem> topologicalSortItems(
            List<RoadmapItem> items,
            Map<Long, List<Course>> prereqMap,
            Map<Long, CurriculumCourse> curriculumByCourseId) {
        Set<Long> movableCourseIds = items.stream()
                .map(i -> i.getCourse().getId())
                .collect(Collectors.toSet());
        List<RoadmapItem> remaining = new ArrayList<>(items);
        remaining.sort(Comparator
                .comparingInt((RoadmapItem i) -> {
                    CurriculumCourse cc = curriculumByCourseId.get(i.getCourse().getId());
                    return cc != null && cc.getSemester() != null ? cc.getSemester() : 8;
                })
                .thenComparing(i -> i.getCourse().getCode()));

        List<RoadmapItem> result = new ArrayList<>();
        Set<Long> placed = new HashSet<>();
        while (!remaining.isEmpty()) {
            boolean progressed = false;
            var it = remaining.iterator();
            while (it.hasNext()) {
                RoadmapItem item = it.next();
                Long cid = item.getCourse().getId();
                boolean depsSatisfied = prereqMap.getOrDefault(cid, List.of()).stream()
                        .map(Course::getId)
                        .filter(movableCourseIds::contains)
                        .allMatch(placed::contains);
                if (depsSatisfied) {
                    result.add(item);
                    placed.add(cid);
                    it.remove();
                    progressed = true;
                }
            }
            if (!progressed) {
                result.addAll(remaining);
                break;
            }
        }
        return result;
    }

    private Map<Long, Integer> computeDownstreamDepths(
            Set<Long> movableIds,
            Map<Long, List<Course>> prereqMap) {
        Map<Long, List<Long>> dependents = new HashMap<>();
        for (Long courseId : movableIds) {
            for (Course req : prereqMap.getOrDefault(courseId, List.of())) {
                if (req != null && req.getId() != null && movableIds.contains(req.getId())) {
                    dependents.computeIfAbsent(req.getId(), k -> new ArrayList<>()).add(courseId);
                }
            }
        }
        Map<Long, Integer> depths = new HashMap<>();
        for (Long courseId : movableIds) {
            computeDepthMemo(courseId, dependents, depths, new HashSet<>());
        }
        return depths;
    }

    private int computeDepthMemo(
            Long courseId,
            Map<Long, List<Long>> dependents,
            Map<Long, Integer> depths,
            Set<Long> visiting) {
        if (depths.containsKey(courseId)) {
            return depths.get(courseId);
        }
        if (!visiting.add(courseId)) {
            return 0;
        }
        int maxChild = 0;
        for (Long depId : dependents.getOrDefault(courseId, List.of())) {
            maxChild = Math.max(maxChild, 1 + computeDepthMemo(depId, dependents, depths, visiting));
        }
        visiting.remove(courseId);
        depths.put(courseId, maxChild);
        return maxChild;
    }

    private int chooseBalancedSemester(
            Course course,
            int minSem,
            int maxSem,
            int recommendedSem,
            int credits,
            Map<Integer, Integer> semCredits) {
        int bestSem = minSem;
        int bestLoad = Integer.MAX_VALUE;
        boolean bestSeasonMatch = false;
        int bestDistance = Integer.MAX_VALUE;

        for (int s = minSem; s <= maxSem; s++) {
            int load = semCredits.getOrDefault(s, 0);
            boolean seasonMatch = isSeasonCompatible(course, s);
            int distance = Math.abs(s - Math.min(maxSem, Math.max(minSem, recommendedSem)));
            if (load < bestLoad
                    || (load == bestLoad && seasonMatch && !bestSeasonMatch)
                    || (load == bestLoad && seasonMatch == bestSeasonMatch && distance < bestDistance)) {
                bestLoad = load;
                bestSeasonMatch = seasonMatch;
                bestDistance = distance;
                bestSem = s;
            }
        }
        return bestSem;
    }

    private boolean isSeasonCompatible(Course course, int semester) {
        if (course == null || course.getSeason() == null || "BOTH".equalsIgnoreCase(course.getSeason())) {
            return true;
        }
        String semesterSeason = (semester % 2 == 1) ? "FALL" : "SPRING";
        return semesterSeason.equalsIgnoreCase(course.getSeason());
    }

    private int chooseFastTrackSemester(
            Course course,
            int minSem,
            int maxSem,
            int credits,
            Map<Integer, Integer> semCredits) {
        String code = course.getCode() != null ? course.getCode().toUpperCase(Locale.ROOT) : "";
        boolean isFinalCapstone = "CSS479".equals(code) || "INF420".equals(code);
        if (isFinalCapstone && maxSem == TOTAL_SEMESTERS) {
            return TOTAL_SEMESTERS;
        }
        int fastTrackCap = 35;
        int preInternshipMaxSem = Math.min(maxSem, Math.max(minSem, 6));
        for (int s = minSem; s <= preInternshipMaxSem; s++) {
            int load = semCredits.getOrDefault(s, 0);
            if (load + credits <= fastTrackCap) {
                return s;
            }
        }
        for (int s = minSem; s <= maxSem; s++) {
            int load = semCredits.getOrDefault(s, 0);
            if (load + credits <= MAX_RECOMMENDED_CREDITS) {
                return s;
            }
        }
        int bestSem = minSem;
        int bestLoad = Integer.MAX_VALUE;
        for (int s = minSem; s <= maxSem; s++) {
            int load = semCredits.getOrDefault(s, 0);
            if (load < bestLoad) {
                bestLoad = load;
                bestSem = s;
            }
        }
        return bestSem;
    }

    protected List<RoadmapItem> initializeDefaultRoadmap(Student student) {
        String studentId = student.getStudentId();
        int currentSem = clampSemester(student.getCurrentSemester() != null ? student.getCurrentSemester() : 1);

        Map<Long, CurriculumCourse> curriculumByCourseId = loadCurriculumByCourseId(student);
        List<CurriculumCourse> curriculumList = loadCurriculumList(student);
        Map<Long, List<Course>> prereqMap = loadPrerequisiteMap();

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

        // Fill remaining capacity in each semester (up to 30 ECTS) with prerequisite-valid career-aligned electives
        String careerGoal = student.getCareerGoal();
        List<CurriculumCourse> remainingElectives = curriculumList.stream()
                .filter(cc -> cc.getCourse() != null && cc.getCourse().getId() != null)
                .filter(cc -> !placedByCourseId.containsKey(cc.getCourse().getId()))
                .sorted(Comparator
                        .comparingInt((CurriculumCourse cc) -> cc.getSemester() != null ? cc.getSemester() : 6)
                        .thenComparingInt(cc -> matchesCareerGoal(cc.getCourse(), careerGoal) ? 0 : 1)
                        .thenComparing(cc -> cc.getCourse().getCode()))
                .toList();

        for (CurriculumCourse cc : remainingElectives) {
            Course c = cc.getCourse();
            int credits = c.getCredits() != null ? c.getCredits() : 5;
            int targetSem = clampSemester(cc.getSemester());
            if (!arePrereqsPlacedBefore(c.getId(), targetSem, prereqMap, placedByCourseId)) {
                continue;
            }
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
                    if (!placedByCourseId.containsKey(c.getId())
                            && arePrereqsPlacedBefore(c.getId(), s, prereqMap, placedByCourseId)) {
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

    private boolean arePrereqsPlacedBefore(
            Long courseId,
            int targetSemester,
            Map<Long, List<Course>> prereqMap,
            Map<Long, RoadmapItem> placedByCourseId) {
        List<Course> reqs = prereqMap.getOrDefault(courseId, List.of());
        for (Course req : reqs) {
            if (req == null || req.getId() == null) continue;
            RoadmapItem reqItem = placedByCourseId.get(req.getId());
            if (reqItem == null || clampSemester(reqItem.getSemester()) >= targetSemester) {
                return false;
            }
        }
        return true;
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
                if (!"COMPLETED".equals(status)
                        && !"BOTH".equalsIgnoreCase(courseSeason)
                        && !courseSeason.equalsIgnoreCase(semesterSeason)) {
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
