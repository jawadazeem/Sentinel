package infrastructure.logger;

public abstract class LoggerDecorator implements Logger {
    Logger logger;

    public LoggerDecorator(Logger logger) {
        this.logger = logger;
    }
}
