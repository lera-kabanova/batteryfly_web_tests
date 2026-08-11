package org.example.auth;

import org.example.auth.assertions.AuthAssertions;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// UI-регресс формы входа

class AuthUiTest extends AuthTestBase {

    @Test
    @DisplayName("CAUTH-UI-01: показать/скрыть пароль (иконка глаза)")
    void passwordVisibilityToggle_showsAndHidesPassword() {
        LoginPage loginPage = openLoginPage();
        loginPage.typePassword("Test123!");

        Assertions.assertEquals("password", loginPage.getPasswordInputType(), "Пароль должен быть скрыт по умолчанию");

        loginPage.togglePasswordVisibility();
        Assertions.assertEquals("text", loginPage.getPasswordInputType(),
                "После клика по иконке глаза пароль должен стать видимым");

        loginPage.togglePasswordVisibility();
        Assertions.assertEquals("password", loginPage.getPasswordInputType(),
                "После повторного клика пароль должен снова скрыться");
    }
}
