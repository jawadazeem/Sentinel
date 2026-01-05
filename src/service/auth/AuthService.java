package service.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import infrastructure.repository.UserRepository;
import user.User;

public class AuthService {

    private final UserRepository userRepo;

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // Hash a plain password using Argon2id
    public String hashPassword(String password) {
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        return argon2.hash(
                3,        // rounds (how many times the work is repeated)
                65536,    // memory in KB (64 MB)
                1,        // threads
                password
        );
    }

    // Login
    public boolean authenticate(String username, String password) {
        User user = userRepo.load(username);
        if (user == null) return false;

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        return argon2.verify(user.getPasswordHash(), password);
    }

    // Register new user
    public void saveUser(String username, String password) {
        String hash = hashPassword(password);
        User user = new User(username, hash);
        userRepo.save(user);
    }

    // Delete user (password required)
    public boolean removeUser(String username, String password) {
        User user = userRepo.load(username);
        if (user == null) return false;

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        if (!argon2.verify(user.getPasswordHash(), password)) {
            return false;
        }

        userRepo.remove(user);
        return true;
    }

    // Change password (old password required)
    public boolean changeUserPassword(String username, String oldPass, String newPass) {
        User user = userRepo.load(username);
        if (user == null) return false;

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        if (!argon2.verify(user.getPasswordHash(), oldPass)) {
            return false;
        }

        String newHash = hashPassword(newPass);
        user.setPasswordHash(newHash);
        userRepo.save(user);
        return true;
    }

    public boolean hasAnyUsers() {
        return userRepo.countUsers() > 0;
    }

    public boolean userExists(String username) {
        return userRepo.load(username) != null;
    }
}
