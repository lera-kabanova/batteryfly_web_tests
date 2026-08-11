package org.example.auth;

import org.example.auth.assertions.AuthAssertions;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Security-регрессии входа

class AuthSecurityTest extends AuthTestBase {

    @Test
    @DisplayName("CAUTH-SEC-01: SQL-injection-подобная строка в email/пароль не обходит валидацию")
    void loginWithSqlInjectionLikeInput_doesNotBypassValidation() {
        LoginPage loginPage = openLoginPage();
        loginPage.typeEmail(AuthTestConfig.SQL_INJECTION_PAYLOAD);
        loginPage.typePassword(AuthTestConfig.SQL_INJECTION_PAYLOAD);

        AuthAssertions.assertLoginButtonDisabled(loginPage);
    }

    @Test
    @DisplayName("CAUTH-SEC-02: XSS-подобная строка в пароле не исполняется")
    void loginWithXssLikePassword_doesNotExecuteScript() {
        LoginPage loginPage = openLoginPage();
        // Если бы скрипт исполнился как window.alert(), последующая WebDriver-команда упала бы
        // с UnhandledAlertException - её отсутствие здесь уже часть проверки.
        loginPage.login(AuthTestConfig.VALID_EMAIL, AuthTestConfig.XSS_PAYLOAD);

        AuthAssertions.assertErrorMessageEquals(loginPage, AuthTestConfig.EXPECTED_INVALID_CREDENTIALS_ERROR);
        AuthAssertions.assertOnLoginForm(driver, wait);
    }

//    @Test
//    @DisplayName("CAUTH-SEC-03: N подряд неверных попыток должны приводить к rate-limit/lockout")
//    @Disabled("BUG-006 (qa-discovery/bugs.md): защита от подбора пароля сейчас отсутствует на проде. "
//            + "Тест написан заранее как regression-guard - включить после того, как Brute Force "
//            + "Detection будет включена в конфигурации Keycloak realm 'batteryfly'.")
//    void repeatedFailedLoginAttempts_shouldTriggerLockout() {
//        LoginPage loginPage = openLoginPage();
//        for (int attempt = 0; attempt < 6; attempt++) {
//            loginPage.login(AuthTestConfig.VALID_EMAIL, "WrongPassword" + attempt + "!");
//        }
//
//        String errorAfterRepeatedAttempts = loginPage.getErrorBannerTextOrEmpty();
//        Assertions.assertNotEquals(AuthTestConfig.EXPECTED_INVALID_CREDENTIALS_ERROR, errorAfterRepeatedAttempts,
//                "Ожидалось сообщение о блокировке/rate-limit после множества неверных попыток");
//    }
}
