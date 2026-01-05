package infrastructure.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TimestampLogger decorates another Logger and attaches timestamps to messages.
 *
 * This implementation uses a single background consumer thread plus a bounded
 * blocking queue to deliver log notifications to registered listeners. That
 * design avoids creating many short-lived tasks or busy-waits which previously
 * caused high CPU usage and UI freezes.
 */
public class TimestampLogger extends LoggerDecorator {
    // Bounded in-memory store of recent timed logs
    private final List<String> timedLogs = new ArrayList<>();

    // Thread-safe list of listeners (low-cost iteration; safe for concurrent add/remove)
    private final CopyOnWriteArrayList<LogListener> logListeners = new CopyOnWriteArrayList<>();

    // Bounded queue for listener notifications; consumer thread will block on take()
    private final LinkedBlockingQueue<String> listenerQueue;
    private final Thread workerThread;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private static final int MAX_TIMED_LOGS = 2000;
    private static final int LISTENER_QUEUE_CAPACITY = 1000;

    public TimestampLogger(Logger logger) {
        super(logger);

        this.listenerQueue = new LinkedBlockingQueue<>(LISTENER_QUEUE_CAPACITY);

        // Single background worker consumes the queue and notifies listeners.
        workerThread = new Thread(this::processQueue, "timestamp-logger-worker");
        workerThread.setDaemon(true);
        workerThread.start();

        // Add a shutdown hook to attempt a clean shutdown when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
            try {
                workerThread.join(500);
            } catch (InterruptedException ignored) {
            }
        }));
    }

    /**
     * Background loop that takes payloads from the queue and dispatches them to listeners.
     * Uses blocking take() to avoid busy waiting and high CPU.
     */
    private void processQueue() {
        try {
            while (running.get()) {
                String payload;
                try {
                    // Block until an item is available or thread is interrupted
                    payload = listenerQueue.take();
                } catch (InterruptedException ie) {
                    // If we're asked to stop, break out; otherwise continue
                    if (!running.get()) break;
                    continue;
                }

                // Notify all listeners (CopyOnWriteArrayList is safe for concurrent modification)
                for (LogListener l : logListeners) {
                    try {
                        l.receiveLog(payload);
                    } catch (Exception e) {
                        // Listener exceptions should not stop the logger
                        System.err.println("Listener error: " + e.getMessage());
                    }
                }
            }
        } finally {
            // Drain remaining entries on shutdown (best-effort delivery)
            String payload;
            while ((payload = listenerQueue.poll()) != null) {
                for (LogListener l : logListeners) {
                    try {
                        l.receiveLog(payload);
                    } catch (Exception ignored) {
                        // ignore
                    }
                }
            }
        }
    }

    @Override
    public void log(String update, LogLevel logLevel) {
        Instant timestamp = Instant.now();

        // Delegate to the wrapped logger
        logger.log(update, logLevel);

        // Store a bounded history of timed logs
        synchronized (timedLogs) {
            if (timedLogs.size() >= MAX_TIMED_LOGS) {
                timedLogs.remove(0);
            }
            timedLogs.add(" [" + timestamp + "] " + update);
        }

        // Prepare payload and enqueue for listener notification. If the queue is full,
        // remove the oldest element to make room (drop-oldest) and retry once. This prevents
        // unbounded growth while still attempting to deliver recent logs.
        final String payload = "[" + timestamp + "] [" + logLevel + "] " + update;
        if (!listenerQueue.offer(payload)) {
            // Drop oldest and try again (best-effort)
            listenerQueue.poll();
            listenerQueue.offer(payload);
        }
    }

    /**
     * Returns a snapshot copy of the stored logs.
     */
    public List<String> getLogs() {
        synchronized (timedLogs) {
            return new ArrayList<>(timedLogs);
        }
    }

    @Override
    public void registerListener(LogListener logListener) {
        logListeners.add(logListener);
    }

    @Override
    public void removeListener(LogListener logListener) {
        logListeners.remove(logListener);
    }

    /**
     * Initiates a shutdown of the background worker.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            workerThread.interrupt();
        }
    }

    @Override
    public String toString() {
        return "TimestampLogger{logger=" + logger + "}";
    }
}
