package org.example.auth.assertions;

import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Доменные проверки модуля Authentication, вынесенные из тел тестов — по аналогии с
 * {@code org.example.business.assertions.UsersPageAssertions}.
 */
public final class AuthAssertions {

    private static final String KEYCLOAK_AUTH_PATH_MARKER = "/auth/realms/";

    private AuthAssertions() {
    }

    public static void assertLoginSucceeded(AuthenticatedAreaPage authenticatedArea) {
        Assertions.assertTrue(authenticatedArea.isWelcomeVisible(),
                "После входа не появилось приветствие 'Добро пожаловать' — вход не удался или элемент не найден");
    }

    /**
     * Редирект с Keycloak-формы обратно в приложение — SPA-навигация, а не мгновенное событие
     * (подтверждено живой проверкой: tools/playwright-codegen/explore-protected-route-redirect.js),
     * поэтому используется {@code wait.until}, а не мгновенное чтение URL.
     */
    public static void assertRedirectedAwayFromLoginForm(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(d -> !d.getCurrentUrl().contains(KEYCLOAK_AUTH_PATH_MARKER));
        } catch (TimeoutException e) {
            Assertions.fail("Ожидался редирект с формы входа Keycloak, но URL всё ещё содержит её путь: "
                    + driver.getCurrentUrl());
        }
    }

    public static void assertOnLoginForm(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(d -> d.getCurrentUrl().contains(KEYCLOAK_AUTH_PATH_MARKER));
        } catch (TimeoutException e) {
            Assertions.fail("Ожидалась форма входа Keycloak, но URL так и не сменился: " + driver.getCurrentUrl());
        }
    }

    public static void assertErrorMessageEquals(LoginPage loginPage, String expectedMessage) {
        String actual = loginPage.getErrorBannerTextOrEmpty();
        Assertions.assertEquals(expectedMessage, actual, "Текст баннера ошибки не совпадает с ожидаемым");
    }

    public static void assertLoginButtonDisabled(LoginPage loginPage) {
        Assertions.assertFalse(loginPage.isLoginButtonEnabled(),
                "Кнопка «Войти» должна быть визуально disabled (через CSS-класс), но выглядит активной");
    }

    public static void assertLoginButtonEnabled(LoginPage loginPage) {
        Assertions.assertTrue(loginPage.isLoginButtonEnabled(),
                "Кнопка «Войти» должна быть активна (валидный ввод), но выглядит disabled");
    }
}
