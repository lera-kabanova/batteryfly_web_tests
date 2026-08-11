package org.example.auth.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Минимальный набор элементов авторизованной области, нужных ИСКЛЮЧИТЕЛЬНО модулю
 * Authentication (подтверждение успешного входа и logout: qa-discovery/test-modules.md,
 * модуль 1). Полноценный Page Object главного экрана принадлежит отдельному модулю
 * Dashboard (qa-discovery/pages/home.md) и сознательно не реализуется здесь, чтобы не
 * выходить за рамки задачи.
 */
public class AuthenticatedAreaPage {

    private static final By WELCOME_TEXT = By.xpath("//span[text()='Добро пожаловать']");
    private static final By WELCOME_CLICKABLE_AREA = By.xpath("//span[text()='Добро пожаловать']/..");
    private static final By LOGOUT_BUTTON = By.xpath("//span[text()='Выйти']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AuthenticatedAreaPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isWelcomeVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(WELCOME_TEXT)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Клик по приветствию/аватару — реальная навигация на /profile (подтверждено живой
     * проверкой 2026-07-16, tools/playwright-codegen/explore-navigation-avatar-back.js),
     * а не выпадающее меню. Используется также модулем Navigation & Access Control.
     */
    public AuthenticatedAreaPage clickWelcomeArea() {
        wait.until(ExpectedConditions.elementToBeClickable(WELCOME_CLICKABLE_AREA)).click();
        return this;
    }

    /** Переходит на /profile кликом по приветствию и завершает выход. */
    public LoginPage logout() {
        clickWelcomeArea();
        WebElement logoutButton = wait.until(ExpectedConditions.visibilityOfElementLocated(LOGOUT_BUTTON));
        logoutButton.click();
        return new LoginPage(driver, wait);
    }
}
