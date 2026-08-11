package org.example.charging.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * {@code /history} -> вкладка "Транзакции". Единственный экран модуля Charging БЕЗ единого
 * data-testid (подтверждено живой проверкой
 * tools/playwright-codegen/explore-history-record-structure.js, 2026-07-22).
 * <p>
 * Список отсортирован "По дате" (новые сверху). Каждая запись — фиксированный набор из 8
 * {@code <span>} в одном родителе, в этом порядке: адрес, "# {id}", "{сумма} BYN", "{kWh}",
 * "kW*h", "{минуты}м", "{дата}", "{время}".
 */
public class HistoryTransactionsPage {

    private static final By TRANSACTIONS_TAB = By.xpath("//*[text()='Транзакции']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public HistoryTransactionsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public HistoryTransactionsPage open(String baseUrl) {
        driver.get(baseUrl + "history");
        wait.until(ExpectedConditions.elementToBeClickable(TRANSACTIONS_TAB)).click();
        return this;
    }

    /** Показания одной записи-сессии из списка транзакций. */
    public record TransactionRecord(String address, double byn, double kWh, int durationMinutes,
                                     String date, String time) {
    }

    /** Самая свежая (первая в списке) запись для указанной станции. */
    public TransactionRecord latestRecordForStation(String stationId) {
        WebElement recordCard = findLatestRecordCard(stationId);
        List<WebElement> spans = recordCard.findElements(By.tagName("span"));

        return new TransactionRecord(
                spans.get(0).getText().trim(),
                parseDouble(spans.get(2).getText()),
                parseDouble(spans.get(3).getText()),
                parseInt(spans.get(5).getText()),
                spans.get(6).getText().trim(),
                spans.get(7).getText().trim()
        );
    }

    /**
     * Открывает панель деталей ("Информация о заправке") для самой свежей записи станции —
     * встраивается прямо в /history, НЕ отдельный URL. Подтверждено живой проверкой
     * tools/playwright-codegen/explore-history-detail-chart-pdf.js, 2026-07-24.
     */
    public TransactionDetailPanel openDetailForStation(String stationId) {
        WebElement recordCard = findLatestRecordCard(stationId);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", recordCard);
        js.executeScript("arguments[0].click();", recordCard);
        return new TransactionDetailPanel(driver, wait);
    }

    private WebElement findLatestRecordCard(String stationId) {
        By stationLocator = By.xpath("//span[text()='# " + stationId + "']");
        WebElement stationSpan = wait.until(ExpectedConditions.visibilityOfElementLocated(stationLocator));
        return stationSpan.findElement(By.xpath("../.."));
    }

    private double parseDouble(String text) {
        return Double.parseDouble(text.trim().replace(",", ".").replaceAll("[^0-9.]", ""));
    }

    private int parseInt(String text) {
        return Integer.parseInt(text.replaceAll("[^0-9]", ""));
    }
}
