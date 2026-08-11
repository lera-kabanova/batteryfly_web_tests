package org.example.auth;

import org.example.auth.assertions.AuthAssertions;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

//выход из системы
class AuthLogoutTest extends AuthTestBase {

    @Test
    @DisplayName("CAUTH-LOGOUT-01: logout возвращает на форму входа")
    void logout_afterValidLogin_returnsToLoginForm() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);

        AuthenticatedAreaPage authenticatedArea = new AuthenticatedAreaPage(driver, wait);
        AuthAssertions.assertLoginSucceeded(authenticatedArea);

        authenticatedArea.logout();

        AuthAssertions.assertOnLoginForm(driver, wait);
    }
}
