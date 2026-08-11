package org.example.registration;

import org.example.registration.assertions.RegistrationAssertions;
import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Валидация полей регистрации (без финального сабмита с новым email — новый аккаунт не создаётся).
 * Источник: qa-discovery/test-modules.md, модуль 2.
 */
class RegistrationValidationTest extends RegistrationTestBase {

    @Test
    @DisplayName("REG-VAL-01: невалидный формат email на шаге 1 -> подсказка и disabled-кнопка")
    void step1_invalidEmailFormat_showsHintAndDisablesSubmit() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.INVALID_EMAIL_FORMAT, RegistrationTestConfig.VALID_PASSWORD_FORMAT);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_EMAIL_INVALID_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-VAL-02: слабый пароль на шаге 1 -> подсказка и disabled-кнопка")
    void step1_weakPassword_showsHintAndDisablesSubmit() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.WEAK_PASSWORD);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_PASSWORD_PATTERN_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-VAL-03: валидные email+пароль на шаге 1 -> кнопка enabled и переход на шаг 2")
    void step1_validInput_enablesSubmitAndProceedsToStep2() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT);

        RegistrationAssertions.assertSubmitEnabled(page);

        page.submitStep1();
        // Успешный переход на шаг 2 подтверждается тем, что fillStep2 находит поле имени (wait.until).
        page.fillStep2("QA Temp", RegistrationTestConfig.VALID_TEST_PHONE);
    }

    @Test
    @DisplayName("REG-VAL-04: невалидное имя (скрипт/спецсимволы) на шаге 2 -> подсказка")
    void step2_invalidName_showsHint() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2(RegistrationTestConfig.INVALID_NAME_WITH_SCRIPT, RegistrationTestConfig.VALID_TEST_PHONE);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_NAME_PATTERN_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-VAL-05: невалидный/неполный телефон на шаге 2 -> подсказка")
    void step2_invalidPhone_showsHint() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("QA Temp", RegistrationTestConfig.INCOMPLETE_PHONE);

        RegistrationAssertions.assertHintPresent(page, RegistrationTestConfig.EXPECTED_PHONE_PATTERN_HINT);
        RegistrationAssertions.assertSubmitDisabled(page);
    }
}
