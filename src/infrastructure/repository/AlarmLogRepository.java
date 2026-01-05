package infrastructure.repository;

import alarm.Alarm;
import alarm.AlarmSeverity;
import alarm.AlarmStatus;
import devices.model.DeviceType;
import infrastructure.logger.LogLevel;
import infrastructure.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlarmLogRepository {
    private final Connection connection;
    private final Logger logger;

    public AlarmLogRepository(Logger logger) {
        this.logger = logger;
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public void save(Alarm alarm) {
        String sql = """
        INSERT INTO alarm_logs (id, device_type, device_id, alarm_status, alarm_severity, timestamp)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            alarm_status = excluded.alarm_status,
            timestamp = excluded.timestamp
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, alarm.getId().toString());
            ps.setString(2, alarm.getDeviceType().toString());
            ps.setString(3, alarm.getDeviceId().toString());
            ps.setString(4, alarm.getStatus().toString());
            ps.setString(5, alarm.getSeverity().toString());
            ps.setString(6, alarm.getTimestamp().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log("Database Save Error: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    private Alarm mapRowToAlarm(ResultSet rs) throws SQLException {
        return new Alarm(
            UUID.fromString(rs.getString("id")),
            DeviceType.valueOf(rs.getString("device_type")),
            UUID.fromString(rs.getString("device_id")),
            AlarmStatus.valueOf(rs.getString("alarm_status")),
            AlarmSeverity.valueOf(rs.getString("alarm_severity")),
            LocalDateTime.parse(rs.getString("timestamp"))
        );
    }

    public List<Alarm> loadAll() {
        String sql = "SELECT * FROM alarm_logs";
        List<Alarm> alarms = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                alarms.add(mapRowToAlarm(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return alarms;
    }

    public List<Alarm> loadAllActiveAlarms() {
        String sql = "SELECT * FROM alarm_logs WHERE alarm_status = 'ACTIVE'";
        List<Alarm> alarms = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                alarms.add(mapRowToAlarm(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return alarms;
    }
}
