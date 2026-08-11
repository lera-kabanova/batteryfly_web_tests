package org.example.charging.pages.support;

import org.openqa.selenium.WebElement;

/**
 * На batteryfly.io "disabled"-состояние кнопок реализовано через CSS-класс, а НЕ через
 * нативный HTML {@code disabled} атрибут — см. qa-discovery/observations.md. Дублирует
 * аналогичные утилиты других модулей намеренно — модули независимы друг от друга.
 */
public final class DisabledStateUtil {

    private DisabledStateUtil() {
    }

    public static boolean isVisuallyDisabled(WebElement element) {
        String classAttribute = element.getAttribute("class");
        return classAttribute != null && classAttribute.toLowerCase().contains("disabled");
    }
}
