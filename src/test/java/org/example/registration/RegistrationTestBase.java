package org.example.registration;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.example.registration.pages.RegistrationPage;
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

public abstract class RegistrationTestBase {

    protected WebDriver driver;
    protected WebDriverWait wait;

    @RegisterExtension
    static RegistrationTestWatcher TEST_WATCHER = new RegistrationTestWatcher();

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUpRegistrationSession() {
        driver = new ChromeDriver(freshChromeOptions());
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDownRegistrationSession() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected static ChromeOptions freshChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--incognito");

        options.addArguments("--lang=ru-RU");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", "ru-RU,ru");
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    protected RegistrationPage openRegistrationPage() {
        return new RegistrationPage(driver, wait).openFromLoginForm(RegistrationTestConfig.BASE_URL);
    }
}
