package infrastructure.repository;

import devices.api.Device;
import infrastructure.logger.LogLevel;
import infrastructure.logger.Logger;
import infrastructure.subscribers.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.valueOf;

public class SubscriberRepository {
    private final Connection connection;
    private final Logger logger;

    public SubscriberRepository(Logger logger) {
        this.logger = logger;
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    private Optional<Subscriber> mapRowToSubscriber(ResultSet rs) throws SQLException {

        int Id = rs.getInt("id");
        SubscriberType subscriberType = SubscriberType.valueOf(rs.getString("type"));
        SubscriberStatus subscriberStatus = SubscriberStatus.valueOf(rs.getString("status"));

        if (subscriberType.equals(SubscriberType.POLICE_STATION_LINK)) {
            return Optional.of(new PoliceStationLink (Id, subscriberStatus));
        } else if (subscriberType.equals(SubscriberType.SECURITY_TEAM_PHONE_APP_ALARM)) {
            return Optional.of(new SecurityTeamPhoneAppAlarm(Id, subscriberStatus));
        }

        return Optional.empty();
    }

    public void save(Subscriber subscriber) {
        String sql = """
        INSERT INTO subscribers (id, type, status)
        VALUES (?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            type = excluded.type,
            status = excluded.status
    """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, subscriber.getId());
            ps.setString(2, subscriber.getSubscriberType().toString());
            ps.setString(3, subscriber.getSubscriberStatus().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log("Database Save Error: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    public void remove(Subscriber subscriber) {
        String sql = "DELETE FROM subscribers WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, valueOf(subscriber.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
    }

    public List<Subscriber> loadAll() {
        String sql = "SELECT * FROM subscribers";
        List<Subscriber> subscribers = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                subscribers.add(mapRowToSubscriber(rs).orElse(null));
            }
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
        return subscribers;
    }

    public void removeAll() {
        String sql = "DELETE FROM subscribers";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
    }


}
