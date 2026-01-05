package devices.api;

import infrastructure.subscribers.Subscriber;

public interface Observable {
    void addSubscriber(Subscriber s);
    void removeSubscriber(Subscriber s);
    void updateSubscriber(Subscriber s, String updateMessage);
    void updateAllSubscribers(String updateMessage);
}
