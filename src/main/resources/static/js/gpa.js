function applyPresetGrades(grade) {
    document.querySelectorAll('.gpa-grade-select').forEach(sel => {
        sel.value = grade;
    });
    runGpaSimulation();
}

function runGpaSimulation() {
    const errorBox = document.getElementById('gpa-error-alert');
    if (errorBox) {
        errorBox.style.display = 'none';
        errorBox.textContent = '';
    }

    const params = new URLSearchParams();
    document.querySelectorAll('.gpa-grade-select').forEach(sel => {
        const courseId = sel.getAttribute('data-course-id');
        if (courseId) {
            params.append('grade_' + courseId, sel.value);
        }
    });

    fetch('/gpa/simulate', {
        method: 'POST',
        headers: Object.assign(
            {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'Accept': 'application/json'
            },
            window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {}
        ),
        body: params.toString()
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                if (errorBox) {
                    errorBox.textContent = data.error || 'Unable to simulate GPA.';
                    errorBox.style.display = 'block';
                }
                return null;
            }
            return data;
        })
        .then(result => {
            if (result) {
                renderGpaSimulation(result);
            }
        })
        .catch(() => {
            if (errorBox) {
                errorBox.textContent = 'Network error while recalculating projected GPA.';
                errorBox.style.display = 'block';
            }
        });
}

function renderGpaSimulation(result) {
    const currentEl = document.getElementById('current-gpa-value');
    const semesterEl = document.getElementById('semester-gpa-value');
    const projectedEl = document.getElementById('projected-gpa-value');
    const deltaEl = document.getElementById('gpa-delta-value');
    const standingEl = document.getElementById('projected-standing-badge');

    if (currentEl) currentEl.textContent = result.formattedCurrentGpa || 'N/A';
    if (semesterEl) semesterEl.textContent = result.formattedSemesterGpa || 'N/A';
    if (projectedEl) projectedEl.textContent = result.formattedProjectedGpa || 'N/A';
    if (deltaEl) deltaEl.textContent = result.formattedGpaDelta || '0.00';
    if (standingEl) standingEl.textContent = result.projectedStandingLabel || '';

    const probationBox = document.getElementById('gpa-probation-warning');
    const probationMsg = document.getElementById('gpa-probation-msg');
    if (probationBox) {
        probationBox.style.display = result.academicProbationRisk ? 'flex' : 'none';
        if (probationMsg && result.probationWarningMessage) {
            probationMsg.textContent = result.probationWarningMessage;
        }
    }

    const scholarshipBox = document.getElementById('gpa-scholarship-warning');
    const scholarshipMsg = document.getElementById('gpa-scholarship-msg');
    if (scholarshipBox) {
        scholarshipBox.style.display = result.belowScholarshipThreshold ? 'flex' : 'none';
        if (scholarshipMsg && result.scholarshipWarningMessage) {
            scholarshipMsg.textContent = result.scholarshipWarningMessage;
        }
    }

    const safeBox = document.getElementById('gpa-safe-banner');
    if (safeBox) {
        safeBox.style.display = result.riskLevel === 'SAFE' ? 'flex' : 'none';
    }

    if (Array.isArray(result.items)) {
        result.items.forEach(item => {
            const cell = document.getElementById('points-cell-' + item.courseId);
            if (cell) {
                cell.textContent = Number(item.gradePoints || 0).toFixed(2);
            }
        });
    }
}
