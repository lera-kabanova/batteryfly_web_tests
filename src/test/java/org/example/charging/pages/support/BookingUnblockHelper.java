package org.example.charging.pages.support;

import org.example.auth.AuthTestConfig;
import org.example.charging.ChargingTestConfig;
import org.example.charging.pages.BookingConfirmationPage;
import org.example.charging.pages.ChargingConfirmationPage;
import org.example.charging.pages.ChargingSessionPage;
import org.example.charging.pages.StationConnectorWizardPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Делает booking/queue-тесты независимыми от порядка запуска и друг от друга. Продукт блокирует
 * повторную бронь/очередь на аккаунте после того, как предыдущая была отменена/истекла БЕЗ
 * зарядки между ними (см. {@link ChargingTestConfig#REBOOKING_BLOCKED_TEXT}). Если в одном
 * прогоне {@code mvn test} один booking-тест выполнится раньше другого на ТОМ ЖЕ аккаунте,
 * второй тест без этой проверки упадёт с "Повторное бронирование будет доступно после выполнения
 * зарядки", даже если сам по себе написан правильно.
 * <p>
 * Статический метод (не завязан на {@code ChargingTestBase}) - переиспользуется и обычными
 * booking-тестами (через один {@code driver}/{@code wait}), и {@code QueueTest} (два независимых
 * driver/wait для двух одновременных пользователей).
 * <p>
 * ИНЦИДЕНТ 2026-07-24: диалог блокировки ("Повторное бронирование будет доступно...") временный
 * и, судя по всему, сам исчезает с экрана - если сначала ЖДАТЬ ПОЛНЫЙ таймаут кнопки
 * "Активировать" (через {@code BookingConfirmationPage.isLoaded()}, ~15с) и ТОЛЬКО ПОТОМ читать
 * текст страницы на предмет блокировки, диалог к этому моменту уже успевает пропасть, и текст
 * блокировки не находится - разблокировка молча не срабатывает. Нужно ждать ОДНО ИЗ ДВУХ (кнопку
 * "Активировать" ИЛИ текст блокировки), какое бы ни появилось раньше, а не проверять их по очереди.
 */
public final class BookingUnblockHelper {

    private static final By ACTIVATE_BUTTON = By.xpath("//button[contains(text(),'Активировать')]");
    private static final By REBOOKING_BLOCKED_MESSAGE =
            By.xpath("//*[contains(text(),'" + ChargingTestConfig.REBOOKING_BLOCKED_TEXT + "')]");

    private BookingUnblockHelper() {
    }

    /**
     * Вызывать ПЕРЕД тем, как тест делает свою реальную бронь/постановку в очередь (уже
     * залогинившись нужным аккаунтом). Пробует забронировать станцию #49 и, если видит
     * блокировку, сам выполняет одну минимальную реальную зарядку (Полный бак + Мой баланс,
     * цель 1%), чтобы разблокировать аккаунт.
     */
    public static void ensureAccountCanBook(WebDriver driver, WebDriverWait wait) {
        attemptReserveClick(driver, wait);
        if (!waitForBlockedMessage(driver, wait)) {
            return; // аккаунт уже может бронировать (кнопка "Активировать" появилась первой) - ничего разблокировать не нужно
        }
        unblockAccount(driver, wait);
    }

    /**
     * Делает РЕАЛЬНУЮ попытку брони (выбор карточки "Забронировать" + "Далее") и возвращает
     * загруженный экран подтверждения, готовый к {@code clickActivate()}. Если видит блокировку
     * повторной брони - разблокирует (минимальная зарядка) и повторяет попытку ЕЩЁ ОДИН раз.
     */
    public static BookingConfirmationPage reserveWithAutoUnblock(WebDriver driver, WebDriverWait wait) {
        attemptReserveClick(driver, wait);
        if (waitForBlockedMessage(driver, wait)) {
            unblockAccount(driver, wait);
            attemptReserveClick(driver, wait);
        }
        return new BookingConfirmationPage(driver, wait);
    }

    /** Выбирает карточку "Забронировать" и кликает "Далее" - без ожидания результата. */
    private static void attemptReserveClick(WebDriver driver, WebDriverWait wait) {
        StationConnectorWizardPage wizard = new StationConnectorWizardPage(driver, wait)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizard.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        try {
            wizard.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_RESERVE);
        } catch (RuntimeException e) {
            captureDiagnostics(driver, "reserve-card-select-failed");
            throw e;
        }
        new StationConnectorWizardPage(driver, wait).clickNextForBooking();
    }

    /**
     * Ждёт, пока НЕ появится ОДНО ИЗ ДВУХ: кнопка "Активировать" (не заблокирован) ИЛИ текст
     * блокировки повторной брони (заблокирован) - какое бы ни отрендерилось раньше. Возвращает
     * true, только если это оказался именно текст блокировки.
     */
    private static boolean waitForBlockedMessage(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(ACTIVATE_BUTTON),
                    ExpectedConditions.visibilityOfElementLocated(REBOOKING_BLOCKED_MESSAGE)));
        } catch (Exception e) {
            captureDiagnostics(driver, "neither-activate-nor-blocked-message-appeared");
            return false;
        }
        return !driver.findElements(REBOOKING_BLOCKED_MESSAGE).isEmpty();
    }

    private static void unblockAccount(WebDriver driver, WebDriverWait wait) {
        System.out.println("[INFO] Аккаунт заблокирован от повторной брони (см. ChargingTestConfig.REBOOKING_BLOCKED_TEXT) "
                + "- выполняю минимальную реальную зарядку для разблокировки.");
        try {
            driver.findElement(By.xpath("//button[text()='Ok']")).click();
        } catch (Exception ignored) {
            // Диалог мог закрыться сам/иначе - не критично, едем дальше на главный экран.
        }
        performMinimalUnblockCharge(driver, wait);
    }

    private static void captureDiagnostics(WebDriver driver, String name) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("target", "screenshots");
            java.nio.file.Files.createDirectories(dir);
            java.io.File src = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
            java.nio.file.Files.copy(src.toPath(), dir.resolve(name + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String body = driver.findElement(By.tagName("body")).getText();
            java.nio.file.Files.writeString(java.nio.file.Paths.get("target", name + "-body.txt"), body, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("[DIAG] " + name + " - URL: " + driver.getCurrentUrl());
            System.out.println("[DIAG] " + name + " - Body: " + body.replace("\n", " | "));
        } catch (Exception e) {
            System.out.println("[WARN] Диагностика не удалась: " + e);
        }
    }

    private static void performMinimalUnblockCharge(WebDriver driver, WebDriverWait wait) {
        driver.get(AuthTestConfig.BASE_URL);
        StationConnectorWizardPage unblockWizard = new StationConnectorWizardPage(driver, wait)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        unblockWizard.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        unblockWizard.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK);
        unblockWizard.carousel().selectByTestId(ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
        ChargingConfirmationPage confirmation = unblockWizard.clickNext();
        ChargingSessionPage session = confirmation.clickPayAndCharge();
        session.confirmChargeStarted();
        session.pollUntilPercentReached(1);
        session.stopAndConfirm();
        session.waitForFinalText();
        session.clickKrutoToFinish();
        System.out.println("[INFO] Разблокировка завершена - аккаунт снова может бронировать.");
    }
}
