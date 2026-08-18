package org.example.charging;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.example.auth.AuthTestConfig;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.example.charging.pages.BookingActionSheet;
import org.example.charging.pages.BookingConfirmationPage;
import org.example.charging.pages.ChargingConfirmationPage;
import org.example.charging.pages.ChargingSessionPage;
import org.example.charging.pages.StationConnectorWizardPage;
import org.example.charging.pages.support.BookingUnblockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class QueueTest {

    private WebDriver driverA;
    private WebDriver driverB;
    private WebDriverWait waitA;
    private WebDriverWait waitB;
    private ChargingSessionPage sessionA;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        driverA = newDriver();
        waitA = new WebDriverWait(driverA, Duration.ofSeconds(15));
        driverB = newDriver();
        waitB = new WebDriverWait(driverB, Duration.ofSeconds(15));
    }

    private WebDriver newDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu");
        options.addArguments("--remote-allow-origins=*", "--no-sandbox", "--disable-dev-shm-usage", "--incognito");
        options.addArguments("--window-size=1280,900");

        options.addArguments("--lang=ru-RU");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", "ru-RU,ru");
        options.setExperimentalOption("prefs", prefs);

        return new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        try {
            if (sessionA != null) {
                driverA.get(AuthTestConfig.BASE_URL + "charge");
                try {
                    waitA.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[text()='Остановить']")));
                } catch (Exception ignored) {
                }
                sessionA.emergencyStopIfActive();
            }
        } catch (Exception e) {
            System.out.println("[WARN] Cleanup A (charging) failed: " + e);
        }
        try {
            driverA.get(AuthTestConfig.BASE_URL);
            BookingActionSheet sheetA = new BookingActionSheet(driverA, waitA);
            if (sheetA.isBannerVisible()) {
                sheetA.expand().clickCancel().confirmCancel();
            }
        } catch (Exception e) {
            System.out.println("[WARN] Cleanup A (booking) failed: " + e
                    + " - ПРОВЕРЬТЕ ВРУЧНУЮ, СТАНЦИЯ #49 МОЖЕТ ОСТАВАТЬСЯ ЗАБРОНИРОВАНА ПОЛЬЗОВАТЕЛЕМ 1.");
        }
        try {
            driverB.get(AuthTestConfig.BASE_URL);
            BookingActionSheet sheetB = new BookingActionSheet(driverB, waitB);
            if (sheetB.isBannerVisible()) {
                sheetB.expand().clickCancel().confirmCancel();
            } else if (sheetB.isQueueBannerVisible()) {
                sheetB.expandQueueBanner().clickCancel().confirmCancel();
            }
        } catch (Exception e) {
            System.out.println("[WARN] Cleanup B (booking/queue) failed: " + e
                    + " - ПРОВЕРЬТЕ ВРУЧНУЮ, СТАНЦИЯ #49 МОЖЕТ ОСТАВАТЬСЯ ЗАНЯТА ПОЛЬЗОВАТЕЛЕМ 2.");
        }
        if (driverA != null) {
            driverA.quit();
        }
        if (driverB != null) {
            driverB.quit();
        }
    }

    private void loginAs(WebDriver driver, WebDriverWait wait, String email, String password) {
        LoginPage loginPage = new LoginPage(driver, wait).open(AuthTestConfig.BASE_URL);
        loginPage.login(email, password);
        AuthenticatedAreaPage area = new AuthenticatedAreaPage(driver, wait);
        area.isWelcomeVisible();
    }

    @Test
    @DisplayName("QUEUE-16: пользователь 1 бронирует и заряжается, пользователь 2 встаёт в очередь, продвижение после завершения")
    void queue_userChargingUserQueued_progressesAfterFirstFinishes() throws IOException {

        loginAs(driverB, waitB, AuthTestConfig.USER_EMAIL_CINEMA, AuthTestConfig.SECOND_USER_PASSWORD);
        BookingUnblockHelper.ensureAccountCanBook(driverB, waitB);

        loginAs(driverA, waitA, "lerakab5@gmail.com", "Lera123!");
        BookingConfirmationPage confirmationA = BookingUnblockHelper.reserveWithAutoUnblock(driverA, waitA);
        Assertions.assertTrue(confirmationA.isLoaded(), "Экран подтверждения брони пользователя 1 не загрузился");
        confirmationA.clickActivate();

        driverA.get(AuthTestConfig.BASE_URL);
        BookingActionSheet sheetA = new BookingActionSheet(driverA, waitA).waitForBannerVisible();
        Assertions.assertTrue(sheetA.isBannerVisible(),
                "У пользователя 1 должна появиться плашка брони с обратным отсчётом");
        int countdownA = sheetA.readCountdownSeconds();
        Assertions.assertTrue(countdownA > 0 && countdownA <= 15 * 60,
                "Обратный отсчёт брони пользователя 1 должен быть в пределах 15 минут, получено секунд: " + countdownA);
        screenshot(driverA, "queue-01-user-a-reserved");

        StationConnectorWizardPage wizardB = new StationConnectorWizardPage(driverB, waitB)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizardB.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        screenshot(driverB, "queue-02-wizard-b-busy");
        String wizardBBody = driverB.findElement(By.tagName("body")).getText();
        Assertions.assertTrue(wizardBBody.contains("Стать в очередь"),
                "Пользователь 2 должен увидеть предложение встать в очередь, получено: " + wizardBBody);

        BookingConfirmationPage queueConfirmation = wizardB.clickNextForBooking();
        Assertions.assertTrue(queueConfirmation.isLoaded(), "Экран подтверждения постановки в очередь не загрузился");
        String queueBody = queueConfirmation.getPageBodyText();
        Assertions.assertTrue(queueBody.contains("Постановка в очередь"),
                "Ожидался режим 'Постановка в очередь', получено: " + queueBody);
        queueConfirmation.clickActivate();

        driverB.get(AuthTestConfig.BASE_URL);
        BookingActionSheet queueSheetB = new BookingActionSheet(driverB, waitB).waitForQueueBannerVisible();
        Assertions.assertTrue(queueSheetB.isQueueBannerVisible(),
                "У пользователя 2 должна появиться плашка очереди с номером позиции ('В очереди N машина')");
        int queuePosition = queueSheetB.readQueuePosition();
        Assertions.assertTrue(queuePosition >= 1, "Позиция в очереди должна быть >= 1, получено: " + queuePosition);
        screenshot(driverB, "queue-03-user-b-queued");

        driverA.get(AuthTestConfig.BASE_URL);
        StationConnectorWizardPage wizardAFromBooking = new BookingActionSheet(driverA, waitA)
                .waitForBannerVisible().expand().clickStartCharging();
        wizardAFromBooking.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK);
        wizardAFromBooking.carousel().selectByTestId(ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
        ChargingConfirmationPage confirmationChargeA = wizardAFromBooking.clickNext();
        sessionA = confirmationChargeA.clickPayAndCharge();
        sessionA.confirmChargeStarted();
        Assertions.assertTrue(driverA.getCurrentUrl().contains("/charge"), "Сессия пользователя 1 должна перейти на /charge");

        sessionA.pollUntilPercentReached(3);

        driverB.navigate().refresh();
        BookingActionSheet sheetBDuringCharge = new BookingActionSheet(driverB, waitB);
        try {
            sheetBDuringCharge.waitForQueueBannerVisible();
        } catch (Exception ignored) {
        }
        if (!sheetBDuringCharge.isQueueBannerVisible()) {
            screenshot(driverB, "queue-during-charge-diag");
            String bodyDuringCharge = driverB.findElement(By.tagName("body")).getText();
            Assertions.fail("Пока пользователь 1 заряжается, плашка пользователя 2 должна оставаться в статусе "
                    + "очереди. Скриншот: target/screenshots/queue-during-charge-diag.png. Body: " + bodyDuringCharge);
        }

        driverA.get(AuthTestConfig.BASE_URL + "charge");
        sessionA.stopAndConfirm();
        Assertions.assertTrue(sessionA.waitForFinalText(), "Сессия пользователя 1 не завершилась штатно");
        sessionA.clickKrutoToFinish();
        sessionA = null;

        sleep(30_000);
        driverB.navigate().refresh();
        BookingActionSheet promotedSheet = new BookingActionSheet(driverB, waitB);
        try {
            promotedSheet.waitForBannerVisible();
        } catch (Exception ignored) {
        }
        boolean promoted = promotedSheet.isBannerVisible();
        if (!promoted) {
            screenshot(driverB, "queue-promotion-timeout");
            String bodyAtTimeout = driverB.findElement(By.tagName("body")).getText();
            Assertions.fail("Пользователь 2 не продвинулся по очереди за 30с после завершения сессии пользователя 1. "
                    + "Скриншот: target/screenshots/queue-promotion-timeout.png. Body: " + bodyAtTimeout);
        }

        int countdownAfterPromotion = promotedSheet.readCountdownSeconds();
        Assertions.assertTrue(countdownAfterPromotion > 0 && countdownAfterPromotion <= 15 * 60,
                "Обратный отсчёт после продвижения должен быть в пределах 15 минут, получено секунд: " + countdownAfterPromotion);
        screenshot(driverB, "queue-04-user-b-promoted");
        System.out.println("[INFO] Пользователь 2 продвинулся по очереди, обратный отсчёт: " + countdownAfterPromotion + "с");

        promotedSheet.expand().clickCancel().confirmCancel();
        System.out.println("[INFO] Бронь пользователя 2 отменена после проверки продвижения по очереди.");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void screenshot(WebDriver driver, String name) throws IOException {
        Path dir = Paths.get("target", "screenshots");
        Files.createDirectories(dir);
        File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Files.copy(src.toPath(), dir.resolve(name + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

}
