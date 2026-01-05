package devices.api;

import devices.model.DeviceType;

import java.util.UUID;

public interface Identifiable {
    UUID getId();
    DeviceType getDeviceType();
}
