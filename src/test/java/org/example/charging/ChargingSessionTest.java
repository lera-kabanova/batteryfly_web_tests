package org.example.charging;

import org.example.auth.AuthTestConfig;
import org.example.charging.pages.ChargingConfirmationPage;
import org.example.charging.pages.ChargingSessionPage;
import org.example.charging.pages.ChargingSessionPage.ChargeReading;
import org.example.charging.pages.HistoryTransactionsPage;
import org.example.charging.pages.HistoryTransactionsPage.TransactionRecord;
import org.example.charging.pages.StationConnectorWizardPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.util.List;

class ChargingSessionTest extends ChargingTestBase {

    private ChargingSessionPage activeSession;

    @AfterEach
    void stopChargingIfStillActive() {
        if (activeSession != null) {
            activeSession.emergencyStopIfActive();
        }
    }

    @Test
    @DisplayName("CHG-FULL-01: Полный бак + Мой баланс")
    void fullCycle_fullTankWithBalance_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK, ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
    }

    @Test
    @DisplayName("CHG-FULL-02: Зарядить на 80% + Мой баланс")
    void fullCycle_eightyPercentWithBalance_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_80_PERCENT, ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
    }

    @Test
    @DisplayName("CHG-FULL-03: Полный бак + Карта")
    void fullCycle_fullTankWithCard_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK, ChargingTestConfig.PAYMENT_CARD_TESTID_CARD);
    }

    @Test
    @DisplayName("CHG-FULL-04: Зарядить на 80% + Карта")
    void fullCycle_eightyPercentWithCard_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_80_PERCENT, ChargingTestConfig.PAYMENT_CARD_TESTID_CARD);
    }

    @Test
    @DisplayName("CHG-FULL-05: Свои условия + Мой баланс")
    void fullCycle_customKwhWithBalance_completesAndAppearsInHistory() {
        runCustomAmountCycle(ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
    }

    @Test
    @DisplayName("CHG-FULL-06: Свои условия + Карта")
    void fullCycle_customKwhWithCard_completesAndAppearsInHistory() {
        runCustomAmountCycle(ChargingTestConfig.PAYMENT_CARD_TESTID_CARD);
    }

    @Test
    @DisplayName("CHG-CONCURRENT-01: попытка второй зарядки, пока первая активна, предлагает встать в очередь")
    void secondSessionAttempt_whileFirstActive_offersQueue() {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();
        ChargingConfirmationPage confirmation = wizard.clickNext();

        activeSession = confirmation.clickPayAndCharge();
        activeSession.confirmChargeStarted();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/charge"),
                "Кнопка \"Оплатить и зарядить\" должна запускать переход на /charge");

        activeSession.pollUntilPercentReached(2);

        openStationWizard();
        String bodyText = driver.findElement(By.tagName("body")).getText();
        Assertions.assertTrue(bodyText.contains("Стать в очередь"),
                "Повторный заход на занятую станцию должен предлагать встать в очередь, а не "
                        + "обычный визард выбора объёма/оплаты. Текст экрана: " + bodyText);

        driver.get(AuthTestConfig.BASE_URL + "charge");
        activeSession.stopAndConfirm();
        Assertions.assertTrue(activeSession.waitForFinalText(),
                "Не дождались текста 'Завершена в HH:MM' после остановки зарядки");
        activeSession.clickKrutoToFinish();
    }

    private void runFullCycle(String volumeCardTestId, String paymentCardTestId) {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();
        wizard.carousel().selectByTestId(volumeCardTestId);
        wizard.carousel().selectByTestId(paymentCardTestId);
        ChargingConfirmationPage confirmation = wizard.clickNext();

        activeSession = confirmation.clickPayAndCharge();
        activeSession.confirmChargeStarted();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/charge"),
                "Кнопка \"Оплатить и зарядить\" должна запускать переход на /charge");

        List<ChargeReading> readings = activeSession.pollUntilPercentReached(ChargingTestConfig.TARGET_CHARGE_PERCENT);
        assertPercentGrowsMonotonically(readings);
        assertCountersConsistentWithPercent(readings);

        activeSession.clickStop();
        Assertions.assertEquals("Желаете остановить заправку?", activeSession.readStopDialogText());
        activeSession.cancelStop();
        Assertions.assertTrue(activeSession.isActive(),
                "После отмены в confirm-диалоге зарядка должна продолжаться");

        activeSession.stopAndConfirm();
        Assertions.assertTrue(activeSession.waitForFinalText(),
                "Не дождались текста 'Завершена в HH:MM' после остановки зарядки");
        activeSession.clickKrutoToFinish();
        Assertions.assertFalse(driver.getCurrentUrl().contains("/charge"),
                "\"Круто\" должен закрывать финальный экран и уводить с /charge");

        assertSessionAppearsInHistory(readings);
    }

    private void runCustomAmountCycle(String paymentCardTestId) {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();
        wizard.carousel().selectCustomKwh(ChargingTestConfig.CUSTOM_KWH_AMOUNT);
        wizard.carousel().selectByTestId(paymentCardTestId);
        ChargingConfirmationPage confirmation = wizard.clickNext();

        Assertions.assertEquals(ChargingTestConfig.CUSTOM_KWH_AMOUNT, confirmation.getSummaryRowValue("kW*h"),
                "Пречек должен показывать ровно введённый объём kWh");
        Assertions.assertFalse(confirmation.getSummaryRowValue("BYN").isBlank(),
                "Пречек должен показывать пересчитанную сумму BYN");

        activeSession = confirmation.clickPayAndCharge();
        activeSession.confirmChargeStarted();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/charge"),
                "Кнопка \"Оплатить и зарядить\" должна запускать переход на /charge");

        List<ChargeReading> readings = activeSession.pollUntilSessionEnds();
        assertPercentGrowsMonotonically(readings);
        assertCountersConsistentWithPercent(readings);

        Assertions.assertTrue(activeSession.waitForFinalText(),
                "Не дождались текста 'Завершена в HH:MM' после автоматической остановки зарядки");
        activeSession.clickKrutoToFinish();
        Assertions.assertFalse(driver.getCurrentUrl().contains("/charge"),
                "\"Круто\" должен закрывать финальный экран и уводить с /charge");

        assertSessionAppearsInHistory(readings);
    }


    private void assertSessionAppearsInHistory(List<ChargeReading> readings) {
        HistoryTransactionsPage history = new HistoryTransactionsPage(driver, wait).open(AuthTestConfig.BASE_URL);
        TransactionRecord record = history.latestRecordForStation(ChargingTestConfig.STATION_ID);

        if (readings.isEmpty()) {
            Assertions.assertTrue(record.durationMinutes() >= 0, "Длительность сессии не может быть отрицательной");
            return;
        }

        ChargeReading lastReading = readings.get(readings.size() - 1);
        Assertions.assertTrue(record.kWh() >= lastReading.kWh(),
                "kW*h в истории (" + record.kWh() + ") не может быть меньше последнего показания во время зарядки ("
                        + lastReading.kWh() + ")");
        Assertions.assertTrue(record.byn() >= lastReading.byn(),
                "BYN в истории (" + record.byn() + ") не может быть меньше последнего показания во время зарядки ("
                        + lastReading.byn() + ")");
        Assertions.assertTrue(record.durationMinutes() >= 0, "Длительность сессии не может быть отрицательной");
    }

    private void assertPercentGrowsMonotonically(List<ChargeReading> readings) {
        for (int i = 1; i < readings.size(); i++) {
            int previous = readings.get(i - 1).percent();
            int current = readings.get(i).percent();
            Assertions.assertTrue(current >= previous,
                    "Индикатор процента должен расти монотонно, а он упал с "
                            + previous + "% до " + current + "%");
        }
    }

    private void assertCountersConsistentWithPercent(List<ChargeReading> readings) {
        for (int i = 1; i < readings.size(); i++) {
            ChargeReading previous = readings.get(i - 1);
            ChargeReading current = readings.get(i);
            Assertions.assertTrue(current.kWh() >= previous.kWh(),
                    "Счётчик kW*h не должен уменьшаться при росте процента: "
                            + previous.kWh() + " -> " + current.kWh());
            Assertions.assertTrue(current.byn() >= previous.byn(),
                    "Счётчик BYN не должен уменьшаться при росте процента: "
                            + previous.byn() + " -> " + current.byn());
            Assertions.assertTrue(current.kW() >= 0,
                    "Счётчик kW не должен быть отрицательным: " + current.kW());
        }
    }

}
