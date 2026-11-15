document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('loginForm');
    const studentIdInput = document.getElementById('studentId');
    const passwordInput = document.getElementById('password');
    const studentIdHint = document.getElementById('studentIdHint');
    const passwordHint = document.getElementById('passwordHint');

    function checkField(input, hint) {
        const val = input.value ? input.value.trim() : '';
        if (!val) {
            input.classList.add('input-error');
            if (hint) hint.classList.add('visible');
            return false;
        } else {
            input.classList.remove('input-error');
            if (hint) hint.classList.remove('visible');
            return true;
        }
    }

    studentIdInput.addEventListener('input', function () {
        checkField(studentIdInput, studentIdHint);
    });

    passwordInput.addEventListener('input', function () {
        checkField(passwordInput, passwordHint);
    });

    form.addEventListener('submit', function (event) {
        const isIdValid = checkField(studentIdInput, studentIdHint);
        const isPwValid = checkField(passwordInput, passwordHint);

        if (!isIdValid || !isPwValid) {
            event.preventDefault();
            if (!isIdValid) {
                studentIdInput.focus();
            } else {
                passwordInput.focus();
            }
        }
    });
});
