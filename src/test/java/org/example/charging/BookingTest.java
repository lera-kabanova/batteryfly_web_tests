package org.example.charging;

import org.example.auth.AuthTestConfig;
import org.example.charging.pages.BookingActionSheet;
import org.example.charging.pages.BookingConfirmationPage;
import org.example.charging.pages.StationConnectorWizardPage;
import org.example.charging.pages.support.BookingUnblockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingTest extends ChargingTestBase {

    private boolean bookingMayBeActive = false;

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

    private void openWizardAndSelectReserve() {
        StationConnectorWizardPage wizard = openStationWizard();
        wizard.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_RESERVE);
    }

    @Test
    @Order(1)
    @DisplayName("BOOK-10-NEG: бронь с нулевым балансом заблокирована с корректным сообщением")
    void reserveWithZeroBalance_isBlocked() {
        loginAsUserWithNullBalance(); // 1227320@mtp.by - баланс 0 BYN
        openWizardAndSelectReserve();
        StationConnectorWizardPage wizard = new StationConnectorWizardPage(driver, wait);
        wizard.clickNextForBooking();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'" + ChargingTestConfig.INSUFFICIENT_BALANCE_FOR_BOOKING_TEXT + "')]")));

        String bodyText = driver.findElement(By.tagName("body")).getText();
        Assertions.assertTrue(bodyText.contains(ChargingTestConfig.INSUFFICIENT_BALANCE_FOR_BOOKING_TEXT),
                "Ожидалось сообщение о нехватке средств для брони, получено: " + bodyText);
    }

    @Test
    @Order(2)
    @DisplayName("BOOK-10-POS: бронь с положительным балансом разрешена (экран подтверждения загружается)")
    void reserveWithPositiveBalance_isAllowed() {
        loginAsSecondUser(); // cinemawebwelcome@gmail.com - баланс положительный
        BookingConfirmationPage confirmation = BookingUnblockHelper.reserveWithAutoUnblock(driver, wait);

        Assertions.assertTrue(confirmation.isLoaded(),
                "С положительным балансом экран подтверждения брони должен загрузиться (кнопка 'Активировать')");
        // тест проверяет только допуск до экрана подтверждения
    }


    @Test
    @Order(3)
    @DisplayName("BOOK-LIFECYCLE: плашка+отсчёт, маршрут, keep/cancel диалог, блокировка повторной брони")
    void bookingLifecycle_bannerRouteCancelDialogAndRebookingBlock() {
        loginAsSecondUser();
        BookingConfirmationPage confirmation = BookingUnblockHelper.reserveWithAutoUnblock(driver, wait);
        Assertions.assertTrue(confirmation.isLoaded(), "Экран подтверждения брони не загрузился");
        confirmation.clickActivate();
        bookingMayBeActive = true;

        // BOOK-09: плашка с обратным отсчётом ~15 минут, которая уменьшается
        driver.get(AuthTestConfig.BASE_URL);
        BookingActionSheet sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
        Assertions.assertTrue(sheet.isBannerVisible(), "Плашка активной брони не появилась на главном экране");
        int firstReading = sheet.readCountdownSeconds();
        Assertions.assertTrue(firstReading > 14 * 60,
                "Ожидался обратный отсчёт около 15 минут сразу после брони, получено секунд: " + firstReading);

        sleep(3000);
        driver.navigate().refresh();
        sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
        int secondReading = sheet.readCountdownSeconds();
        Assertions.assertTrue(secondReading < firstReading,
                "Обратный отсчёт должен уменьшаться: было " + firstReading + "с, стало " + secondReading + "с");

        // BOOK-14: кнопка "Проложить маршрут" работает
        int tabsBefore = driver.getWindowHandles().size();
        sheet.expand();
        Assertions.assertDoesNotThrow(sheet::clickBuildRoute,
                "Кнопка 'Проложить маршрут' должна быть кликабельна");
        int tabsAfter = driver.getWindowHandles().size();
        if (tabsAfter > tabsBefore) {
            closeExtraTabsAndReturnToFirst();
        } else {
            driver.get(AuthTestConfig.BASE_URL);
        }

        // BOOK-13: "Сохранить бронь" оставляет бронь активной
        sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
        sheet.expand().clickCancel().keepBooking();
        driver.navigate().refresh();
        Assertions.assertTrue(new BookingActionSheet(driver, wait).waitForBannerVisible().isBannerVisible(),
                "После 'Сохранить бронь' плашка активной брони должна остаться на главном экране");

        // BOOK-12: "Отменить бронирование" подтверждает отмену, плашка исчезает
        sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
        sheet.expand().clickCancel();
        String dialogText = sheet.getSheetBodyText();
        Assertions.assertTrue(dialogText.contains(ChargingTestConfig.BOOKING_CANCEL_DIALOG_LINE_1)
                        && dialogText.contains(ChargingTestConfig.BOOKING_CANCEL_DIALOG_LINE_2),
                "Ожидался точный текст диалога отмены брони, получено: " + dialogText);
        sheet.confirmCancel();
        bookingMayBeActive = false; // уже отменена этим тестом, @AfterEach не должен пытаться снова
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//*[contains(text(),'Забронировано')]")));
        Assertions.assertFalse(new BookingActionSheet(driver, wait).isBannerVisible(),
                "После подтверждения отмены плашка брони должна исчезнуть");

        // BOOK-15-NEG: повторная бронь сразу после отмены недоступна
        openWizardAndSelectReserve();
        StationConnectorWizardPage secondAttempt = new StationConnectorWizardPage(driver, wait);
        secondAttempt.clickNextForBooking();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'" + ChargingTestConfig.REBOOKING_BLOCKED_TEXT + "')]")));
        String bodyText = driver.findElement(By.tagName("body")).getText();
        Assertions.assertTrue(bodyText.contains(ChargingTestConfig.REBOOKING_BLOCKED_TEXT),
                "Ожидалось сообщение о блокировке повторной брони, получено: " + bodyText);
    }


    private void closeExtraTabsAndReturnToFirst() {
        var handles = driver.getWindowHandles().stream().toList();
        String first = handles.get(0);
        for (String handle : handles) {
            if (!handle.equals(first)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(first);
        driver.get(AuthTestConfig.BASE_URL);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
