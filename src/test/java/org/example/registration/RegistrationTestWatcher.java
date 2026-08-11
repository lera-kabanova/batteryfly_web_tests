package org.example.registration;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Пишет в консоль результат каждого теста модуля Registration: PASS/FAIL/DISABLED/ABORTED.
 * Аналог {@code org.example.auth.AuthTestWatcher} / {@code org.example.business.BusinessTestWatcher},
 * продублирован намеренно — модули не должны зависеть друг от друга.
 */
public class RegistrationTestWatcher implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.println("[PASS] " + context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        System.out.println("[FAIL] " + context.getDisplayName() + ": " + cause);
        cause.printStackTrace(System.out);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        System.out.println("[DISABLED] " + context.getDisplayName() + " - " + reason.orElse("no reason"));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        System.out.println("[ABORTED] " + context.getDisplayName() + " - " + cause);
        if (cause != null) {
            cause.printStackTrace(System.out);
        }
    }
}
