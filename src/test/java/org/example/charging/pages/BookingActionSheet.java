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

    public void clickBuildRoute() {
        clickActionByText("Проложить маршрут");
    }

    public void clickOpenBarrier() {
        clickActionByText("Открыть шлагбаум");
    }

    /**
     * ВНИМАНИЕ: реально запускает зарядную сессию по уже активной брони (аналог
     * {@link ChargingConfirmationPage#clickPayAndCharge()}, но без экрана подтверждения — бронь
     * уже подтверждена ранее). Существование и видимость кнопки подтверждены живой проверкой;
     * сам переход на /charge — НЕТ, проверить перед первым реальным запуском (сценарий 15).
     */
    public ChargingSessionPage clickStartCharging() {
        clickActionByText("Начать заправку");
        return new ChargingSessionPage(driver, wait);
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
        wait.until(ExpectedConditions.invisibilityOfElementLocated(BANNER));
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
