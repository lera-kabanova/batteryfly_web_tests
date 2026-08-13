package org.example.auth;

import org.example.auth.assertions.AuthAssertions;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

//негативные сценарии входа
class AuthNegativeTest extends AuthTestBase {

    @Test
    @DisplayName("CAUTH-NEG-01: неверный пароль")
    void loginWithInvalidPassword_showsError() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(AuthTestConfig.VALID_EMAIL, "WrongPassword123!");

        AuthAssertions.assertErrorMessageEquals(loginPage, AuthTestConfig.EXPECTED_INVALID_CREDENTIALS_ERROR);
        AuthAssertions.assertOnLoginForm(driver, wait);
    }

    @Test
    @Disabled("Flaky на живом сайте, нестабильно проходит/падает - временно отключено до стабилизации")
    @DisplayName("CAUTH-NEG-02: незарегистрированный email")
    void loginWithUnregisteredEmail_showsError() {
        LoginPage loginPage = openLoginPage();
        loginPage.login(AuthTestConfig.UNREGISTERED_EMAIL, "Password123!");

        AuthAssertions.assertErrorMessageEquals(loginPage, AuthTestConfig.EXPECTED_INVALID_CREDENTIALS_ERROR);
        AuthAssertions.assertOnLoginForm(driver, wait);
    }

    @Test
    @DisplayName("CAUTH-NEG-03: пустой email -> кнопка «Войти» остаётся disabled")
    void loginWithEmptyEmail_buttonStaysDisabled() {
        LoginPage loginPage = openLoginPage();
        loginPage.typePassword(AuthTestConfig.VALID_PASSWORD);

        AuthAssertions.assertLoginButtonDisabled(loginPage);
    }

    @Test
    @DisplayName("CAUTH-NEG-04: пустой пароль -> кнопка «Войти» остаётся disabled")
    void loginWithEmptyPassword_buttonStaysDisabled() {
        LoginPage loginPage = openLoginPage();
        loginPage.typeEmail(AuthTestConfig.VALID_EMAIL);

        AuthAssertions.assertLoginButtonDisabled(loginPage);
    }

    @Test
    @DisplayName("CAUTH-NEG-05: оба поля пустые -> кнопка «Войти» остаётся disabled")
    void loginWithBothFieldsEmpty_buttonStaysDisabled() {
        LoginPage loginPage = openLoginPage();

        AuthAssertions.assertLoginButtonDisabled(loginPage);
    }

    @Test
    @DisplayName("CAUTH-NEG-06: невалидный формат email -> кнопка «Войти» остаётся disabled")
    void loginWithInvalidEmailFormat_buttonStaysDisabled() {
        LoginPage loginPage = openLoginPage();
        loginPage.typeEmail(AuthTestConfig.INVALID_EMAIL_FORMAT);
        loginPage.typePassword(AuthTestConfig.VALID_PASSWORD);

        AuthAssertions.assertLoginButtonDisabled(loginPage);
    }
}
