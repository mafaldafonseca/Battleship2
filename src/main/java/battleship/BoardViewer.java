package battleship;


import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXPanel;

import javax.swing.*;
import java.awt.*;

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

    public JButton[][] getGridButtons() {
        return gridButtons;
    }

}
