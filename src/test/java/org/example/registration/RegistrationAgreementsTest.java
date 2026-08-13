package org.example.registration;

import org.example.registration.assertions.RegistrationAssertions;
import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationAgreementsTest extends RegistrationTestBase {

    @Test
    @DisplayName("REG-AGR-01: кнопка сабмита disabled, пока оба согласия не включены")
    void submitButton_disabledUntilBothAgreementsChecked() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("QA Temp", RegistrationTestConfig.VALID_TEST_PHONE);

        RegistrationAssertions.assertSubmitDisabled(page);
    }

    @Test
    @DisplayName("REG-AGR-02: клик по обоим переключателям включает кнопку подтверждения регистрации")
    void bothToggles_correctClick_enablesSubmitButton() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("QA Temp", RegistrationTestConfig.VALID_TEST_PHONE);

        RegistrationAssertions.assertSubmitDisabled(page);
        page.agreements().acceptBoth();

        RegistrationAssertions.assertSubmitEnabled(page);
    }
}
