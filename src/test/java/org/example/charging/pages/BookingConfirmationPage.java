package org.example.charging.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Экран подтверждения брони ("Бронирование" — адрес, режим "Бронирование", стоимость 0 BYN,
 * коннектор), появляющийся после выбора карточки "Забронировать"/"Стать в очередь"
 * ({@code charge-volume-card-reserve}) и клика "Далее". ОТДЕЛЬНЫЙ экран от
 * {@link ChargingConfirmationPage} — не содержит кнопку "Оплатить и зарядить", вместо неё
 * "Активировать". Подтверждено живой проверкой tools/playwright-codegen/explore-booking-happy-path.js
 * и {@code BookingExplorationTest}, 2026-07-24.
 */
public class BookingConfirmationPage {

    private static final By ACTIVATE_BUTTON =
            By.xpath("//button[contains(text(),'Активировать')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public BookingConfirmationPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(ACTIVATE_BUTTON)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPageBodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    /**
     * Реально активирует бронь/постановку в очередь (бесплатно, но занимает станцию на 15 минут
     * реального времени на боевом сайте). Клик через JS по той же причине, что и
     * {@link ChargingConfirmationPage#clickPayAndCharge()} (анимированный переход экрана).
     */
    public void clickActivate() {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(ACTIVATE_BUTTON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
    }
}
