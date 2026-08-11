package org.example.auth.pages.support;

import org.openqa.selenium.WebElement;

/**
 * На batteryfly.io "disabled"-состояние кнопок реализовано через CSS-класс
 * (например {@code disabled-SIwIG}), а НЕ через нативный HTML {@code disabled} атрибут —
 * см. qa-discovery/observations.md. Обычная проверка {@code WebElement#isEnabled()} этого
 * не отражает, поэтому здесь используется явная проверка класса.
 */
public final class DisabledStateUtil {

    private DisabledStateUtil() {
    }

    public static boolean isVisuallyDisabled(WebElement element) {
        String classAttribute = element.getAttribute("class");
        return classAttribute != null && classAttribute.toLowerCase().contains("disabled");
    }
}
