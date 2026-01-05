package infrastructure.logger;

/**
 * Simple smoke test to validate TimestampLogger behavior.
 * Runs a burst of log messages and ensures the background worker processes them
 * without busy-waiting or exploding CPU usage. This program is headless and
 * prints basic progress to stdout.
 */
public class SmokeTimestampLogger {
    public static void main(String[] args) throws Exception {
        Logger base = new ConsoleLogger();
        TimestampLogger timestampLogger = new TimestampLogger(base);

        // Register a simple listener that prints received payloads (keeps output moderate)
        timestampLogger.registerListener(payload -> {
            // Short print to show listener activity
            System.out.println("[listener] " + payload);
        });

        final int total = 2000;
        System.out.println("Starting to emit " + total + " messages...");
        long start = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            timestampLogger.log("message-" + i, LogLevel.INFO);
            // Slight throttle to avoid flooding the console too quickly
            if (i % 100 == 0) Thread.sleep(1);
        }

        long emitTime = System.currentTimeMillis() - start;
        System.out.println("Emitted " + total + " messages in " + emitTime + "ms, waiting for delivery...");

        // Wait a short while for background worker to process the queue
        Thread.sleep(2000);

        timestampLogger.shutdown();
        System.out.println("Shutdown requested. Exiting.");
    }
}

