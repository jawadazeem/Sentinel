package infrastructure.repository;

import devices.api.Device;
import devices.api.HardwareLink;
import devices.model.DeviceStatus;
import devices.model.DeviceType;
import infrastructure.factories.DeviceFactory;
import infrastructure.factories.MotionDeviceFactory;
import infrastructure.factories.SmokeDeviceFactory;
import infrastructure.factories.ThermalDeviceFactory;
import infrastructure.logger.LogLevel;
import infrastructure.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.valueOf;

public class DeviceRepository {
    private final Connection connection;
    private final Logger logger;
    private final HardwareLink hardwareLink;
    Map<DeviceType, DeviceFactory> deviceTypeDeviceFactoryMap = new HashMap<>();

    public DeviceRepository(Logger logger, HardwareLink hardwareLink) {
        this.logger = logger;
        this.hardwareLink = hardwareLink;
        this.connection = DatabaseManager.getInstance().getConnection();

        deviceTypeDeviceFactoryMap.put(DeviceType.MOTION_DEVICE, new MotionDeviceFactory());
        deviceTypeDeviceFactoryMap.put(DeviceType.SMOKE_DEVICE, new SmokeDeviceFactory());
        deviceTypeDeviceFactoryMap.put(DeviceType.THERMAL_DEVICE, new ThermalDeviceFactory());
    }

    public void save(Device device) {
        String sql = """
            INSERT INTO devices (id, type, status, battery_level)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                type = excluded.type,
                status = excluded.status,
                battery_level = excluded.battery_level
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, valueOf(device.getId()));
            ps.setString(2, valueOf(device.getDeviceType()));
            ps.setString(3, valueOf(device.getDeviceStatus()));
            ps.setDouble(4, device.getBatteryLife());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
    }

    public void remove(Device device) {
        String sql = "DELETE FROM devices WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, valueOf(device.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
    }

    public void removeAll() {
        String sql = "DELETE FROM devices";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
    }

    public Device findById(String id) {
        String sql = "SELECT * FROM devices WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                UUID Id = UUID.fromString(rs.getString("id"));
                DeviceType deviceType = DeviceType.valueOf(rs.getString("type"));
                DeviceStatus deviceStatus = DeviceStatus.valueOf(rs.getString("status"));
                int batteryLife = rs.getInt("battery_level");

                UUID idFromDb = UUID.fromString(rs.getString("id"));
                int batt = rs.getInt("battery_level");

                Device device = deviceTypeDeviceFactoryMap.get(deviceType).create(Id, logger, hardwareLink);
                device.setBatteryLife(batt);
                device.setDeviceStatus(deviceStatus);
                return device;
            }
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
        return null;
    }

    public List<Device> loadAll() {
        String sql = "SELECT * FROM devices";
        List<Device> devices = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                devices.add(mapRowToDevice(rs));
            }
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
        return devices;
     }

    private Device mapRowToDevice(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        DeviceType type = DeviceType.valueOf(rs.getString("type"));
        DeviceStatus status = DeviceStatus.valueOf(rs.getString("status"));
        int batt = rs.getInt("battery_level");

        Device device = deviceTypeDeviceFactoryMap.get(type).create(id, logger, hardwareLink);
        device.setBatteryLife(batt);
        device.setDeviceStatus(status);
        return device;
    }
}
