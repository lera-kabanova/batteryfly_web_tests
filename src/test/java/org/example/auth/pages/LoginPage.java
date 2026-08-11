package org.example.auth.pages;

import org.example.auth.pages.support.DisabledStateUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Страница входа batteryfly.io (Keycloak, кастомная тема). Локаторы подтверждены
 * qa-discovery/locators.md и живой проверкой 2026-07-14 через
 * tools/playwright-codegen/explore-password-toggle.js и explore-login-error-banner.js.
 */
public class LoginPage {

    private static final By EMAIL_INPUT = By.id("username");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By SUBMIT_BUTTON = By.xpath("//button[text()='Войти']");
    private static final By ERROR_BANNER = By.cssSelector("span.theme-header_description");
    // Иконка "глаз" — div.cursorPointer-BdkrJ, следующий сосед input#password внутри одного
    // display-flex-контейнера (подтверждено дампом DOM, см. explore-password-toggle.js).
    private static final By PASSWORD_VISIBILITY_TOGGLE =
            By.xpath("//input[@id='password']/following-sibling::div[contains(@class,'cursorPointer')]");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        return this;
    }

    public LoginPage typeEmail(String email) {
        WebElement emailEl = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        emailEl.clear();
        emailEl.sendKeys(email);
        return this;
    }

    public LoginPage typePassword(String password) {
        WebElement passwordEl = driver.findElement(PASSWORD_INPUT);
        passwordEl.clear();
        passwordEl.sendKeys(password);
        return this;
    }

    public void submit() {
        driver.findElement(SUBMIT_BUTTON).click();
    }

    /** Заполняет оба поля и сабмитит форму (не проверяет результат — см. assertions). */
    public void login(String email, String password) {
        typeEmail(email);
        typePassword(password);
        submit();
    }

    public boolean isLoginButtonEnabled() {
        WebElement button = driver.findElement(SUBMIT_BUTTON);
        return !DisabledStateUtil.isVisuallyDisabled(button);
    }

    /** Нативный HTML {@code disabled}-атрибут кнопки (ожидается {@code null} — см. DisabledStateUtil). */
    public String getNativeDisabledAttribute() {
        return driver.findElement(SUBMIT_BUTTON).getAttribute("disabled");
    }

    public String getErrorBannerTextOrEmpty() {
        try {
            WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_BANNER));
            return error.getText();
        } catch (TimeoutException e) {
            return "";
        }
    }

    public String getPasswordInputType() {
        return driver.findElement(PASSWORD_INPUT).getAttribute("type");
    }

    public LoginPage togglePasswordVisibility() {
        wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_VISIBILITY_TOGGLE)).click();
        return this;
    }
}
