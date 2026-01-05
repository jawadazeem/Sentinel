package infrastructure.factories;

import devices.api.Device;
import devices.api.HardwareLink;
import devices.impl.SmokeDevice;
import infrastructure.logger.Logger;

import java.util.UUID;

public class SmokeDeviceFactory implements DeviceFactory {

    @Override
    public Device create(UUID Id, Logger logger, HardwareLink hardwareLink) {
        return new SmokeDevice(Id, logger, hardwareLink);
    }

    @Override
    public Device create(Logger logger, HardwareLink hardwareLink) {
        return new SmokeDevice(UUID.randomUUID(), logger, hardwareLink);
    }
}
