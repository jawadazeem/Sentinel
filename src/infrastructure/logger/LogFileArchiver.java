package infrastructure.logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LogFileArchiver implements LogListener {
    final Path file;
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "logfile-archiver");
        t.setDaemon(true);
        return t;
    });

    public LogFileArchiver(String filepath) {
        this.file = Path.of(filepath);
        try {
            if (Files.notExists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            System.err.println("Unable to create log file: " + e.getMessage());
        }

        // Ensure executor is shutdown cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    writeExecutor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                writeExecutor.shutdownNow();
            }
        }));
    }

    @Override
    public void receiveLog(String update) {
    }
}
