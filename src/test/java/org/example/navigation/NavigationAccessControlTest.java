package org.example.navigation;

import org.example.auth.AuthTestConfig;
import org.example.navigation.assertions.NavigationAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Прямой заход на защищённые маршруты без сессии -> редирект на форму входа.
 * Источник: qa-discovery/test-modules.md, модуль 5 (Navigation & Access Control).
 */
class NavigationAccessControlTest extends NavigationTestBase {

    static Stream<String> protectedRoutes() {
        return NavigationTestConfig.PROTECTED_ROUTES.stream();
    }

    @ParameterizedTest(name = "NAV-GUARD /{0}: без сессии -> редирект на вход")
    @MethodSource("protectedRoutes")
    @DisplayName("NAV-GUARD: защищённый маршрут без сессии редиректит на вход")
    void protectedRoute_withoutSession_redirectsToLogin(String route) {
        driver.get(AuthTestConfig.BASE_URL + route);

        NavigationAssertions.assertRedirectedToLogin(driver, wait);
    }
}
