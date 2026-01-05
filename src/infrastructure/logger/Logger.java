package infrastructure.logger;

import java.util.List;

public interface Logger {
    void log(String update, LogLevel level);
    List<String> getLogs();
    void registerListener(LogListener logListener);
    void removeListener(LogListener logListener);
}
