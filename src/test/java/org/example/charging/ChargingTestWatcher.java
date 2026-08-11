package org.example.charging;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Пишет в консоль результат каждого теста модуля Charging: PASS/FAIL/DISABLED/ABORTED.
 * Дублирует аналогичные watcher'ы других модулей намеренно — тривиальный boilerplate.
 */
public class ChargingTestWatcher implements TestWatcher {

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
