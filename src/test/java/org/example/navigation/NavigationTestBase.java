package org.example.navigation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.example.auth.AuthTestConfig;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Общая база для тестов модуля Navigation & Access Control — qa-discovery/test-modules.md,
 * модуль 5. В отличие от Registration (независимый соседний модуль), Navigation явно
 * ЗАВИСИТ от Authentication (qa-discovery/automation-roadmap.md, Phase 4), поэтому здесь
 * переиспользуются {@code org.example.auth.pages.LoginPage}/{@code AuthenticatedAreaPage} и
 * {@code AuthTestConfig} вместо дублирования формы входа.
 */
public abstract class NavigationTestBase {

    protected WebDriver driver;
    protected WebDriverWait wait;

    @RegisterExtension
    static NavigationTestWatcher TEST_WATCHER = new NavigationTestWatcher();

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUpNavigationSession() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--incognito");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDownNavigationSession() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Логинится валидными кредами общего read-only тестового аккаунта и ЖДЁТ подтверждения
     * успешного входа (приветствие), прежде чем возвращать управление тесту. Без этого ожидания
     * последующий driver.get() на другой маршрут иногда стартует до завершения OIDC-редиректа
     * (гонка состояний) — подтверждено 2026-07-16 (см. NavigationAuthenticatedRoutesTest).
     */
    protected AuthenticatedAreaPage loginAsValidUser() {
        LoginPage loginPage = new LoginPage(driver, wait).open(AuthTestConfig.BASE_URL);
        loginPage.login(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);

        AuthenticatedAreaPage authenticatedArea = new AuthenticatedAreaPage(driver, wait);
        authenticatedArea.isWelcomeVisible();
        return authenticatedArea;
    }
}
