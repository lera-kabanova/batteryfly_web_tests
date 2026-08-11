package org.example.navigation;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Пишет в консоль результат каждого теста модуля Navigation & Access Control:
 * PASS/FAIL/DISABLED/ABORTED. Дублирует {@code org.example.auth.AuthTestWatcher} намеренно —
 * тривиальный boilerplate, а не значимая доменная логика, которую стоило бы шарить.
 */
public class NavigationTestWatcher implements TestWatcher {

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
