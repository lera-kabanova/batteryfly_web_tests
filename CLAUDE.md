# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Selenium/JUnit 5 UI test suite (Java 17, Maven) targeting the live production site **https://batteryfly.io/** (an EV charging app). There is no application source code here — this repo only contains browser-driven end-to-end tests. Tests run against the real site over the network, not a local/mocked instance, so they are slow, order-independent by design, and can fail due to site changes, network issues, or shared test data (see `1227509@gmail.com` / `lerakab5@gmail.com` accounts reused across tests).

## Commands

```powershell
# Run all tests
mvn test

# Run one test class
mvn -Dtest=org.example.registration.RegistrationLinksTest test

# Run a single test method
mvn -Dtest=org.example.registration.RegistrationLinksTest#publicOfferLink_pointsToCorrectPdf test

# Reuse the last generated registration email instead of generating a new one
mvn -DreuseEmail=true test

# Compile only
mvn clean compile
```

There is no lint config and no CI config in this repo.

### Archived legacy flat tests

`RegisterTest.java`, `AuthTest.java`, `ChargingTest.java`, and `Profile.java` — the original, pre-Page-Object test classes described below — were moved on 2026-07-24 to `archive/legacy-flat-tests/`, **outside** `src/test/java`, so Maven no longer compiles or runs them. They are kept for reference only. Their scenarios are superseded by the `org.example.auth`, `org.example.registration`, and `org.example.charging` packages (see below); `Profile.java`'s password-change flow has no current equivalent.

## Architecture

Each test class is self-contained: it owns its own `ChromeDriver`/`WebDriverWait` setup in `@BeforeEach`/`@BeforeAll` and teardown in `@AfterEach`. There is no shared base class or page-object layer — locators and flows are duplicated per file. When fixing a selector, check whether the same flow (e.g. login) appears in multiple classes and needs updating in each.

- **`RegisterTest.java`** (archived) — registration flow against `https://batteryfly.io/`. Uses `WebDriverManager.chromedriver().setup()` for driver binary management. Generates a unique email per run (`test+<timestamp>@example.com`) and persists it to `target/last_test_email.txt`; pass `-DreuseEmail=true` to reuse the last one instead of generating a new email. Registration is a two-step form (email/password → name/phone/agreements); `agreementsLinks()` specifically checks links on the **second** step (after `fillFirstStep`), not the landing page — this was a prior bugfix (see `BUGFIX_REPORT_V1.1.md`). Saves a screenshot to `target/screenshots/agreementsLinks_page.png` on that test's run.
- **`AuthTest.java`** (archived) — login flow (valid credentials, invalid password, unregistered email) using a fixed real account (`1227509@gmail.com` / `Lera123!`). Runs Chrome with `--incognito`.
- **`ChargingTest.java`** (archived) — full end-to-end charging session flow: login → select station/connector → choose charge volume/payment method → start charging → poll charge percentage in a loop until 40% → stop → confirm. Includes `swipeIfNeededUntilVisible`, which simulates touch swipe gestures via `JavascriptExecutor`-dispatched `TouchEvent`s to scroll a carousel UI until the target card is active (`scaleY(1)` in its inline style), with a JS-click fallback. These tests can take several minutes (uses a 400s `WebDriverWait` while polling charge %).
- **`Profile.java`** (archived) — password-change flow (login → open profile → settings → change password → verify forced logout → log in with new password). See the Surefire discovery caveat above.

The current, actively-run test suite lives instead in `org.example.auth`, `org.example.registration`, `org.example.navigation`, `org.example.charging`, and `org.example.business` — each with its own page-object classes, a shared `*TestBase`/`*TestConfig`/`*TestWatcher`, and assertion helpers. See `automation/automation-checklist.md` for scenario-by-scenario coverage of these packages.

`org.example.charging` also covers Booking/Reservation and Queue (`BookingTest`, `BookingExpiryTest`, `QueueTest`, page objects `BookingConfirmationPage`/`BookingActionSheet`) and History detail + PDF (`HistoryDetailTest`, `TransactionDetailPanel`) — these are the same product feature as regular charging (reserving/queueing is a card in the same volume carousel, `charge-volume-card-reserve`), not separate modules. A second real consumer account, `AuthTestConfig.SECOND_USER_EMAIL` (`cinemawebwelcome@gmail.com`), is used for multi-user/positive-balance scenarios instead of `lerakab5@gmail.com` (the user's personal email) where possible; `QueueTest` needs two simultaneous real users and uses `lerakab5@gmail.com` for the paying/charging role specifically because `cinemawebwelcome`'s balance is currently too low for real charging (see `automation/automation-checklist.md` for balance state and known account-specific quirks, e.g. a stuck payment transaction).

### Shared conventions across tests
- Locators are mostly `By.xpath`/`By.cssSelector` matching **Russian-language UI text** (e.g. `"Войти"`, `"Продолжить регистрации"`, `"Добро пожаловать"`) — the site's primary locale is Russian.
- Some CSS class selectors are auto-generated/hashed (e.g. `div.container-pQiEc`, `div.content-g720k`) and are brittle — if a test breaks after a UI deploy, these are the first things to re-check via DevTools.
- Chrome runs non-headless by default (`--start-maximized` or default window); `--headless=new` is available but commented out in `RegisterTest`.
- No page-object model, no explicit wait constants shared across files — timeouts are set per-class (10–20s typical, up to 400s in `ChargingTest` for charge completion).
