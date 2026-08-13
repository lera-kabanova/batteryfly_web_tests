package org.example.registration;

import org.example.registration.assertions.RegistrationAssertions;
import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationExistingEmailTest extends RegistrationTestBase {

    @Test
    @DisplayName("REG-EXIST-01: регистрация с уже существующим email -> «Email уже существует»")
    void registrationWithExistingEmail_showsAlreadyExistsError() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("QA Temp", RegistrationTestConfig.VALID_TEST_PHONE);
        page.agreements().acceptBoth();

        RegistrationAssertions.assertSubmitEnabled(page);
        page.submitFinal();

        RegistrationAssertions.assertGlobalErrorContains(page, RegistrationTestConfig.EXPECTED_EMAIL_EXISTS_ERROR_SUBSTRING);
    }
}
