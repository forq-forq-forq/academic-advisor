package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.dto.CompletedCourseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CumulativeGpaServiceTest {
    private final CumulativeGpaService service = new CumulativeGpaService();

    @ParameterizedTest
    @CsvSource({"A,4.00", "A-,3.67", "B+,3.33", "B,3.00", "B-,2.67", "C+,2.33",
            "C,2.00", "C-,1.67", "D+,1.33", "D,1.00", "F,0.00", "FX,0.00", "AW,0.00"})
    void usesPublishedSduHandbookGradePoints(String grade, double points) {
        assertEquals(points, service.calculate(List.of(course(grade, 3))));
    }

    @Test
    void weightsByCreditsRatherThanAveragingGrades() {
        assertEquals(3.43, service.calculate(List.of(course("A", 3), course("B", 4))));
    }

    @Test
    void roundsOnceAtTheEndIncludingExactHalfwayValues() {
        assertEquals(3.67, service.calculate(List.of(course("A", 1), course("B+", 1))));
        assertEquals(3.50, service.calculate(List.of(course("A", 7), course("C+", 3))));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Not recorded", "P", "NP", "I", "IP", "W", "AU", "A+", "unknown"})
    void excludesMissingNonGpaAndUnsupportedGradesFromBothSums(String grade) {
        assertEquals(4.0, service.calculate(List.of(course("A", 3), course(grade, 9))));
        assertNull(service.calculate(List.of(course(grade, 9))));
    }

    @Test
    void includesZeroPointGradesInTheDenominator() {
        assertEquals(1.0, service.calculate(List.of(course("A", 3), course("F", 3),
                course("FX", 3), course("AW", 3))));
        assertEquals(0.0, service.calculate(List.of(course("F", 3))));
    }

    @Test
    void safelyExcludesInvalidCreditsAndIncompleteRecords() {
        assertNull(service.calculate(List.of()));
        assertNull(service.calculate(Arrays.asList(null, course("A", null), course("A", 0), course("A", -3))));
        assertEquals(3.0, service.calculate(List.of(course("B", 3), course("A", 0))));
    }

    @Test
    void normalizesCaseAndSurroundingWhitespaceWithoutDependingOnLocale() {
        assertEquals(3.67, service.calculate(List.of(course(" a- ", 4))));
    }

    @Test
    void largeCreditValuesDoNotOverflowIntegerArithmetic() {
        assertEquals(3.5, service.calculate(List.of(course("A", Integer.MAX_VALUE), course("B", Integer.MAX_VALUE))));
    }

    private CompletedCourseDto course(String grade, Integer credits) {
        return new CompletedCourseDto("TEST", "Test course", credits, grade, "Fall 2024");
    }
}
