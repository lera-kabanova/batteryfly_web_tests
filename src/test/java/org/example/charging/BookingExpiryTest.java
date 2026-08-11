package org.example.charging;

import org.example.auth.AuthTestConfig;
import org.example.charging.pages.BookingActionSheet;
import org.example.charging.pages.BookingConfirmationPage;
import org.example.charging.pages.support.BookingUnblockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BOOK-11: бронь + плашка на главном экране. Упрощено по просьбе пользователя 2026-07-24 —
 * тест только делает бронь и проверяет, что плашка появилась; реальное 15-минутное ожидание
 * истечения убрано из этого теста (было источником нестабильности из-за частых перезагрузок
 * страницы). Использует {@link BookingUnblockHelper#reserveWithAutoUnblock}, чтобы не зависеть от
 * того, в каком порядке `mvn test` запустит этот класс относительно {@code BookingTest}/
 * {@code QueueTest} на том же аккаунте — ОДНА попытка выбора карточки "Забронировать" в общем
 * случае, разблокировка только если реально понадобится.
 */
class BookingExpiryTest extends ChargingTestBase {

    private boolean bookingMayBeActive = false;

    /** Отменяет бронь после теста - иначе она виснет на станции #49 на все 15 минут таймера. */
    @AfterEach
    void cancelBookingIfStillActive() {
        if (!bookingMayBeActive) {
            return;
        }
        try {
            driver.get(AuthTestConfig.BASE_URL);
            BookingActionSheet sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
            sheet.expand().clickCancel().confirmCancel();
            System.out.println("[CLEANUP] Активная бронь отменена в @AfterEach.");
        } catch (Exception e) {
            System.out.println("[WARN] Не удалось отменить бронь в @AfterEach: " + e
                    + " - ПРОВЕРЬТЕ ВРУЧНУЮ, СТАНЦИЯ #49 МОЖЕТ ОСТАВАТЬСЯ ЗАБРОНИРОВАНА.");
        }
    }

    @Test
    @DisplayName("BOOK-11: бронь активируется и плашка появляется на главном экране")
    void booking_showsBannerOnHomeScreen() {
        loginAsSecondUser();

        BookingConfirmationPage confirmation = BookingUnblockHelper.reserveWithAutoUnblock(driver, wait);
        if (!confirmation.isLoaded()) {
            diagnose("book11-confirmation-not-loaded");
        }
        Assertions.assertTrue(confirmation.isLoaded(), "Экран подтверждения брони не загрузился");
        confirmation.clickActivate();
        bookingMayBeActive = true;

        driver.get(AuthTestConfig.BASE_URL);
        BookingActionSheet sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
        Assertions.assertTrue(sheet.isBannerVisible(), "Плашка активной брони не появилась на главном экране");
        System.out.println("[INFO] Бронь активирована, плашка на главном экране подтверждена. "
                + "Обратный отсчёт: " + sheet.readCountdownSeconds() + "с");
    }

    private void diagnose(String name) {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target", "screenshots"));
            java.io.File src = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
            java.nio.file.Files.copy(src.toPath(), java.nio.file.Paths.get("target", "screenshots", name + ".png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String body = driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
            java.nio.file.Files.writeString(java.nio.file.Paths.get("target", name + "-body.txt"), body, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("[DIAG] URL: " + driver.getCurrentUrl());
            System.out.println("[DIAG] Body: " + body.replace("\n", " | "));
        } catch (Exception e) {
            System.out.println("[WARN] Диагностика не удалась: " + e);
        }
    }
}
