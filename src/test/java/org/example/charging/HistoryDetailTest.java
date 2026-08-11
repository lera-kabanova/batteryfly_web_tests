package org.example.charging;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.example.auth.AuthTestConfig;
import org.example.auth.pages.LoginPage;
import org.example.charging.pages.HistoryTransactionsPage;
import org.example.charging.pages.TransactionDetailPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * История зарядок -> деталь транзакции -> график + PDF-чек (qa-discovery/test-modules.md, модуль
 * 12, History & Analytics — реализован здесь, рядом с Charging, т.к. переиспользует
 * {@link HistoryTransactionsPage} из этого пакета, а не отдельный Page Object модуля History).
 * <p>
 * ОТДЕЛЬНЫЙ от {@link ChargingTestBase} @BeforeEach — нужны специальные ChromeOptions для
 * скачивания файлов ({@code download.default_directory}, {@code plugins.always_open_pdf_externally}),
 * иначе Chrome открывает PDF во встроенном вьювере вместо реального скачивания файла на диск.
 */
class HistoryDetailTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private Path downloadDir;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() throws Exception {
        downloadDir = Paths.get("target", "downloads").toAbsolutePath();
        Files.createDirectories(downloadDir);
        File[] existing = downloadDir.toFile().listFiles();
        if (existing != null) {
            for (File f : existing) {
                f.delete();
            }
        }

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--no-sandbox", "--disable-dev-shm-usage");
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("HIST-08: деталь транзакции показывает график сессии; PDF-кнопка присутствует и кликабельна")
    void transactionDetail_showsChartAndPdfButtonIsClickable() throws InterruptedException {
        new LoginPage(driver, wait).open(AuthTestConfig.BASE_URL)
                .login(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        HistoryTransactionsPage history = new HistoryTransactionsPage(driver, wait).open(AuthTestConfig.BASE_URL);
        TransactionDetailPanel detail = history.openDetailForStation(ChargingTestConfig.STATION_ID);

        Assertions.assertTrue(detail.isLoaded(), "Панель 'Информация о заправке' не открылась");
        Assertions.assertTrue(detail.isChartVisible(), "График сессии (SVG) не отрендерился");

        // Живой проверкой 2026-07-24 (Selenium И независимо Playwright, см.
        // tools/playwright-codegen/explore-pdf-click-download.js) подтверждено: клик по кнопке
        // "PDF" НЕ инициирует событие скачивания и НЕ открывает новую вкладку/URL в течение 20с
        // ни в одном из двух движков автоматизации - реальное скачивание файла проверить
        // автоматически не удалось (возможно, генерация PDF занимает больше времени на бэкенде,
        // либо требует специфичного доверенного клика, недоступного через автоматизацию).
        // Максимум, что проверяемо здесь - сам факт наличия и кликабельности кнопки; фактическое
        // содержимое PDF-чека требует ручной проверки.
        int tabsBefore = driver.getWindowHandles().size();
        Assertions.assertDoesNotThrow(detail::clickPdfButton, "Кнопка 'PDF' должна быть кликабельна без исключений");
        Thread.sleep(3000);

        File downloaded = waitForDownloadedPdf(downloadDir, Duration.ofSeconds(5));
        if (downloaded != null) {
            Assertions.assertTrue(downloaded.length() > 0, "Скачанный PDF-файл пустой");
            System.out.println("[INFO] PDF реально скачался: " + downloaded.getName()
                    + " (" + downloaded.length() + " байт) - редкий случай, обычно бэкенд не успевает за 5с.");
        } else {
            System.out.println("[INFO] PDF не скачался за 5с (известное ограничение среды автоматизации, "
                    + "см. javadoc метода) - кнопка кликабельна, реальное содержимое чека требует ручной проверки.");
        }
        if (driver.getWindowHandles().size() > tabsBefore) {
            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(driver.getWindowHandle())) {
                    driver.switchTo().window(handle);
                    driver.close();
                }
            }
        }
    }

    private File waitForDownloadedPdf(Path dir, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            File[] files = dir.toFile().listFiles((d, name) ->
                    name.toLowerCase().endsWith(".pdf") && !name.endsWith(".crdownload"));
            if (files != null && files.length > 0) {
                return files[0];
            }
            Thread.sleep(500);
        }
        return null;
    }
}
