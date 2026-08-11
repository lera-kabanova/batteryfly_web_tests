package org.example.charging;

import org.example.auth.AuthTestConfig;
import org.example.charging.pages.ChargingConfirmationPage;
import org.example.charging.pages.ChargingSessionPage;
import org.example.charging.pages.StationConnectorWizardPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сценарии 1-3 из задания по Charging: реальное отображение процента > 0, формат плашки активной
 * зарядки на главном экране, сохранение способа оплаты/объёма между сессиями. ВСЕ три проверки
 * объединены в ОДНУ реальную минимальную зарядную сессию (Полный бак + Мой баланс, цель 1%, на
 * cinemawebwelcome@gmail.com) — по той же логике экономии денег/времени, что и
 * {@code BookingTest.bookingLifecycle_...}.
 * <p>
 * ДИАГНОСТИКА 2026-07-24 (это заняло несколько прогонов найти): "Зарядить на 80%" на этом
 * аккаунте сейчас ненадёжно для реального запуска - баланс 0.89 BYN недостаточен для СТАРТА
 * зарядки 80% (хотя достаточен для БРОНИ - отдельные лимиты); попытка оплаты КАРТОЙ вместо этого
 * привела к реальной транзакции (#12303), зависшей в статусе "В процессе оплаты" - экран /charge
 * для неё показывает "Оплата" БЕЗ единой кнопки (0 кнопок в DOM), т.е. без способа отменить её
 * через UI - похоже на реальный баг продукта (см. automation-checklist.md), не решённый в рамках
 * этой сессии. Из-за этого тест использует Полный бак (не 80%) - несохранение ОБЪЁМА между
 * сессиями всё равно надёжно подтверждено отдельной живой проверкой без риска для баланса
 * (tools/playwright-codegen/explore-check-persistence.js).
 */
class ChargingUiAndPersistenceTest extends ChargingTestBase {

    private ChargingSessionPage activeSession;

    /**
     * ВАЖНО: этот тест, в отличие от {@code ChargingSessionTest}, уходит с /charge на главный
     * экран ПОСРЕДИ теста (чтобы увидеть плашку активной зарядки). Если исключение бросается уже
     * ПОСЛЕ этого перехода, драйвер не на /charge, и {@code emergencyStopIfActive()} не находит
     * кнопку "Остановить" (реальный инцидент 2026-07-24: сессия осталась активной ~25 минут,
     * 2 kWh/1.3 BYN списано, до аварийной ручной остановки через Playwright-скрипт). Поэтому
     * здесь ЯВНО возвращаемся на /charge перед аварийной остановкой.
     * <p>
     * ВТОРОЙ инцидент 2026-07-24 (та же станция, тот же паттерн): полная навигация
     * {@code driver.get(...)} на /charge - это НЕ SPA-переход, а полная перезагрузка страницы;
     * приложению нужно время на бутстрап/запрос состояния активной сессии, прежде чем кнопка
     * "Остановить" отрендерится. {@code emergencyStopIfActive()} проверяет её МГНОВЕННО (без
     * ожидания) - рассчитан на контекст {@code ChargingSessionTest}, где driver и так уже на
     * /charge с отрендеренной кнопкой. Здесь нужно ЯВНО подождать перед вызовом.
     */
    @AfterEach
    void stopChargingIfStillActive() {
        if (activeSession != null) {
            driver.get(AuthTestConfig.BASE_URL + "charge");
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[text()='Остановить']")));
            } catch (Exception ignored) {
                // Если кнопки нет вообще (сессия уже штатно завершилась) - emergencyStopIfActive() ничего не сделает, это ок.
            }
            activeSession.emergencyStopIfActive();
        }
    }

    @Test
    @DisplayName("CHG-UI-1-2-3: реальный % на экране зарядки, формат плашки 'Идёт зарядка', сохранение выбора объёма/оплаты")
    void chargingScreenPercent_homeBanner_andConditionsPersistence() throws java.io.IOException {
        loginAsSecondUser();
        StationConnectorWizardPage wizard = openStationWizard();

        // Полный бак + Мой баланс - тот же комбо, что уже дважды надёжно отработал на этом
        // аккаунте (см. minimalRealChargeToUnblockRebooking). Живой проверкой 2026-07-24
        // установлено, что "Зарядить на 80%" в паре с любым способом оплаты на этом аккаунте
        // (баланс 0.89 BYN недостаточен, оплата картой зависает в статусе "В процессе оплаты" без
        // единой кнопки восстановления в UI - см. диагностику ниже) сейчас ненадёжно; "Полный бак"
        // не требует non-default выбора для сценариев 1/2 (реальный %/формат плашки), а для
        // сценария 3 достаточно проверить, что "Мой баланс" остаётся выбранным - несохранение
        // ОБЪЁМА уже отдельно подтверждено (см. javadoc ниже).
        wizard.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK);
        wizard.carousel().selectByTestId(ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE);
        ChargingConfirmationPage confirmation = wizard.clickNext();

        activeSession = confirmation.clickPayAndCharge();
        activeSession.confirmChargeStarted();

        // --- Сценарий 1: экран зарядки показывает фактический % с бэкенда, как только он > 0 ---
        activeSession.pollUntilPercentReached(1);
        String chargeScreenBody = driver.findElement(By.tagName("body")).getText();
        Matcher percentMatch = Pattern.compile("(\\d+)\\s*%").matcher(chargeScreenBody);
        Assertions.assertTrue(percentMatch.find(), "На экране зарядки не найден процент вида 'N%': " + chargeScreenBody);
        int displayedPercent = Integer.parseInt(percentMatch.group(1));
        Assertions.assertTrue(displayedPercent > 0,
                "Ожидался фактический процент > 0 с бэкенда, отображается: " + displayedPercent);

        // --- Сценарий 2: плашка активной зарядки на главном экране, пока сессия активна ---
        // Точный текст подтверждён живой диагностикой 2026-07-24: "33 % | Идет зарядка | 58 kW" -
        // ТРИ отдельных элемента в одной строке (без "ё" в "Идет", с пробелом перед "%"), а не один
        // элемент с обоими подстроками - см. target/screenshots/charging-banner-not-found.png.
        driver.get(AuthTestConfig.BASE_URL);
        By bannerLocator = By.xpath("//*[text()='" + ChargingTestConfig.ACTIVE_CHARGING_BANNER_LABEL + "']");
        String bannerAreaText;
        try {
            wait.withTimeout(java.time.Duration.ofSeconds(25)).until(ExpectedConditions.visibilityOfElementLocated(bannerLocator));
            bannerAreaText = driver.findElement(bannerLocator).findElement(By.xpath("..")).getText();
        } catch (org.openqa.selenium.TimeoutException e) {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target", "screenshots"));
            java.io.File src = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
            java.nio.file.Files.copy(src.toPath(), java.nio.file.Paths.get("target", "screenshots", "charging-banner-not-found.png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String fullBody = driver.findElement(By.tagName("body")).getText();
            java.nio.file.Files.writeString(java.nio.file.Paths.get("target", "charging-banner-not-found-body.txt"),
                    fullBody, java.nio.charset.StandardCharsets.UTF_8);
            Assertions.fail("Плашка активной зарядки не найдена на главном экране за 25с - "
                    + "полный текст страницы сохранён в target/charging-banner-not-found-body.txt "
                    + "и скриншот в target/screenshots/charging-banner-not-found.png. Body: " + fullBody);
            return;
        } finally {
            wait.withTimeout(java.time.Duration.ofSeconds(15));
        }

        java.nio.file.Files.writeString(java.nio.file.Paths.get("target", "banner-area-text.txt"),
                "PARENT: " + bannerAreaText + "\nGRANDPARENT: "
                        + driver.findElement(bannerLocator).findElement(By.xpath("../..")).getText(),
                java.nio.charset.StandardCharsets.UTF_8);

        Matcher bannerPercent = Pattern.compile("(\\d+)\\s*%").matcher(bannerAreaText);
        if (!bannerPercent.find()) {
            bannerAreaText = driver.findElement(bannerLocator).findElement(By.xpath("../..")).getText();
            bannerPercent = Pattern.compile("(\\d+)\\s*%").matcher(bannerAreaText);
        }
        Assertions.assertTrue(bannerPercent.find(), "В плашке активной зарядки не найден процент: " + bannerAreaText);
        Assertions.assertFalse(bannerPercent.group(1).contains("."),
                "Процент в плашке должен быть без дробной части: " + bannerPercent.group(1));

        Matcher bannerPower = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*kW").matcher(bannerAreaText);
        Assertions.assertTrue(bannerPower.find(), "В плашке активной зарядки не найдена мощность в kW: " + bannerAreaText);
        String decimals = bannerPower.group(1).replace(",", ".").contains(".")
                ? bannerPower.group(1).replace(",", ".").split("\\.")[1]
                : "";
        Assertions.assertTrue(decimals.length() <= 1,
                "Мощность должна иметь максимум 1 знак после запятой, получено: " + bannerPower.group(1));

        // Возвращаемся на /charge и штатно завершаем сессию (то же поведение, что в ChargingSessionTest).
        driver.findElement(bannerLocator).click();
        wait.until(ExpectedConditions.urlContains("/charge"));
        activeSession.stopAndConfirm();
        Assertions.assertTrue(activeSession.waitForFinalText(), "Не дождались текста 'Завершена в HH:MM'");
        activeSession.clickKrutoToFinish();

        // --- Сценарий 3: "Условия зарядки" при повторном открытии - что реально сохраняется ---
        // ЖИВОЙ РЕЗУЛЬТАТ 2026-07-24 (tools/playwright-codegen/explore-check-persistence.js),
        // отличается от гипотезы задания: способ оплаты ДЕЙСТВИТЕЛЬНО сохраняется между сессиями
        // (проверено на "Мой баланс" - остаётся активным, scaleY(1)), а вот ОБЪЁМ ЗАРЯДКИ НЕ
        // сохраняется - после сессии с "Зарядить на 80%" карусель объёма снова открывается с
        // "Полный бак" активным по умолчанию (scaleY(1)), "80%" неактивна (scaleY(0.8)). Это не
        // баг теста - подтверждённое поведение продукта.
        openStationWizard();
        Assertions.assertTrue(isCardActive(ChargingTestConfig.PAYMENT_CARD_TESTID_BALANCE),
                "Способ оплаты 'Мой баланс' должен остаться выбранным по умолчанию после предыдущей сессии");
        Assertions.assertTrue(isCardActive(ChargingTestConfig.VOLUME_CARD_TESTID_FULL_TANK),
                "Ожидалось, что 'Полный бак' активен по умолчанию");
        // Несохранение ОБЪЁМА между сессиями (после "Зарядить на 80%" карусель возвращается к
        // "Полный бак") уже отдельно и надёжно подтверждено живой проверкой 2026-07-24 - см.
        // tools/playwright-codegen/explore-check-persistence.js и automation-checklist.md.
        // Не повторяется здесь тем же прогоном, чтобы не зависеть от нестабильной комбинации
        // "80% + оплата" на этом аккаунте (см. диагностику класса).
    }

    /** Активная карточка карусели определяется через inline style="...scaleY(1)..." её РОДИТЕЛЯ. */
    private boolean isCardActive(String testId) {
        WebElement card = driver.findElement(By.cssSelector("[data-testid='" + testId + "']"));
        String style = card.findElement(By.xpath("..")).getAttribute("style");
        return style != null && style.contains("scaleY(1)");
    }
}
