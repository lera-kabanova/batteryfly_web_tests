package org.example.charging.pages.support;

import org.example.auth.AuthTestConfig;
import org.example.charging.ChargingTestConfig;
import org.example.charging.pages.BookingActionSheet;
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
        cancelOwnStaleBookingIfAny(driver, wait);
        if (attemptReserveClick(driver, wait) && !waitForBlockedMessage(driver, wait)) {
            return; // аккаунт уже может бронировать (кнопка "Активировать" появилась первой) - ничего разблокировать не нужно
        }
        unblockAccount(driver, wait);
        if (!attemptReserveClickWithRetries(driver, wait)) {
            throw new IllegalStateException("Карточка 'Забронировать' так и не появилась после разблокировки "
                    + "и нескольких повторных попыток - аккаунт может быть заблокирован дольше ожидаемого, "
                    + "проверьте вручную.");
        }
    }

    /**
     * Делает РЕАЛЬНУЮ попытку брони (выбор карточки "Забронировать" + "Далее") и возвращает
     * загруженный экран подтверждения, готовый к {@code clickActivate()}. Если видит блокировку
     * повторной брони - разблокирует (минимальная зарядка) и повторяет попытку.
     */
    public static BookingConfirmationPage reserveWithAutoUnblock(WebDriver driver, WebDriverWait wait) {
        cancelOwnStaleBookingIfAny(driver, wait);
        if (attemptReserveClick(driver, wait) && !waitForBlockedMessage(driver, wait)) {
            return new BookingConfirmationPage(driver, wait);
        }
        unblockAccount(driver, wait);
        if (!attemptReserveClickWithRetries(driver, wait)) {
            throw new IllegalStateException("Карточка 'Забронировать' так и не появилась после разблокировки "
                    + "и нескольких повторных попыток - аккаунт может быть заблокирован дольше ожидаемого, "
                    + "проверьте вручную.");
        }
        return new BookingConfirmationPage(driver, wait);
    }

    /**
     * ИДЕЯ ПОЛЬЗОВАТЕЛЯ 2026-08-18: часть падений "карточка Забронировать не найдена" на самом деле
     * была не кулдауном, а тем, что у ЭТОГО ЖЕ аккаунта уже висела СВОЯ активная бронь/очередь,
     * оставшаяся с прошлого (упавшего) прогона - карточку и не должно быть, пока есть активная
     * бронь. Раньше это дорого лечилось через целую разблокирующую зарядку. Дешевле и вернее -
     * сначала проверить, нет ли у аккаунта уже своей брони/очереди на главном экране, и если есть -
     * просто отменить её, как это уже делает {@code @AfterEach} тестов, только теперь ПЕРЕД стартом.
     */
    private static void cancelOwnStaleBookingIfAny(WebDriver driver, WebDriverWait wait) {
        driver.get(AuthTestConfig.BASE_URL);
        BookingActionSheet sheet = new BookingActionSheet(driver, wait);
        if (sheet.isBannerVisible()) {
            System.out.println("[INFO] У аккаунта уже есть активная бронь с прошлого прогона - отменяю перед новой попыткой.");
            sheet.expand().clickCancel().confirmCancel();
        } else if (sheet.isQueueBannerVisible()) {
            System.out.println("[INFO] У аккаунта уже есть активная позиция в очереди с прошлого прогона - отменяю перед новой попыткой.");
            sheet.expandQueueBanner().clickCancel().confirmCancel();
        }
    }

    /**
     * ИНЦИДЕНТ 2026-08-18: живой проверкой подтверждено, что даже СРАЗУ после успешной
     * разблокирующей зарядки (включая её собственную 30-секундную паузу в {@link #unblockAccount})
     * карточка "Забронировать" иногда ВСЁ ЕЩЁ отсутствует в карусели - таймер снятия кулдауна после
     * реальной зарядки, судя по всему, ДРУГОЙ (и более долгий), чем таймер освобождения коннектора
     * после отмены брони, под который была откалибрована пауза в unblockAccount. Поэтому вместо
     * одной попытки - несколько с паузой между ними.
     */
    private static boolean attemptReserveClickWithRetries(WebDriver driver, WebDriverWait wait) {
        int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attemptReserveClick(driver, wait)) {
                return true;
            }
            if (attempt < maxAttempts) {
                System.out.println("[INFO] Карточка 'Забронировать' всё ещё недоступна после разблокировки - "
                        + "жду ещё 30с (попытка " + (attempt + 1) + "/" + maxAttempts + ").");
                sleep(30_000);
            }
        }
        return false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Выбирает карточку "Забронировать" и кликает "Далее" - без ожидания результата дальше.
     * Возвращает {@code false}, если карточка "Забронировать" вообще НЕ отрисована в карусели
     * (ничего не кликает в этом случае).
     * <p>
     * ИНЦИДЕНТ 2026-08-18: пока аккаунт в кулдауне после предыдущей брони/очереди (см.
     * {@link ChargingTestConfig#REBOOKING_BLOCKED_TEXT}), карточка "Забронировать" не просто
     * блокируется при выборе - она вообще ОТСУТСТВУЕТ в DOM карусели (подтверждено живой
     * проверкой: на заблокированном аккаунте карусель показывает только 3 карточки объёма вместо
     * 4, сколько её ни свайпай - целевой карточки там физически нет). Раньше код пытался вслепую
     * докрутить до несуществующей карточки и падал с непонятным NoSuchElementException/
     * TimeoutException, из-за чего self-healing разблокировка ниже вообще не успевала сработать.
     * Теперь это состояние проверяется и обрабатывается ДО попытки выбора карточки.
     */
    private static boolean attemptReserveClick(WebDriver driver, WebDriverWait wait) {
        StationConnectorWizardPage wizard = new StationConnectorWizardPage(driver, wait)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizard.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);

        By reserveCardLocator = By.cssSelector("[data-testid='" + ChargingTestConfig.VOLUME_CARD_TESTID_RESERVE + "']");
        if (driver.findElements(reserveCardLocator).isEmpty()) {
            System.out.println("[INFO] Карточка 'Забронировать' не найдена в карусели - аккаунт всё ещё "
                    + "в кулдауне после предыдущей брони/очереди, разблокировка нужна.");
            return false;
        }

        try {
            wizard.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_RESERVE);
        } catch (RuntimeException e) {
            captureDiagnostics(driver, "reserve-card-select-failed");
            throw e;
        }
        new StationConnectorWizardPage(driver, wait).clickNextForBooking();
        return true;
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

        // Коннектор не освобождается на бэкенде мгновенно после остановки зарядки - попытка брони
        // сразу после показывает "Коннектор временно недоступен..." (подтверждено вручную
        // пользователем, 2026-08-17: ошибка пропадает примерно через 30с). Простая пауза вместо
        // retry-цикла - каждая повторная попытка брони делает реальные клики по визарду станции,
        // и в цикле это оказалось рискованнее одной паузы перед единственной попыткой.
        System.out.println("[INFO] Жду 30с, чтобы коннектор освободился на бэкенде перед повторной бронью.");
        sleep(30_000);
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

    private static final By ANY_VOLUME_CARD =
            By.cssSelector("[data-testid^='" + "charge-volume-card-" + "']");

    /**
     * ИНЦИДЕНТ 2026-08-18 (QueueTest, два аккаунта подряд разблокируются на одной станции #49):
     * если ДРУГОЙ аккаунт только что закончил свою собственную разблокирующую зарядку на этой же
     * станции, коннектор ещё не успевает освободиться на бэкенде - карусель объёма показывает
     * "Стать в очередь" вместо карточек (ни одной, даже "Полный бак"). Один retry с паузой вместо
     * немедленного падения.
     */
    private static void performMinimalUnblockCharge(WebDriver driver, WebDriverWait wait) {
        StationConnectorWizardPage unblockWizard = openWizardWaitingForFreeStation(driver, wait);
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

    /**
     * ИНЦИДЕНТ 2026-08-18: один retry с 30с паузой иногда всё ещё оказывался недостаточным (та же
     * природа, что и в {@link #attemptReserveClickWithRetries} - освобождение станции на бэкенде
     * после чужой сессии может занимать больше 30с). Несколько попыток вместо одной.
     */
    private static StationConnectorWizardPage openWizardWaitingForFreeStation(WebDriver driver, WebDriverWait wait) {
        int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            driver.get(AuthTestConfig.BASE_URL);
            StationConnectorWizardPage wizard = new StationConnectorWizardPage(driver, wait)
                    .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
            wizard.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);

            if (!driver.findElements(ANY_VOLUME_CARD).isEmpty()) {
                return wizard;
            }
            if (attempt < maxAttempts) {
                System.out.println("[INFO] Станция ещё занята чужой только что завершённой сессией - жду 30с и "
                        + "пробую снова (попытка " + (attempt + 1) + "/" + maxAttempts + ").");
                sleep(30_000);
            }
        }
        throw new IllegalStateException("Станция #49 остаётся занятой после " + maxAttempts
                + " попыток с паузами - проверьте вручную.");
    }
}
