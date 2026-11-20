package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlanPdfExportServiceTest {

    @Autowired
    private PlanPdfExportService planPdfExportService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RoadmapService roadmapService;

    @Test
    @DisplayName("US-23: generatePlanPdf builds a valid PDF 1.4 document with course codes, credits, and workload summary")
    void generatePlanPdf_shouldContainValidPdfStructureAndCourseSummary() {
        cartItemRepository.deleteAll();
        Course inf202 = courseRepository.findByCode("INF202").orElseThrow();
        cartService.addCourse("240103000", inf202.getId());

        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        CartDto cart = cartService.getCart("240103000");
        RoadmapBoardDto roadmap = roadmapService.getRoadmap("240103000");

        byte[] pdfBytes = planPdfExportService.generatePlanPdf(student, cart, roadmap, "all");
        String pdfContent = new String(pdfBytes, StandardCharsets.ISO_8859_1);

        assertThat(pdfContent).startsWith("%PDF-1.4");
        assertThat(pdfContent).contains("%%EOF");
        assertThat(pdfContent).contains("xref");
        assertThat(pdfContent).contains("John Doe");
        assertThat(pdfContent).contains("240103000");
        assertThat(pdfContent).contains("STUDENT PROFILE");
        assertThat(pdfContent).contains("SEMESTER CART PLAN");
        assertThat(pdfContent).contains("INF202");
        assertThat(pdfContent).contains("5 ECTS");
        assertThat(pdfContent).contains("Workload Summary: 5 / 40 ECTS");
        assertThat(pdfContent).contains("4-YEAR DEGREE ROADMAP");
        assertThat(pdfContent).contains("Page 1 of");
        // Verify vector template graphics operators are emitted (color fills & rectangles)
        assertThat(pdfContent).contains(" rg");
        assertThat(pdfContent).contains(" re ");
    }

    @Test
    @DisplayName("PDF Template Engine: supports scope filtering (cart vs roadmap), alert banner on overload, and loads template config")
    void generatePlanPdf_shouldSupportScopesOverloadWarningAndTemplateConfig() {
        PdfTemplateEngine engine = new PdfTemplateEngine();
        assertThat(engine.getConfig().branding.headerTitle).contains("SDU Academic Advisor");
        assertThat(engine.getConfig().cartColumns).hasSize(5);
        assertThat(engine.getConfig().roadmapColumns).hasSize(6);

        Student student = studentRepository.findByStudentId("240103000").orElseThrow();
        CartDto overloadedCart = new CartDto(
                java.util.List.of(new kz.edu.sdu.advisor.model.dto.CartItemDto(1L, 1L, "INF202", "Database Management Systems", 5)),
                45,
                40,
                "Overload",
                "red",
                false,
                "INF202"
        );
        RoadmapBoardDto roadmap = roadmapService.getRoadmap("240103000");

        String cartOnlyPdf = new String(
                planPdfExportService.generatePlanPdf(student, overloadedCart, roadmap, "cart"),
                StandardCharsets.ISO_8859_1);
        assertThat(cartOnlyPdf)
                .contains("SEMESTER CART PLAN")
                .contains("WARNING: Credit limit exceeded by 5 ECTS")
                .doesNotContain("4-YEAR DEGREE ROADMAP");

        String roadmapOnlyPdf = new String(
                planPdfExportService.generatePlanPdf(student, overloadedCart, roadmap, "roadmap"),
                StandardCharsets.ISO_8859_1);
        assertThat(roadmapOnlyPdf)
                .contains("4-YEAR DEGREE ROADMAP")
                .doesNotContain("SEMESTER CART PLAN");
    }
}
