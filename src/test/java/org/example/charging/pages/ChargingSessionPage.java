package org.example.charging.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран активной зарядки (/charge). Используется ТОЛЬКО в {@code ChargingSessionTest},
 * который сознательно не запускается автоматически (реальные деньги, реальное время ожидания).
 * Порт проверенной боем логики из {@code org.example.ChargingTest.waitForChargeAndStop}.
 */
public class ChargingSessionPage {

    private static final By STOP_BUTTON = By.xpath("//button[text()='Остановить']");
    private static final By CONFIRM_YES = By.xpath("//span[text()='Да']");
    private static final By CONFIRM_CANCEL = By.xpath("//span[text()='Отмена']");
    private static final By STOP_DIALOG_TEXT = By.xpath("//*[text()='Желаете остановить заправку?']");
    private static final By FINAL_TEXT = By.xpath("//span[contains(text(), 'Завершена в')]");
    private static final By KRUTO_BUTTON = By.xpath("//button[text()='Круто']");
    private static final By PERCENT_TEXTS = By.xpath("//*[contains(text(), '%')]");

    /**
     * Счётчики kW*h / kW / BYN — каждый это span-лейбл с точным текстом единицы измерения,
     * значение лежит в непосредственно предшествующем ему элементе того же родителя (подтверждено
     * живой проверкой tools/playwright-codegen/explore-charge-counters-structure.js). "[1]" в конце
     * — предикат ШАГА preceding-sibling (reverse-ось), поэтому берёт БЛИЖАЙШИЙ предшествующий
     * элемент, а не самый дальний.
     */
    private static final By KWH_VALUE = By.xpath("//span[text()='kW*h']/preceding-sibling::*[1]");
    private static final By KW_VALUE = By.xpath("//span[text()='kW']/preceding-sibling::*[1]");
    private static final By BYN_VALUE = By.xpath("//span[text()='BYN']/preceding-sibling::*[1]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public ChargingSessionPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    /** Один снимок показаний экрана /charge в момент, когда процент заряда изменился. */
    public record ChargeReading(int percent, double kWh, double kW, double byn) {
    }

    /**
     * Ждёт смены URL на /charge. ВЫЗЫВАТЬ ТОЛЬКО ПОСЛЕ того, как вызывающий код уже сохранил
     * этот объект (например в поле {@code activeSession}) - см. подробности инцидента
     * 2026-07-22 в {@code ChargingConfirmationPage#clickPayAndCharge()}: если это ожидание
     * бросит исключение раньше, чем вызывающий код получит ссылку на сессию, аварийная остановка
     * в {@code @AfterEach} не сможет её найти и остановить.
     */
    public void confirmChargeStarted() {
        new WebDriverWait(driver, Duration.ofSeconds(90)).until(ExpectedConditions.urlContains("/charge"));
    }

    /**
     * Блокирующий опрос процента заряда до достижения targetPercent, с попутным сбором показаний
     * kW*h/kW/BYN на каждое новое значение процента (порт из исходного ChargingTest.java, с
     * двумя фиксами после реального сбоя 2026-07-16):
     * <p>
     * 1. Проценты перерисовываются React'ом в реальном времени во время зарядки — обычный
     * {@code findElement()+getText()} рискует поймать {@link StaleElementReferenceException}
     * между поиском элемента и чтением текста. Раньше это исключение падало необработанным
     * ПРЯМО НА ЭКРАНЕ ЗАРЯДКИ, тест падал раньше {@link #stopAndConfirm()}, и реальная сессия
     * оставалась активной (деньги продолжали списываться). Теперь оно перехватывается и опрос
     * просто повторяется.
     * <p>
     * 2. Раньше сравнение было точным ({@code text.startsWith(targetPercent)}) — если процент
     * "перепрыгивал" через targetPercent (например 39% сразу на 41%), цикл не завершался бы
     * НИКОГДА. Теперь используется {@code >= targetPercent}.
     * <p>
     * 3. Добавлен жёсткий дедлайн (20 минут) — при любой другой непредвиденной проблеме тест
     * упадёт с понятной ошибкой вместо бесконечного зависания.
     * <p>
     * Возвращённый список — по одному {@link ChargeReading} на каждое НОВОЕ значение процента
     * (не на каждый опрос) — используется тестами для проверки монотонного роста % и
     * согласованного обновления kW*h/kW/BYN.
     */
    public List<ChargeReading> pollUntilPercentReached(int targetPercent) {
        List<ChargeReading> samples = new ArrayList<>();
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(20).toMillis();
        Integer lastPercent = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                Integer percent = readPercent();
                if (percent != null && !percent.equals(lastPercent)) {
                    lastPercent = percent;
                    samples.add(new ChargeReading(percent, readKwh(), readKw(), readByn()));
                    if (percent >= targetPercent) {
                        return samples;
                    }
                }
            } catch (StaleElementReferenceException e) {
                // Элемент перерисовался между findElement() и getText() - страница активно
                // обновляется во время зарядки, это ожидаемо. Просто повторяем опрос.
            }
            sleep(500);
        }
        throw new IllegalStateException("Зарядка не достигла " + targetPercent
                + "% за 20 минут ожидания - см. qa-discovery/charging-flows.md. "
                + "Сессия могла остаться активной, проверьте вручную!");
    }

    /**
     * Для режима "Фиксированная сумма" ({@code selectCustomKwh}) — в отличие от "Полный бак"/"80%",
     * такая сессия останавливается САМА по достижении заданного объёма, ручной клик "Остановить"
     * не нужен (подтверждено вручную пользователем, 2026-08-17). Опрашивает показания, как
     * {@link #pollUntilPercentReached}, но выходит не по достижении процента, а когда кнопка
     * "Остановить" пропадает с экрана - то есть сессия уже сама завершилась.
     */
    public List<ChargeReading> pollUntilSessionEnds() {
        List<ChargeReading> samples = new ArrayList<>();

        // ВАЖНО: сначала дожидаемся, что кнопка "Остановить" реально отрисовалась - иначе на самой
        // первой итерации ниже она "отсутствует" просто потому, что страница /charge ещё не
        // дорисовалась, а НЕ потому, что сессия уже завершилась. Без этого ожидания опрос мог
        // вернуть пустой список показаний ДО того, как прочитал хоть один процент (инцидент
        // 2026-08-17: IndexOutOfBoundsException в assertSessionAppearsInHistory на пустом списке).
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(STOP_BUTTON));
        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Кнопка \"Остановить\" так и не появилась на экране зарядки - опрос не начат.", e);
        }

        long deadline = System.currentTimeMillis() + Duration.ofMinutes(20).toMillis();
        Integer lastPercent = null;
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(STOP_BUTTON).isEmpty()) {
                return samples;
            }
            try {
                Integer percent = readPercent();
                if (percent != null && !percent.equals(lastPercent)) {
                    lastPercent = percent;
                    samples.add(new ChargeReading(percent, readKwh(), readKw(), readByn()));
                }
            } catch (StaleElementReferenceException e) {
                // Элемент перерисовался между findElement() и getText() - страница активно
                // обновляется во время зарядки, это ожидаемо. Просто повторяем опрос.
            }
            sleep(500);
        }
        throw new IllegalStateException("Зарядка 'Фиксированная сумма' не завершилась сама за 20 минут "
                + "ожидания. Сессия могла остаться активной, проверьте вручную!");
    }

    private Integer readPercent() {
        for (WebElement el : driver.findElements(PERCENT_TEXTS)) {
            String text = el.getText().trim();
            if (text.matches("\\d+\\s*%")) {
                return Integer.parseInt(text.replaceAll("[^0-9]", ""));
            }
        }
        return null;
    }

    private double readKwh() {
        return readNumeric(KWH_VALUE);
    }

    private double readKw() {
        return readNumeric(KW_VALUE);
    }

    private double readByn() {
        return readNumeric(BYN_VALUE);
    }

    /**
     * kW*h иногда отображается как "{доставлено}/{цель}" в ОДНОМ элементе (например "3.5/40"
     * - см. реальный инцидент 2026-07-22, когда наивный strip нецифровых символов из "1/40"
     * склеивал их в "140" и ломал проверку монотонности). Берём только часть ДО "/".
     */
    private double readNumeric(By locator) {
        String text = driver.findElement(locator).getText().trim().replace(",", ".");
        String delivered = text.split("/")[0];
        return Double.parseDouble(delivered.replaceAll("[^0-9.]", ""));
    }

    /**
     * Клики выполняются через JS ({@code element.click()}), а не нативным Selenium-кликом по
     * координатам - см. подробное объяснение в {@code ChargingConfirmationPage.clickPayAndCharge()}
     * (инцидент 2026-07-16 с меню "Мое авто", открывавшимся из-за клика по координатам во время
     * анимации перехода между экранами).
     */
    public void stopAndConfirm() {
        clickStop();
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(400));
        WebElement confirmYes = longWait.until(ExpectedConditions.elementToBeClickable(CONFIRM_YES));
        jsClick(confirmYes);
    }

    /** Открывает confirm-диалог остановки ("Желаете остановить заправку?" + "Да"/"Отмена"). */
    public void clickStop() {
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(400));
        WebElement stopButton = longWait.until(ExpectedConditions.elementToBeClickable(STOP_BUTTON));
        jsClick(stopButton);
    }

    /** Точный текст confirm-диалога остановки — подтверждено живой проверкой 2026-07-22. */
    public String readStopDialogText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(STOP_DIALOG_TEXT)).getText();
    }

    /** Отмена в confirm-диалоге остановки — зарядка должна продолжаться (см. {@link #isActive()}). */
    public void cancelStop() {
        WebElement cancelButton = wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_CANCEL));
        jsClick(cancelButton);
    }

    /** Кнопка "Остановить" всё ещё на экране => сессия зарядки активна (не остановлена/не завершена). */
    public boolean isActive() {
        return !driver.findElements(STOP_BUTTON).isEmpty();
    }

    /**
     * "Круто" закрывает финальный экран ПОСЛЕ появления текста "Завершена в HH:MM" - подтверждено
     * живой проверкой 2026-07-22: клик до появления этого текста просто уводит на главный экран,
     * не дожидаясь финализации. Вызывать только после {@link #waitForFinalText()}.
     */
    public void clickKrutoToFinish() {
        // Как и clickStop()/stopAndConfirm() - обычного короткого wait иногда не хватает под
        // нагрузкой (см. QueueTest, где одновременно работают два реальных браузера): backend
        // может отвечать медленнее, чем в одиночных тестах.
        // ВАЖНО (инцидент 2026-08-17): в QueueTest этого 400-секундного ожидания оказалось
        // НЕДОСТАТОЧНО - таймаут повторился и на полных 400с. Это значит, что дело не в скорости
        // бэкенда, а в том, что кнопка "Круто" в сценарии с параллельной очередью, возможно,
        // вообще не появляется в ожидаемом виде. Поэтому при таймауте дампим скриншот и текст
        // страницы, чтобы увидеть, что реально на экране, а не гадать вслепую.
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(400));
        try {
            WebElement kruto = longWait.until(ExpectedConditions.elementToBeClickable(KRUTO_BUTTON));
            jsClick(kruto);
        } catch (TimeoutException e) {
            dumpDiagnostics("kruto-button-not-clickable");
            throw e;
        }
    }

    private void dumpDiagnostics(String name) {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target", "screenshots"));
            java.io.File src = ((org.openqa.selenium.TakesScreenshot) driver)
                    .getScreenshotAs(org.openqa.selenium.OutputType.FILE);
            java.nio.file.Files.copy(src.toPath(), java.nio.file.Paths.get("target", "screenshots", name + ".png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String body = driver.findElement(By.tagName("body")).getText();
            java.nio.file.Files.writeString(java.nio.file.Paths.get("target", name + "-body.txt"),
                    "URL: " + driver.getCurrentUrl() + "\n\n" + body, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception diagError) {
            System.out.println("[WARN] Не удалось сохранить диагностику '" + name + "': " + diagError);
        }
    }

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Аварийная остановка — best-effort, вызывается из {@code @AfterEach} тестов независимо от
     * того, где именно упал тест. Если кнопки "Остановить" уже нет (сессия и так не активна или
     * уже остановлена штатно) - ничего не делает. Введено после реального инцидента 2026-07-16,
     * когда упавший тест оставил настоящую зарядную сессию активной.
     * <p>
     * Дополнительно (инцидент 2026-07-22) дожидается финализации и кликает "Круто". Раньше
     * аварийная остановка заканчивалась на "Да" (деньги переставали списываться, но станция
     * оставалась занятой). Пока сессия не финализирована кликом "Круто", станция #49 продолжает
     * считаться занятой - следующий тест, пытающийся начать новую зарядку, вместо обычного
     * визарда видит экран "Стать в очередь" и падает на поиске data-testid карточек объёма. В
     * одном реальном прогоне так упали ВСЕ тесты подряд после первого, аварийно остановленного
     * без клика "Круто".
     */
    public void emergencyStopIfActive() {
        try {
            if (driver.findElements(STOP_BUTTON).isEmpty()) {
                return;
            }
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
            jsClick(shortWait.until(ExpectedConditions.elementToBeClickable(STOP_BUTTON)));
            jsClick(shortWait.until(ExpectedConditions.elementToBeClickable(CONFIRM_YES)));
            System.out.println("[CLEANUP] Аварийно остановлена активная зарядная сессия.");

            try {
                WebDriverWait finalizeWait = new WebDriverWait(driver, Duration.ofSeconds(400));
                finalizeWait.until(ExpectedConditions.visibilityOfElementLocated(FINAL_TEXT));
                jsClick(finalizeWait.until(ExpectedConditions.elementToBeClickable(KRUTO_BUTTON)));
                System.out.println("[CLEANUP] Финализация завершена, нажата \"Круто\" - станция освобождена.");
            } catch (Exception finalizeError) {
                System.out.println("[WARN] Не удалось дождаться финализации/нажать \"Круто\" в @AfterEach: "
                        + finalizeError + " - СЛЕДУЮЩИЙ ТЕСТ МОЖЕТ УПАСТЬ ИЗ-ЗА ЗАНЯТОЙ СТАНЦИИ (деньги уже "
                        + "не списываются, это не денежный риск, а риск ложного падения следующего теста).");
            }
        } catch (Exception e) {
            System.out.println("[WARN] Не удалось аварийно остановить зарядку в @AfterEach: " + e
                    + " - ПРОВЕРЬТЕ ВРУЧНУЮ, СЕССИЯ МОЖЕТ БЫТЬ АКТИВНА.");
        }
    }

    /** Ждёт финальный текст "Завершена в HH:MM" — появляется не сразу, нужен долгий wait. */
    public boolean waitForFinalText() {
        try {
            WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(400));
            return longWait.until(ExpectedConditions.visibilityOfElementLocated(FINAL_TEXT)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
