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

    /**
     * Ссылка на активную зарядную сессию, если она была запущена в текущем тесте. Используется
     * ТОЛЬКО для аварийной остановки в {@link #stopChargingIfStillActive()} - см. инцидент
     * 2026-07-16, когда упавший тест оставил реальную зарядную сессию активной (StaleElement
     * во время опроса процента + отсутствие timeout, см. ChargingSessionPage.waitUntilPercentReached).
     */
    private ChargingSessionPage activeSession;

    /**
     * Выполняется ПОСЛЕ каждого теста (JUnit5 запускает @AfterEach подкласса РАНЬШЕ, чем
     * ChargingTestBase.tearDownChargingSession(), то есть аварийная остановка успевает
     * произойти до driver.quit()). Идемпотентно: если сессия уже остановлена штатно или не
     * запускалась вовсе - ничего не делает.
     */
    @AfterEach
    void stopChargingIfStillActive() {
        if (activeSession != null) {
            activeSession.emergencyStopIfActive();
        }
    }

    @Test
    @DisplayName("CHG-FULL-01: полный цикл — Полный бак + Мой баланс")
    void fullCycle_fullTankWithBalance_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK, ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
    }

    @Test
    @DisplayName("CHG-FULL-02: полный цикл — Зарядить на 80% + Мой баланс")
    void fullCycle_eightyPercentWithBalance_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_80_PERCENT, ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
    }

    @Test
    @DisplayName("CHG-FULL-03: полный цикл — Полный бак + Карта")
    void fullCycle_fullTankWithCard_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK, ChargingTestConfig.PAYMENT_CARD_TESTID_CARD);
    }

    @Test
    @DisplayName("CHG-FULL-04: полный цикл — Зарядить на 80% + Карта")
    void fullCycle_eightyPercentWithCard_completesAndAppearsInHistory() {
        runFullCycle(ChargingTestConfig.VOLUME_CARD_TESTID_80_PERCENT, ChargingTestConfig.PAYMENT_CARD_TESTID_CARD);
    }

    /**
     * CHG-CONCURRENT-01: пока первая сессия активна, повторный заход на ту же станцию/коннектор
     * не открывает обычный визард выбора объёма/оплаты, а предлагает "Стать в очередь" (сайт НЕ
     * блокирует и не показывает ошибку - подтверждено живой проверкой
     * tools/playwright-codegen/explore-stop-cancel-finalize-history-concurrent.js, 2026-07-22).
     * В очередь сознательно не встаём (не кликаем "Далее" на этом экране) - тест только проверяет
     * сам факт предложения встать в очередь. Первую сессию останавливаем сразу после старта, не
     * дожидаясь целевого процента - деньги/время сессии здесь не относятся к сути теста.
     */
    @Test
    @DisplayName("CHG-CONCURRENT-01: попытка второй зарядки, пока первая активна, предлагает встать в очередь")
    void secondSessionAttempt_whileFirstActive_offersQueue() {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();
        ChargingConfirmationPage confirmation = wizard.clickNext();

        // ВАЖНО: activeSession присваивается ДО ожидания навигации - см. ChargingConfirmationPage
        // #clickPayAndCharge() про инцидент 2026-07-22 (аварийная остановка не срабатывала,
        // потому что activeSession оставался null, если ожидание URL падало по таймауту).
        activeSession = confirmation.clickPayAndCharge();
        activeSession.confirmChargeStarted();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/charge"),
                "Кнопка \"Оплатить и зарядить\" должна запускать переход на /charge");

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

    /**
     * Общий сценарий для всех CHG-FULL-*: выбор объёма и способа оплаты по data-testid (обе
     * карусели на одном экране, см. {@link StationConnectorWizardPage}) → "Далее" → "Оплатить и
     * зарядить" (реальные деньги) → рост % с попутным сбором показаний kW*h/kW/BYN →
     * confirm-диалог остановки (текст + отмена) → реальная остановка → финализация → "Круто" →
     * проверка записи в /history → Транзакции.
     */
    private void runFullCycle(String volumeCardTestId, String paymentCardTestId) {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();
        wizard.carousel().selectByTestId(volumeCardTestId);
        wizard.carousel().selectByTestId(paymentCardTestId);
        ChargingConfirmationPage confirmation = wizard.clickNext();

        // ВАЖНО: activeSession присваивается ДО ожидания навигации - см. ChargingConfirmationPage
        // #clickPayAndCharge() про инцидент 2026-07-22 (аварийная остановка не срабатывала,
        // потому что activeSession оставался null, если ожидание URL падало по таймауту).
        activeSession = confirmation.clickPayAndCharge();
        activeSession.confirmChargeStarted();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/charge"),
                "Кнопка \"Оплатить и зарядить\" должна запускать переход на /charge");

        List<ChargeReading> readings = activeSession.pollUntilPercentReached(ChargingTestConfig.TARGET_CHARGE_PERCENT);
        assertPercentGrowsMonotonically(readings);
        assertCountersConsistentWithPercent(readings);

        // Confirm-диалог остановки: точный текст + отмена не должна останавливать зарядку.
        activeSession.clickStop();
        Assertions.assertEquals("Желаете остановить заправку?", activeSession.readStopDialogText());
        activeSession.cancelStop();
        Assertions.assertTrue(activeSession.isActive(),
                "После отмены в confirm-диалоге зарядка должна продолжаться");

        // Реальная остановка → финализация → "Круто".
        activeSession.stopAndConfirm();
        Assertions.assertTrue(activeSession.waitForFinalText(),
                "Не дождались текста 'Завершена в HH:MM' после остановки зарядки");
        activeSession.clickKrutoToFinish();
        Assertions.assertFalse(driver.getCurrentUrl().contains("/charge"),
                "\"Круто\" должен закрывать финальный экран и уводить с /charge");

        assertSessionAppearsInHistory(readings);
    }

    /**
     * Запись сессии в /history → Транзакции: сверяем станцию и то, что итоговые показания не
     * меньше последнего снятого во время опроса значения (между последним опросом и реальным
     * кликом "Остановить" могло набежать ещё немного энергии/суммы).
     */
    private void assertSessionAppearsInHistory(List<ChargeReading> readings) {
        HistoryTransactionsPage history = new HistoryTransactionsPage(driver, wait).open(AuthTestConfig.BASE_URL);
        TransactionRecord record = history.latestRecordForStation(ChargingTestConfig.STATION_ID);
        ChargeReading lastReading = readings.get(readings.size() - 1);

        Assertions.assertTrue(record.kWh() >= lastReading.kWh(),
                "kW*h в истории (" + record.kWh() + ") не может быть меньше последнего показания во время зарядки ("
                        + lastReading.kWh() + ")");
        Assertions.assertTrue(record.byn() >= lastReading.byn(),
                "BYN в истории (" + record.byn() + ") не может быть меньше последнего показания во время зарядки ("
                        + lastReading.byn() + ")");
        Assertions.assertTrue(record.durationMinutes() >= 0, "Длительность сессии не может быть отрицательной");
    }

    /** Процент не должен падать между двумя последовательными НОВЫМИ показаниями. */
    private void assertPercentGrowsMonotonically(List<ChargeReading> readings) {
        for (int i = 1; i < readings.size(); i++) {
            int previous = readings.get(i - 1).percent();
            int current = readings.get(i).percent();
            Assertions.assertTrue(current >= previous,
                    "Индикатор процента должен расти монотонно, а он упал с "
                            + previous + "% до " + current + "%");
        }
    }

    /**
     * kW*h и BYN — накопительные счётчики (переданная энергия и списанная сумма), поэтому не
     * должны уменьшаться при росте процента. kW — это мгновенная мощность, у неё нормально
     * колебаться (зарядная кривая), поэтому для неё проверяется только неотрицательность.
     */
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
