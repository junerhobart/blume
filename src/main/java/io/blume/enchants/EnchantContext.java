package io.blume.enchants;

public final class EnchantContext {

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    private EnchantContext() {}

    public static boolean isProcessing() {
        return PROCESSING.get();
    }

    public static void runWhileProcessing(Runnable action) {
        boolean previous = PROCESSING.get();
        PROCESSING.set(true);
        try {
            action.run();
        } finally {
            PROCESSING.set(previous);
        }
    }
}
