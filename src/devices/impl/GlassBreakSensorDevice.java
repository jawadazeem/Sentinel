package devices.impl;

import devices.api.HardwareLink;
import devices.model.DeviceType;
import infrastructure.logger.LogLevel;
import infrastructure.logger.Logger;
import infrastructure.subscribers.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GlassBreakSensorDevice extends BaseDevice {
    private final List<Subscriber> subscriberList = new ArrayList<>();
    private int signalStrength = -70;
    private int batteryLife = 80;

    public GlassBreakSensorDevice(UUID id, Logger logger, HardwareLink hardwareLink) {
        super(id, logger, hardwareLink);
    }

    public GlassBreakSensorDevice(Logger logger, HardwareLink hardwareLink) {
        this(UUID.randomUUID(), logger, hardwareLink);
    }

    @Override
    public void addSubscriber(Subscriber s) {
        subscriberList.add(s);
    }

    @Override
    public void removeSubscriber(Subscriber s) {
        if (subscriberList.contains(s)) {
            subscriberList.remove(s);
        } else {
            logger.log(s + " was tried to be removed but it was not a subscriber", LogLevel.INFO);
        }
    }

    @Override
    public void updateSubscriber(Subscriber s, String updateMessage) {
        s.receiveUpdate(updateMessage);
        logger.log(updateMessage, LogLevel.INFO);
    }

    @Override
    public void updateAllSubscribers(String updateMessage) {
        for (Subscriber s : subscriberList) {
            s.receiveUpdate(updateMessage);
        }

        logger.log(updateMessage, LogLevel.INFO);
    }

    @Override
    public void setBatteryLife(int batteryLife) {
        if (batteryLife >=0 && batteryLife <= 100) {
            this.batteryLife = batteryLife;
        } else {
            System.err.println("Battery level must be within range 0-100 inclusive.");
        }
    }

    @Override
    public boolean isBatteryFull() {
        return batteryLife == 100;
    }

    @Override
    public boolean isBatteryEmpty() {
        return batteryLife == 0;
    }

    @Override
    public UUID getId() {
        return Id;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.GLASS_BREAK_SENSOR_DEVICE;
    }

    @Override
    public int getSignalStrength() {
        return signalStrength;
    }

    @Override
    public void setSignalStrength(int signalStrength) {
        if (signalStrength > 0) {
            System.err.println("Signal strength must be greater than zero for a glass break sensor device.");
        }
        this.signalStrength = signalStrength;
    }

    @Override
    public int getBatteryLife() {
        return batteryLife;
    }

    @Override
    public void performSelfCheck() {
        if (!ping()) {
            logger.log("CRITICAL: PING to Glass Break Sensor Device (" + Id + ") Failed", LogLevel.CRITICAL);
        }

        if (signalStrength < -100) {
            logger.log("CRITICAL: Signal strength is very weak on Glass Break Sensor Device (" + Id + ")", LogLevel.CRITICAL);
        }

        if (batteryLife <= 10) {
            logger.log("CRITICAL: Battery less than 10% on Glass Break Sensor Device (" + Id + ")", LogLevel.CRITICAL);
        }
    }

    @Override
    public String toString() {
        return "GlassBreakSensorDevice{" +
                "subscriberList=" + subscriberList +
                '}';
    }
}
