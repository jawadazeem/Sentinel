package devices.api;

import devices.model.DeviceStatus;

public interface Device extends Identifiable, Pingable, BatteryMonitored, Recoverable, Observable, Connectable {

    void performSelfCheck();
    void setDeviceStatus(DeviceStatus deviceStatus);
    DeviceStatus getDeviceStatus();
}
