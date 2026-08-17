package org.example.auth;

// Конфигурация тестового окружения модуля Auth для batteryfly.io.

public final class AuthTestConfig {

    private AuthTestConfig() {
    }

    public static final String BASE_URL = "https://batteryfly.io/";

    public static final String VALID_EMAIL = "1227509@gmail.com";
    public static final String VALID_PASSWORD = "Lera123!";

    public static final String EMAIL_NULL_BALANCE = "1227320@mtp.by";

    public static final String USER_EMAIL_CINEMA = "cinemawebwelcome@gmail.com";
    public static final String SECOND_USER_PASSWORD = "Lera123!";

    public static final String LERAKAB5_EMAIL = "lerakab5@gmail.com";
    public static final String LERAKAB5_PASSWORD = "Lera123!";

    public static final String UNREGISTERED_EMAIL = "unregistered_test_user_9999@gmail.com";
    public static final String INVALID_EMAIL_FORMAT = "not-an-email";

    // Единый текст ошибки для неверного пароля/незарегистрированного email
    public static final String EXPECTED_INVALID_CREDENTIALS_ERROR =
            "Неверный логин/пароль. Если вы регистрировались ранее, воспользуйтесь восстановлением пароля";

    // Защищённый маршрут для проверки редиректа неавторизованного доступа
    public static final String PROTECTED_ROUTE = BASE_URL + "profile";

    public static final String SQL_INJECTION_PAYLOAD = "' OR '1'='1";
    public static final String XSS_PAYLOAD = "<script>alert('xss')</script>";
}
