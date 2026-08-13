package org.example.registration;

import org.example.registration.assertions.RegistrationAssertions;
import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationValidationTest extends RegistrationTestBase {

    @Test
    @DisplayName("REG-VAL-01: невалидный формат email")
    void step1_invalidEmailFormat_showsHintAndDisablesSubmit() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.INVALID_EMAIL_FORMAT, RegistrationTestConfig.VALID_PASSWORD_FORMAT);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_EMAIL_INVALID_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-VAL-02: слабый пароль")
    void step1_weakPassword_showsHintAndDisablesSubmit() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.WEAK_PASSWORD);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_PASSWORD_PATTERN_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-VAL-03: валидные email+пароль")
    void step1_validInput_enablesSubmitAndProceedsToStep2() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT);

        RegistrationAssertions.assertSubmitEnabled(page);

        page.submitStep1();
        page.fillStep2("Test", RegistrationTestConfig.VALID_TEST_PHONE);
    }

    @Test
    @DisplayName("REG-VAL-04: невалидное имя")
    void step2_invalidName_showsHint() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2(RegistrationTestConfig.INVALID_NAME_WITH_SCRIPT, RegistrationTestConfig.VALID_TEST_PHONE);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_NAME_PATTERN_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-VAL-05: невалидный телефон")
    void step2_invalidPhone_showsHint() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("Test", RegistrationTestConfig.INCOMPLETE_PHONE);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_PHONE_PATTERN_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }
}
