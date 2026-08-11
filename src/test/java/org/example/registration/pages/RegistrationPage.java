package org.example.registration.pages;

import org.example.registration.pages.support.AgreementToggleComponent;
import org.example.registration.pages.support.DisabledStateUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Страница регистрации batteryfly.io (Keycloak, кастомная тема) — оба шага на одном URL.
 * Локаторы подтверждены qa-discovery/locators.md и живой проверкой 2026-07-14
 * (tools/playwright-codegen/explore-agreement-toggles.js, explore-registration-validation.js).
 */
public class RegistrationPage {

    private static final By REGISTRATION_TAB = By.xpath("//*[text()='Регистрация']");
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By CONTINUE_BUTTON = By.xpath("//button[contains(.,'Продолжить')]");
    private static final By NAME_INPUT = By.id("firstName");
    private static final By PHONE_INPUT = By.id("user.attributes.phoneNumber");
    private static final By VALIDATION_HINT = By.cssSelector("div.float-input-hint-LoOCs");
    private static final By GLOBAL_ERROR_BANNER = By.cssSelector("span.theme-header_description");
    private static final By OFFER_LINK = By.xpath("//a[contains(.,'Публичной оферты')]");
    private static final By PRIVACY_LINK = By.xpath("//a[contains(.,'политики конфиденциальности')]");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final AgreementToggleComponent agreements;

    public RegistrationPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.agreements = new AgreementToggleComponent(driver, wait);
    }

    public RegistrationPage openFromLoginForm(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.elementToBeClickable(REGISTRATION_TAB)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        return this;
    }

    /** Заполняет email+пароль шага 1. Пароль отправляет TAB для гарантированного blur/валидации. */
    public RegistrationPage fillStep1(String email, String password) {
        WebElement emailEl = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        emailEl.clear();
        emailEl.sendKeys(email);

        WebElement passwordEl = driver.findElement(PASSWORD_INPUT);
        passwordEl.clear();
        passwordEl.sendKeys(password);
        passwordEl.sendKeys(Keys.TAB);
        return this;
    }

    public boolean isStep1SubmitEnabled() {
        return isContinueButtonEnabled();
    }

    public RegistrationPage submitStep1() {
        wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BUTTON)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(NAME_INPUT));
        return this;
    }

    /** Заполняет имя+телефон шага 2. Телефон отправляет TAB для гарантированного blur/валидации. */
    public RegistrationPage fillStep2(String name, String phone) {
        WebElement nameEl = wait.until(ExpectedConditions.visibilityOfElementLocated(NAME_INPUT));
        nameEl.clear();
        nameEl.sendKeys(name);

        WebElement phoneEl = driver.findElement(PHONE_INPUT);
        phoneEl.clear();
        phoneEl.sendKeys(phone);
        phoneEl.sendKeys(Keys.TAB);
        return this;
    }

    public AgreementToggleComponent agreements() {
        return agreements;
    }

    public boolean isFinalSubmitEnabled() {
        return isContinueButtonEnabled();
    }

    public void submitFinal() {
        wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BUTTON)).click();
    }

    private boolean isContinueButtonEnabled() {
        return !DisabledStateUtil.isVisuallyDisabled(driver.findElement(CONTINUE_BUTTON));
    }

    public List<String> getValidationHintTexts() {
        return driver.findElements(VALIDATION_HINT).stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public String getGlobalErrorTextOrEmpty() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(GLOBAL_ERROR_BANNER)).getText();
        } catch (TimeoutException e) {
            return "";
        }
    }

    public String getPublicOfferHref() {
        return driver.findElement(OFFER_LINK).getAttribute("href");
    }

    public String getPrivacyPolicyHref() {
        return driver.findElement(PRIVACY_LINK).getAttribute("href");
    }
}
