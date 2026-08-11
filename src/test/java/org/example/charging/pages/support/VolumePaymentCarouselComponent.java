package org.example.charging.pages.support;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Карусель выбора объёма зарядки ИЛИ способа оплаты. Порт проверенной боем логики
 * {@code org.example.ChargingTest.swipeIfNeededUntilVisible} — карусель эмулирует нативные
 * touch-жесты через {@code JavascriptExecutor}, обычный {@code scrollIntoView()}/wheel() здесь
 * не работает. Активная карточка определяется через inline {@code style="...scaleY(1)..."}.
 * <p>
 * Карточки находятся по {@code data-testid} (например {@code charge-volume-card-80-percent},
 * {@code payment-method-card}) — подтверждено живой проверкой
 * tools/playwright-codegen/explore-full-testid-dump.js, 2026-07-22. Это ЕДИНСТВЕННОЕ место на
 * сайте, где встречены data-testid (см. qa-discovery/locators.md — в остальном на сайте их нет
 * вообще), и оно даёт СТРОГО ОДНОЗНАЧНЫЙ элемент на карточку, в отличие от поиска по видимому
 * тексту (у нескольких вложенных узлов может быть один и тот же {@code textContent}).
 * <p>
 * Структура вокруг карточки (подтверждено той же проверкой) везде одинаковая и фиксированная:
 * {@code [data-testid=...]} → родитель ("item-zplpI", несёт {@code transform: scaleY(...)},
 * определяющий активность карточки) → дед ("container-zHUzO", сам свайпаемый контейнер карусели).
 * Так как на экране ДВЕ разные карусели с одинаковым классом container-zHUzO (объём и оплата),
 * важно брать контейнер именно СВОЕЙ карточки (через .parentElement.parentElement от неё), а не
 * первый попавшийся на странице с этим классом.
 */
public class VolumePaymentCarouselComponent {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public VolumePaymentCarouselComponent(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isCardActive(WebElement card) {
        String style = card.getAttribute("style");
        return style != null && style.contains("scaleY(1)");
    }

    /** Находит карточку по data-testid, свайпает её собственную карусель при необходимости, затем кликает. */
    public void selectByTestId(String testId) {
        WebElement card = driver.findElement(By.cssSelector("[data-testid='" + testId + "']"));
        swipeUntilActiveThenClick(card);
    }

    /**
     * ИНЦИДЕНТ 2026-07-24: если карточка так и не становится активной (scaleY(1)) после всех
     * попыток свайпа, метод раньше МОЛЧА кликал по ней всё равно - а клик по НЕактивной карточке
     * ничего не выбирает (см. класс-javadoc), из-за чего реально оставался выбран ПРЕДЫДУЩИЙ
     * активный режим (например "Полный бак" вместо "Забронировать" - карточка брони последняя,
     * 4-я, дальше всех от дефолтного "Полный бак", и 6 попыток свайпа иногда не хватало её
     * докрутить). Тест при этом не падал сразу - падал заметно позже и непонятно на экране
     * "Начните зарядку"/поиске кнопки "Далее" уже в неправильном контексте. Теперь: попыток
     * больше, и если карточка ВСЁ РАВНО не активна после финального клика - бросаем понятную
     * ошибку сразу, а не продолжаем с неверно выбранной карточкой.
     */
    private void swipeUntilActiveThenClick(WebElement targetElement) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement cardContainer = targetElement.findElement(By.xpath(".."));
        WebElement container = cardContainer.findElement(By.xpath(".."));

        String style = cardContainer.getAttribute("style");
        if (style != null && style.contains("scaleY(1)")) {
            js.executeScript("arguments[0].click();", targetElement);
            return;
        }

        int maxAttempts = 12;
        int attempts = 0;
        while (attempts < maxAttempts) {
            style = cardContainer.getAttribute("style");
            if (style != null && style.contains("scaleY(1)")) {
                break;
            }
            js.executeScript(
                    "var el = arguments[0];"
                            + "var rect = el.getBoundingClientRect();"
                            + "var startX = rect.left + (rect.width * 0.6);"
                            + "var endX = rect.left + (rect.width * 0.53);"
                            + "var centerY = rect.top + (rect.height / 2);"
                            + "var tStart = new Touch({ identifier: Date.now(), target: el, clientX: startX, clientY: centerY });"
                            + "el.dispatchEvent(new TouchEvent('touchstart', { touches: [tStart], targetTouches: [tStart], changedTouches: [tStart], bubbles: true, cancelable: true }));"
                            + "var tMove = new Touch({ identifier: Date.now(), target: el, clientX: endX, clientY: centerY });"
                            + "el.dispatchEvent(new TouchEvent('touchmove', { touches: [tMove], targetTouches: [tMove], changedTouches: [tMove], bubbles: true, cancelable: true }));"
                            + "el.dispatchEvent(new TouchEvent('touchend', { touches: [], targetTouches: [], changedTouches: [tMove], bubbles: true, cancelable: true }));",
                    container
            );
            sleep(1300);
            attempts++;
        }

        style = cardContainer.getAttribute("style");
        if (style == null || !style.contains("scaleY(1)")) {
            throw new IllegalStateException("Карточка карусели так и не стала активной после "
                    + maxAttempts + " попыток свайпа (style=" + style + ") - клик по неактивной "
                    + "карточке ничего не выбрал бы, оставив выбранным предыдущий активный режим.");
        }
        js.executeScript("arguments[0].click();", targetElement);
        sleep(500);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
