package battleship;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários da classe Game")
public class GameTest {

	private Game game;
	private final PrintStream originalOut = System.out;
	private ByteArrayOutputStream outContent;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet());
		outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		game = null;
	}

	@Test
	@DisplayName("Construtor inicializa contadores a zero e listas vazias")
	void constructor() {
		assertNotNull(game);
		assertNotNull(game.getMyFleet());
		assertNotNull(game.getAlienFleet());
		assertNotNull(game.getAlienMoves());
		assertNotNull(game.getMyMoves());
		assertNotNull(game.getPdfExporter());
		assertTrue(game.getAlienMoves().isEmpty());
		assertTrue(game.getMyMoves().isEmpty());
		assertEquals(0, game.getInvalidShots());
		assertEquals(0, game.getRepeatedShots());
		assertEquals(0, game.getHits());
		assertEquals(0, game.getSunkShips());
	}

	@Test
	@DisplayName("fireSingleShot: tiro fora do tabuleiro incrementa invalidShots")
	void fireSingleShotInvalid() {
		Position invalidPosition = new Position(-1, 5);
		IGame.ShotResult result = game.fireSingleShot(invalidPosition, false);

		assertFalse(result.valid());
		assertFalse(result.repeated());
		assertNull(result.ship());
		assertFalse(result.sunk());
		assertEquals(1, game.getInvalidShots());
	}

	@Test
	@DisplayName("fireSingleShot: tiro marcado como repetido incrementa repeatedShots")
	void fireSingleShotRepeatedFlag() {
		Position position = new Position(2, 3);
		IGame.ShotResult result = game.fireSingleShot(position, true);

		assertTrue(result.valid());
		assertTrue(result.repeated());
		assertNull(result.ship());
		assertFalse(result.sunk());
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireSingleShot: tiro repetido de uma jogada anterior incrementa repeatedShots")
	void fireSingleShotRepeatedFromHistory() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);

		IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), false);
		assertTrue(result.valid());
		assertTrue(result.repeated());
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireSingleShot: tiro válido na água devolve válido sem navio")
	void fireSingleShotMiss() {
		IGame.ShotResult result = game.fireSingleShot(new Position(4, 4), false);

		assertTrue(result.valid());
		assertFalse(result.repeated());
		assertNull(result.ship());
		assertFalse(result.sunk());
		assertEquals(0, game.getHits());
		assertEquals(0, game.getSunkShips());
	}

	@Test
	@DisplayName("fireSingleShot: acerto em navio com várias posições sem o afundar")
	void fireSingleShotHitWithoutSinking() {
		Ship frigate = new Frigate(Compass.EAST, new Position(3, 3));
		game.getMyFleet().addShip(frigate);

		IGame.ShotResult result = game.fireSingleShot(new Position(3, 3), false);

		assertTrue(result.valid());
		assertFalse(result.repeated());
		assertEquals(frigate, result.ship());
		assertFalse(result.sunk());
		assertEquals(1, game.getHits());
		assertEquals(0, game.getSunkShips());
	}

	@Test
	@DisplayName("fireSingleShot: tiro que afunda navio incrementa hits e sunkShips")
	void fireSingleShotSinkShip() {
		Ship barge = new Barge(Compass.NORTH, new Position(1, 1));
		game.getMyFleet().addShip(barge);

		IGame.ShotResult result = game.fireSingleShot(new Position(1, 1), false);

		assertTrue(result.valid());
		assertFalse(result.repeated());
		assertEquals(barge, result.ship());
		assertTrue(result.sunk());
		assertEquals(1, game.getHits());
		assertEquals(1, game.getSunkShips());
	}

	@Test
	@DisplayName("fireShots: lança IllegalArgumentException se número de tiros != NUMBER_SHOTS")
	void fireShotsWrongSize() {
		List<IPosition> twoShots = List.of(new Position(1, 1), new Position(1, 2));
		assertThrows(IllegalArgumentException.class, () -> game.fireShots(twoShots));
	}

	@Test
	@DisplayName("fireShots: deteta tiro duplicado dentro da mesma rajada")
	void fireShotsDuplicateInSameMove() {
		Position p1 = new Position(2, 2);
		Position p2 = new Position(2, 3);
		game.fireShots(List.of(p1, p1, p2));

		assertEquals(1, game.getAlienMoves().size());
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("repeatedShot: devolve true após disparo e false antes do disparo")
	void repeatedShotCheck() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position));

		game.fireShots(List.of(position, new Position(2, 4), new Position(2, 5)));
		assertTrue(game.repeatedShot(position));
		assertFalse(game.repeatedShot(new Position(9, 9)));
	}

	@Test
	@DisplayName("getRemainingShips: atualiza contagem ao adicionar e afundar navios")
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips());
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips());
		ship2.sink();
		assertEquals(1, game.getRemainingShips());
	}

	@Test
	@DisplayName("readEnemyFire: lê posições no formato compacto (ex: A1 B2 C3)")
	void readEnemyFireCompactFormat() {
		Scanner scanner = new Scanner("A1 B2 C3\n");
		String json = game.readEnemyFire(scanner);

		assertNotNull(json);
		assertTrue(json.contains("\"row\" : \"A\""));
		assertTrue(json.contains("\"column\" : 1"));
		assertEquals(1, game.getAlienMoves().size());
	}

	@Test
	@DisplayName("readEnemyFire: lê posições separadas por espaço (ex: A 1 B 2 C 3)")
	void readEnemyFireSeparatedFormat() {
		Scanner scanner = new Scanner("A 1 B 2 C 3\n");
		String json = game.readEnemyFire(scanner);

		assertNotNull(json);
		assertTrue(json.contains("\"row\" : \"B\""));
		assertTrue(json.contains("\"column\" : 2"));
		assertEquals(1, game.getAlienMoves().size());
	}

	@Test
	@DisplayName("readEnemyFire: lança exceção quando letra de coluna não é seguida de número")
	void readEnemyFireIncompleteColumn() {
		Scanner scanner = new Scanner("A B 2 C 3\n");
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(scanner));
	}

	@Test
	@DisplayName("readEnemyFire: lança exceção quando número de posições é insuficiente")
	void readEnemyFireNotEnoughShots() {
		Scanner scanner = new Scanner("A1 B2\n");
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(scanner));
	}

	@Test
	@DisplayName("randomEnemyFire: gera tiros aleatórios quando há muitas posições livres")
	void randomEnemyFireNormal() {
		Ship sunkBarge = new Barge(Compass.NORTH, new Position(0, 0));
		sunkBarge.sink();
		game.getMyFleet().addShip(sunkBarge);

		String json = game.randomEnemyFire();
		assertNotNull(json);
		assertEquals(1, game.getAlienMoves().size());
		assertEquals(Game.NUMBER_SHOTS, game.getAlienMoves().get(0).getShots().size());
	}

	@Test
	@DisplayName("randomEnemyFire: cobre ramo com menos de NUMBER_SHOTS posições utilizáveis")
	void randomEnemyFireFewCandidates() {
		// Preenche quase todo o tabuleiro com jogadas anteriores para deixar apenas 1 posição livre
		List<IPosition> allExceptOne = new ArrayList<>();
		for (int r = 0; r < Game.BOARD_SIZE; r++) {
			for (int c = 0; c < Game.BOARD_SIZE; c++) {
				if (!(r == 9 && c == 9)) {
					allExceptOne.add(new Position(r, c));
				}
			}
		}
		// Agrupa em lotes de NUMBER_SHOTS
		for (int i = 0; i < allExceptOne.size(); i += Game.NUMBER_SHOTS) {
			game.fireShots(allExceptOne.subList(i, i + Game.NUMBER_SHOTS));
		}

		String json = game.randomEnemyFire();
		assertNotNull(json);
		assertTrue(json.contains("\"row\" : \"J\""));
		assertTrue(json.contains("\"column\" : 10"));
	}

	@Test
	@DisplayName("printBoard / printMyBoard / printAlienBoard: cobre navios, tiros, adjacentes e legenda")
	void printBoardsCoverage() {
		Ship floatingShip = new Frigate(Compass.EAST, new Position(2, 2));
		Ship sunkShip = new Barge(Compass.NORTH, new Position(7, 7));
		game.getMyFleet().addShip(floatingShip);
		game.getMyFleet().addShip(sunkShip);

		// Dispara um tiro certeiro no navio a flutuar, um tiro que afunda o Barge, um na água e um fora do tabuleiro
		game.fireShots(List.of(
				new Position(2, 2),   // acerto em floatingShip
				new Position(7, 7),   // afunda sunkShip (ativa SHIP_ADJACENT_MARKER)
				new Position(0, 0)    // tiro na água
		));
		// Tiro numa posição adjacente ao navio afundado e um tiro fora do tabuleiro
		game.fireShots(List.of(
				new Position(6, 7),   // tiro em posição adjacente a navio afundado
				new Position(-1, -1), // tiro fora do tabuleiro (!shot.isInside())
				new Position(5, 5)
		));

		game.printMyBoard(true, true);
		game.printMyBoard(false, false);
		game.printAlienBoard(true, false);

		String console = outContent.toString();
		assertTrue(console.contains("LEGENDA"));
		assertTrue(console.contains("*"));
		assertTrue(console.contains("o"));
		assertTrue(console.contains("#"));
		assertTrue(console.contains("-"));
	}

	@Test
	@DisplayName("jsonShots: serializa corretamente lista de posições")
	void jsonShotsSerialization() {
		String json = Game.jsonShots(List.of(new Position(0, 0), new Position(1, 2)));
		assertTrue(json.contains("\"row\" : \"A\""));
		assertTrue(json.contains("\"column\" : 1"));
		assertTrue(json.contains("\"row\" : \"B\""));
		assertTrue(json.contains("\"column\" : 3"));
	}

	@Test
	@DisplayName("over: executa encerramento do jogo e imprime mensagem final")
	void gameOver() {
		assertDoesNotThrow(() -> game.over());
		assertTrue(outContent.toString().contains("Java Sparrow"));
	}
}