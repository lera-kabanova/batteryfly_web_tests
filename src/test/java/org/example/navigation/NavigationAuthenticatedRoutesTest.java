package org.example.navigation;

import org.example.auth.AuthTestConfig;
import org.example.navigation.assertions.NavigationAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Маршруты, обычно достигаемые через иконки шапки (чат/ассистент/"+"/уведомления) и меню
 * профиля, открываются напрямую по URL в авторизованной сессии без редиректа. Тестируется
 * через прямой переход по URL, а не клик по иконке — у иконок шапки нет стабильных локаторов
 * (qa-discovery/observations.md). Источник: qa-discovery/test-modules.md, модуль 5.
 */
class NavigationAuthenticatedRoutesTest extends NavigationTestBase {

    static Stream<String> routes() {
        return NavigationTestConfig.PROTECTED_ROUTES.stream();
    }

    @BeforeEach
    void loginFirst() {
        loginAsValidUser();
    }

    @ParameterizedTest(name = "NAV-ROUTE /{0}: доступен в авторизованной сессии")
    @MethodSource("routes")
    @DisplayName("NAV-ROUTE: маршрут доступен без редиректа при активной сессии")
    void authenticatedRoute_staysOnRoute(String route) {
        driver.get(AuthTestConfig.BASE_URL + route);

        NavigationAssertions.assertStaysOnRoute(driver, wait, route);
    }
}
