package org.example.auth;

import org.example.auth.assertions.AuthAssertions;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Smoke-тест для Auth

class AuthSmokeTest extends AuthTestBase {

    @Test
    @DisplayName("CAUTH-SMOKE-01: успешный вход валидными кредами")
    void loginWithValidCredentials_succeedsAndReachesAuthenticatedArea() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);

        AuthenticatedAreaPage authenticatedArea = new AuthenticatedAreaPage(driver, wait);
        AuthAssertions.assertLoginSucceeded(authenticatedArea);
        AuthAssertions.assertRedirectedAwayFromLoginForm(driver, wait);

        System.out.println("Вход валидными кредами: успех, приветствие и редирект подтверждены.");
    }
}
