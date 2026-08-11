package org.example.navigation;

import org.example.auth.AuthTestConfig;
import org.example.navigation.pages.StationDetailPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Deep-linking на карточку станции и навигация "назад". Источник: qa-discovery/test-modules.md,
 * модуль 5; локаторы подтверждены qa-discovery/charging-flows.md (способ №4) и живой проверкой
 * 2026-07-16 (tools/playwright-codegen/explore-station-back-button.js).
 */
class NavigationDeepLinkTest extends NavigationTestBase {

    @Test
    @DisplayName("NAV-DEEPLINK-01: deep link на станцию открывается напрямую в авторизованной сессии")
    void stationDeepLink_opensDirectlyWhenAuthenticated() {
        loginAsValidUser();

        driver.get(AuthTestConfig.BASE_URL + NavigationTestConfig.STATION_DEEP_LINK_PATH);

        StationDetailPage station = new StationDetailPage(driver, wait);
        Assertions.assertTrue(station.isLoaded(), "Карточка станции не открылась по deep link");
    }

    @Test
    @DisplayName("NAV-BACK-01: кнопка «назад» на карточке станции возвращает на главный экран")
    void backButton_onStationDetail_returnsToHome() {
        loginAsValidUser();
        driver.get(AuthTestConfig.BASE_URL + NavigationTestConfig.STATION_DEEP_LINK_PATH);

        StationDetailPage station = new StationDetailPage(driver, wait);
        Assertions.assertTrue(station.isLoaded());

        station.clickBack();

        wait.until(d -> d.getCurrentUrl().equals(AuthTestConfig.BASE_URL));
        Assertions.assertEquals(AuthTestConfig.BASE_URL, driver.getCurrentUrl());
    }
}
