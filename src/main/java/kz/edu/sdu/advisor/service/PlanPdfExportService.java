package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapCourseCardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapSemesterDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Generates templated, styled PDF 1.4 academic study plan documents summarizing
 * the student's Semester Cart and/or 4-Year Degree Roadmap (US-23) via {@link PdfTemplateEngine}.
 */
@Service
public class PlanPdfExportService {

    private final PdfTemplateEngine pdfTemplateEngine;

    @Autowired
    public PlanPdfExportService(PdfTemplateEngine pdfTemplateEngine) {
        this.pdfTemplateEngine = pdfTemplateEngine;
    }

    public PlanPdfExportService() {
        this(new PdfTemplateEngine());
    }

    public byte[] generatePlanPdf(Student student, CartDto cart, RoadmapBoardDto roadmap, String scope) {
        String normalizedScope = scope != null ? scope.trim().toLowerCase(Locale.ROOT) : "all";
        PdfTemplateEngine.PdfTemplateConfig template = pdfTemplateEngine.getConfig();

        String name = student != null && student.getName() != null ? student.getName() : "Student";
        String studentId = student != null && student.getStudentId() != null ? student.getStudentId() : "-";
        String majorName = (student != null && student.getMajor() != null && student.getMajor().getName() != null)
                ? student.getMajor().getName()
                : "Information Systems";
        String catalogYear = (student != null && student.getCatalogYear() != null)
                ? String.valueOf(student.getCatalogYear())
                : "2024";
        String currentSemester = (student != null && student.getCurrentSemester() != null)
                ? "Semester " + student.getCurrentSemester()
                : "Semester 1";
        String gpaText = (student != null && student.getGpa() != null)
                ? String.format(Locale.US, "%.2f", student.getGpa())
                : "0.00";
        String careerGoal = (student != null && student.getCareerGoal() != null && !student.getCareerGoal().isBlank())
                ? student.getCareerGoal()
                : "B.Sc. Degree Completion";

        PdfTemplateEngine.PdfRenderSession session = pdfTemplateEngine.newSession(
                template.branding.headerTitle,
                name + " (" + studentId + ")");

        session.renderHeaderBanner(
                template.branding.headerTitle,
                template.branding.headerSubtitle,
                "Generated: " + LocalDate.now(),
                "SCOPE: " + normalizedScope.toUpperCase(Locale.ROOT));

        session.renderProfileCard(
                "Student: " + name + " (" + studentId + ")",
                "Major: " + majorName + " | Catalog Year: " + catalogYear,
                currentSemester + " | Cumulative GPA: " + gpaText,
                careerGoal);

        if (!"roadmap".equals(normalizedScope) && cart != null) {
            renderCartSection(session, template, cart);
        }

        if (!"cart".equals(normalizedScope) && roadmap != null) {
            renderRoadmapSection(session, template, roadmap);
        }

        return session.toPdfBytes();
    }

    private void renderCartSection(
            PdfTemplateEngine.PdfRenderSession session,
            PdfTemplateEngine.PdfTemplateConfig template,
            CartDto cart) {

        List<CartItemDto> items = cart.items() != null ? cart.items() : List.of();
        session.renderSectionHeader(template.branding.cartSectionTitle, items.size() + " course(s)");

        String summaryLine = String.format(Locale.US,
                "Workload Summary: %d / %d ECTS (%s)",
                cart.totalCredits(), cart.maxCredits(), cart.workloadState());
        session.renderCartSummaryCard(
                summaryLine,
                cart.workloadState(),
                cart.totalCredits(),
                cart.maxCredits(),
                items.size());

        if (cart.exceedsLimit()) {
            session.renderAlertBanner(
                    "WARNING: Credit limit exceeded by " + cart.excessCredits() + " ECTS",
                    true);
        }

        if (items.isEmpty()) {
            session.renderEmptyBox("No courses in semester cart.");
        } else {
            session.renderTableHeader(template.cartColumns);
            for (int i = 0; i < items.size(); i++) {
                CartItemDto item = items.get(i);
                session.renderCartRow(
                        i + 1,
                        item.code(),
                        item.name(),
                        item.credits(),
                        i % 2 == 1,
                        template.cartColumns);
            }
            session.renderCartTotalRow(cart.totalCredits(), items.size());
        }
    }

    private void renderRoadmapSection(
            PdfTemplateEngine.PdfRenderSession session,
            PdfTemplateEngine.PdfTemplateConfig template,
            RoadmapBoardDto roadmap) {

        session.renderSectionHeader(template.branding.roadmapSectionTitle, roadmap.totalCredits() + " ECTS Total");

        String summaryLine = String.format(Locale.US,
                "Total Roadmap: %d ECTS across %d courses | Pacing: %s",
                roadmap.totalCredits(), roadmap.totalCourses(), roadmap.activePacingStrategy());
        session.renderRoadmapOverviewStrip(
                summaryLine,
                roadmap.activePacingStrategy(),
                roadmap.prerequisiteConflictCount(),
                roadmap.overloadedSemesterCount(),
                roadmap.seasonConflictCount());

        if (roadmap.semesters() == null || roadmap.semesters().isEmpty()) {
            session.renderEmptyBox("No roadmap semesters configured.");
            return;
        }

        for (RoadmapSemesterDto sem : roadmap.semesters()) {
            List<RoadmapCourseCardDto> courses = sem.courses() != null ? sem.courses() : List.of();
            int minRequiredHeight = courses.isEmpty() ? 58 : 72;
            String semHeaderTitle = String.format(Locale.US,
                    "%s - %d ECTS [%s]",
                    sem.title(), sem.totalCredits(), sem.workloadState());

            session.renderSemesterHeader(semHeaderTitle, sem.workloadState(), sem.overloaded(), minRequiredHeight);

            if (courses.isEmpty()) {
                session.renderEmptyBox("No courses scheduled in " + sem.title() + ".");
            } else {
                session.renderTableHeader(template.roadmapColumns);
                for (int i = 0; i < courses.size(); i++) {
                    RoadmapCourseCardDto card = courses.get(i);
                    String warningNote = buildCardWarningNote(card);
                    session.renderRoadmapCourseRow(
                            card.code(),
                            card.name(),
                            card.category(),
                            card.season(),
                            card.credits(),
                            card.status(),
                            warningNote,
                            i % 2 == 1,
                            template.roadmapColumns);
                }
                session.moveDown(10);
            }
        }
    }

    private String buildCardWarningNote(RoadmapCourseCardDto card) {
        List<String> parts = new java.util.ArrayList<>();
        if (card.prerequisiteViolation() && card.violatedPrerequisites() != null && !card.violatedPrerequisites().isEmpty()) {
            parts.add("Prereq conflict: " + String.join(", ", card.violatedPrerequisites()));
        }
        if (card.seasonMismatch() && card.seasonWarning() != null && !card.seasonWarning().isBlank()) {
            parts.add(card.seasonWarning());
        }
        return String.join(" | ", parts);
    }
}
