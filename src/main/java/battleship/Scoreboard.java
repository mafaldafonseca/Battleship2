package battleship;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Guarda e consulta os resultados dos jogos passados numa base de dados SQLite.
 * O ficheiro da base de dados (scoreboard.db) é criado na diretoria de execução.
 */
public class Scoreboard {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DB_URL = "jdbc:sqlite:scoreboard.db";

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Cria a tabela dos jogos, caso ainda não exista.
     */
    public Scoreboard() {
        String sql = "CREATE TABLE IF NOT EXISTS jogos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "data TEXT NOT NULL, "
                + "rajadas INTEGER NOT NULL, "
                + "duracao_ms INTEGER NOT NULL)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            LOGGER.error("Erro ao criar a base de dados do scoreboard: {}", e.getMessage());
        }
    }

    /**
     * Regista um jogo terminado.
     *
     * @param rajadas   número de rajadas necessárias para afundar a frota
     * @param duracaoMs duração total do jogo, em milissegundos
     */
    public void registar(int rajadas, long duracaoMs) {
        String sql = "INSERT INTO jogos (data, rajadas, duracao_ms) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().format(FORMATO_DATA));
            ps.setInt(2, rajadas);
            ps.setLong(3, duracaoMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao guardar o jogo no scoreboard: {}", e.getMessage());
        }
    }

    /**
     * Imprime na consola os melhores jogos (menos rajadas e, em caso de empate, menor duração).
     *
     * @param limite número máximo de jogos a mostrar
     */
    public void imprimirTop(int limite) {
        String sql = "SELECT data, rajadas, duracao_ms FROM jogos "
                + "ORDER BY rajadas ASC, duracao_ms ASC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("================ SCOREBOARD ================");
                System.out.printf("%-4s %-20s %-8s %s%n", "#", "Data", "Rajadas", "Duração");
                int posicao = 1;
                while (rs.next()) {
                    System.out.printf("%-4d %-20s %-8d %.1fs%n",
                            posicao++,
                            rs.getString("data"),
                            rs.getInt("rajadas"),
                            rs.getLong("duracao_ms") / 1000.0);
                }
                if (posicao == 1) {
                    System.out.println("Ainda não há jogos registados.");
                }
                System.out.println("============================================");
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao ler o scoreboard: {}", e.getMessage());
        }
    }
}