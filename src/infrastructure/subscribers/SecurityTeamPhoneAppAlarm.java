package infrastructure.subscribers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class SecurityTeamPhoneAppAlarm implements Subscriber, SmsSendable {
    private final Deque<String> updates = new ArrayDeque<>();
    private int Id;
    private SubscriberStatus subscriberStatus;
    private final SubscriberType subscriberType = SubscriberType.SECURITY_TEAM_PHONE_APP_ALARM;

    public SecurityTeamPhoneAppAlarm(int id, SubscriberStatus subscriberStatus) {
        Id = id;
        this.subscriberStatus = subscriberStatus;
    }

    public SecurityTeamPhoneAppAlarm() {
        this(UUID.randomUUID().hashCode(), SubscriberStatus.ACTIVE);
    }

    @Override
    public void receiveUpdate(String update) {
        updates.add(update);
    }

    @Override
    public void sendLatestUpdateAsSms() {
        String latestUpdate = updates.peek();
        // TODO: Inject SmsService as dependency and implement SMS logic.
    }

    @Override
    public void sendUpdateAsSms(String update) {
        // TODO: Inject SmsService as dependency and implement SMS logic.
    }

    public Deque<String> getUpdates() {
        return updates;
    }

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public SubscriberStatus getSubscriberStatus() {
        return subscriberStatus;
    }

    @Override
    public SubscriberType getSubscriberType() {
        return subscriberType;
    }

    @Override
    public String toString() {
        return "SecurityTeamPhoneAppAlarm{" +
                "updates=" + updates +
                '}';
    }
}
