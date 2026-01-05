package infrastructure.subscribers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Receives alert messages from the SecurityHub and represents
 * the police station's connection to the system.
 *
 * This class does not control devices or trigger alarms.
 * It only records and exposes the alerts that were sent to it.
 */

public class PoliceStationLink implements Subscriber {
    private final List<String> updates = new ArrayList<>();
    private int Id;
    private SubscriberStatus subscriberStatus;
    private final SubscriberType subscriberType = SubscriberType.POLICE_STATION_LINK;

    public PoliceStationLink(int id, SubscriberStatus subscriberStatus) {
        Id = id;
        this.subscriberStatus = subscriberStatus;
    }

    public PoliceStationLink() {
        this(UUID.randomUUID().hashCode(), SubscriberStatus.ACTIVE);
    }

    @Override
    public void receiveUpdate(String update) {
        updates.add(update);
    }

    public List<String> getUpdates() {
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

    public SubscriberType getSubscriberType() {
        return subscriberType;
    }

    @Override
    public String toString() {
        return "PoliceStationLink{" +
                "updates=" + updates +
                '}';
    }
}
