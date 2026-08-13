package org.example.charging;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.example.auth.AuthTestConfig;
import org.example.auth.pages.AuthenticatedAreaPage;
import org.example.auth.pages.LoginPage;
import org.example.charging.pages.StationConnectorWizardPage;
import org.example.charging.pages.support.BookingUnblockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Общая база для тестов модуля Charging — qa-discovery/test-modules.md, модуль 8.
 * Явно зависит от Authentication (переиспользует {@code org.example.auth.pages.LoginPage})
 * и от минимального Station/Connector Page Object, реализованного внутри этого же пакета
 * (полноценный модуль Station Detail & Connector Selection не реализован — вне рамок задачи).
 */
public abstract class ChargingTestBase {

    protected WebDriver driver;
    protected WebDriverWait wait;

    @RegisterExtension
    static ChargingTestWatcher TEST_WATCHER = new ChargingTestWatcher();

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUpChargingSession() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--incognito");
        // Карусель зависит от geometry контейнера (getBoundingClientRect в touch-свайпе) -
        // без фиксированного размера окна ChromeDriver стартует с маленьким дефолтным viewport,
        // что могло быть причиной несработавшего свайпа при выборе "Зарядить на 80%" (2026-07-16).
        options.addArguments("--window-size=1280,900");

        options.addArguments("--lang=ru-RU");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", "ru-RU,ru");
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDownChargingSession() {
        if (driver != null) {
            driver.quit();
        }
    }

    /** Логинится валидными кредами общего read-only тестового аккаунта, ждёт подтверждения входа. */
    protected AuthenticatedAreaPage loginAsValidUser() {
        return loginAs(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);
    }

    protected AuthenticatedAreaPage loginAsUserWithNullBalance() {
        return loginAs(AuthTestConfig.EMAIL_NULL_BALANCE, AuthTestConfig.VALID_PASSWORD);
    }

    /**
     * Логинится вторым реальным аккаунтом ({@code cinemawebwelcome@gmail.com}) — используется для
     * multi-user/booking сценариев, где нужен положительный баланс (в отличие от VALID_EMAIL,
     * у которого баланс 0 BYN на 2026-07-24 после серии платных CHG-FULL-* прогонов).
     */
    protected AuthenticatedAreaPage loginAsSecondUser() {
        return loginAs(AuthTestConfig.USER_EMAIL_CINEMA, AuthTestConfig.SECOND_USER_PASSWORD);
    }

    private AuthenticatedAreaPage loginAs(String email, String password) {
        LoginPage loginPage = new LoginPage(driver, wait).open(AuthTestConfig.BASE_URL);
        loginPage.login(email, password);

        AuthenticatedAreaPage authenticatedArea = new AuthenticatedAreaPage(driver, wait);
        authenticatedArea.isWelcomeVisible();
        return authenticatedArea;
    }

    /** Открывает станцию-эмулятор #49 и выбирает коннектор CCS2 — ещё БЕЗ трат денег. */
    protected StationConnectorWizardPage openStationWizard() {
        StationConnectorWizardPage wizard = new StationConnectorWizardPage(driver, wait)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizard.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        return wizard;
    }

    /**
     * Делает booking-тесты независимыми от порядка запуска и друг от друга — см.
     * {@link BookingUnblockHelper}. Вызывать ПЕРЕД тем, как тест делает свою реальную бронь, уже
     * залогинившись нужным аккаунтом.
     */
    protected void ensureAccountCanBook() {
        BookingUnblockHelper.ensureAccountCanBook(driver, wait);
    }
}
