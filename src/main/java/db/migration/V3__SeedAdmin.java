package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class V3__SeedAdmin extends BaseJavaMigration {
    public void migrate(Context context) throws Exception {
        String username = context.getConfiguration().getPlaceholders().get("admin_username");
        String password = context.getConfiguration().getPlaceholders().get("admin_password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("admin_username and admin_password Flyway placeholders are required");
        }

        boolean userExists = false;
        try (PreparedStatement checkStmt = context.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM admins WHERE username = ?")) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    userExists = rs.getInt(1) > 0;
                }
            }
        }
        if (userExists)
            return;

        String encodedPassword = new BCryptPasswordEncoder().encode(password);
        try (PreparedStatement insert = context.getConnection().prepareStatement(
                "INSERT into admins (id,username,password,created_at,updated_at) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        )) {
            insert.setObject(1, UUID.randomUUID());
            insert.setString(2, username);
            insert.setString(3, encodedPassword);
            insert.execute();
        }
    }
}
