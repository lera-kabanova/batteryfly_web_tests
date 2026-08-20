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

    // пользователь 1227509@gmail.com - для зарядных сессий
    protected AuthenticatedAreaPage loginAsValidUser() {
        return loginAs(AuthTestConfig.VALID_EMAIL, AuthTestConfig.VALID_PASSWORD);
    }

    protected AuthenticatedAreaPage loginAsUserWithNullBalance() {
        return loginAs(AuthTestConfig.EMAIL_NULL_BALANCE, AuthTestConfig.VALID_PASSWORD);
    }

    // cinemawebwelcome@gmail.com - должен быть положительный баланс
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

    protected StationConnectorWizardPage openStationWizard() {
        StationConnectorWizardPage wizard = new StationConnectorWizardPage(driver, wait)
                .openStation(AuthTestConfig.BASE_URL, ChargingTestConfig.STATION_DEEP_LINK_PATH);
        wizard.selectConnector(ChargingTestConfig.CONNECTOR_TEXT_FRAGMENT);
        return wizard;
    }

    protected void ensureAccountCanBook() {
        BookingUnblockHelper.ensureAccountCanBook(driver, wait);
    }
}
