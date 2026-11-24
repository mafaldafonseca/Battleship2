package battleship;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários da classe Move")
public class MoveTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Construtor, getters e toString funcionam corretamente")
    void constructorAndGetters() {
        List<IPosition> shots = List.of(new Position(1, 1), new Position(1, 2), new Position(1, 3));
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false)
        );

        Move move = new Move(1, shots, results);

        assertEquals(1, move.getNumber());
        assertEquals(shots, move.getShots());
        assertEquals(results, move.getShotResults());
        assertEquals("Move{number=1, shots=3, results=3}", move.toString());
    }

    @Test
    @DisplayName("processEnemyFire (verbose=false): calcula JSON sem imprimir resumo de jogada")
    void processEnemyFireNonVerbose() {
        List<IPosition> shots = List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2));
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(false, false, null, false)
        );

        Move move = new Move(2, shots, results);
        String json = move.processEnemyFire(false);

        assertTrue(json.contains("\"validShots\" : 1"));
        assertTrue(json.contains("\"repeatedShots\" : 1"));
        assertTrue(json.contains("\"outsideShots\" : 1"));
        assertTrue(json.contains("\"missedShots\" : 1"));
        assertFalse(outContent.toString().contains("Jogada nº2 ->"));
    }

    @Test
    @DisplayName("processEnemyFire (verbose=true): apenas tiros repetidos (singular e plural)")
    void processEnemyFireOnlyRepeated() {
        List<IPosition> shots = List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2));

        // 1 repetido + 2 inválidos (exteriores) -> singular em repetido, plural em exterior
        List<IGame.ShotResult> oneRepeatedTwoInvalid = List.of(
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(false, false, null, false),
                new IGame.ShotResult(false, false, null, false)
        );
        Move move1 = new Move(1, shots, oneRepeatedTwoInvalid);
        move1.processEnemyFire(true);

        // 3 repetidos -> plural em repetidos, 0 exteriores
        List<IGame.ShotResult> threeRepeated = List.of(
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(true, true, null, false)
        );
        Move move2 = new Move(2, shots, threeRepeated);
        move2.processEnemyFire(true);

        String console = outContent.toString();
        assertTrue(console.contains("Jogada nº1 -> 1 tiro repetido, 2 tiros exteriores"));
        assertTrue(console.contains("Jogada nº2 -> 3 tiros repetidos"));
    }

    @Test
    @DisplayName("processEnemyFire (verbose=true): mistura de acertos, água, repetido e exterior")
    void processEnemyFireHitsWaterRepeatedAndOutside() {
        IShip frigate = new Frigate(Compass.EAST, new Position(2, 2));
        List<IPosition> shots = List.of(new Position(2, 2), new Position(3, 3), new Position(4, 4));

        // 1 tiro válido num navio + 1 tiro repetido + 1 tiro exterior
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(true, false, frigate, false),
                new IGame.ShotResult(true, true, null, false),
                new IGame.ShotResult(false, false, null, false)
        );

        Move move = new Move(3, shots, results);
        String json = move.processEnemyFire(true);

        String console = outContent.toString();
        assertTrue(console.contains("Jogada nº3 -> 1 tiro válido: 1 tiro num(a) Fragata , 1 tiro repetido, 1 tiro exterior"));
        assertTrue(json.contains("\"hitsOnBoats\""));
        assertTrue(json.contains("\"Fragata\""));
    }

    @Test
    @DisplayName("processEnemyFire (verbose=true): múltiplos tiros na água e múltiplos acertos no mesmo navio")
    void processEnemyFireMultipleHitsAndMisses() {
        IShip galleon = new Galleon(Compass.NORTH, new Position(4, 4));
        List<IPosition> shots = List.of(new Position(4, 4), new Position(5, 4), new Position(0, 0));

        // 2 tiros no mesmo barco (sem afundar) + 1 tiro na água
        List<IGame.ShotResult> results1 = List.of(
                new IGame.ShotResult(true, false, galleon, false),
                new IGame.ShotResult(true, false, galleon, false),
                new IGame.ShotResult(true, false, null, false)
        );
        Move move1 = new Move(4, shots, results1);
        move1.processEnemyFire(true);

        // 3 tiros na água (plural)
        List<IGame.ShotResult> results2 = List.of(
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false)
        );
        Move move2 = new Move(5, shots, results2);
        move2.processEnemyFire(true);

        String console = outContent.toString();
        assertTrue(console.contains("Jogada nº4 -> 3 tiros válidos: 2 tiros num(a) Galeao + 1 tiro na água"));
        assertTrue(console.contains("Jogada nº5 -> 3 tiros válidos: 3 tiros na água"));
    }

    @Test
    @DisplayName("processEnemyFire (verbose=true): afundamento de um e vários navios do mesmo tipo")
    void processEnemyFireSunkShips() {
        IShip barge1 = new Barge(Compass.NORTH, new Position(1, 1));
        IShip barge2 = new Barge(Compass.NORTH, new Position(3, 3));
        IShip caravel = new Caravel(Compass.EAST, new Position(6, 6));
        List<IPosition> shots = List.of(new Position(1, 1), new Position(3, 3), new Position(6, 6));

        // 2 barcas afundadas (plural) + 1 tiro numa caravela (não afundada)
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(true, false, barge1, true),
                new IGame.ShotResult(true, false, barge2, true),
                new IGame.ShotResult(true, false, caravel, false)
        );

        Move move = new Move(6, shots, results);
        String json = move.processEnemyFire(true);

        String console = outContent.toString();
        assertTrue(console.contains("Jogada nº6 -> 3 tiros válidos: 2 Barcas ao fundo + 1 tiro num(a) Caravela "));
        assertTrue(json.contains("\"sunkBoats\""));
        assertTrue(json.contains("\"count\" : 2"));
        assertTrue(json.contains("\"hitsOnBoats\""));
    }
}