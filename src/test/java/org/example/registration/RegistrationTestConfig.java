package org.example.registration;

public final class RegistrationTestConfig {

    private RegistrationTestConfig() {
    }

    public static final String BASE_URL = "https://batteryfly.io/";

    public static final String EXISTING_EMAIL = "lerakab5@gmail.com";

    public static final String WEAK_PASSWORD = "123";
    public static final String VALID_PASSWORD_FORMAT = "Test123!";
    public static final String INVALID_EMAIL_FORMAT = "not-an-email";

    public static final String INVALID_NAME_WITH_SCRIPT = "<script>alert(1)</script>";
    public static final String INCOMPLETE_PHONE = "+37529";
    public static final String VALID_TEST_PHONE = "+375291112233";

    public static final String EXPECTED_EMAIL_INVALID_HINT = "Укажите валидный email";
    public static final String EXPECTED_PASSWORD_PATTERN_HINT = "Длина 8 символов, цифры и латиница в разных регистрах";
    public static final String EXPECTED_NAME_PATTERN_HINT = "Укажите корректное имя";
    public static final String EXPECTED_PHONE_PATTERN_HINT = "Укажите корректный телефон";
    public static final String EXPECTED_EMAIL_EXISTS_ERROR_SUBSTRING = "Email уже существует";

    public static final String PUBLIC_OFFER_URL = "https://batteryfly.by/files/public_offer.pdf";
    public static final String PRIVACY_POLICY_URL = "https://batteryfly.by/files/personal_data.pdf";
}
