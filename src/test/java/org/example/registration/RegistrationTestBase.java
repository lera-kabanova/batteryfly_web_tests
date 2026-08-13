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
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--incognito");
        return options;
    }

    protected RegistrationPage openRegistrationPage() {
        return new RegistrationPage(driver, wait).openFromLoginForm(RegistrationTestConfig.BASE_URL);
    }
}
