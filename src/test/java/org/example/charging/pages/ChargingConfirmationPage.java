package org.example.charging.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Экран подтверждения перед оплатой ("Начните зарядку" — адрес, режим заправки, тариф,
 * способ оплаты), появляющийся после клика "Далее". Подтверждено живой проверкой 2026-07-16
 * (explore-charging-next-step.js).
 * <p>
 * ГРАНИЦА МОДУЛЯ: кнопка "Оплатить и зарядить" здесь — это точка, за которой начинается
 * реальное списание денег и старт настоящей зарядной сессии. {@link #clickPayAndCharge()}
 * используется ТОЛЬКО в {@code ChargingSessionTest}, который сознательно не запускается
 * автоматически при реализации этого модуля.
 */
public class ChargingConfirmationPage {

    private static final By PAY_AND_CHARGE_BUTTON = By.xpath("//button[text()='Оплатить и зарядить']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public ChargingConfirmationPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(PAY_AND_CHARGE_BUTTON)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Полный текст экрана подтверждения — используется для безопасной проверки выбранного режима. */
    public String getPageBodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    /**
     * Читает значение строки экрана подтверждения по её лейблу (например "kW*h", "BYN", "Режим
     * заправки"). Структура строки подтверждена живой проверкой 2026-08-17: лейбл лежит в своём
     * {@code <div>}, значение — сосед этого div (span сразу после него), НЕ его child.
     */
    public String getSummaryRowValue(String label) {
        By valueLocator = By.xpath("//*[text()='" + label + "']/../following-sibling::span");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(valueLocator)).getText();
    }

    /**
     * ВНИМАНИЕ: реально запускает зарядную сессию и списывает деньги с боевого баланса.
     * Использовать ТОЛЬКО в тестах, явно предназначенных для ручного запуска.
     * <p>
     * Клик выполняется через {@code element.click()} на стороне JS, а не нативным Selenium-кликом
     * по координатам, и не фиксированной паузой перед обычным кликом - таймаут-based обход
     * ненадёжен, так как длительность анимации перехода не гарантирована. Причина - реальный
     * инцидент 2026-07-16: сразу после этого клика на экране зарядки открывалось меню "Мое авто",
     * хотя код его не искал и не трогал по локатору. Экран "Начните зарядку" -> "/charge" меняется
     * с анимацией перехода (см. также touch-swipe карусели объёма), и обычный клик Selenium по
     * координатам центра кнопки мог физически попасть на элемент, оказавшийся в этой точке экрана
     * в момент клика. JS-клик вызывает click() напрямую на найденном DOM-узле независимо от того,
     * что сейчас нарисовано в этой точке экрана.
     * <p>
     * Переход на /charge НЕ мгновенный - кнопка показывает спиннер, пока идёт бэкенд-вызов
     * (авторизация платежа + старт сессии); подтверждено живой проверкой
     * tools/playwright-codegen/explore-charge-counters-structure.js (URL остаётся /station/49
     * ещё как минимум 2.5с после клика).
     * <p>
     * ВАЖНО ПРО БЕЗОПАСНОСТЬ ДЕНЕГ: этот метод НЕ ждёт смены URL сам - он должен успеть ВЕРНУТЬ
     * {@link ChargingSessionPage} до какого-либо ожидания, которое могло бы бросить исключение.
     * Инцидент 2026-07-22: раньше ожидание URL было прямо здесь, ДО return; когда клик по факту
     * запускал реальную сессию на сервере, но клиентская навигация просто была медленнее
     * таймаута, метод бросал TimeoutException, вызывающий тест никогда не получал ссылку на
     * {@link ChargingSessionPage} (переменная так и осталась {@code null}), и аварийная остановка
     * в {@code @AfterEach} не срабатывала - реальная сессия провисела активной без присмотра до
     * конца прогона (5 минут, 3.5 kWh, 2.28 BYN). Правильный порядок в вызывающем коде:
     * присвоить результат {@code activeSession}, и ТОЛЬКО ПОТОМ звать
     * {@link ChargingSessionPage#confirmChargeStarted()}, чтобы аварийная остановка была
     * гарантированно на связи ещё до любого ожидания, которое может упасть по таймауту.
     */
    public ChargingSessionPage clickPayAndCharge() {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(PAY_AND_CHARGE_BUTTON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        return new ChargingSessionPage(driver, wait);
    }
}
