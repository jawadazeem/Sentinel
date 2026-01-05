package user;

public class User {

    private final String username;
    private String passwordHash;   // must be mutable so it can be changed

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    // needed for password changes
    public void setPasswordHash(String newHash) {
        this.passwordHash = newHash;
    }
}