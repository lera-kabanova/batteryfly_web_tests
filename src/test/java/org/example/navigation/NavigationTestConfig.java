package org.example.navigation;

import java.util.List;

/**
 * Конфигурация тестового окружения модуля Navigation & Access Control
 * (qa-discovery/test-modules.md, модуль 5). Модуль явно зависит от Authentication
 * (qa-discovery/automation-roadmap.md, Phase 4 зависит от Phase 2) — учётные данные и base URL
 * переиспользуются напрямую из {@code org.example.auth.AuthTestConfig}, а не дублируются.
 */
public final class NavigationTestConfig {

    private NavigationTestConfig() {
    }

    /** Все защищённые маршруты consumer-приложения (qa-discovery/site-map.md). */
    public static final List<String> PROTECTED_ROUTES = List.of(
            "profile", "settings", "history", "cars", "cards",
            "chat", "assistant", "scanner", "request", "notification"
    );

    /** Станция-эмулятор, безопасная для навигационных тестов (qa-discovery/observations.md). */
    public static final String STATION_DEEP_LINK_PATH = "station/49";
}
