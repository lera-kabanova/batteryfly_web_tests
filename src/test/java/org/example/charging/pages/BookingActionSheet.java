package org.example.charging.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Плашка активной брони/очереди на главном экране ("HH:MM Забронировано" + шеврон) и раскрываемое
 * из неё меню действий ("Действие": Начать заправку / Открыть шлагбаум / Проложить маршрут /
 * Отменить). Клик "Отменить" превращает ТОТ ЖЕ модал в диалог подтверждения ("Ваше бронирование
 * будет отменено." + "Желаете отменить бронирование?" + кнопки "Отменить бронирование"/
 * "Сохранить бронь") — это второе состояние одного и того же bottom sheet, а не отдельный экран.
 * Подтверждено живой проверкой {@code BookingExplorationTest}, 2026-07-24 (аккаунт
 * cinemawebwelcome@gmail.com, станция #49). См. также {@code ChargingTestConfig.BOOKING_*}.
 */
public class BookingActionSheet {

    private static final By BANNER = By.xpath("//*[contains(text(),'Забронировано')]");
    private static final Pattern COUNTDOWN_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");

    /**
     * Плашка позиции в очереди ("В очереди N машина/машины") — ОТДЕЛЬНОЕ состояние от {@link #BANNER}
     * ("Забронировано"), с другим текстом. Подтверждено живой проверкой 2026-08-18
     * (QueueBannerDiagnosticTest, аккаунт cinemawebwelcome, станция #49): пока пользователь ждёт в
     * очереди (ещё не продвинулся), на главном экране показывается именно "В очереди N машина", а
     * не "Забронировано" — плашка переходит в "Забронировано" только ПОСЛЕ продвижения по очереди.
     */
    private static final By QUEUE_BANNER = By.xpath("//*[contains(text(),'В очереди')]");
    private static final Pattern QUEUE_POSITION_PATTERN = Pattern.compile("В очереди\\s+(\\d+)");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public BookingActionSheet(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isBannerVisible() {
        return !driver.findElements(BANNER).isEmpty();
    }

    /**
     * Ждёт появления плашки на главном экране — активация брони не отражается на /  мгновенно
     * (гонка подтверждена живым прогоном BookingTest, 2026-07-24: без ожидания плашка иногда ещё
     * не успевала отрисоваться сразу после {@code driver.get(BASE_URL)}).
     */
    public BookingActionSheet waitForBannerVisible() {
        wait.until(ExpectedConditions.presenceOfElementLocated(BANNER));
        return this;
    }

    public boolean isQueueBannerVisible() {
        return !driver.findElements(QUEUE_BANNER).isEmpty();
    }

    /** Ждёт появления плашки очереди ("В очереди N машина") на главном экране. */
    public BookingActionSheet waitForQueueBannerVisible() {
        wait.until(ExpectedConditions.presenceOfElementLocated(QUEUE_BANNER));
        return this;
    }

    /** Разбирает число позиции из плашки "В очереди N машина". */
    public int readQueuePosition() {
        String bannerAreaText = driver.findElement(QUEUE_BANNER).findElement(By.xpath("..")).getText();
        Matcher matcher = QUEUE_POSITION_PATTERN.matcher(bannerAreaText);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Не удалось найти номер позиции в плашке очереди: " + bannerAreaText);
        }
        return Integer.parseInt(matcher.group(1));
    }

    /** Разбирает "HH:MM" рядом с плашкой брони в общее число секунд обратного отсчёта. */
    public int readCountdownSeconds() {
        String bannerAreaText = driver.findElement(BANNER).findElement(By.xpath("..")).getText();
        Matcher matcher = COUNTDOWN_PATTERN.matcher(bannerAreaText);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Не удалось найти обратный отсчёт HH:MM рядом с плашкой брони: " + bannerAreaText);
        }
        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        return minutes * 60 + seconds;
    }

    /** Раскрывает плашку в меню "Действие" (Начать заправку / Открыть шлагбаум / Проложить маршрут / Отменить). */
    public BookingActionSheet expand() {
        wait.until(ExpectedConditions.elementToBeClickable(BANNER)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='Действие']")));
        return this;
    }

    /** То же самое, но для плашки очереди ("В очереди N машина") - до продвижения по очереди. */
    public BookingActionSheet expandQueueBanner() {
        wait.until(ExpectedConditions.elementToBeClickable(QUEUE_BANNER)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='Действие']")));
        return this;
    }

    public void clickBuildRoute() {
        clickActionByText("Проложить маршрут");
    }

    public void clickOpenBarrier() {
        clickActionByText("Открыть шлагбаум");
    }

    /**
     * ИНЦИДЕНТ 2026-08-18: живой проверкой подтверждено, что "Начать заправку" НЕ пропускает визард
     * и НЕ ведёт сразу на /charge (как предполагалось раньше) - переводит на ТОТ ЖЕ экран выбора
     * объёма/оплаты ("Полный бак" / "Мой баланс" / "Далее"), что и обычный вход через
     * {@code StationConnectorWizardPage.openStation()}. Бронь просто даёт право начать зарядку на
     * занятой станции, но условия (объём, оплата) всё равно нужно выбрать заново через визард.
     */
    public StationConnectorWizardPage clickStartCharging() {
        clickActionByText("Начать заправку");
        return new StationConnectorWizardPage(driver, wait);
    }

    /** Открывает подтверждение отмены брони (то же меню, второе состояние). */
    public BookingActionSheet clickCancel() {
        clickActionByText("Отменить");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[text()='Желаете отменить бронирование?']")));
        return this;
    }

    public String getSheetBodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    /**
     * "Отменить бронирование" — подтверждает отмену, освобождает станцию/коннектор. Ждёт, пока
     * плашка реально не исчезнет с главного экрана - клик по кнопке сам по себе не гарантирует,
     * что отмена применилась (без этого ожидания вызывающий код мог посчитать бронь отменённой,
     * хотя она оставалась активной - плашка это единственный надёжный признак фактической отмены).
     */
    public void confirmCancel() {
        clickActionByText("Отменить бронирование");
        wait.until(d -> d.findElements(BANNER).isEmpty() && d.findElements(QUEUE_BANNER).isEmpty());
    }

    /** "Сохранить бронь" — закрывает диалог без отмены, бронь остаётся активной. */
    public void keepBooking() {
        clickActionByText("Сохранить бронь");
    }

    private void clickActionByText(String text) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()='" + text + "']")));
        element.click();
    }
}
