package org.example.navigation.assertions;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Доменные проверки модуля Navigation & Access Control, вынесенные из тел тестов.
 */
public final class NavigationAssertions {

    private static final String KEYCLOAK_AUTH_PATH_MARKER = "/auth/realms/";

    private NavigationAssertions() {
    }

    public static void assertRedirectedToLogin(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(d -> d.getCurrentUrl().contains(KEYCLOAK_AUTH_PATH_MARKER));
        } catch (TimeoutException e) {
            Assertions.fail("Ожидался редирект на форму входа, но URL так и не изменился: " + driver.getCurrentUrl());
        }
    }

    public static void assertStaysOnRoute(WebDriver driver, WebDriverWait wait, String routePath) {
        try {
            wait.until(d -> d.getCurrentUrl().contains(routePath)
                    && !d.getCurrentUrl().contains(KEYCLOAK_AUTH_PATH_MARKER));
        } catch (TimeoutException e) {
            Assertions.fail("Ожидалось остаться на маршруте '" + routePath + "', но URL: " + driver.getCurrentUrl());
        }
    }
}
