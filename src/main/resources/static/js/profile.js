function prefillTranscriptForm(btn) {
    const courseId = btn.getAttribute('data-course-id');
    const grade = btn.getAttribute('data-grade');
    const semester = btn.getAttribute('data-semester');

    const courseSelect = document.getElementById('courseId');
    const gradeSelect = document.getElementById('letterGrade');
    const semesterInput = document.getElementById('semesterTaken');

    if (courseId) courseSelect.value = courseId;
    if (grade && grade !== 'Not recorded') gradeSelect.value = grade;
    if (semester && semester !== 'Not recorded') semesterInput.value = semester;
    courseSelect.focus();
}
