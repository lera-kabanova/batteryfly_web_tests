package org.example.navigation;

import org.example.auth.AuthTestConfig;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.navigation.assertions.NavigationAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Навигация через аватар/приветствие и сохранение сессии на вложенных маршрутах.
 * Источник: qa-discovery/test-modules.md, модуль 5.
 */
class NavigationAvatarAndSessionTest extends NavigationTestBase {

    @Test
    @DisplayName("NAV-AVATAR-01: клик по приветствию/аватару ведёт на /profile")
    void clickingWelcomeArea_navigatesToProfile() {
        AuthenticatedAreaPage home = loginAsValidUser();
        Assertions.assertTrue(home.isWelcomeVisible(), "Приветствие не появилось после входа");

        home.clickWelcomeArea();

        wait.until(d -> d.getCurrentUrl().contains("/profile"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("/profile"),
                "Ожидался переход на /profile после клика по приветствию, URL: " + driver.getCurrentUrl());
    }

    @Test
    @DisplayName("NAV-SESSION-01: сессия сохраняется при обновлении страницы (F5) на вложенном маршруте")
    void session_persistsAfterRefresh_onNestedRoute() {
        loginAsValidUser();
        driver.get(AuthTestConfig.BASE_URL + "history");
        NavigationAssertions.assertStaysOnRoute(driver, wait, "history");

        driver.navigate().refresh();

        NavigationAssertions.assertStaysOnRoute(driver, wait, "history");
    }
}
