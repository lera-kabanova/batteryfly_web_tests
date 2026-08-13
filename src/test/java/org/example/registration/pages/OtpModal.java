package org.example.registration.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * OTP-модал подтверждения телефона, появляющийся ПОСЛЕ успешной регистрации (уже внутри
 * авторизованного приложения — qa-discovery/pages/registration.md). Визуально код показывается
 * как 4 отдельных div-блока ({@code .text-CApzR}), но это только ОТОБРАЖЕНИЕ — реальный ввод
 * идёт в ОДИН скрытый {@code <input type="number" maxlength="4">}, который синхронизирует все 4
 * визуальных блока. Подтверждено живой проверкой 2026-08-13 (диагностический прогон:
 * {@code sendKeys} напрямую на {@code .text-CApzR} кидал {@code ElementNotInteractableException}
 * — у div нет ни {@code contenteditable}, ни {@code tabindex}; клик в точке блока через
 * {@code Actions} реально печатал в скрытый input и обновлял видимый блок).
 * <p>
 * ИНЦИДЕНТ 2026-08-13 (заголовок): "Подтвердите номер телефона" визуально на двух строках, но
 * это ДВА РАЗНЫХ элемента без пробела между ними в string-value поддерева — ни
 * {@code contains(text(), 'Подтвердите номер телефона')} (матчит только ПЕРВЫЙ текстовый узел),
 * ни {@code contains(., '...')} (склеивает без пробела) не находили модал, хотя на скриншоте он
 * был полностью виден. Локатор сужен до одного слова "Подтвердите".
 * <p>
 * ИНЦИДЕНТ 2026-08-13 (ошибка после неверного кода): баннер ошибки здесь — ОТДЕЛЬНЫЙ компонент
 * (розовый блок с CSS-переменной {@code --color-error-background} в inline {@code style}), а НЕ
 * {@code span.theme-header_description}, который используется для баннеров ошибок на других
 * экранах (вход/шаг 1-2 регистрации, см. {@code RegistrationPage.GLOBAL_ERROR_BANNER}). У
 * элемента нет собственного стабильного class — матчим по CSS-переменной в style, затем берём
 * первый вложенный {@code span} (второй span в этом же блоке — текст кнопки "Ok").
 */
public class OtpModal {

    private static final By TITLE = By.xpath("//*[contains(text(),'Подтвердите')]");
    private static final By CODE_INPUT = By.cssSelector("input[type='number'][maxlength='4']");
    private static final By ERROR_BANNER = By.cssSelector("div[style*='color-error-background'] span");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OtpModal(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(TITLE)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void enterCode(String fourDigitCode) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(CODE_INPUT));
        input.sendKeys(fourDigitCode);
    }

    public String getErrorTextOrEmpty() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER)).getText();
        } catch (TimeoutException e) {
            return "";
        }
    }
}
