package kz.edu.sdu.advisor.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Reusable zero-dependency PDF 1.4 template & layout engine.
 * Reads structural theme geometry, palette, branding labels, and column definitions
 * from {@code classpath:pdf/academic-plan-template.json} and renders structured visual
 * components (banners, cards, progress bars, zebra-striped tables, badges, and footers)
 * with automatic height-aware multi-page pagination.
 */
@Component
public class PdfTemplateEngine {

    private static final String DEFAULT_TEMPLATE_PATH = "pdf/academic-plan-template.json";
    private static final double BEZIER_KAPPA = 0.5523;

    private final PdfTemplateConfig config;

    public PdfTemplateEngine() {
        this.config = loadTemplateConfig(DEFAULT_TEMPLATE_PATH);
    }

    public PdfTemplateEngine(String templateResourcePath) {
        this.config = loadTemplateConfig(templateResourcePath);
    }

    public PdfTemplateConfig getConfig() {
        return config;
    }

    public PdfRenderSession newSession(String runningHeaderLeft, String runningHeaderRight) {
        return new PdfRenderSession(config, runningHeaderLeft, runningHeaderRight);
    }

    private PdfTemplateConfig loadTemplateConfig(String resourcePath) {
        if (resourcePath != null && !resourcePath.isBlank()) {
            try {
                ClassPathResource resource = new ClassPathResource(resourcePath);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        PdfTemplateConfig loaded = new ObjectMapper().readValue(is, PdfTemplateConfig.class);
                        if (loaded != null) {
                            return PdfTemplateConfig.withDefaults(loaded);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fall back safely to default theme configuration
            }
        }
        return PdfTemplateConfig.defaultConfig();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Template Render Session
    // ─────────────────────────────────────────────────────────────────────────────

    public static final class PdfRenderSession {

        private final PdfTemplateConfig config;
        private final String runningHeaderLeft;
        private final String runningHeaderRight;
        private final List<StringBuilder> pages = new ArrayList<>();
        private double cursorY;

        private PdfRenderSession(PdfTemplateConfig config, String runningHeaderLeft, String runningHeaderRight) {
            this.config = config;
            this.runningHeaderLeft = sanitizeAscii(runningHeaderLeft);
            this.runningHeaderRight = sanitizeAscii(runningHeaderRight);
            startNewPage(false);
        }

        public PdfTemplateConfig config() {
            return config;
        }

        public double contentLeft() {
            return config.page.marginLeft;
        }

        public double contentWidth() {
            return config.page.width - config.page.marginLeft - config.page.marginRight;
        }

        public double contentRight() {
            return config.page.width - config.page.marginRight;
        }

        public void moveDown(double dy) {
            cursorY -= dy;
        }

        public boolean ensureSpace(double requiredHeight) {
            if (cursorY - requiredHeight < config.page.marginBottom) {
                startNewPage(true);
                return true;
            }
            return false;
        }

        public void startNewPage(boolean drawRunningHeader) {
            pages.add(new StringBuilder());
            cursorY = config.page.height - config.page.marginTop;
            if (drawRunningHeader) {
                double left = contentLeft();
                double width = contentWidth();
                drawText(runningHeaderLeft, left, cursorY - 9, 8.5, true, config.palette.brandDark);
                drawRightAlignedText(runningHeaderRight, left + width, cursorY - 9, 8.5, false, config.palette.textMuted);
                drawLine(left, cursorY - 14, left + width, cursorY - 14, config.palette.borderLight, 0.75);
                cursorY -= 24;
            }
        }

        public void renderHeaderBanner(String title, String subtitle, String datePill, String scopePill) {
            ensureSpace(58);
            double x = contentLeft();
            double w = contentWidth();
            double h = 52;
            double bottomY = cursorY - h;

            drawRoundedBox(x, bottomY, w, h, 6, config.palette.brandPrimary, config.palette.brandDark, 0.9);
            drawRoundedBox(x + 12, bottomY + 8, 3.5, h - 16, 1.5, config.palette.surfaceWhite, null, 0);

            drawText(title, x + 22, bottomY + h - 22, 14.0, true, config.palette.surfaceWhite);
            drawText(subtitle, x + 22, bottomY + 12, 9.0, false, config.palette.brandLight);

            double pillRight = x + w - 12;
            if (scopePill != null && !scopePill.isBlank()) {
                double pillW = Math.max(76, estimateTextWidth(scopePill, 7.5, true) + 14);
                drawPillAtRight(scopePill, pillRight, bottomY + 28, 15, pillW, 7.5,
                        config.palette.surfaceWhite, config.palette.brandDark, null);
            }
            if (datePill != null && !datePill.isBlank()) {
                double dateW = Math.max(76, estimateTextWidth(datePill, 7.5, false) + 14);
                drawPillAtRight(datePill, pillRight, bottomY + 9, 14, dateW, 7.5,
                        config.palette.brandDark, config.palette.surfaceWhite, null);
            }

            cursorY = bottomY - 12;
        }

        public void renderProfileCard(
                String studentLine,
                String majorLine,
                String standingLine,
                String goalLine) {
            ensureSpace(64);
            double x = contentLeft();
            double w = contentWidth();
            double h = 58;
            double bottomY = cursorY - h;

            drawRoundedBox(x, bottomY, w, h, 5, config.palette.surfaceMuted, config.palette.borderSlate, 0.75);
            drawRoundedBox(x + 6, bottomY + 7, 3.5, h - 14, 1.5, config.palette.brandPrimary, null, 0);

            double col1X = x + 16;
            double col2X = x + w * 0.52;
            double col2MaxW = w * 0.45;

            drawText("STUDENT PROFILE", col1X, bottomY + h - 13, 7.0, true, config.palette.textMuted);
            drawText(fitSingleLine(studentLine, w * 0.47, 9.5, true), col1X, bottomY + h - 25, 9.5, true, config.palette.textPrimary);

            drawText("PROGRAM & CATALOG", col2X, bottomY + h - 13, 7.0, true, config.palette.textMuted);
            drawText(fitSingleLine(majorLine, col2MaxW, 9.0, false), col2X, bottomY + h - 25, 9.0, false, config.palette.textPrimary);

            drawLine(x + 12, bottomY + h / 2.0 - 2, x + w - 12, bottomY + h / 2.0 - 2, config.palette.borderLight, 0.6);

            drawText("ACADEMIC STANDING", col1X, bottomY + 18, 7.0, true, config.palette.textMuted);
            drawText(fitSingleLine(standingLine, w * 0.47, 8.5, false), col1X, bottomY + 7, 8.5, false, config.palette.textSecondary);

            drawText("CAREER FOCUS", col2X, bottomY + 18, 7.0, true, config.palette.textMuted);
            drawText(fitSingleLine(goalLine, col2MaxW, 8.5, false), col2X, bottomY + 7, 8.5, false, config.palette.textSecondary);

            cursorY = bottomY - 14;
        }

        public void renderSectionHeader(String title, String rightBadgeText) {
            ensureSpace(32);
            double x = contentLeft();
            double w = contentWidth();
            double topY = cursorY;

            fillRect(x, topY - 16, 4, 16, config.palette.brandPrimary);
            drawText(title, x + 10, topY - 12, 12.0, true, config.palette.brandDark);
            if (rightBadgeText != null && !rightBadgeText.isBlank()) {
                drawRightAlignedText(rightBadgeText, x + w, topY - 12, 8.5, true, config.palette.textMuted);
            }
            drawLine(x, topY - 21, x + w, topY - 21, config.palette.brandBorder, 1.0);
            cursorY = topY - 28;
        }

        public void renderCartSummaryCard(
                String workloadSummaryText,
                String workloadState,
                int totalCredits,
                int maxCredits,
                int courseCount) {
            ensureSpace(46);
            double x = contentLeft();
            double w = contentWidth();
            double h = 40;
            double bottomY = cursorY - h;

            String badgeBg = config.palette.infoBg;
            String badgeFg = config.palette.infoText;
            String barColor = config.palette.brandPrimary;
            String stateUpper = workloadState != null ? workloadState.toUpperCase(Locale.ROOT) : "LIGHT";
            if (stateUpper.contains("OVERLOAD") || totalCredits > maxCredits) {
                badgeBg = config.palette.dangerBg;
                badgeFg = config.palette.dangerText;
                barColor = "#DC2626";
            } else if (stateUpper.contains("BALANCED")) {
                badgeBg = config.palette.successBg;
                badgeFg = config.palette.successText;
                barColor = "#16A34A";
            }

            drawRoundedBox(x, bottomY, w, h, 4, config.palette.surfaceMuted, config.palette.borderSlate, 0.75);
            drawText(workloadSummaryText, x + 12, bottomY + h - 16, 10.0, true, config.palette.textPrimary);

            double ratio = maxCredits > 0 ? Math.min(1.0, Math.max(0.0, (double) totalCredits / maxCredits)) : 0.0;
            drawProgressBar(x + 12, bottomY + 8, 250, 6, ratio, config.palette.borderLight, barColor);

            String countLabel = courseCount + (courseCount == 1 ? " course selected" : " courses selected");
            drawRightAlignedText(countLabel, x + w - 98, bottomY + 14, 8.5, false, config.palette.textSecondary);
            drawPillAtRight(stateUpper, x + w - 12, bottomY + 11, 18, 76, 8.0, badgeBg, badgeFg, null);

            cursorY = bottomY - 10;
        }

        public void renderAlertBanner(String message, boolean danger) {
            List<String> lines = wrapText(message, contentWidth() - 28, 9.0, true);
            double h = Math.max(24, lines.size() * 12 + 12);
            ensureSpace(h + 8);

            double x = contentLeft();
            double w = contentWidth();
            double bottomY = cursorY - h;
            String bg = danger ? config.palette.dangerBg : config.palette.warnBg;
            String fg = danger ? config.palette.dangerText : config.palette.warnText;
            String border = danger ? "#FCA5A5" : "#FCD34D";

            drawRoundedBox(x, bottomY, w, h, 4, bg, border, 0.85);
            double textY = bottomY + h - 15;
            for (String line : lines) {
                drawText(line, x + 12, textY, 9.0, true, fg);
                textY -= 12;
            }
            cursorY = bottomY - 8;
        }

        public void renderEmptyBox(String message) {
            ensureSpace(32);
            double x = contentLeft();
            double w = contentWidth();
            double h = 26;
            double bottomY = cursorY - h;
            drawRoundedBox(x, bottomY, w, h, 4, config.palette.surfaceMuted, config.palette.borderLight, 0.75);
            drawCenteredText(message, x + w / 2.0, bottomY + 9, 9.0, false, config.palette.textMuted);
            cursorY = bottomY - 10;
        }

        public void renderTableHeader(List<ColumnSpec> columns) {
            ensureSpace(24);
            drawTableHeaderImmediate(columns);
        }

        private void drawTableHeaderImmediate(List<ColumnSpec> columns) {
            double x = contentLeft();
            double w = contentWidth();
            double h = 20;
            double bottomY = cursorY - h;

            drawBox(x, bottomY, w, h, config.palette.brandLight, config.palette.brandBorder, 0.75);
            double colX = x;
            for (ColumnSpec col : columns) {
                double textY = bottomY + 6.5;
                if ("right".equalsIgnoreCase(col.align)) {
                    drawRightAlignedText(col.label, colX + col.width - 8, textY, 8.0, true, config.palette.brandDark);
                } else if ("center".equalsIgnoreCase(col.align)) {
                    drawCenteredText(col.label, colX + col.width / 2.0, textY, 8.0, true, config.palette.brandDark);
                } else {
                    drawText(col.label, colX + 8, textY, 8.0, true, config.palette.brandDark);
                }
                colX += col.width;
            }
            cursorY = bottomY;
        }

        public void renderCartRow(
                int index,
                String code,
                String name,
                int credits,
                boolean zebra,
                List<ColumnSpec> columns) {
            ColumnSpec titleCol = columns.size() >= 3 ? columns.get(2) : new ColumnSpec("name", "Course Title", 274, "left");
            List<String> titleLines = wrapText(name, titleCol.width - 14, 9.0, false);
            if (titleLines.isEmpty()) {
                titleLines = List.of("-");
            }
            double rowH = Math.max(22, titleLines.size() * 12 + 10);

            if (ensureSpace(rowH + 4)) {
                drawTableHeaderImmediate(columns);
            }

            double x = contentLeft();
            double w = contentWidth();
            double bottomY = cursorY - rowH;
            String fillHex = zebra ? config.palette.surfaceZebra : config.palette.surfaceWhite;
            drawBox(x, bottomY, w, rowH, fillHex, config.palette.borderLight, 0.6);

            double colX = x;
            for (ColumnSpec col : columns) {
                drawLine(colX, bottomY, colX, bottomY + rowH, config.palette.borderLight, 0.4);
                double midY = bottomY + rowH / 2.0 - 3.0;
                switch (col.key) {
                    case "index" -> drawCenteredText(String.valueOf(index), colX + col.width / 2.0, midY, 8.5, false, config.palette.textMuted);
                    case "code" -> drawText(code, colX + 8, midY, 9.0, true, config.palette.brandDark);
                    case "name" -> {
                        double textY = bottomY + rowH - 14;
                        for (String line : titleLines) {
                            drawText(line, colX + 8, textY, 9.0, false, config.palette.textPrimary);
                            textY -= 12;
                        }
                    }
                    case "credits" -> drawRightAlignedText(credits + " ECTS", colX + col.width - 8, midY, 8.5, true, config.palette.textPrimary);
                    case "status" -> drawPillCentered("IN CART", colX + col.width / 2.0, bottomY + (rowH - 14) / 2.0, 14, 56, 7.0,
                            config.palette.infoBg, config.palette.infoText, null);
                    default -> {
                    }
                }
                colX += col.width;
            }
            cursorY = bottomY;
        }

        public void renderCartTotalRow(int totalCredits, int courseCount) {
            ensureSpace(24);
            double x = contentLeft();
            double w = contentWidth();
            double h = 21;
            double bottomY = cursorY - h;

            drawBox(x, bottomY, w, h, config.palette.brandLight, config.palette.brandBorder, 0.75);
            drawText("Total Planned Courses: " + courseCount, x + 10, bottomY + 7, 8.5, true, config.palette.brandDark);
            drawRightAlignedText("Total: " + totalCredits + " ECTS", x + w - 10, bottomY + 7, 9.0, true, config.palette.brandDark);
            cursorY = bottomY - 12;
        }

        public void renderRoadmapOverviewStrip(
                String summaryText,
                String pacingStrategy,
                int prereqConflicts,
                int overloadedSemesters,
                int seasonConflicts) {
            ensureSpace(44);
            double x = contentLeft();
            double w = contentWidth();
            double h = 36;
            double bottomY = cursorY - h;

            drawRoundedBox(x, bottomY, w, h, 4, config.palette.surfaceMuted, config.palette.borderSlate, 0.75);
            drawText(fitSingleLine(summaryText, w - 130, 9.5, true), x + 12, bottomY + 21, 9.5, true, config.palette.textPrimary);

            String healthText = (prereqConflicts == 0 && overloadedSemesters == 0 && seasonConflicts == 0)
                    ? "All prerequisites, workload limits, and course seasons are valid."
                    : String.format(Locale.US, "Alerts: %d prereq conflict(s) | %d overloaded semester(s) | %d season mismatch(es)",
                            prereqConflicts, overloadedSemesters, seasonConflicts);
            String healthColor = (prereqConflicts == 0 && overloadedSemesters == 0 && seasonConflicts == 0)
                    ? config.palette.successText
                    : config.palette.warnText;
            drawText(healthText, x + 12, bottomY + 8, 8.0, false, healthColor);

            String pacingLabel = pacingStrategy != null && !pacingStrategy.isBlank() ? pacingStrategy : "BALANCED";
            drawPillAtRight(pacingLabel, x + w - 12, bottomY + 11, 15, 76, 7.5,
                    config.palette.infoBg, config.palette.infoText, config.palette.brandBorder);

            cursorY = bottomY - 12;
        }

        public void renderSemesterHeader(
                String headerTitle,
                String workloadState,
                boolean overloaded,
                int minSpaceNeeded) {
            ensureSpace(minSpaceNeeded);
            double x = contentLeft();
            double w = contentWidth();
            double h = 23;
            double bottomY = cursorY - h;

            String bg = overloaded ? config.palette.warnBg : config.palette.brandLight;
            String border = overloaded ? "#F59E0B" : config.palette.brandBorder;
            String titleColor = overloaded ? config.palette.warnText : config.palette.brandDark;

            drawBox(x, bottomY, w, h, bg, border, 0.8);
            fillRect(x, bottomY, 4, h, overloaded ? "#D97706" : config.palette.brandPrimary);
            drawText(headerTitle, x + 10, bottomY + 7.5, 9.5, true, titleColor);

            String badgeLabel = overloaded ? "OVERLOAD" : (workloadState != null ? workloadState.toUpperCase(Locale.ROOT) : "LIGHT");
            String badgeBg = overloaded ? config.palette.dangerBg
                    : ("BALANCED".equalsIgnoreCase(workloadState) ? config.palette.successBg : config.palette.surfaceWhite);
            String badgeFg = overloaded ? config.palette.dangerText
                    : ("BALANCED".equalsIgnoreCase(workloadState) ? config.palette.successText : config.palette.brandDark);
            drawPillAtRight(badgeLabel, x + w - 8, bottomY + 4.5, 14, 68, 7.0, badgeBg, badgeFg, border);

            cursorY = bottomY;
        }

        public void renderRoadmapCourseRow(
                String code,
                String name,
                String category,
                String season,
                int credits,
                String status,
                String warningNote,
                boolean zebra,
                List<ColumnSpec> columns) {
            ColumnSpec titleCol = columns.size() >= 2 ? columns.get(1) : new ColumnSpec("name", "Course Title", 216, "left");
            List<String> titleLines = wrapText(name, titleCol.width - 14, 8.5, false);
            if (titleLines.isEmpty()) {
                titleLines = List.of("-");
            }
            List<String> warningLines = (warningNote != null && !warningNote.isBlank())
                    ? wrapText(warningNote, titleCol.width - 14, 7.5, true)
                    : List.of();

            int textLineCount = titleLines.size() + warningLines.size();
            double rowH = Math.max(21, textLineCount * 10.5 + 9);

            if (ensureSpace(rowH + 4)) {
                drawTableHeaderImmediate(columns);
            }

            double x = contentLeft();
            double w = contentWidth();
            double bottomY = cursorY - rowH;
            String fillHex = zebra ? config.palette.surfaceZebra : config.palette.surfaceWhite;
            drawBox(x, bottomY, w, rowH, fillHex, config.palette.borderLight, 0.55);

            double colX = x;
            for (ColumnSpec col : columns) {
                drawLine(colX, bottomY, colX, bottomY + rowH, config.palette.borderLight, 0.35);
                double midY = bottomY + rowH / 2.0 - 2.8;
                switch (col.key) {
                    case "code" -> drawText(code, colX + 6, midY, 8.5, true, config.palette.brandDark);
                    case "name" -> {
                        double textY = bottomY + rowH - 12.5;
                        for (String line : titleLines) {
                            drawText(line, colX + 7, textY, 8.5, false, config.palette.textPrimary);
                            textY -= 10.5;
                        }
                        for (String wLine : warningLines) {
                            drawText(wLine, colX + 7, textY, 7.5, true, config.palette.dangerText);
                            textY -= 10.0;
                        }
                    }
                    case "category" -> renderCategoryBadge(category, colX + col.width / 2.0, bottomY + (rowH - 13) / 2.0);
                    case "season" -> drawCenteredText(formatSeasonShort(season), colX + col.width / 2.0, midY, 8.0, false, config.palette.textSecondary);
                    case "credits" -> drawRightAlignedText(credits + " ECTS", colX + col.width - 7, midY, 8.2, true, config.palette.textPrimary);
                    case "status" -> renderStatusBadge(status, colX + col.width / 2.0, bottomY + (rowH - 13) / 2.0);
                    default -> {
                    }
                }
                colX += col.width;
            }
            cursorY = bottomY;
        }

        private void renderCategoryBadge(String category, double centerX, double bottomY) {
            String upper = category != null ? category.toUpperCase(Locale.ROOT) : "CORE";
            String bg = config.palette.neutralBg;
            String fg = config.palette.neutralText;
            if ("CORE".equals(upper)) {
                bg = config.palette.infoBg;
                fg = config.palette.infoText;
            } else if ("ELECTIVE".equals(upper)) {
                bg = "#F3E8FF";
                fg = "#6B21A8";
            } else if ("GENERAL".equals(upper)) {
                bg = config.palette.warnBg;
                fg = config.palette.warnText;
            }
            drawPillCentered(upper, centerX, bottomY, 13, 56, 6.8, bg, fg, null);
        }

        private void renderStatusBadge(String status, double centerX, double bottomY) {
            String upper = status != null ? status.toUpperCase(Locale.ROOT) : "PLANNED";
            String bg = config.palette.neutralBg;
            String fg = config.palette.neutralText;
            if ("COMPLETED".equals(upper)) {
                bg = config.palette.successBg;
                fg = config.palette.successText;
            } else if ("ENROLLED".equals(upper)) {
                bg = config.palette.infoBg;
                fg = config.palette.infoText;
            }
            drawPillCentered(upper, centerX, bottomY, 13, 60, 6.8, bg, fg, null);
        }

        private String formatSeasonShort(String season) {
            if ("FALL".equalsIgnoreCase(season)) {
                return "Fall";
            }
            if ("SPRING".equalsIgnoreCase(season)) {
                return "Spring";
            }
            return "Fall/Spr";
        }

        // ── Low-Level PDF Vector Drawing Helpers ──

        private StringBuilder currentStream() {
            return pages.get(pages.size() - 1);
        }

        private void fillRect(double x, double y, double w, double h, String fillHex) {
            StringBuilder sb = currentStream();
            sb.append("q\n")
              .append(parseHexRgb(fillHex)).append(" rg\n")
              .append(fmt(x)).append(" ").append(fmt(y)).append(" ")
              .append(fmt(w)).append(" ").append(fmt(h)).append(" re f\n")
              .append("Q\n");
        }

        private void drawBox(double x, double y, double w, double h, String fillHex, String strokeHex, double lineWidth) {
            StringBuilder sb = currentStream();
            sb.append("q\n");
            if (fillHex != null) {
                sb.append(parseHexRgb(fillHex)).append(" rg\n");
            }
            if (strokeHex != null) {
                sb.append(parseHexRgb(strokeHex)).append(" RG\n")
                  .append(fmt(lineWidth)).append(" w\n");
            }
            sb.append(fmt(x)).append(" ").append(fmt(y)).append(" ")
              .append(fmt(w)).append(" ").append(fmt(h)).append(" re ");
            if (fillHex != null && strokeHex != null) {
                sb.append("B\n");
            } else if (fillHex != null) {
                sb.append("f\n");
            } else {
                sb.append("S\n");
            }
            sb.append("Q\n");
        }

        private void drawRoundedBox(double x, double y, double w, double h, double radius,
                                    String fillHex, String strokeHex, double lineWidth) {
            double r = Math.min(radius, Math.min(w, h) / 2.0);
            if (r <= 0.5) {
                drawBox(x, y, w, h, fillHex, strokeHex, lineWidth);
                return;
            }
            double k = r * BEZIER_KAPPA;
            StringBuilder sb = currentStream();
            sb.append("q\n");
            if (fillHex != null) {
                sb.append(parseHexRgb(fillHex)).append(" rg\n");
            }
            if (strokeHex != null) {
                sb.append(parseHexRgb(strokeHex)).append(" RG\n")
                  .append(fmt(lineWidth)).append(" w\n");
            }
            sb.append(fmt(x + r)).append(" ").append(fmt(y)).append(" m\n")
              .append(fmt(x + w - r)).append(" ").append(fmt(y)).append(" l\n")
              .append(fmt(x + w - r + k)).append(" ").append(fmt(y)).append(" ")
              .append(fmt(x + w)).append(" ").append(fmt(y + r - k)).append(" ")
              .append(fmt(x + w)).append(" ").append(fmt(y + r)).append(" c\n")
              .append(fmt(x + w)).append(" ").append(fmt(y + h - r)).append(" l\n")
              .append(fmt(x + w)).append(" ").append(fmt(y + h - r + k)).append(" ")
              .append(fmt(x + w - r + k)).append(" ").append(fmt(y + h)).append(" ")
              .append(fmt(x + w - r)).append(" ").append(fmt(y + h)).append(" c\n")
              .append(fmt(x + r)).append(" ").append(fmt(y + h)).append(" l\n")
              .append(fmt(x + r - k)).append(" ").append(fmt(y + h)).append(" ")
              .append(fmt(x)).append(" ").append(fmt(y + h - r + k)).append(" ")
              .append(fmt(x)).append(" ").append(fmt(y + h - r)).append(" c\n")
              .append(fmt(x)).append(" ").append(fmt(y + r)).append(" l\n")
              .append(fmt(x)).append(" ").append(fmt(y + r - k)).append(" ")
              .append(fmt(x + r - k)).append(" ").append(fmt(y)).append(" ")
              .append(fmt(x + r)).append(" ").append(fmt(y)).append(" c h ");

            if (fillHex != null && strokeHex != null) {
                sb.append("B\n");
            } else if (fillHex != null) {
                sb.append("f\n");
            } else {
                sb.append("S\n");
            }
            sb.append("Q\n");
        }

        private void drawLine(double x1, double y1, double x2, double y2, String strokeHex, double lineWidth) {
            StringBuilder sb = currentStream();
            sb.append("q\n")
              .append(parseHexRgb(strokeHex)).append(" RG\n")
              .append(fmt(lineWidth)).append(" w\n")
              .append(fmt(x1)).append(" ").append(fmt(y1)).append(" m ")
              .append(fmt(x2)).append(" ").append(fmt(y2)).append(" l S\n")
              .append("Q\n");
        }

        private void drawProgressBar(double x, double y, double w, double h, double ratio, String trackHex, String fillHex) {
            drawRoundedBox(x, y, w, h, h / 2.0, trackHex, null, 0);
            double fillW = w * Math.min(1.0, Math.max(0.0, ratio));
            if (fillW >= 2.0) {
                drawRoundedBox(x, y, fillW, h, Math.min(h / 2.0, fillW / 2.0), fillHex, null, 0);
            }
        }

        private void drawPillCentered(String text, double centerX, double bottomY, double h,
                                      double minW, double fontSize, String fillHex, String textHex, String borderHex) {
            String clean = sanitizeAscii(text);
            double w = Math.max(minW, estimateTextWidth(clean, fontSize, true) + 12);
            double x = centerX - w / 2.0;
            drawRoundedBox(x, bottomY, w, h, h / 2.0, fillHex, borderHex, borderHex != null ? 0.6 : 0);
            drawCenteredText(clean, centerX, bottomY + (h - fontSize * 0.72) / 2.0, fontSize, true, textHex);
        }

        private void drawPillAtRight(String text, double rightX, double bottomY, double h,
                                     double minW, double fontSize, String fillHex, String textHex, String borderHex) {
            String clean = sanitizeAscii(text);
            double w = Math.max(minW, estimateTextWidth(clean, fontSize, true) + 12);
            double centerX = rightX - w / 2.0;
            drawRoundedBox(rightX - w, bottomY, w, h, h / 2.0, fillHex, borderHex, borderHex != null ? 0.6 : 0);
            drawCenteredText(clean, centerX, bottomY + (h - fontSize * 0.72) / 2.0, fontSize, true, textHex);
        }

        private void drawText(String text, double x, double baselineY, double fontSize, boolean bold, String colorHex) {
            drawTextToStream(currentStream(), text, x, baselineY, fontSize, bold, colorHex);
        }

        private void drawCenteredText(String text, double centerX, double baselineY, double fontSize, boolean bold, String colorHex) {
            double width = estimateTextWidth(text, fontSize, bold);
            drawTextToStream(currentStream(), text, centerX - width / 2.0, baselineY, fontSize, bold, colorHex);
        }

        private void drawRightAlignedText(String text, double rightX, double baselineY, double fontSize, boolean bold, String colorHex) {
            double width = estimateTextWidth(text, fontSize, bold);
            drawTextToStream(currentStream(), text, rightX - width, baselineY, fontSize, bold, colorHex);
        }

        private void drawTextToStream(
                StringBuilder sb,
                String text,
                double x,
                double baselineY,
                double fontSize,
                boolean bold,
                String colorHex) {
            String clean = sanitizeAscii(text);
            String font = bold ? "/F2" : "/F1";
            sb.append("BT ")
              .append(parseHexRgb(colorHex)).append(" rg ")
              .append(font).append(" ").append(fmt(fontSize)).append(" Tf ")
              .append(fmt(x)).append(" ").append(fmt(baselineY)).append(" Td (")
              .append(escapePdfString(clean)).append(") Tj ET\n");
        }

        public byte[] toPdfBytes() {
            int totalPages = pages.size();
            for (int i = 0; i < totalPages; i++) {
                StringBuilder stream = pages.get(i);
                double left = contentLeft();
                double right = contentRight();
                stream.append("q\n")
                      .append(parseHexRgb(config.palette.borderLight)).append(" RG\n")
                      .append("0.7 w\n")
                      .append(fmt(left)).append(" 44 m ").append(fmt(right)).append(" 44 l S\n")
                      .append("Q\n");
                drawTextToStream(stream, config.branding.footerText, left, 28, 8.5, false, config.palette.textMuted);
                String pageLabel = "Page " + (i + 1) + " of " + totalPages;
                double labelW = estimateTextWidth(pageLabel, 8.5, false);
                drawTextToStream(stream, pageLabel, right - labelW, 28, 8.5, true, config.palette.textSecondary);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeAscii(out, "%PDF-1.4\n");

            List<Long> offsets = new ArrayList<>();
            offsets.add(0L);

            List<Integer> pageObjectIds = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                pageObjectIds.add(6 + 2 * i);
            }

            offsets.add((long) out.size());
            writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            offsets.add((long) out.size());
            StringBuilder kids = new StringBuilder();
            for (Integer pid : pageObjectIds) {
                kids.append(pid).append(" 0 R ");
            }
            writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + totalPages + " >>\nendobj\n");

            offsets.add((long) out.size());
            writeAscii(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            offsets.add((long) out.size());
            writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

            String mediaBox = String.format(Locale.US, "[0 0 %.0f %.0f]", config.page.width, config.page.height);
            for (int i = 0; i < totalPages; i++) {
                int contentObjId = 5 + 2 * i;
                int pageObjId = 6 + 2 * i;
                byte[] streamBytes = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);

                offsets.add((long) out.size());
                writeAscii(out, contentObjId + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
                out.writeBytes(streamBytes);
                writeAscii(out, "\nendstream\nendobj\n");

                offsets.add((long) out.size());
                writeAscii(out, pageObjId + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox " + mediaBox
                        + " /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObjId + " 0 R >>\nendobj\n");
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
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Typography & Color Utilities
    // ─────────────────────────────────────────────────────────────────────────────

    public static List<String> wrapText(String text, double maxWidthPt, double fontSize, boolean bold) {
        String clean = sanitizeAscii(text).trim();
        if (clean.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        String[] words = clean.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.isEmpty()) {
                if (estimateTextWidth(word, fontSize, bold) <= maxWidthPt) {
                    current.append(word);
                } else {
                    lines.addAll(splitLongWord(word, maxWidthPt, fontSize, bold));
                }
            } else {
                String candidate = current + " " + word;
                if (estimateTextWidth(candidate, fontSize, bold) <= maxWidthPt) {
                    current.append(' ').append(word);
                } else {
                    lines.add(current.toString());
                    current.setLength(0);
                    if (estimateTextWidth(word, fontSize, bold) <= maxWidthPt) {
                        current.append(word);
                    } else {
                        lines.addAll(splitLongWord(word, maxWidthPt, fontSize, bold));
                    }
                }
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static List<String> splitLongWord(String word, double maxWidthPt, double fontSize, boolean bold) {
        List<String> parts = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            String candidate = chunk.toString() + ch;
            if (!chunk.isEmpty() && estimateTextWidth(candidate, fontSize, bold) > maxWidthPt) {
                parts.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(ch);
        }
        if (!chunk.isEmpty()) {
            parts.add(chunk.toString());
        }
        return parts;
    }

    public static String fitSingleLine(String text, double maxWidthPt, double fontSize, boolean bold) {
        String clean = sanitizeAscii(text).trim();
        if (estimateTextWidth(clean, fontSize, bold) <= maxWidthPt) {
            return clean;
        }
        String suffix = "...";
        StringBuilder sb = new StringBuilder(clean);
        while (sb.length() > 1 && estimateTextWidth(sb + suffix, fontSize, bold) > maxWidthPt) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString().trim() + suffix;
    }

    public static double estimateTextWidth(String text, double fontSize, boolean bold) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        double emWidth = 0.0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            emWidth += charEmWidth(c);
        }
        double weightFactor = bold ? 1.06 : 1.0;
        return emWidth * fontSize * weightFactor;
    }

    private static double charEmWidth(char c) {
        if (c == ' ') return 0.28;
        if ("ilIjtf!.,:;'|`".indexOf(c) >= 0) return 0.28;
        if ("r()[]{}-/\\\"".indexOf(c) >= 0) return 0.34;
        if ("MWm@%&".indexOf(c) >= 0) return 0.88;
        if (c >= 'A' && c <= 'Z') return 0.68;
        if (c >= '0' && c <= '9') return 0.56;
        return 0.52;
    }

    private static String parseHexRgb(String hex) {
        if (hex == null || hex.isBlank()) {
            return "0.118 0.161 0.231";
        }
        String clean = hex.trim().startsWith("#") ? hex.trim().substring(1) : hex.trim();
        if (clean.length() != 6) {
            return "0.118 0.161 0.231";
        }
        try {
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return String.format(Locale.US, "%.3f %.3f %.3f", r / 255.0, g / 255.0, b / 255.0);
        } catch (NumberFormatException ex) {
            return "0.118 0.161 0.231";
        }
    }

    private static String fmt(double val) {
        return String.format(Locale.US, "%.2f", val);
    }

    public static String sanitizeAscii(String text) {
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

    private static String escapePdfString(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Template Configuration Model
    // ─────────────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PdfTemplateConfig {
        public String name = "sdu-academic-plan-default";
        public String version = "1.0";
        public PageSpec page = new PageSpec();
        public BrandingSpec branding = new BrandingSpec();
        public PaletteSpec palette = new PaletteSpec();
        public List<ColumnSpec> cartColumns = defaultCartColumns();
        public List<ColumnSpec> roadmapColumns = defaultRoadmapColumns();

        public static PdfTemplateConfig defaultConfig() {
            return new PdfTemplateConfig();
        }

        public static PdfTemplateConfig withDefaults(PdfTemplateConfig src) {
            PdfTemplateConfig d = defaultConfig();
            if (src.name != null && !src.name.isBlank()) d.name = src.name;
            if (src.version != null && !src.version.isBlank()) d.version = src.version;
            if (src.page != null) {
                if (src.page.width > 0) d.page.width = src.page.width;
                if (src.page.height > 0) d.page.height = src.page.height;
                if (src.page.marginLeft > 0) d.page.marginLeft = src.page.marginLeft;
                if (src.page.marginRight > 0) d.page.marginRight = src.page.marginRight;
                if (src.page.marginTop > 0) d.page.marginTop = src.page.marginTop;
                if (src.page.marginBottom > 0) d.page.marginBottom = src.page.marginBottom;
            }
            if (src.branding != null) {
                d.branding.headerTitle = firstNonBlank(src.branding.headerTitle, d.branding.headerTitle);
                d.branding.headerSubtitle = firstNonBlank(src.branding.headerSubtitle, d.branding.headerSubtitle);
                d.branding.footerText = firstNonBlank(src.branding.footerText, d.branding.footerText);
                d.branding.cartSectionTitle = firstNonBlank(src.branding.cartSectionTitle, d.branding.cartSectionTitle);
                d.branding.roadmapSectionTitle = firstNonBlank(src.branding.roadmapSectionTitle, d.branding.roadmapSectionTitle);
            }
            if (src.palette != null) {
                d.palette.brandDark = firstNonBlank(src.palette.brandDark, d.palette.brandDark);
                d.palette.brandPrimary = firstNonBlank(src.palette.brandPrimary, d.palette.brandPrimary);
                d.palette.brandLight = firstNonBlank(src.palette.brandLight, d.palette.brandLight);
                d.palette.brandBorder = firstNonBlank(src.palette.brandBorder, d.palette.brandBorder);
                d.palette.surfaceWhite = firstNonBlank(src.palette.surfaceWhite, d.palette.surfaceWhite);
                d.palette.surfaceMuted = firstNonBlank(src.palette.surfaceMuted, d.palette.surfaceMuted);
                d.palette.surfaceZebra = firstNonBlank(src.palette.surfaceZebra, d.palette.surfaceZebra);
                d.palette.borderSlate = firstNonBlank(src.palette.borderSlate, d.palette.borderSlate);
                d.palette.borderLight = firstNonBlank(src.palette.borderLight, d.palette.borderLight);
                d.palette.textPrimary = firstNonBlank(src.palette.textPrimary, d.palette.textPrimary);
                d.palette.textSecondary = firstNonBlank(src.palette.textSecondary, d.palette.textSecondary);
                d.palette.textMuted = firstNonBlank(src.palette.textMuted, d.palette.textMuted);
                d.palette.successBg = firstNonBlank(src.palette.successBg, d.palette.successBg);
                d.palette.successText = firstNonBlank(src.palette.successText, d.palette.successText);
                d.palette.warnBg = firstNonBlank(src.palette.warnBg, d.palette.warnBg);
                d.palette.warnText = firstNonBlank(src.palette.warnText, d.palette.warnText);
                d.palette.dangerBg = firstNonBlank(src.palette.dangerBg, d.palette.dangerBg);
                d.palette.dangerText = firstNonBlank(src.palette.dangerText, d.palette.dangerText);
                d.palette.infoBg = firstNonBlank(src.palette.infoBg, d.palette.infoBg);
                d.palette.infoText = firstNonBlank(src.palette.infoText, d.palette.infoText);
                d.palette.neutralBg = firstNonBlank(src.palette.neutralBg, d.palette.neutralBg);
                d.palette.neutralText = firstNonBlank(src.palette.neutralText, d.palette.neutralText);
            }
            if (src.cartColumns != null && !src.cartColumns.isEmpty()) {
                d.cartColumns = src.cartColumns.stream().filter(Objects::nonNull).toList();
            }
            if (src.roadmapColumns != null && !src.roadmapColumns.isEmpty()) {
                d.roadmapColumns = src.roadmapColumns.stream().filter(Objects::nonNull).toList();
            }
            return d;
        }

        private static String firstNonBlank(String candidate, String fallback) {
            return (candidate != null && !candidate.isBlank()) ? candidate : fallback;
        }

        private static List<ColumnSpec> defaultCartColumns() {
            return List.of(
                    new ColumnSpec("index", "#", 28, "center"),
                    new ColumnSpec("code", "Course", 78, "left"),
                    new ColumnSpec("name", "Course Title", 274, "left"),
                    new ColumnSpec("credits", "Credits", 70, "right"),
                    new ColumnSpec("status", "Status", 90, "center")
            );
        }

        private static List<ColumnSpec> defaultRoadmapColumns() {
            return List.of(
                    new ColumnSpec("code", "Course", 68, "left"),
                    new ColumnSpec("name", "Course Title", 216, "left"),
                    new ColumnSpec("category", "Category", 72, "center"),
                    new ColumnSpec("season", "Season", 62, "center"),
                    new ColumnSpec("credits", "Credits", 48, "right"),
                    new ColumnSpec("status", "Status", 74, "center")
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageSpec {
        public double width = 612;
        public double height = 792;
        public double marginLeft = 36;
        public double marginRight = 36;
        public double marginTop = 30;
        public double marginBottom = 56;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrandingSpec {
        public String headerTitle = "SDU Academic Advisor - Study Plan Export";
        public String headerSubtitle = "Official Semester Plan & 4-Year Degree Roadmap";
        public String footerText = "SDU Academic Advisor | Academic Planning Report";
        public String cartSectionTitle = "SEMESTER CART PLAN";
        public String roadmapSectionTitle = "4-YEAR DEGREE ROADMAP (8 SEMESTERS)";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaletteSpec {
        public String brandDark = "#364FC7";
        public String brandPrimary = "#4C6EF5";
        public String brandLight = "#EDF2FF";
        public String brandBorder = "#BAC8FF";
        public String surfaceWhite = "#FFFFFF";
        public String surfaceMuted = "#F8FAFC";
        public String surfaceZebra = "#F1F5F9";
        public String borderSlate = "#CBD5E1";
        public String borderLight = "#E2E8F0";
        public String textPrimary = "#1E293B";
        public String textSecondary = "#475569";
        public String textMuted = "#64748B";
        public String successBg = "#DCFCE7";
        public String successText = "#166534";
        public String warnBg = "#FEF3C7";
        public String warnText = "#92400E";
        public String dangerBg = "#FEE2E2";
        public String dangerText = "#991B1B";
        public String infoBg = "#E0E7FF";
        public String infoText = "#3730A3";
        public String neutralBg = "#E2E8F0";
        public String neutralText = "#334155";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColumnSpec {
        public String key;
        public String label;
        public double width;
        public String align;

        public ColumnSpec() {
        }

        public ColumnSpec(String key, String label, double width, String align) {
            this.key = key;
            this.label = label;
            this.width = width;
            this.align = align;
        }
    }
}
