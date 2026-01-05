package infrastructure.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

public class ConsoleLogger implements Logger {
    // Bounded in-memory log buffer to avoid unbounded memory growth
    final List<String> logs = new ArrayList<>();
    final List<LogListener> logListeners = new ArrayList<>();

    // Single-threaded executor to offload listener I/O so callers to log() are not blocked
    private final ThreadPoolExecutor listenerExecutor;

    private static final int LISTENER_QUEUE_CAPACITY = 2000;

    private static ThreadFactory daemonFactory(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }

    private static final int MAX_LOG_ENTRIES = 1000;

    public ConsoleLogger() {
        // Single-threaded executor with bounded queue to avoid unbounded task accumulation.
        listenerExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(LISTENER_QUEUE_CAPACITY),
                daemonFactory("logger-listener-dispatch"));
        listenerExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // Ensure executor is shutdown when JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            listenerExecutor.shutdown();
            try {
                if (!listenerExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    listenerExecutor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                listenerExecutor.shutdownNow();
            }
        }));
    }

    @Override
    public void log(String update, LogLevel logLevel) {
        String formatted = "[" + logLevel + "] " + update;

        // Keep log buffer bounded to prevent OOM
        List<LogListener> snapshot;
        synchronized (this) {
            if (logs.size() >= MAX_LOG_ENTRIES) {
                logs.remove(0);
            }
            logs.add(formatted);
            // Snapshot listeners while holding lock to avoid concurrency issues
            snapshot = new ArrayList<>(logListeners);
        }

        // Dispatch to listeners asynchronously to avoid high CPU / blocking on slow listeners (file I/O, UI updates)
        for (LogListener l : snapshot) {
            try {
                listenerExecutor.execute(() -> {
                    try {
                        l.receiveLog(formatted);
                    } catch (Throwable t) {
                        // Protect the logging system from failing listeners
                        System.err.println("Log listener error: " + t.getMessage());
                    }
                });
            } catch (Throwable rejected) {
                // If executor queue is full, drop this notification to avoid memory/CPU surge.
                System.err.println("Dropping log listener notification due to backpressure");
            }
        }
    }

    @Override
    public synchronized List<String> getLogs() {
        // Return a copy to avoid exposing internal mutable list
        return new ArrayList<>(logs);
    }

    @Override
    public synchronized void registerListener(LogListener logListener) {
        logListeners.add(logListener);
    }

    @Override
    public synchronized void removeListener(LogListener logListener) {
        logListeners.remove(logListener);
    }
}
