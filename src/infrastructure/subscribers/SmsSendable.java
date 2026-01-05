package infrastructure.subscribers;

public interface SmsSendable {
    void sendLatestUpdateAsSms();
    void sendUpdateAsSms(String update);
}
