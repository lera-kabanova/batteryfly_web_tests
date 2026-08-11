package org.example.auth;

import org.example.auth.assertions.AuthAssertions;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Сценарии сессии и защиты маршрутов

class AuthSessionTest extends AuthTestBase {

    @Test
    @DisplayName("CAUTH-SESSION-01: прямой заход на защищённый маршрут без сессии редиректит на вход")
    void protectedRoute_withoutSession_redirectsToLogin() {
        driver.get(AuthTestConfig.PROTECTED_ROUTE);

        AuthAssertions.assertOnLoginForm(driver, wait);
    }

    @Test
    @DisplayName("CAUTH-SESSION-02: сессия сохраняется при обновлении страницы (F5)")
    void session_persistsAfterPageRefresh() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);
        AuthAssertions.assertLoginSucceeded(new AuthenticatedAreaPage(driver, wait));

        driver.navigate().refresh();

        AuthenticatedAreaPage afterRefresh = new AuthenticatedAreaPage(driver, wait);
        AuthAssertions.assertLoginSucceeded(afterRefresh);
        AuthAssertions.assertRedirectedAwayFromLoginForm(driver, wait);
    }
}
