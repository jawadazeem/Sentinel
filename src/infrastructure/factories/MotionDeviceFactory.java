package infrastructure.factories;

import devices.api.Device;
import devices.api.HardwareLink;
import devices.impl.MotionDevice;
import infrastructure.logger.Logger;

import java.util.UUID;

public class MotionDeviceFactory implements DeviceFactory {

    @Override
    public Device create(UUID Id, Logger logger, HardwareLink hardwareLink) {
        return new MotionDevice(Id, logger, hardwareLink);
    }

    @Override
    public Device create(Logger logger, HardwareLink hardwareLink) {
        return new MotionDevice(UUID.randomUUID(), logger, hardwareLink);
    }
}
