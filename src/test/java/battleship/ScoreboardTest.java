
package battleship;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardTest {

    @TempDir
    Path pastaTemporaria;

    private Scoreboard scoreboard;
    private String dbUrl;
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        dbUrl = "jdbc:sqlite:" + pastaTemporaria.resolve("scoreboard.db");
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(
                output, true, StandardCharsets.UTF_8));

        scoreboard = new Scoreboard(dbUrl);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String duracao(double segundos) {
        return String.format(Locale.getDefault(), "%.1fs", segundos);
    }

    @Test
    @DisplayName("Verificar criacao do Scoreboard")
    void constructor() {
        assertNotNull(scoreboard);
    }

    @Test
    @DisplayName("Verificar tratamento de erro ao criar o Scoreboard")
    void constructorComErroDeLigacao() {
        assertDoesNotThrow(() -> new Scoreboard("invalid-jdbc-url"));
    }

    @Test
    @DisplayName("Verificar registo de um jogo")
    void registar() throws Exception {
        scoreboard.registar(10, 5000);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT rajadas, duracao_ms FROM jogos")) {

            assertTrue(rs.next());
            assertEquals(10, rs.getInt("rajadas"));
            assertEquals(5000, rs.getLong("duracao_ms"));
            assertFalse(rs.next());
        }
    }

    @Test
    @DisplayName("Verificar tratamento de erro ao registar um jogo")
    void registarComErroDeLigacao() {
        Scoreboard scoreboardInvalido = new Scoreboard("invalid-jdbc-url");

        assertDoesNotThrow(() -> scoreboardInvalido.registar(10, 5000));
    }

    @Test
    @DisplayName("Verificar apresentacao do Scoreboard vazio")
    void imprimirTopVazio() {
        scoreboard.imprimirTop(10);

        String resultado = output.toString(
                StandardCharsets.UTF_8);

        assertTrue(resultado.contains("SCOREBOARD"));
        assertTrue(resultado.contains(
                "Ainda não há jogos registados."));
    }

    @Test
    @DisplayName("Verificar tratamento de erro ao ler o Scoreboard")
    void imprimirTopComErroDeLigacao() {
        Scoreboard scoreboardInvalido = new Scoreboard("invalid-jdbc-url");

        assertDoesNotThrow(() -> scoreboardInvalido.imprimirTop(10));
    }

    @Test
    @DisplayName("Verificar apresentacao de jogos registados")
    void imprimirTop() {
        scoreboard.registar(10, 5000);
        scoreboard.registar(5, 3000);

        scoreboard.imprimirTop(10);

        String resultado = output.toString(
                StandardCharsets.UTF_8);

        assertTrue(resultado.contains("SCOREBOARD"));
        assertTrue(resultado.contains(duracao(5.0)));
        assertTrue(resultado.contains(duracao(3.0)));
    }

    @Test
    @DisplayName("Verificar limite de resultados")
    void imprimirTopComLimite() {
        scoreboard.registar(10, 5000);
        scoreboard.registar(5, 3000);

        scoreboard.imprimirTop(1);

        String resultado = output.toString(
                StandardCharsets.UTF_8);

        assertTrue(resultado.contains("SCOREBOARD"));
        assertTrue(resultado.contains(duracao(3.0)));
        assertFalse(resultado.contains(duracao(5.0)));
    }

    @Test
    @DisplayName("Verificar Scoreboard com limite zero")
    void imprimirTopLimiteZero() {
        scoreboard.registar(10, 5000);

        scoreboard.imprimirTop(0);

        String resultado = output.toString(
                StandardCharsets.UTF_8);

        assertTrue(resultado.contains(
                "Ainda não há jogos registados."));
    }

    @Test
    @DisplayName("Verificar ordenacao por numero de rajadas")
    void imprimirTopOrdenacao() {
        scoreboard.registar(15, 2000);
        scoreboard.registar(5, 8000);
        scoreboard.registar(10, 4000);

        scoreboard.imprimirTop(3);

        String resultado = output.toString(
                StandardCharsets.UTF_8);

        int primeiro = resultado.indexOf(duracao(8.0));
        int segundo = resultado.indexOf(duracao(4.0));
        int terceiro = resultado.indexOf(duracao(2.0));

        assertTrue(primeiro >= 0);
        assertTrue(segundo > primeiro);
        assertTrue(terceiro > segundo);
    }

    @Test
    @DisplayName("Verificar desempate pela menor duracao")
    void imprimirTopDesempate() {
        scoreboard.registar(5, 9000);
        scoreboard.registar(5, 3000);

        scoreboard.imprimirTop(2);

        String resultado = output.toString(
                StandardCharsets.UTF_8);

        int primeiro = resultado.indexOf(duracao(3.0));
        int segundo = resultado.indexOf(duracao(9.0));

        assertTrue(primeiro >= 0);
        assertTrue(segundo > primeiro);
    }
}
