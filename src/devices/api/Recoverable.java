package devices.api;

public interface Recoverable {
    void incrementFailureCount();
    void resetFailureCount();
    int getFailureCount(); }