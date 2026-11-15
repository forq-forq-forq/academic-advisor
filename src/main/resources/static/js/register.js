const facultySelect = document.getElementById('facultyCode');
const majorSelect = document.getElementById('majorCode');
const emailInput = document.getElementById('email');
const studentIdInput = document.getElementById('studentId');

function filterMajors() {
    const selectedFaculty = facultySelect.value;
    let visibleCount = 0;
    let lastVisibleValue = '';

    for (const option of majorSelect.options) {
        if (!option.value) {
            option.hidden = false;
            continue;
        }
        const belongs = !selectedFaculty || (option.getAttribute('data-faculty-code') === selectedFaculty);
        option.hidden = !belongs;
        if (belongs) {
            visibleCount++;
            lastVisibleValue = option.value;
        }
    }

    if (majorSelect.selectedOptions[0]?.hidden) {
        majorSelect.value = '';
    }
    if (visibleCount === 1 && !majorSelect.value) {
        majorSelect.value = lastVisibleValue;
    }
}

if (facultySelect.options.length === 2 && !facultySelect.value) {
    facultySelect.selectedIndex = 1;
}

emailInput.addEventListener('blur', () => {
    if (!studentIdInput.value.trim() && emailInput.value.includes('@')) {
        studentIdInput.value = emailInput.value.split('@')[0].trim();
    }
});

facultySelect.addEventListener('change', filterMajors);
filterMajors();

const careerGoalInput = document.getElementById('careerGoal');
const aspirationChips = document.querySelectorAll('#aspiration-presets .aspiration-chip');

function syncAspirationChips() {
    if (!careerGoalInput) {
        return;
    }
    const current = careerGoalInput.value.trim().toLowerCase();
    aspirationChips.forEach(chip => {
        const target = (chip.dataset.goal || '').trim().toLowerCase();
        chip.classList.toggle('active', Boolean(current) && current === target);
    });
}

if (careerGoalInput && aspirationChips.length > 0) {
    aspirationChips.forEach(chip => {
        chip.addEventListener('click', () => {
            careerGoalInput.value = chip.dataset.goal || '';
            syncAspirationChips();
            careerGoalInput.focus();
        });
    });
    careerGoalInput.addEventListener('input', syncAspirationChips);
    syncAspirationChips();
}
