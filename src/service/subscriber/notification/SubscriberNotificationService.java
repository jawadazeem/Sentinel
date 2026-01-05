package service.subscriber.notification;

import infrastructure.logger.LogLevel;
import infrastructure.logger.Logger;
import infrastructure.repository.SubscriberRepository;
import infrastructure.subscribers.Subscriber;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless Service Layer
 */
public class SubscriberNotificationService {
    private final SubscriberRepository subscriberRepository;
    private final Logger logger;

    public SubscriberNotificationService(SubscriberRepository subscriberRepository, Logger logger) {
        this.logger = logger;
        this.subscriberRepository = subscriberRepository;
    }

    public void sendToAll(String update) {
        List<Subscriber> subscribers = subscriberRepository.loadAll();
        for (Subscriber s : subscribers) {
            s.receiveUpdate(update);
        }
        logger.log(update + " was sent to all registered subscribers.", LogLevel.INFO);
    }

    public void removeSubscriber(Subscriber subscriber) {
        subscriberRepository.remove(subscriber);
        logger.log(subscriber + " was removed as a subscriber", LogLevel.INFO);
    }

    public void addSubscriber(Subscriber subscriber) {
        subscriberRepository.save(subscriber);
        logger.log(subscriber + " was added as a subscriber", LogLevel.INFO);
    }

    public List<Subscriber> getSubscribers() {
        return subscriberRepository.loadAll();
    }
}
