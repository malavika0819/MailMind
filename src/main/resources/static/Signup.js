document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('signupForm');
    const usernameInput = document.getElementById('username');
    const emailInput = document.getElementById('email-id');
    const passwordInput = document.getElementById('password');
    const termsCheckbox = document.getElementById('terms');
    const genderRadios = document.querySelectorAll('input[name="gender"]');

    const strengthBar = document.querySelector('.strength-bar');
    const strengthText = document.querySelector('.strength-text');

    function showError(inputElement, message) {
        const formGroup = inputElement.closest('.form-group');
        const errorElement = formGroup.querySelector('.error-message');
        if (errorElement) {
            errorElement.textContent = message;
        }
        inputElement.classList.add('invalid');
        inputElement.classList.remove('valid');
    }

    function showSuccess(inputElement) {
        const formGroup = inputElement.closest('.form-group');
        const errorElement = formGroup.querySelector('.error-message');
        if (errorElement) {
            errorElement.textContent = '';
        }
        inputElement.classList.add('valid');
        inputElement.classList.remove('invalid');
    }

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    function checkPasswordStrength(password) {
        let strength = 0;
        if (!password) { // Handle empty password case explicitly
            strengthBar.style.width = '0%';
            strengthBar.style.backgroundColor = '#e0e0e0';
            strengthText.textContent = '';
            return;
        }
        if (password.length >= 8) strength++;
        if (password.match(/[a-z]/)) strength++;
        if (password.match(/[A-Z]/)) strength++;
        if (password.match(/[0-9]/)) strength++;
        if (password.match(/[^a-zA-Z0-9\s]/)) strength++; // Special characters (excluding space)


        let barWidth = (strength / 5) * 100;
        let text = "";
        let color = "#e0e0e0";

        switch (strength) {
            case 0: // Fall through if password is very short but not empty
            case 1:
                text = "Weak";
                color = "#e74c3c";
                break;
            case 2:
                text = "Fair";
                color = "#f39c12";
                break;
            case 3:
                text = "Good";
                color = "#f1c40f";
                break;
            case 4:
                text = "Strong";
                color = "#2ecc71";
                break;
            case 5:
                text = "Very Strong";
                color = "#1abc9c";
                break;
        }
        strengthBar.style.width = barWidth + '%';
        strengthBar.style.backgroundColor = color;
        strengthText.textContent = text;
    }

    function validateField(inputElement, checks) {
        for (const check of checks) {
            if (!check.test(inputElement.value.trim())) {
                showError(inputElement, check.message);
                return false;
            }
        }
        showSuccess(inputElement);
        return true;
    }

    usernameInput.addEventListener('blur', function () {
        validateField(this, [
            { test: (value) => value !== '', message: 'Username is required.' },
            { test: (value) => value.length >= 3, message: 'Username must be at least 3 characters.' }
        ]);
    });

    emailInput.addEventListener('blur', function () {
        validateField(this, [
            { test: (value) => value !== '', message: 'Email ID is required.' },
            { test: (value) => isValidEmail(value), message: 'Please enter a valid email address.' }
        ]);
    });

    passwordInput.addEventListener('input', function () { // Changed to 'input' for live strength check
        checkPasswordStrength(this.value);
        // Don't show error on input, only on blur or submit for password length
        if (this.classList.contains('invalid') && this.value.length >= 8) {
            showSuccess(this); // Clear error if length requirement is met while typing
        } else if (this.classList.contains('invalid') && this.value === '') {
             // If it was invalid and now empty, keep the error message (or re-evaluate on blur)
        }
    });
     passwordInput.addEventListener('blur', function () {
        if (this.value === '') {
            showError(this, 'Password is required.');
        } else if (this.value.length < 8) {
            showError(this, 'Password must be at least 8 characters.');
        } else {
            showSuccess(this);
        }
        // Ensure strength meter reflects current state even if empty after blur
        checkPasswordStrength(this.value);
    });


    termsCheckbox.addEventListener('change', function() {
        const formGroup = this.closest('.form-group');
        const errorElement = formGroup.querySelector('.error-message');
        if (!this.checked) {
            if (errorElement) errorElement.textContent = 'You must agree to the terms.';
        } else {
            if (errorElement) errorElement.textContent = '';
        }
    });

    function validateGender() {
        const checkedGender = document.querySelector('input[name="gender"]:checked');
        const genderErrorElement = document.querySelector('.gender-error'); // Ensure this selector is correct
        if (!checkedGender) {
            if(genderErrorElement) genderErrorElement.textContent = 'Please select your gender.';
            return false;
        } else {
            if(genderErrorElement) genderErrorElement.textContent = '';
            return true;
        }
    }

    genderRadios.forEach(radio => radio.addEventListener('change', validateGender));

    form.addEventListener('submit', function (event) {
        let isValidForm = true;

        isValidForm = validateField(usernameInput, [
            { test: (value) => value !== '', message: 'Username is required.' },
            { test: (value) => value.length >= 3, message: 'Username must be at least 3 characters.' }
        ]) && isValidForm;

        isValidForm = validateField(emailInput, [
            { test: (value) => value !== '', message: 'Email ID is required.' },
            { test: (value) => isValidEmail(value), message: 'Please enter a valid email address.' }
        ]) && isValidForm;

        if (passwordInput.value === '') {
            showError(passwordInput, 'Password is required.');
            isValidForm = false;
        } else if (passwordInput.value.length < 8) {
            showError(passwordInput, 'Password must be at least 8 characters.');
            isValidForm = false;
        } else {
            showSuccess(passwordInput);
        }

        if (!validateGender()) {
            isValidForm = false;
        }

        const termsGroup = termsCheckbox.closest('.form-group');
        const termsErrorElement = termsGroup.querySelector('.error-message');
        if (!termsCheckbox.checked) {
            if(termsErrorElement) termsErrorElement.textContent = 'You must agree to the terms and conditions.';
            isValidForm = false;
        } else {
            if(termsErrorElement) termsErrorElement.textContent = '';
        }

        if (!isValidForm) {
            event.preventDefault();
            const firstInvalid = form.querySelector('.invalid, input[type="radio"]:invalid, input[type="checkbox"]:invalid'); // Simplified selector
            if (firstInvalid) {
                // Find the actual input element if the invalid class is on a group or label
                let focusableElement = firstInvalid;
                if(firstInvalid.classList.contains('form-group')) {
                    focusableElement = firstInvalid.querySelector('input, select, textarea');
                }
                if(focusableElement) focusableElement.focus();
            }
        } else {
            const submitButton = form.querySelector('.submit-button');
            submitButton.textContent = 'Creating Account...';
            submitButton.disabled = true;
        }
    });

    const inputs = form.querySelectorAll('input[type="text"], input[type="email"], input[type="password"]');
    inputs.forEach(input => {
        input.addEventListener('focus', () => { /* Can add focused class if needed for more styling */ });
        input.addEventListener('blur', () => { /* Can remove focused class */ });
    });
});