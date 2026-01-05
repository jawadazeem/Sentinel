package infrastructure.subscribers;

public interface Subscriber {
    void receiveUpdate(String update);
    int getId();
    SubscriberStatus getSubscriberStatus();
    SubscriberType getSubscriberType();
}
