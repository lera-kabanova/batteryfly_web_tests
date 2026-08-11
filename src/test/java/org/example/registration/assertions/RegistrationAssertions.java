package org.example.registration.assertions;

import org.example.registration.pages.RegistrationPage;
import org.junit.jupiter.api.Assertions;

/**
 * Доменные проверки модуля Registration, вынесенные из тел тестов — по аналогии с
 * {@code org.example.auth.assertions.AuthAssertions} / {@code org.example.business.assertions.UsersPageAssertions}.
 */
public final class RegistrationAssertions {

    private RegistrationAssertions() {
    }

    public static void assertSubmitDisabled(RegistrationPage page) {
        Assertions.assertFalse(page.isFinalSubmitEnabled(),
                "Кнопка «Продолжить регистрацию» должна быть disabled");
    }

    public static void assertSubmitEnabled(RegistrationPage page) {
        Assertions.assertTrue(page.isFinalSubmitEnabled(),
                "Кнопка «Продолжить регистрацию» должна быть enabled");
    }

    public static void assertHintPresent(RegistrationPage page, String expectedHint) {
        Assertions.assertTrue(page.getValidationHintTexts().contains(expectedHint),
                "Ожидалась подсказка валидации: '" + expectedHint + "', получено: " + page.getValidationHintTexts());
    }

    public static void assertGlobalErrorContains(RegistrationPage page, String expectedSubstring) {
        String actual = page.getGlobalErrorTextOrEmpty();
        Assertions.assertTrue(actual.contains(expectedSubstring),
                "Ожидалась ошибка, содержащая '" + expectedSubstring + "', получено: '" + actual + "'");
    }
}
