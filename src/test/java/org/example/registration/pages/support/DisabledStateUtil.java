package org.example.registration.pages.support;

import org.openqa.selenium.WebElement;

/**
 * На batteryfly.io "disabled"-состояние кнопок реализовано через CSS-класс
 * (например {@code disabled-SIwIG}), а НЕ через нативный HTML {@code disabled} атрибут —
 * см. qa-discovery/observations.md. Дублирует {@code org.example.auth.pages.support.DisabledStateUtil}
 * намеренно — модули независимы друг от друга.
 */
public final class DisabledStateUtil {

    private DisabledStateUtil() {
    }

    public static boolean isVisuallyDisabled(WebElement element) {
        String classAttribute = element.getAttribute("class");
        return classAttribute != null && classAttribute.toLowerCase().contains("disabled");
    }
}
