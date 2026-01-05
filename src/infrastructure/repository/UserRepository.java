package infrastructure.repository;

import devices.api.Device;
import infrastructure.logger.LogLevel;
import infrastructure.logger.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import user.User;

public class UserRepository {
    private final Connection connection;
    private final Logger logger;

    public UserRepository(Logger logger) {
        this.logger = logger;
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public void save(User user) {
        String sql = """
        INSERT INTO users (username, password_hash)
        VALUES (?, ?)
        ON CONFLICT(username) DO UPDATE SET
            password_hash = excluded.password_hash
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log("Database Save Error: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    public void remove(User user) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
    }

    public User load(String username) {
        String sql = "SELECT username, password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String usernameFromDb = rs.getString("username");
                String passwordHashFromDb = rs.getString("password_hash");
                return new User(usernameFromDb, passwordHashFromDb);
            }
        } catch (SQLException e) {
            logger.log("SQL Exception in load", LogLevel.ERROR);
        }
        return null;
    }

    public String getPasswordHashFromDatabase(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password_hash");
            }
            return null;
        } catch (SQLException e ) {
            logger.log("SQL Exception in load", LogLevel.ERROR);
        }
        return null;
    }

    public void setPasswordHash(String username, String newHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log("SQL Exception in setPasswordHash: " + e.getMessage(), LogLevel.ERROR);
        }
    }

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log("SQL Exception in countUsers: " + e.getMessage(), LogLevel.ERROR);
        }
        return 0;
    }
}
