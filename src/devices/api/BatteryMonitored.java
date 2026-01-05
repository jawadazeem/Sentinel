package devices.api;

public interface BatteryMonitored {
    int getBatteryLife();
    boolean isBatteryFull();
    boolean isBatteryEmpty();
    void setBatteryLife(int batteryLife);
}
