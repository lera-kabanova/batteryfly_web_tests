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

/**
 * Бронирование/очередь коннектора (qa-discovery/test-modules.md не выделяет для этого отдельный
 * модуль — на продукте это та же карточка карусели объёма зарядки, что "Полный бак"/"80%", см.
 * {@link ChargingTestConfig#VOLUME_CARD_TESTID_RESERVE}). Бесплатно (0 BYN).
 * <p>
 * ВАЖНОЕ ОГРАНИЧЕНИЕ ПРОДУКТА (найдено живой проверкой 2026-07-24, см. REBOOKING_BLOCKED_TEXT):
 * отмена брони БЕЗ выполненной зарядки блокирует аккаунт от повторной брони до следующей реальной
 * зарядки. Это делает отдельные независимые тесты на каждый cancel-сценарий (banner/countdown,
 * confirm-отмену, keep-бронь, кнопку маршрута, блокировку повтора) невозможными без реальной
 * зарядки МЕЖДУ каждым из них. Поэтому все они объединены в ОДИН лайфцикл-тест на ОДНОЙ реальной
 * брони — по той же логике экономии, что и {@code ChargingSessionTest#runFullCycle}, которая
 * объединяет много проверок в одну платную сессию вместо N отдельных.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingTest extends ChargingTestBase {

    private boolean bookingMayBeActive = false;

    /**
     * JUnit5 запускает @AfterEach подкласса РАНЬШЕ {@code ChargingTestBase.tearDownChargingSession()}
     * — аналог {@code ChargingSessionTest.stopChargingIfStillActive()} для сценария зарядки.
     */
    @AfterEach
    void cancelBookingIfStillActive() {
        if (!bookingMayBeActive) {
            return;
        }
        try {
            driver.get(AuthTestConfig.BASE_URL);
            // waitForBannerVisible() - НЕ мгновенный isBannerVisible() - критично здесь: гонка тут
            // силентно пропускала отмену и оставляла реальную бронь висеть на станции #49 навсегда
            // (реальный инцидент 2026-07-24, обнаружен по "NoSuchElementException:
            // charge-volume-card-reserve" во всех последующих тестах - см. automation-checklist.md).
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
        // clickNextForBooking() всё равно переходит в попытку — блокировка происходит модалкой поверх,
        // рендерится не мгновенно (подтверждено живой диагностикой BookingDiagnosticTest, 2026-07-24).
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
        loginAsSecondUser(); // cinemawebwelcome@gmail.com - баланс положительный, разблокирован реальной зарядкой
        // reserveWithAutoUnblock() сам разблокирует аккаунт, если предыдущий booking-тест в этом
        // же прогоне его заблокировал - см. BookingUnblockHelper.
        BookingConfirmationPage confirmation = BookingUnblockHelper.reserveWithAutoUnblock(driver, wait);

        Assertions.assertTrue(confirmation.isLoaded(),
                "С положительным балансом экран подтверждения брони должен загрузиться (кнопка 'Активировать')");
        // Бронь сознательно НЕ активируется - тест проверяет только допуск до экрана подтверждения,
        // не занимая станцию реальной 15-минутной бронью без необходимости.
    }

    /**
     * Объединяет BOOK-09 (плашка+обратный отсчёт), BOOK-14 (кнопка "Проложить маршрут"), BOOK-13
     * ("Сохранить бронь" оставляет бронь активной), BOOK-12 ("Отменить бронирование" убирает
     * плашку) и BOOK-15-NEG (повторная бронь после отмены без зарядки заблокирована) — ВСЕ на
     * ОДНОЙ реальной брони, т.к. продукт не позволяет создать вторую бронь после отмены первой без
     * зарядки между ними (см. javadoc класса).
     */
    @Test
    @Order(3)
    @DisplayName("BOOK-LIFECYCLE: плашка+отсчёт, маршрут, keep/cancel диалог, блокировка повторной брони")
    void bookingLifecycle_bannerRouteCancelDialogAndRebookingBlock() {
        loginAsSecondUser();
        BookingConfirmationPage confirmation = BookingUnblockHelper.reserveWithAutoUnblock(driver, wait);
        Assertions.assertTrue(confirmation.isLoaded(), "Экран подтверждения брони не загрузился");
        confirmation.clickActivate();
        bookingMayBeActive = true;

        // --- BOOK-09: плашка с обратным отсчётом ~15 минут, который уменьшается ---
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

        // --- BOOK-14: кнопка "Проложить маршрут" присутствует и кликабельна ---
        // Живой проверкой 2026-07-24 подтверждено: клик НЕ открывает новую вкладку и НЕ меняет URL
        // в этом тестовом окружении (headed Chrome без обработчика внешнего deep link на карты) -
        // максимум, что проверяемо автоматически, это сам факт наличия и кликабельности кнопки
        // (см. также аналогичное ограничение для иконок шапки в NavigationTest, qa-discovery/observations.md).
        int tabsBefore = driver.getWindowHandles().size();
        sheet.expand();
        Assertions.assertDoesNotThrow(sheet::clickBuildRoute,
                "Кнопка 'Проложить маршрут' должна быть кликабельна без исключений");
        int tabsAfter = driver.getWindowHandles().size();
        if (tabsAfter > tabsBefore) {
            closeExtraTabsAndReturnToFirst();
        } else {
            driver.get(AuthTestConfig.BASE_URL);
        }

        // --- BOOK-13: "Сохранить бронь" оставляет бронь активной ---
        sheet = new BookingActionSheet(driver, wait).waitForBannerVisible();
        sheet.expand().clickCancel().keepBooking();
        driver.navigate().refresh();
        Assertions.assertTrue(new BookingActionSheet(driver, wait).waitForBannerVisible().isBannerVisible(),
                "После 'Сохранить бронь' плашка активной брони должна остаться на главном экране");

        // --- BOOK-12: "Отменить бронирование" подтверждает отмену, плашка исчезает ---
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

        // --- BOOK-15-NEG: повторная бронь сразу после отмены (без зарядки) заблокирована ---
        openWizardAndSelectReserve();
        StationConnectorWizardPage secondAttempt = new StationConnectorWizardPage(driver, wait);
        secondAttempt.clickNextForBooking();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'" + ChargingTestConfig.REBOOKING_BLOCKED_TEXT + "')]")));
        String bodyText = driver.findElement(By.tagName("body")).getText();
        Assertions.assertTrue(bodyText.contains(ChargingTestConfig.REBOOKING_BLOCKED_TEXT),
                "Ожидалось сообщение о блокировке повторной брони, получено: " + bodyText);
    }

    /**
     * BOOK-15-POS (позитивная часть — "повторная бронь разрешена ПОСЛЕ зарядки") подтверждена
     * ВРУЧНУЮ живым прогоном 2026-07-24 (см. automation-checklist.md): после реальной минимальной
     * зарядки (Полный бак + Мой баланс, цель 1%) на cinemawebwelcome@gmail.com сообщение
     * REBOOKING_BLOCKED_TEXT переставало появляться. Не автоматизирована как отдельный regression-
     * тест по той же причине экономии денег/времени, что и CHG-FULL-02/03/04 — каждый прогон
     * требовал бы ещё одной полной зарядной сессии только чтобы разблокировать аккаунт, а после
     * {@link #bookingLifecycle_bannerRouteCancelDialogAndRebookingBlock()} аккаунт УЖЕ заблокирован
     * этим же тестом намеренно (для проверки BOOK-15-NEG) — следующий прогон сюиты потребует новой
     * ручной разблокировки (см. BookingDiagnosticTest, тоже временный/диагностический).
     */

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
