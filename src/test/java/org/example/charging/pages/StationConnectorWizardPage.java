package org.example.charging.pages;

import org.example.charging.pages.support.DisabledStateUtil;
import org.example.charging.pages.support.VolumePaymentCarouselComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Карточка станции + выбор коннектора + карусель объёма зарядки, вплоть до кнопки "Далее"
 * (НЕ включая экран оплаты — см. {@link ChargingConfirmationPage}). Минимальный Page Object,
 * нужный ИСКЛЮЧИТЕЛЬНО модулю Charging (qa-discovery/test-modules.md, модуль 8). Полноценный
 * Page Object станции (избранное, маршрут, график загруженности) принадлежит отдельному модулю
 * Station Detail & Connector Selection (модуль 7) и сознательно не реализуется здесь.
 * <p>
 * Подтверждено живой проверкой 2026-07-16 (explore-charging-wizard-safe.js): сразу после выбора
 * коннектора карусель объёма уже показывает "Полный бак" как активную карточку по умолчанию
 * (scaleY(1)), и кнопка "Далее" сразу enabled.
 * <p>
 * Есть отдельная (вторая) карусель "Способ оплаты" на том же экране, с картами "Мой баланс" /
 * "•••• 0000" (карта/корп. счёт) / "Бонусный счёт PowerBank" — по умолчанию активен "Мой баланс".
 * Обе карусели переиспользуют один и тот же хэшированный CSS-класс, см. предупреждение в
 * {@link VolumePaymentCarouselComponent} — {@link #carousel()} корректно работает для карточек
 * ЛЮБОЙ из двух каруселей, если вызывать {@code carousel().selectByTestId(...)} с data-testid
 * нужной карточки (объёма ИЛИ оплаты, см. {@link org.example.charging.ChargingTestConfig}).
 */
public class StationConnectorWizardPage {

    private static final By NEXT_BUTTON = By.xpath("//button[text()='Далее']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final VolumePaymentCarouselComponent carousel;

    public StationConnectorWizardPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.carousel = new VolumePaymentCarouselComponent(driver, wait);
    }

    public StationConnectorWizardPage openStation(String baseUrl, String stationPath) {
        driver.get(baseUrl + stationPath);
        return this;
    }

    /**
     * Клик через JS, а не нативным Selenium-кликом по координатам — см. подробное объяснение в
     * {@link ChargingConfirmationPage#clickPayAndCharge()} (инцидент 2026-07-16 с меню "Мое авто").
     * Тот же класс бага воспроизводится и здесь: переход от карточки станции к карусели
     * объёма/оплаты идёт с анимацией, и координатный клик может физически попасть на бейдж
     * "Мой авто", который в этот момент оказывается в точке клика.
     */
    public StationConnectorWizardPage selectConnector(String connectorTextFragment) {
        By connectorLocator = By.xpath("//span[contains(text(), '" + connectorTextFragment + "')]");
        WebElement connector = wait.until(ExpectedConditions.elementToBeClickable(connectorLocator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", connector);
        wait.until(ExpectedConditions.visibilityOfElementLocated(NEXT_BUTTON));
        return this;
    }

    public VolumePaymentCarouselComponent carousel() {
        return carousel;
    }

    public boolean isNextButtonEnabled() {
        return !DisabledStateUtil.isVisuallyDisabled(driver.findElement(NEXT_BUTTON));
    }

    /**
     * Переход на экран подтверждения оплаты — ещё БЕЗ трат денег. Клик через JS по той же причине,
     * что и {@link #selectConnector(String)} — переход на экран подтверждения тоже анимированный.
     */
    public ChargingConfirmationPage clickNext() {
        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(NEXT_BUTTON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
        return new ChargingConfirmationPage(driver, wait);
    }

    /**
     * То же самое "Далее", но когда выбрана карточка "Забронировать"/"Стать в очередь"
     * ({@code VOLUME_CARD_TESTID_RESERVE}) — ведёт на ОТДЕЛЬНЫЙ экран {@link BookingConfirmationPage}
     * ("Бронирование" / "Активировать"), а не на {@link ChargingConfirmationPage}. Подтверждено
     * живой проверкой tools/playwright-codegen/explore-booking-happy-path.js, 2026-07-24.
     * <p>
     * ИНЦИДЕНТ 2026-07-24 (найдена и исправлена НАСТОЯЩАЯ причина): "Далее" вело на ЭКРАН ОБЫЧНОЙ
     * ЗАРЯДКИ вместо брони не из-за проблем с самим кликом, а потому что
     * {@code VolumePaymentCarouselComponent.selectByTestId(RESERVE)} молча не успевал докрутить
     * карусель до последней (4-й) карточки и кликал по неактивной - реально оставался выбран
     * "Полный бак". Ранее добавленный сюда retry-клик по "Далее" САМ стал источником нового бага
     * (повторный клик уже на экране подтверждения, где "Далее" не существует) и убран - при
     * правильно выбранной карточке (см. фикс в VolumePaymentCarouselComponent) обычного
     * одиночного клика достаточно.
     */
    public BookingConfirmationPage clickNextForBooking() {
        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(NEXT_BUTTON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextButton);
        return new BookingConfirmationPage(driver, wait);
    }
}
