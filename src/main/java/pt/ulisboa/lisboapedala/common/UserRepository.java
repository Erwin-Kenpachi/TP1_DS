package pt.ulisboa.lisboapedala.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Repositório de utilizadores do sistema Lisboa Pedala.
 * Gere registo, autenticação e dados de perfil na base de dados persistente.
 * Pertence ao módulo common por ser infraestrutura transversal de autenticação.
 */
public class UserRepository {

    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        initSchema();
    }

    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS app_users (
                    user_id   TEXT PRIMARY KEY,
                    username  TEXT NOT NULL UNIQUE,
                    password  TEXT NOT NULL,
                    is_subscriber INTEGER NOT NULL DEFAULT 0,
                    payment_method TEXT NOT NULL DEFAULT 'MBWAY',
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                );
                """;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar tabela app_users", e);
        }
    }

    /**
     * Regista um novo utilizador. Devolve false se o username já existir.
     */
    public boolean register(String userId, String username, String password, boolean isSubscriber, String paymentMethod) {
        String sql = "INSERT INTO app_users (user_id, username, password, is_subscriber, payment_method) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, username.toLowerCase());
            stmt.setString(3, password); // Em produção usaria hash; aqui é simulação
            stmt.setInt(4, isSubscriber ? 1 : 0);
            stmt.setString(5, paymentMethod);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // UNIQUE constraint violation → username já existe
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return false;
            }
            throw new RuntimeException("Erro ao registar utilizador", e);
        }
    }

    /**
     * Autentica um utilizador pelo username e password.
     * Devolve Optional com os dados se as credenciais forem válidas.
     */
    public Optional<UserProfile> login(String username, String password) {
        String sql = "SELECT * FROM app_users WHERE username = ? AND password = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.toLowerCase());
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserProfile(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getInt("is_subscriber") == 1,
                            rs.getString("payment_method")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao autenticar utilizador", e);
        }
        return Optional.empty();
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM app_users WHERE username = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar username", e);
        }
    }

    /**
     * Perfil imutável de um utilizador autenticado.
     */
    public record UserProfile(String userId, String username, boolean isSubscriber, String paymentMethod) {}
}
