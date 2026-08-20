package org.example.charging.pages.support;

import org.example.charging.ChargingTestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Карусель выбора объёма зарядки ИЛИ способа оплаты. Порт проверенной боем логики
 * {@code org.example.ChargingTest.swipeIfNeededUntilVisible} — карусель эмулирует нативные
 * touch-жесты через {@code JavascriptExecutor}, обычный {@code scrollIntoView()}/wheel() здесь
 * не работает. Активная карточка определяется через inline {@code style="...scaleY(1)..."}.
 * <p>
 * Карточки находятся по {@code data-testid} (например {@code charge-volume-card-80-percent},
 * {@code payment-method-card}) — подтверждено живой проверкой
 * tools/playwright-codegen/explore-full-testid-dump.js, 2026-07-22. Это ЕДИНСТВЕННОЕ подтверждённое
 * живой проверкой место на сайте с data-testid (см. qa-discovery/locators.md — попытка 2026-08-13
 * применить testid из внешнего списка к кнопкам "Войти"/"Далее" провалилась вживую, элементы не
 * найдены), и оно даёт СТРОГО ОДНОЗНАЧНЫЙ элемент на карточку, в отличие от поиска по видимому
 * тексту (у нескольких вложенных узлов может быть один и тот же {@code textContent}).
 * <p>
 * Структура вокруг карточки (подтверждено той же проверкой) везде одинаковая и фиксированная:
 * {@code [data-testid=...]} → родитель ("item-zplpI", несёт {@code transform: scaleY(...)},
 * определяющий активность карточки) → дед ("container-zHUzO", сам свайпаемый контейнер карусели).
 * Так как на экране ДВЕ разные карусели с одинаковым классом container-zHUzO (объём и оплата),
 * важно брать контейнер именно СВОЕЙ карточки (через .parentElement.parentElement от неё), а не
 * первый попавшийся на странице с этим классом.
 */
public class VolumePaymentCarouselComponent {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public VolumePaymentCarouselComponent(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isCardActive(WebElement card) {
        String style = card.getAttribute("style");
        return style != null && style.contains("scaleY(1)");
    }

    private static final By CUSTOM_KWH_INPUT = By.cssSelector("input.inputPower-wi2Wf");
    private static final By CUSTOM_BYN_INPUT = By.cssSelector("input.inputSum-iUJek");

    /**
     * Выбирает карточку "Свои условия" ({@link ChargingTestConfig#VOLUME_CARD_TESTID_CUSTOM}) и
     * вводит объём в kWh — сумма в BYN пересчитывается автоматически. Подтверждено живой проверкой
     * 2026-08-17: пересчёт срабатывает по потере фокуса (blur), НЕ мгновенно при вводе — без
     * {@code Keys.TAB} после ввода поле BYN остаётся {@code 0}, и кнопка "Далее" остаётся disabled.
     * Ждём именно изменения значения BYN, а не фиксированную паузу, чтобы не гадать с таймингом.
     */
    public void selectCustomKwh(String kwh) {
        selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_CUSTOM);
        WebElement kwhInput = driver.findElement(CUSTOM_KWH_INPUT);
        kwhInput.click();
        kwhInput.sendKeys(kwh);
        kwhInput.sendKeys(Keys.TAB);
        wait.until(d -> !"0".equals(d.findElement(CUSTOM_BYN_INPUT).getAttribute("value")));
    }

    /**
     * Находит карточку по data-testid, свайпает её собственную карусель при необходимости, затем кликает.
     * <p>
     * ИНЦИДЕНТ 2026-08-18 (QueueTest): при отладке падения на карточке "Забронировать" сначала
     * казалось, что карусель виртуализирована и далёкие карточки просто не рендерятся в DOM - но
     * живая проверка это опровергла: на свободной небло­кированной станции ВСЕ 4 карточки объёма
     * (включая "Забронировать") присутствуют в DOM сразу, без единого свайпа. Настоящая причина
     * отсутствия карточки "Забронировать" - аккаунт в кулдауне после предыдущей брони/очереди (см.
     * {@code BookingUnblockHelper.attemptReserveClick}, которая теперь явно проверяет и обрабатывает
     * этот случай ДО вызова этого метода). Свайп-поиск ниже - защитный запасной вариант на случай
     * действительно медленного рендера карточки, а не основной механизм для этого сценария.
     */
    public void selectByTestId(String testId) {
        By locator = By.cssSelector("[data-testid='" + testId + "']");
        List<WebElement> existing = driver.findElements(locator);
        if (!existing.isEmpty()) {
            swipeUntilActiveThenClick(existing.get(0));
            return;
        }

        String prefix = testId.startsWith("payment-method") ? "payment-method-" : "charge-volume-card-";
        WebElement anyCardOfSameCarousel = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid^='" + prefix + "']")));
        WebElement container = anyCardOfSameCarousel.findElement(By.xpath("../.."));

        // ИНЦИДЕНТ 2026-08-18: 12 попыток (тот же лимит, что и для активации УЖЕ отрисованной
        // карточки) оказалось недостаточно для ОБНАРУЖЕНИЯ дальней карточки в DOM - похоже, каждый
        // "логический" переход между соседними карточками сам иногда требует нескольких повторов
        // жеста, а тут нужно проехать несколько карточек подряд ("Полный бак" -> ... -> "Забронировать").
        // Увеличено с запасом.
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int maxDiscoveryAttempts = 40;
        int attempts = 0;
        List<WebElement> found = driver.findElements(locator);
        while (found.isEmpty() && attempts < maxDiscoveryAttempts) {
            swipeOnce(js, container);
            sleep(1300);
            found = driver.findElements(locator);
            attempts++;
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("Карточка '" + testId + "' так и не появилась в DOM после "
                    + maxDiscoveryAttempts + " попыток свайпа карусели вслепую.");
        }
        swipeUntilActiveThenClick(found.get(0));
    }

    /**
     * ИНЦИДЕНТ 2026-07-24: если карточка так и не становится активной (scaleY(1)) после всех
     * попыток свайпа, метод раньше МОЛЧА кликал по ней всё равно - а клик по НЕактивной карточке
     * ничего не выбирает (см. класс-javadoc), из-за чего реально оставался выбран ПРЕДЫДУЩИЙ
     * активный режим (например "Полный бак" вместо "Забронировать" - карточка брони последняя,
     * 4-я, дальше всех от дефолтного "Полный бак", и 6 попыток свайпа иногда не хватало её
     * докрутить). Тест при этом не падал сразу - падал заметно позже и непонятно на экране
     * "Начните зарядку"/поиске кнопки "Далее" уже в неправильном контексте. Теперь: попыток
     * больше, и если карточка ВСЁ РАВНО не активна после финального клика - бросаем понятную
     * ошибку сразу, а не продолжаем с неверно выбранной карточкой.
     * <p>
     * ИНЦИДЕНТ 2026-08-18: 12 попыток снова оказалось недостаточно - карусель после разблокирующей
     * зарядки открылась не с дефолтного "Полный бак", а с "Зарядить на 80%", и расстояние до
     * "Забронировать" оказалось больше обычного (застряла на scaleY(0.4) после всех 12 попыток).
     * Расстояние до целевой карточки зависит от того, с какой карточки старт, поэтому лимит должен
     * быть таким же щедрым, как и в свайпе-поиске карточки в {@link #selectByTestId}.
     */
    private void swipeUntilActiveThenClick(WebElement targetElement) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement cardContainer = targetElement.findElement(By.xpath(".."));
        WebElement container = cardContainer.findElement(By.xpath(".."));

        String style = cardContainer.getAttribute("style");
        if (style != null && style.contains("scaleY(1)")) {
            js.executeScript("arguments[0].click();", targetElement);
            return;
        }

        int maxAttempts = 40;
        int attempts = 0;
        while (attempts < maxAttempts) {
            style = cardContainer.getAttribute("style");
            if (style != null && style.contains("scaleY(1)")) {
                break;
            }
            swipeOnce(js, container);
            sleep(1300);
            attempts++;
        }

        style = cardContainer.getAttribute("style");
        if (style == null || !style.contains("scaleY(1)")) {
            throw new IllegalStateException("Карточка карусели так и не стала активной после "
                    + maxAttempts + " попыток свайпа (style=" + style + ") - клик по неактивной "
                    + "карточке ничего не выбрал бы, оставив выбранным предыдущий активный режим.");
        }
        js.executeScript("arguments[0].click();", targetElement);
        sleep(500);
    }

    /**
     * Один эмулированный touch-свайп контейнера карусели справа налево (следующая карточка).
     * <p>
     * ИНЦИДЕНТ 2026-08-18: свайп на 7% ширины контейнера (0.6 -> 0.53) иногда НЕ засчитывался
     * сайтом как настоящий свайп - карточка "Забронировать" застревала на scaleY(0.4) даже после
     * 40 попыток подряд. Похоже, у карусели есть порог по дистанции/скорости, ниже которого жест
     * игнорируется, а синтетические touch-события выполняются мгновенно (без естественной паузы
     * между touchstart/touchmove человека), из-за чего короткий жест не всегда проходит порог.
     * Увеличена дистанция до 80% ширины и добавлена промежуточная точка touchmove, чтобы жест
     * увереннее засчитывался как свайп с первого-второго раза, а не полагаться на количество попыток.
     */
    private void swipeOnce(JavascriptExecutor js, WebElement container) {
        js.executeScript(
                "var el = arguments[0];"
                        + "var rect = el.getBoundingClientRect();"
                        + "var startX = rect.left + (rect.width * 0.9);"
                        + "var midX = rect.left + (rect.width * 0.5);"
                        + "var endX = rect.left + (rect.width * 0.1);"
                        + "var centerY = rect.top + (rect.height / 2);"
                        + "var tStart = new Touch({ identifier: Date.now(), target: el, clientX: startX, clientY: centerY });"
                        + "el.dispatchEvent(new TouchEvent('touchstart', { touches: [tStart], targetTouches: [tStart], changedTouches: [tStart], bubbles: true, cancelable: true }));"
                        + "var tMid = new Touch({ identifier: Date.now(), target: el, clientX: midX, clientY: centerY });"
                        + "el.dispatchEvent(new TouchEvent('touchmove', { touches: [tMid], targetTouches: [tMid], changedTouches: [tMid], bubbles: true, cancelable: true }));"
                        + "var tMove = new Touch({ identifier: Date.now(), target: el, clientX: endX, clientY: centerY });"
                        + "el.dispatchEvent(new TouchEvent('touchmove', { touches: [tMove], targetTouches: [tMove], changedTouches: [tMove], bubbles: true, cancelable: true }));"
                        + "el.dispatchEvent(new TouchEvent('touchend', { touches: [], targetTouches: [], changedTouches: [tMove], bubbles: true, cancelable: true }));",
                container
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
