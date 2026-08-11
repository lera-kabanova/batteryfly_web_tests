package org.example.registration;

import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ссылки на юридические документы на шаге 2 регистрации. Не доходят до финального сабмита.
 */
class RegistrationLinksTest extends RegistrationTestBase {

    @Test
    @DisplayName("REG-LINK-01: ссылка на Публичную оферту ведёт на правильный PDF")
    void publicOfferLink_pointsToCorrectPdf() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("QA Temp", RegistrationTestConfig.VALID_TEST_PHONE);

        Assertions.assertEquals(RegistrationTestConfig.PUBLIC_OFFER_URL, page.getPublicOfferHref());
    }

    @Test
    @DisplayName("REG-LINK-02: ссылка на политику конфиденциальности ведёт на правильный PDF")
    void privacyPolicyLink_pointsToCorrectPdf() {
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(RegistrationTestConfig.EXISTING_EMAIL, RegistrationTestConfig.VALID_PASSWORD_FORMAT).submitStep1();
        page.fillStep2("QA Temp", RegistrationTestConfig.VALID_TEST_PHONE);

        Assertions.assertEquals(RegistrationTestConfig.PRIVACY_POLICY_URL, page.getPrivacyPolicyHref());
    }
}
