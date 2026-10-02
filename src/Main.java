import javax.swing.*;
import java.awt.*;

public class Main extends JFrame implements MenuListener {
    // Variable initialisation
    private final CardLayout menuSwitch;
    private final JPanel mainPanel;
    private final StartMenu startMenu;
    private final BattleShips battleShips;

    private final int START_BOARD_WIDTH = 500;
    private final int START_BOARD_HEIGHT = 400;
    private final String START_MENU = "MENU";
    private final String GAME_MENU = "GAME";
    private boolean firstStart = true;

    Main() {
        this.setSize(START_BOARD_WIDTH, START_BOARD_HEIGHT);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        menuSwitch = new CardLayout();
        mainPanel = new JPanel(menuSwitch);

        startMenu = new StartMenu();
        startMenu.setMenuListener(this);

        battleShips = new BattleShips();
        battleShips.setMenuListener(this);


        mainPanel.add(startMenu, START_MENU);
        mainPanel.add(battleShips, GAME_MENU);
        this.add(mainPanel);

        menuSwitch.show(mainPanel, START_MENU);
        this.setVisible(true);
    }

    @Override
    public void onStartClicked(String difficulty) {
        int GAME_BOARD_WIDTH = 1000;
        int GAME_BOARD_HEIGHT = 800;
        GameManager.setGameState(GameState.GAME_RUNNING);

        menuSwitch.show(mainPanel, GAME_MENU);
        this.setSize(GAME_BOARD_WIDTH, GAME_BOARD_HEIGHT);
        this.setLocationRelativeTo(null);
        battleShips.startGame();
        battleShips.difficulty = difficulty;
        if (!firstStart) { // Only generate boards if it is not the first time playing as the methods are in the contractor
            battleShips.generateShipsBoard();
        } else {
            firstStart = false;
        }
        battleShips.requestFocusInWindow();

    }

    @Override
    public void returnToMenu() {
        GameManager.setGameState(GameState.START_MENU);

        this.setSize(START_BOARD_WIDTH, START_BOARD_HEIGHT);
        this.setLocationRelativeTo(null);
        menuSwitch.show(mainPanel, START_MENU);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

}