package org.example.auth;

import io.github.bonigarcia.wdm.WebDriverManager;
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


public abstract class AuthTestBase {

    protected WebDriver driver;
    protected WebDriverWait wait;

    @RegisterExtension
    static AuthTestWatcher TEST_WATCHER = new AuthTestWatcher();

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUpAuthSession() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--incognito");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDownAuthSession() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected LoginPage openLoginPage() {
        return new LoginPage(driver, wait).open(AuthTestConfig.BASE_URL);
    }
}
