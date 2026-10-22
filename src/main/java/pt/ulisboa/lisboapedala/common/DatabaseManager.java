package pt.ulisboa.lisboapedala.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Gestor central de conexões SQLite para os módulos do sistema.
 * Suporta bases de dados partilhadas em memória para testes e bases de dados locais em ficheiro.
 */
public class DatabaseManager {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private final String jdbcUrl;
    private Connection sharedConnection; // Usado para manter a base de dados SQLite in-memory ativa

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        if (jdbcUrl.contains(":memory:") || jdbcUrl.contains("mode=memory")) {
            try {
                // Mantém conexão viva para impedir que o SQLite in-memory feche e perca o esquema
                this.sharedConnection = DriverManager.getConnection(jdbcUrl);
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inicializar conexão in-memory", e);
            }
        }
    }

    public static DatabaseManager createInMemory() {
        // SQLite in-memory com URI partilhada e identificador único por instância para isolamento de testes
        String uniqueDbName = "mem_" + System.nanoTime();
        return new DatabaseManager("jdbc:sqlite:file:" + uniqueDbName + "?mode=memory&cache=shared");
    }

    public static DatabaseManager createForFile(String filePath) {
        return new DatabaseManager("jdbc:sqlite:" + filePath);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Executa um script SQL a partir do classpath (ex: schema.sql do módulo).
     */
    public void executeScriptFromClasspath(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Script SQL não encontrado: " + resourcePath);
            }
            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                for (String command : sql.split(";")) {
                    String trimmed = command.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao executar script " + resourcePath, e);
        }
    }

    public void close() {
        if (sharedConnection != null) {
            try {
                sharedConnection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
