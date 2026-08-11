package org.example.charging.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Панель деталей транзакции ("Информация о заправке"), открывается кликом по карточке записи в
 * {@link HistoryTransactionsPage} — встраивается в ту же страницу /history, НЕ отдельный экран/URL.
 * Показывает график "Мощность - SOC" (рендерится через {@code <svg>}, НЕ {@code <canvas>}) и кнопку
 * "PDF" для скачивания чека. Подтверждено живой проверкой
 * tools/playwright-codegen/explore-history-detail-chart-pdf.js, 2026-07-24.
 */
public class TransactionDetailPanel {

    private static final By PANEL_TITLE = By.xpath("//*[text()='Информация о заправке']");
    private static final By PDF_BUTTON = By.xpath("//button[normalize-space()='PDF']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public TransactionDetailPanel(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(PANEL_TITLE)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** График сессии рендерится через SVG, а не canvas — подтверждено живой проверкой (11 svg-узлов). */
    public boolean isChartVisible() {
        return !driver.findElements(By.tagName("svg")).isEmpty();
    }

    public void clickPdfButton() {
        wait.until(ExpectedConditions.elementToBeClickable(PDF_BUTTON)).click();
    }
}
