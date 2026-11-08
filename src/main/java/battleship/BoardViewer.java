package battleship;


import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BoardViewer extends JXFrame {
    private static final int BOARD_SIZE = 10;
    private final JButton[][] gridButtons = new JButton[BOARD_SIZE][BOARD_SIZE];

    public BoardViewer() {
        super("Batalha Naval - Tabuleiro");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);

        JXPanel boardPanel = new JXPanel();
        boardPanel.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE, 2, 2));
        boardPanel.setBackground(Color.DARK_GRAY);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(new Color(173, 216, 230)); // Cor inicial de água
                cell.setFont(new Font("Arial", Font.BOLD, 12));
                gridButtons[row][col] = cell;
                boardPanel.add(cell);
            }
        }

        add(boardPanel, BorderLayout.CENTER);
    }

    /**
     * Atualiza o estado visual do tabuleiro com base nas jogadas efetuadas no jogo.
     *
     * @param game Objeto do jogo
     */
    public void updateBoard(IGame game) {
        if (game == null) return;

        // Guardar os tiros efetuados
        List<IMove> moves = game.getAlienMoves();
        IFleet fleet = game.getMyFleet();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {

                // As posições internas da matriz/tabuleiro vão de 0 a 9
                IPosition currentPos = new Position(row, col);

                // Verificar se a posição atual esta presente nas jogadas efetuadas
                boolean wasShot = moves != null && moves.stream()
                        .filter(move -> move != null && move.getShots() != null)
                        .flatMap(move -> move.getShots().stream())
                        .anyMatch(pos -> pos.equals(currentPos));

                if (wasShot) {
                    // Verificar se a posição atingiu algum navio da frota
                    boolean hit = (fleet != null && fleet.getShips() != null) && fleet.getShips().stream()
                            .anyMatch(ship -> ship.occupies(currentPos));

                    if (hit) {
                        gridButtons[row][col].setBackground(Color.RED);
                        gridButtons[row][col].setForeground(Color.WHITE);
                        gridButtons[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                        gridButtons[row][col].setText("*");
                    } else {
                        gridButtons[row][col].setBackground(Color.GRAY);
                        gridButtons[row][col].setForeground(Color.BLACK);
                        gridButtons[row][col].setText("O");
                    }
                }
            }
        }
        repaint();
    }
}
