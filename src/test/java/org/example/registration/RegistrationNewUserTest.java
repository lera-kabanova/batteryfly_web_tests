package org.example.registration;

import org.example.registration.pages.OtpModal;
import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

class RegistrationNewUserTest extends RegistrationTestBase {

    private String generateUniqueEmail() {
        return "test+" + System.currentTimeMillis() + "@example.com";
    }

    @Test
    @DisplayName("REG-NEW-01: полная регистрация нового пользователя -> OTP-модал -> неверный код показывает ошибку")
    void fullRegistration_reachesOtpModal_andRejectsWrongCode() {
        String email = generateUniqueEmail();
        RegistrationPage page = openRegistrationPage();
        page.fillStep1(email, "Test123!").submitStep1();
        page.fillStep2("Test", RegistrationTestConfig.VALID_TEST_PHONE);
        page.agreements().acceptBoth();
        page.submitFinal();

        OtpModal otp = new OtpModal(driver, wait);
        Assertions.assertTrue(otp.isVisible(), "OTP-модал не появился после регистрации");

        otp.enterCode("0000");
        String errorText = otp.getErrorTextOrEmpty();
        Assertions.assertFalse(errorText.isEmpty(), "После неверного OTP-кода должна появиться ошибка");
    }

    @Test
    @DisplayName("REG-NEW-02: один телефон переиспользуется для 2 новых аккаунтов без блокировки")
    void samePhoneAcrossTwoNewAccounts_isNotBlocked() {
        String sharedPhone = RegistrationTestConfig.VALID_TEST_PHONE;

        RegistrationPage first = openRegistrationPage();
        first.fillStep1(generateUniqueEmail(), "Test123!").submitStep1();
        first.fillStep2("Test A", sharedPhone);
        first.agreements().acceptBoth();
        first.submitFinal();

        WebDriver secondDriver = new ChromeDriver(freshChromeOptions());
        try {
            WebDriverWait secondWait = new WebDriverWait(secondDriver, Duration.ofSeconds(15));
            RegistrationPage second = new RegistrationPage(secondDriver, secondWait)
                    .openFromLoginForm(RegistrationTestConfig.BASE_URL);
            second.fillStep1(generateUniqueEmail(), "Test123!").submitStep1();
            second.fillStep2("Test B", sharedPhone);
            second.agreements().acceptBoth();
            second.submitFinal();

            Assertions.assertTrue(second.getGlobalErrorTextOrEmpty().isEmpty(),
                    "Ожидалось отсутствие блокировки по телефону");
        } finally {
            secondDriver.quit();
        }
    }
}
