package org.example.registration.pages.support;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Инкапсулирует ЕДИНСТВЕННЫЙ правильный способ включения обоих переключателей согласий на
 * шаге 2 регистрации. Сайт НЕ использует нативные checkbox — это div-переключатели.
 * <p>
 * BUG-001 (qa-discovery/bugs.md): старый селектор
 * {@code "div.container-pQiEc, div.switch-SXkNU"} матчит КОНТЕЙНЕР + его же вложенный child
 * на каждую строку (итого 4 элемента на 2 переключателя). Клик по всем 4 фактически кликает
 * первый переключатель дважды, а второй — ни разу; кнопка сабмита остаётся disabled.
 * <p>
 * Здесь используется ТОЛЬКО {@code div.container-pQiEc} (ровно 2 элемента), каждый кликается
 * РОВНО ОДИН РАЗ — подтверждено живой проверкой 2026-07-14
 * (tools/playwright-codegen/explore-agreement-toggles.js): после такого клика кнопка сабмита
 * реально теряет класс {@code disabled-SIwIG}.
 */
public class AgreementToggleComponent {

    private static final By TOGGLE_CONTAINERS = By.cssSelector("div.container-pQiEc");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AgreementToggleComponent(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    /** Включает оба переключателя (оферта + политика конфиденциальности), каждый одним кликом. */
    public void acceptBoth() {
        List<WebElement> toggles = driver.findElements(TOGGLE_CONTAINERS);
        for (WebElement toggle : toggles) {
            wait.until(ExpectedConditions.elementToBeClickable(toggle)).click();
        }
    }

    public int toggleCount() {
        return driver.findElements(TOGGLE_CONTAINERS).size();
    }
}
