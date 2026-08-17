package org.example.navigation;

import java.util.List;

public final class NavigationTestConfig {

    private NavigationTestConfig() {
    }

    public static final List<String> PROTECTED_ROUTES = List.of(
            "profile", "settings", "history", "cars", "cards",
            "chat", "assistant", "scanner", "request", "notification"
    );
    Ы
    public static final String STATION_DEEP_LINK_PATH = "station/49";
}
