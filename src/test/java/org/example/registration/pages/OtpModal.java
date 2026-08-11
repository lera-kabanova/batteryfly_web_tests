package org.example.registration.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * OTP-модал подтверждения телефона, появляющийся ПОСЛЕ успешной регистрации (уже внутри
 * авторизованного приложения — qa-discovery/pages/registration.md). Сайт не использует
 * нативные {@code <input>} для кода — это 4 div-блока ({@code .text-CApzR}).
 * <p>
 * ВНИМАНИЕ: {@link #enterCode(String)} не был проверен живым прогоном (модуль сознательно не
 * запускал тесты, создающие новых пользователей, — см. {@code RegistrationNewUserTest}). Если
 * {@code sendKeys} на div-блоках не сработает, потребуется JS-клик/фокус перед вводом.
 */
public class OtpModal {

    private static final By TITLE = By.xpath("//*[contains(text(),'Подтвердите номер телефона')]");
    private static final By CODE_DIGIT_BOXES = By.cssSelector(".text-CApzR");
    private static final By ERROR_BANNER = By.cssSelector("span.theme-header_description");

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
        List<WebElement> boxes = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(CODE_DIGIT_BOXES));
        for (int i = 0; i < Math.min(boxes.size(), fourDigitCode.length()); i++) {
            boxes.get(i).sendKeys(String.valueOf(fourDigitCode.charAt(i)));
        }
    }

    public String getErrorTextOrEmpty() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER)).getText();
        } catch (TimeoutException e) {
            return "";
        }
    }
}
