package battleship;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
public class IMoveTest {

    @Test
    @DisplayName("Ler jogada com vários tiros a partir de um Scanner")
    void testReadMoveWithMultipleShots() {
        String input = "3\n0 1\n2 3\n4 5\n";
        Scanner sc = new Scanner(input);

        Move move = IMove.readMove(1, sc);

        assertNotNull(move);
        assertEquals(1, move.getNumber());
        assertEquals(3, move.getShots().size());
        assertEquals(new Position(0, 1), move.getShots().get(0));
        assertEquals(new Position(2, 3), move.getShots().get(1));
        assertEquals(new Position(4, 5), move.getShots().get(2));
        assertTrue(move.getShotResults().isEmpty());
    }

    @Test
    @DisplayName("Ler jogada sem tiros (0 tiros) para cobrir o ramo em que o ciclo não executa")
    void testReadMoveWithZeroShots() {
        String input = "0\n";
        Scanner sc = new Scanner(input);

        Move move = IMove.readMove(2, sc);

        assertNotNull(move);
        assertEquals(2, move.getNumber());
        assertTrue(move.getShots().isEmpty());
        assertTrue(move.getShotResults().isEmpty());
    }


}
