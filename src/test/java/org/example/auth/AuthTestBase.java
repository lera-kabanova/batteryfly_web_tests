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
import java.util.HashMap;
import java.util.Map;

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
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--incognito");

        options.addArguments("--lang=ru-RU");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", "ru-RU,ru");
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

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