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

/**
 * ВНИМАНИЕ: тесты этого класса создают НАСТОЯЩИЕ новые аккаунты на боевом сайте batteryfly.io
 * (по одному на каждый запуск метода). По просьбе пользователя эти тесты НЕ запускались
 * автоматически при реализации модуля Registration (2026-07-14) — только написаны и
 * скомпилированы. Запустить вручную, когда будет нужно:
 * <pre>
 *   mvn "-Dtest=RegistrationNewUserTest" test
 * </pre>
 * Локатор ввода OTP-кода ({@link OtpModal#enterCode(String)}) не был проверен живым прогоном —
 * см. предупреждение в его Javadoc.
 */
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
        page.fillStep2("QA Auto Test", RegistrationTestConfig.VALID_TEST_PHONE);
        page.agreements().acceptBoth();
        page.submitFinal();

        OtpModal otp = new OtpModal(driver, wait);
        Assertions.assertTrue(otp.isVisible(), "OTP-модал не появился после регистрации");

        otp.enterCode("0000");
        // BUG-003 (qa-discovery/bugs.md): сейчас показывается неверный текст "Неверный пароль"
        // вместо сообщения про код — фиксируем сам факт появления ошибки как regression-guard;
        // текст ошибки нужно будет ужесточить (assertEquals на конкретную строку) после фикса BUG-003.
        String errorText = otp.getErrorTextOrEmpty();
        Assertions.assertFalse(errorText.isEmpty(), "После неверного OTP-кода должна появиться ошибка");
    }

    @Test
    @DisplayName("REG-NEW-02 (BUG-008): один телефон переиспользуется для 2 новых аккаунтов без блокировки")
    void samePhoneAcrossTwoNewAccounts_isNotBlocked() {
        String sharedPhone = RegistrationTestConfig.VALID_TEST_PHONE;

        RegistrationPage first = openRegistrationPage();
        first.fillStep1(generateUniqueEmail(), "Test123!").submitStep1();
        first.fillStep2("QA Auto Test A", sharedPhone);
        first.agreements().acceptBoth();
        first.submitFinal();

        // Вторая регистрация требует независимой неавторизованной сессии — первая уже
        // залогинена после успешного сабмита (см. observations.md), поэтому используется
        // отдельный WebDriver, а не общий driver/wait из RegistrationTestBase.
        WebDriver secondDriver = new ChromeDriver(freshChromeOptions());
        try {
            WebDriverWait secondWait = new WebDriverWait(secondDriver, Duration.ofSeconds(15));
            RegistrationPage second = new RegistrationPage(secondDriver, secondWait)
                    .openFromLoginForm(RegistrationTestConfig.BASE_URL);
            second.fillStep1(generateUniqueEmail(), "Test123!").submitStep1();
            second.fillStep2("QA Auto Test B", sharedPhone);
            second.agreements().acceptBoth();
            second.submitFinal();

            Assertions.assertTrue(second.getGlobalErrorTextOrEmpty().isEmpty(),
                    "Ожидалось отсутствие блокировки по телефону (см. BUG-008) — "
                            + "если сервер теперь блокирует повтор, тест нужно обновить под новое поведение");
        } finally {
            secondDriver.quit();
        }
    }
}
