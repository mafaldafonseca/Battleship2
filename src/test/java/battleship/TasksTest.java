package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TasksTest {

	private final PrintStream originalOut = System.out;
	private final boolean originalInterrupt = Thread.interrupted();

	@TempDir
	Path temporaryDirectory;

	private static class TestDisplay implements Tasks.BoardDisplay {
		private boolean shown;
		private int updates;

		@Override
		public void setVisible(boolean visible) {
			shown = visible;
		}

		@Override
		public void updateBoard(Game game) {
			updates++;
		}
	}

	private static class ScriptedFleet implements IFleet {
		private final Fleet delegate = new Fleet();
		private final IShip ship;
		private int floatingShipChecks;

		private ScriptedFleet() {
			ship = new Barge(Compass.NORTH, new Position(5, 5));
			assertTrue(delegate.addShip(ship));
		}

		@Override
		public List<IShip> getShips() {
			return delegate.getShips();
		}

		@Override
		public boolean addShip(IShip ship) {
			return delegate.addShip(ship);
		}

		@Override
		public List<IShip> getShipsLike(String category) {
			return delegate.getShipsLike(category);
		}

		@Override
		public List<IShip> getFloatingShips() {
			floatingShipChecks++;
			return floatingShipChecks == 3 ? new ArrayList<>() : List.of(ship);
		}

		@Override
		public List<IShip> getSunkShips() {
			return delegate.getSunkShips();
		}

		@Override
		public IShip shipAt(IPosition position) {
			return delegate.shipAt(position);
		}

		@Override
		public void printStatus() {
			delegate.printStatus();
		}
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		Thread.interrupted();
		if (originalInterrupt) {
			Thread.currentThread().interrupt();
		}
	}

	private Fleet fleetWithBarge(boolean sunk) {
		Fleet fleet = new Fleet();
		Barge barge = new Barge(Compass.NORTH, new Position(5, 5));
		if (sunk) {
			barge.sink();
		}
		assertTrue(fleet.addShip(barge));
		return fleet;
	}

	private List<Game> runMenu(String input, Supplier<IFleet> fleetFactory) {
		TestDisplay display = new TestDisplay();
		List<Game> wonGames = new ArrayList<>();
		Scoreboard scoreboard = new Scoreboard(
				"jdbc:sqlite:" + temporaryDirectory.resolve("scoreboard.db"));
		System.setOut(new PrintStream(new ByteArrayOutputStream(), true,
				StandardCharsets.UTF_8));

		Tasks.menu(new Scanner(input), display, scoreboard, fleetFactory, 0,
				wonGames::add);

		assertTrue(display.shown);
		return wonGames;
	}

	@Test
	void menuHelpListaOsComandos() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

		Tasks.menuHelp();

		String ajuda = output.toString(StandardCharsets.UTF_8);
		assertTrue(ajuda.contains("gerafrota"));
		assertTrue(ajuda.contains("scoreboard"));
		assertTrue(ajuda.contains("desisto"));
	}

	@Test
	void readClassicPositionAceitaFormatoCompactoIgnorandoMaiusculas() {
		IPosition position = Tasks.readClassicPosition(new Scanner("c7"));

		assertEquals(new Position(2, 6), position);
	}

	@Test
	void readClassicPositionAceitaFormatoSeparado() {
		IPosition position = Tasks.readClassicPosition(new Scanner("A 3"));

		assertEquals(new Position(0, 2), position);
	}

	@Test
	void readClassicPositionRejeitaEntradaVazia() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("")));
	}

	@Test
	void readClassicPositionRejeitaFormatoInvalido() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("A x")));
	}

	@Test
	void readClassicPositionRejeitaNumeroForaDoFormato() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("A")));
	}

	@Test
	void readClassicPositionRejeitaColunaComMaisDeUmaLetra() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("AB 3")));
	}

	@Test
	void readPositionLeCoordenadasNumericas() {
		Position position = Tasks.readPosition(new Scanner("4 6"));

		assertEquals(new Position(4, 6), position);
	}

	@Test
	void readShipConstroiNavioConhecido() {
		Ship ship = Tasks.readShip(new Scanner("barca 4 6 n"));

		assertNotNull(ship);
		assertEquals("Barca", ship.getCategory());
		assertEquals(Compass.NORTH, ship.getBearing());
		assertEquals(new Position(4, 6), ship.getPosition());
	}

	@Test
	void readShipRetornaNullParaCategoriaDesconhecida() {
		assertNull(Tasks.readShip(new Scanner("desconhecido 4 6 n")));
	}

	@Test
	void readShipSuportaTodasAsOrientacoes() {
		assertEquals(Compass.NORTH, Tasks.readShip(new Scanner("barca 0 0 n")).getBearing());
		assertEquals(Compass.SOUTH, Tasks.readShip(new Scanner("barca 0 0 s")).getBearing());
		assertEquals(Compass.EAST, Tasks.readShip(new Scanner("barca 0 0 e")).getBearing());
		assertEquals(Compass.WEST, Tasks.readShip(new Scanner("barca 0 0 o")).getBearing());
	}

	@Test
	void buildFleetIgnoraNavioDesconhecidoEColisao() {
		StringBuilder input = new StringBuilder("desconhecido 0 0 n barca 0 0 n ");
		int[][] positions = {
				{0, 0}, {0, 2}, {0, 4}, {0, 6}, {0, 8},
				{2, 0}, {2, 2}, {2, 4}, {2, 6}, {2, 8}, {4, 0}
		};
		for (int[] position : positions) {
			input.append("barca ").append(position[0]).append(' ')
					.append(position[1]).append(" n ");
		}

		Fleet fleet = Tasks.buildFleet(new Scanner(input.toString()));

		assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size());
	}

	@Test
	void menuExecutaComandosAntesDeCriarJogoEComandosGerais() {
		List<Game> wonGames = runMenu(
				"estado\nmapa\nrajada\nsimula\ntiros\nscoreboard\n"
						+ "desconhecido\najuda\ndesisto\n",
				Fleet::new);

		assertTrue(wonGames.isEmpty());
	}

	@Test
	void menuExecutaComandosComJogoAtivoSemGanharNaRajada() {
		TestDisplay display = new TestDisplay();
		List<Game> wonGames = new ArrayList<>();
		Scoreboard scoreboard = new Scoreboard(
				"jdbc:sqlite:" + temporaryDirectory.resolve("active.db"));
		System.setOut(new PrintStream(new ByteArrayOutputStream(), true,
				StandardCharsets.UTF_8));

		Tasks.menu(new Scanner("gerafrota\nestado\nmapa\ntiros\n"
						+ "rajada A1 B1 C1\ndesisto\n"),
				display, scoreboard, () -> fleetWithBarge(false), 0, wonGames::add);

		assertTrue(display.shown);
		assertEquals(2, display.updates);
		assertTrue(wonGames.isEmpty());
	}

	@Test
	void menuRegistaVitoriaDepoisDeUmaRajada() {
		List<Game> wonGames = runMenu(
				"gerafrota\nrajada A1 B1 C1\ndesisto\n",
				() -> fleetWithBarge(true));

		assertEquals(1, wonGames.size());
		assertEquals(0, wonGames.get(0).getRemainingShips());
	}

	@Test
	void menuCarregaFrotaPersonalizada() {
		StringBuilder input = new StringBuilder("lefrota ");
		int[][] positions = {
				{0, 0}, {0, 2}, {0, 4}, {0, 6}, {0, 8},
				{2, 0}, {2, 2}, {2, 4}, {2, 6}, {2, 8}, {4, 0}
		};
		for (int[] position : positions) {
			input.append("barca ").append(position[0]).append(' ')
					.append(position[1]).append(" n ");
		}
		input.append("estado\nmapa\ntiros\ndesisto\n");

		assertTrue(runMenu(input.toString(), Fleet::new).isEmpty());
	}

	@Test
	void menuSimulaAteAfundarFrota() {
		List<Game> wonGames = runMenu(
				"gerafrota\nsimula\ndesisto\n",
				() -> fleetWithBarge(false));

		assertEquals(1, wonGames.size());
		assertEquals(0, wonGames.get(0).getRemainingShips());
	}

	@Test
	void menuSimulaRestauraInterrupcaoDuranteEspera() {
		Thread.currentThread().interrupt();
		List<Game> wonGames = runMenu(
				"gerafrota\nsimula\ndesisto\n",
				() -> fleetWithBarge(false));

		assertEquals(1, wonGames.size());
		assertTrue(Thread.currentThread().isInterrupted());
	}

	@Test
	void menuSimulaNaoRegistaVitoriaSeAListaDeNaviosAindaIndicarNavioAtivo() {
		List<Game> wonGames = runMenu(
				"gerafrota\nsimula\ndesisto\n",
				ScriptedFleet::new);

		assertTrue(wonGames.isEmpty());
	}

	@Test
	void metodosDeLeituraRejeitamScannerNuloComAssertionsAtivas() {
		assertThrows(AssertionError.class, () -> Tasks.buildFleet(null));
		assertThrows(AssertionError.class, () -> Tasks.readShip(null));
		assertThrows(AssertionError.class, () -> Tasks.readPosition(null));
	}
}