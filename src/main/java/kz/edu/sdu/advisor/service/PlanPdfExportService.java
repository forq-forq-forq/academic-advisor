package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.model.dto.RoadmapBoardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapCourseCardDto;
import kz.edu.sdu.advisor.model.dto.RoadmapSemesterDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates downloadable PDF 1.4 documents summarizing the student's Semester Cart
 * and/or 4-Year Degree Roadmap (US-23) with zero external PDF dependencies.
 */
@Service
public class PlanPdfExportService {

    private static final int LINES_PER_PAGE = 48;
    private static final int MAX_LINE_CHARS = 88;

    public byte[] generatePlanPdf(Student student, CartDto cart, RoadmapBoardDto roadmap, String scope) {
        String normalizedScope = scope != null ? scope.trim().toLowerCase(Locale.ROOT) : "all";
        List<PdfLine> lines = buildReportLines(student, cart, roadmap, normalizedScope);
        List<List<PdfLine>> pages = paginate(lines);
        return buildPdfDocument(pages);
    }

    private List<PdfLine> buildReportLines(
            Student student,
            CartDto cart,
            RoadmapBoardDto roadmap,
            String scope) {

        List<PdfLine> lines = new ArrayList<>();
        String name = student != null && student.getName() != null ? student.getName() : "Student";
        String studentId = student != null && student.getStudentId() != null ? student.getStudentId() : "-";
        String majorName = (student != null && student.getMajor() != null)
                ? student.getMajor().getName()
                : "Information Systems";
        String catalogYear = (student != null && student.getCatalogYear() != null)
                ? String.valueOf(student.getCatalogYear())
                : "2024";

        lines.add(new PdfLine("SDU Academic Advisor - Study Plan Export", true));
        lines.add(new PdfLine("Student: " + name + " (" + studentId + ")", false));
        lines.add(new PdfLine("Major: " + majorName + " | Catalog Year: " + catalogYear, false));
        lines.add(new PdfLine("--------------------------------------------------------------------------", false));

        if (!"roadmap".equals(scope) && cart != null) {
            lines.add(new PdfLine("SEMESTER CART PLAN", true));
            lines.add(new PdfLine(String.format(
                    "Workload Summary: %d / %d ECTS (%s)",
                    cart.totalCredits(), cart.maxCredits(), cart.workloadState()), false));
            if (cart.exceedsLimit()) {
                lines.add(new PdfLine("WARNING: Credit limit exceeded by " + cart.excessCredits() + " ECTS", false));
            }
            if (cart.items() == null || cart.items().isEmpty()) {
                lines.add(new PdfLine("  (No courses in semester cart)", false));
            } else {
                for (CartItemDto item : cart.items()) {
                    addWrapped(lines, String.format("  * %s - %s (%d ECTS)",
                            item.code(), item.name(), item.credits()), false);
                }
            }
            lines.add(new PdfLine("", false));
        }

        if (!"cart".equals(scope) && roadmap != null) {
            lines.add(new PdfLine("4-YEAR DEGREE ROADMAP (8 SEMESTERS)", true));
            lines.add(new PdfLine(String.format(
                    "Total Roadmap: %d ECTS across %d courses | Pacing: %s",
                    roadmap.totalCredits(), roadmap.totalCourses(), roadmap.activePacingStrategy()), false));
            lines.add(new PdfLine("", false));

            if (roadmap.semesters() != null) {
                for (RoadmapSemesterDto sem : roadmap.semesters()) {
                    lines.add(new PdfLine(String.format("%s - %d ECTS [%s]",
                            sem.title(), sem.totalCredits(), sem.workloadState()), true));
                    if (sem.courses() == null || sem.courses().isEmpty()) {
                        lines.add(new PdfLine("  (No courses scheduled)", false));
                    } else {
                        for (RoadmapCourseCardDto card : sem.courses()) {
                            addWrapped(lines, String.format("  - %s: %s (%d ECTS, %s, %s)",
                                    card.code(), card.name(), card.credits(), card.category(), card.status()), false);
                        }
                    }
                    lines.add(new PdfLine("", false));
                }
            }
        }
        return lines;
    }

    private void addWrapped(List<PdfLine> lines, String text, boolean bold) {
        String clean = sanitizeAscii(text);
        while (clean.length() > MAX_LINE_CHARS) {
            int split = clean.lastIndexOf(' ', MAX_LINE_CHARS);
            if (split <= 10) {
                split = MAX_LINE_CHARS;
            }
            lines.add(new PdfLine(clean.substring(0, split), bold));
            clean = "    " + clean.substring(split).trim();
        }
        lines.add(new PdfLine(clean, bold));
    }

    private List<List<PdfLine>> paginate(List<PdfLine> lines) {
        List<List<PdfLine>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + LINES_PER_PAGE)));
        }
        if (pages.isEmpty()) {
            pages.add(List.of(new PdfLine("SDU Academic Advisor - Empty Plan", true)));
        }
        return pages;
    }

    private byte[] buildPdfDocument(List<List<PdfLine>> pages) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "%PDF-1.4\n");

        List<Long> offsets = new ArrayList<>();
        offsets.add(0L); // object 0 free placeholder

        int pageCount = pages.size();
        // Object 1: Catalog, Object 2: Pages, Object 3: Helvetica, Object 4: Helvetica-Bold
        // Then for each page i: Content object (5 + 2*i), Page object (6 + 2*i)
        List<Integer> pageObjectIds = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            pageObjectIds.add(6 + 2 * i);
        }

        // Obj 1: Catalog
        offsets.add((long) out.size());
        writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // Obj 2: Pages
        offsets.add((long) out.size());
        StringBuilder kids = new StringBuilder();
        for (Integer pid : pageObjectIds) {
            kids.append(pid).append(" 0 R ");
        }
        writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pageCount + " >>\nendobj\n");

        // Obj 3: Font F1 (Helvetica)
        offsets.add((long) out.size());
        writeAscii(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        // Obj 4: Font F2 (Helvetica-Bold)
        offsets.add((long) out.size());
        writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

        for (int i = 0; i < pageCount; i++) {
            int contentObjId = 5 + 2 * i;
            int pageObjId = 6 + 2 * i;
            String streamContent = buildPageContentStream(pages.get(i), i + 1, pageCount);
            byte[] streamBytes = streamContent.getBytes(StandardCharsets.ISO_8859_1);

            offsets.add((long) out.size());
            writeAscii(out, contentObjId + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            out.writeBytes(streamBytes);
            writeAscii(out, "\nendstream\nendobj\n");

            offsets.add((long) out.size());
            writeAscii(out, pageObjId + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObjId + " 0 R >>\nendobj\n");
        }

        long xrefOffset = out.size();
        int totalObjects = offsets.size();
        writeAscii(out, "xref\n0 " + totalObjects + "\n");
        writeAscii(out, "0000000000 65535 f \n");
        for (int i = 1; i < totalObjects; i++) {
            writeAscii(out, String.format(Locale.US, "%010d 00000 n \n", offsets.get(i)));
        }
        writeAscii(out, "trailer\n<< /Size " + totalObjects + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
        return out.toByteArray();
    }

    private String buildPageContentStream(List<PdfLine> pageLines, int pageNumber, int totalPages) {
        StringBuilder sb = new StringBuilder();
        int y = 752;
        for (PdfLine line : pageLines) {
            String font = line.bold() ? "/F2" : "/F1";
            int size = line.bold() ? 11 : 10;
            sb.append("BT ").append(font).append(" ").append(size).append(" Tf 46 ").append(y)
              .append(" Td (").append(escapePdfString(sanitizeAscii(line.text()))).append(") Tj ET\n");
            y -= 14;
        }
        sb.append("BT /F1 9 Tf 46 28 Td (Page ").append(pageNumber).append(" of ").append(totalPages)
          .append(") Tj ET\n");
        return sb.toString();
    }

    private String sanitizeAscii(String text) {
        if (text == null) return "";
        String replaced = text
                .replace('\u2014', '-')
                .replace('\u2013', '-')
                .replace('\u2264', '<')
                .replace('\u2265', '>')
                .replace('\u2022', '*');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < replaced.length(); i++) {
            char ch = replaced.charAt(i);
            if (ch >= 32 && ch <= 126) {
                sb.append(ch);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private String escapePdfString(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private void writeAscii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private record PdfLine(String text, boolean bold) {}
}
