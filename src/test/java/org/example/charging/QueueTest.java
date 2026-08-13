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

/**
 * Очередь на занятой станции (сценарии 16, 17 из задания) — ДВА одновременных реальных
 * пользователя на РАЗНЫХ WebDriver. Пользователь 1 (реально заряжается) = lerakab5@gmail.com
 * (баланс ~11 BYN на 2026-07-24), Пользователь 2 (встаёт в очередь — бесплатно) =
 * cinemawebwelcome@gmail.com. Роли зеркальны по сравнению с остальными Booking-тестами, т.к.
 * cinemawebwelcome сейчас истощён по балансу для реальной зарядки (см. диагностику
 * {@code ChargingUiAndPersistenceTest}). Экран подтверждения постановки в очередь - "Бронирование"
 * / "Режим: Постановка в очередь" / "Стоимость: 0 BYN", тот же {@link BookingConfirmationPage},
 * что и обычная бронь.
 * <p>
 * Не расширяет {@link ChargingTestBase} — нужны ДВА независимых driver/wait одновременно.
 */
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
            driverB.get(AuthTestConfig.BASE_URL);
            BookingActionSheet sheetB = new BookingActionSheet(driverB, waitB).waitForBannerVisible();
            if (sheetB.isBannerVisible()) {
                sheetB.expand().clickCancel().confirmCancel();
            }
        } catch (Exception ignored) {
            // B может не иметь активной брони/очереди на момент очистки - это ок.
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
    @DisplayName("QUEUE-16: пользователь 1 заряжается, пользователь 2 встаёт в очередь, продвижение после завершения")
    void queue_userChargingUserQueued_progressesAfterFirstFinishes() throws IOException {
        // Пользователь 1 (заряжается) = lerakab5 (баланс ~11 BYN на 2026-07-24) - cinemawebwelcome
        // сейчас слишком истощён по балансу для реальной зарядки (см. диагностику
        // ChargingUiAndPersistenceTest), поэтому роли по сравнению с остальными Booking-тестами
        // здесь ЗЕРКАЛЬНО поменяны: cinemawebwelcome встаёт в очередь (бесплатно, не требует много баланса).
        // B логинится и разблокируется ПЕРВЫМ, пока станция ещё свободна - ensureAccountCanBook()
        // сам делает реальную бронь/проверку, а на занятой активной зарядкой станции карточка
        // "Стать в очередь" не имеет своего data-testid (см. ChargingTestConfig), из-за чего
        // ensureAccountCanBook() упал бы, если вызвать его ПОСЛЕ старта сессии A.
        loginAs(driverB, waitB, AuthTestConfig.USER_EMAIL_CINEMA, AuthTestConfig.SECOND_USER_PASSWORD);
        BookingUnblockHelper.ensureAccountCanBook(driverB, waitB);

        loginAs(driverA, waitA, "lerakab5@gmail.com", "Lera123!");
        StationConnectorWizardPage wizardA = new StationConnectorWizardPage(driverA, waitA)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizardA.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        wizardA.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK);
        wizardA.carousel().selectByTestId(ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
        ChargingConfirmationPage confirmationA = wizardA.clickNext();
        sessionA = confirmationA.clickPayAndCharge();
        sessionA.confirmChargeStarted();
        Assertions.assertTrue(driverA.getCurrentUrl().contains("/charge"), "Сессия пользователя 1 должна перейти на /charge");

        StationConnectorWizardPage wizardB = new StationConnectorWizardPage(driverB, waitB)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizardB.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        screenshot(driverB, "queue-01-wizard-b-busy");
        String wizardBBody = driverB.findElement(By.tagName("body")).getText();
        Assertions.assertTrue(wizardBBody.contains("Стать в очередь"),
                "Пользователь 2 должен увидеть предложение встать в очередь, получено: " + wizardBBody);

        // Когда станция занята АКТИВНОЙ ЗАРЯДКОЙ (а не просто чужой бронью), "Стать в очередь" -
        // единственный вариант в карусели объёма и уже активен по умолчанию (нет отдельной
        // карточки с data-testid для выбора, см. ChargingTestConfig) - сразу жмём "Далее".
        BookingConfirmationPage queueConfirmation = wizardB.clickNextForBooking();
        Assertions.assertTrue(queueConfirmation.isLoaded(), "Экран подтверждения постановки в очередь не загрузился");
        String queueBody = queueConfirmation.getPageBodyText();
        Assertions.assertTrue(queueBody.contains("Постановка в очередь"),
                "Ожидался режим 'Постановка в очередь', получено: " + queueBody);
        queueConfirmation.clickActivate();

        // ВАЖНАЯ НАХОДКА 2026-07-24: пока B просто ЖДЁТ в очереди (первый пользователь ещё
        // заряжается), плашка "Забронировано" НЕ появляется на его главном экране - никакого
        // видимого индикатора позиции в очереди нет вообще (см. automation-checklist.md). Плашка
        // с обратным отсчётом появляется ТОЛЬКО ПОСЛЕ продвижения (когда приходит очередь B
        // реально начать зарядку) - это и есть момент "продвижения по очереди", который нужно
        // ловить ПОСЛЕ остановки сессии A, а не сразу после присоединения к очереди.
        driverB.get(AuthTestConfig.BASE_URL);
        String bodyRightAfterJoining = driverB.findElement(By.tagName("body")).getText();
        screenshot(driverB, "queue-02-user-b-waiting-no-banner-expected");
        System.out.println("[INFO] Главный экран B сразу после присоединения к очереди (баннера может не быть - это ожидаемо): "
                + bodyRightAfterJoining.replace("\n", " | "));

        // Останавливаем сессию A сразу после старта - деньги/точное время роли не играют для этого теста.
        driverA.get(AuthTestConfig.BASE_URL + "charge");
        sessionA.stopAndConfirm();
        Assertions.assertTrue(sessionA.waitForFinalText(), "Сессия пользователя 1 не завершилась штатно");
        sessionA.clickKrutoToFinish();
        sessionA = null; // уже корректно завершена, @AfterEach не должен трогать её снова

        // Продвижение B: плашка "Забронировано" (с обратным отсчётом) должна ПОЯВИТЬСЯ в течение
        // ~20-30с после завершения A - подтверждено живой проверкой 2026-07-24 (после ручной
        // остановки сессии первого пользователя вторoй показал "13:14 Забронировано").
        // ВАЖНО: НЕ оборачивать проверку в WebDriverWait ВНУТРИ этой лямбды - если внутренний wait
        // бросит TimeoutException, оно не входит в список игнорируемых исключений внешнего
        // FluentWait и мгновенно провалит весь тест вместо повторной попытки (реальный инцидент
        // 2026-07-24 в BookingExpiryTest, см. automation-checklist.md). Каждая попытка обёрнута в
        // try/catch, трактующий любое исключение как "ещё не продвинулся".
        boolean promoted = false;
        long promotionDeadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
        while (System.currentTimeMillis() < promotionDeadline) {
            try {
                driverB.navigate().refresh();
                if (new BookingActionSheet(driverB, waitB).isBannerVisible()) {
                    promoted = true;
                    break;
                }
            } catch (Exception e) {
                System.out.println("[WARN] Проверка продвижения B упала с исключением, пробуем снова: " + e);
            }
            sleep(3000);
        }
        Assertions.assertTrue(promoted, "Пользователь 2 не продвинулся по очереди за 30с после завершения сессии пользователя 1");

        BookingActionSheet promotedSheet = new BookingActionSheet(driverB, waitB);
        Assertions.assertTrue(promotedSheet.isBannerVisible(),
                "После завершения сессии пользователя 1 пользователь 2 должен продвинуться по очереди "
                        + "и получить активную бронь с обратным отсчётом");
        int countdownAfterPromotion = promotedSheet.readCountdownSeconds();
        Assertions.assertTrue(countdownAfterPromotion > 0 && countdownAfterPromotion <= 15 * 60,
                "Обратный отсчёт после продвижения должен быть в пределах 15 минут, получено секунд: " + countdownAfterPromotion);
        screenshot(driverB, "queue-03-user-b-promoted");
        System.out.println("[INFO] Пользователь 2 продвинулся по очереди, обратный отсчёт: " + countdownAfterPromotion + "с");
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
