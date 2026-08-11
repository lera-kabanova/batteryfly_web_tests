package org.example.navigation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Минимальный Page Object карточки станции, нужный ИСКЛЮЧИТЕЛЬНО модулю Navigation
 * (проверка deep-link и кнопки "назад"). Полноценный Page Object станции принадлежит
 * отдельному модулю Station Detail & Connector Selection (qa-discovery/pages/station-detail.md)
 * и сознательно не реализуется здесь.
 * <p>
 * Локатор кнопки "назад" подтверждён живой проверкой 2026-07-16
 * (tools/playwright-codegen/explore-station-back-button.js): {@code div.back-pqtf0}.
 */
public class StationDetailPage {

    private static final By BACK_BUTTON = By.cssSelector("div.back-pqtf0");
    private static final By CONNECTOR_HEADING = By.xpath("//*[text()='Выберите коннектор']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public StationDetailPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(CONNECTOR_HEADING)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickBack() {
        wait.until(ExpectedConditions.elementToBeClickable(BACK_BUTTON)).click();
    }
}
