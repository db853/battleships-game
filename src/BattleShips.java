import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class BattleShips extends JPanel implements ActionListener, KeyListener, MouseListener {
    private MenuListener listener;
    private final int GAME_BOARD_WIDTH = 1000;
    private final int GAME_BOARD_HEIGHT = 800;

    private final GameBoard playerBoard;
    private final GameBoard botBoard;
    private final int boardXPadding = 50;
    private final int boardYPadding = 100;
    private final int boardGap = 100;
    private final ArrayList<GameSquare> playerSquares = new ArrayList<>();
    private final ArrayList<GameSquare> botSquares = new ArrayList<>();
    private final int[] shipLengths = {2, 3, 3, 4, 4, 5};
    private final ShipDirection[] directions = ShipDirection.values();
    private final HashMap<Integer, Color> shipColours = new HashMap<>();
    private final ArrayList<Ship> playerShips = new ArrayList<>();
    private final ArrayList<Ship> botShips = new ArrayList<>();
    private final Random random = new Random();
    private boolean playerWon = false;
    private String gameOverText = "";
    private int aiTurnClicks = 0;
    public String difficulty;
    private String turnState = "Player's turn";

    private final Color backgroundColour = Color.decode("#16212D");
    private final Color transparentBackground;
    private Rectangle resumeRect;
    private Rectangle leaveRect;
    private final Font textFont = new Font("Arial", Font.PLAIN, 32);
    private final Color hoverColour;
    private Color resumeRectColour;
    private Color leaveRectColour;
    private boolean resumeHovering = false;
    private boolean leaveHovering = false;
    ArrayList<Rectangle> powerRectangles = new ArrayList<>();
    PowerUp[] powerUps = PowerUp.values();
    private String powerUpText = "";
    private PowerUp selectedPowerUp;

    private final Timer gameLoop;
    private final Timer aiTimer;

    BattleShips() {
        this.setPreferredSize(new Dimension(GAME_BOARD_WIDTH, GAME_BOARD_HEIGHT));
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);

        playerBoard = new GameBoard(boardXPadding, boardYPadding);
        botBoard = new GameBoard(GameBoard.width + boardGap + boardXPadding, boardYPadding);

        transparentBackground = Color.decode("#2A3847");
        hoverColour = transparentBackground.brighter();
        resumeRectColour = transparentBackground;
        leaveRectColour = transparentBackground;

        shipColours.put(2, Color.GREEN);
        shipColours.put(3, Color.ORANGE);
        shipColours.put(4, Color.BLUE);
        shipColours.put(5, Color.RED);

        generateShipsBoard();

        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (GameManager.getGameState() == GameState.GAME_PAUSED) {
                    checkHovering(e);
                }
            }
        });

        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCrosshair(e);
            }
        });

        gameLoop = new Timer(1000 / 60, this);

        aiTimer = new Timer(1000, _ -> {
            if (SetTurns.getTurn() == Turns.AI_TURN) {
                aiMove();
            }
        });
    }

    public void setCrosshair(MouseEvent e) {
        if (!(GameManager.getGameState() == GameState.GAME_RUNNING)) {
            return;
        }

        if (botBoard.getBounds().contains(e.getPoint())) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

    }

    public void generatePlayerBoard() {
        int xPositionPlayer = boardXPadding;
        int yPositionPlayer = boardYPadding;

        // Populate player board squares
        for (int i = 1; i <= 150; i++) {
            GameSquare square = new GameSquare(xPositionPlayer, yPositionPlayer);

            if (i % 10 == 0) {
                yPositionPlayer += 40;
                xPositionPlayer = boardXPadding;
            } else {
                xPositionPlayer += 40;
            }

            playerSquares.add(square);
        }
    }

    public void generateEnemyBoard() {
        int xPositionBot = GameBoard.width + boardGap + boardXPadding;
        int yPositionBot = boardYPadding;

        // Populate enemy board squares
        for (int i = 1; i <= 150; i++) {
            GameSquare square = new GameSquare(xPositionBot, yPositionBot);
            square.setHover();
            square.setClick();

            if (i % 10 == 0) {
                yPositionBot += 40;
                xPositionBot = GameBoard.width + boardGap + boardXPadding;
            } else {
                xPositionBot += 40;
            }

            botSquares.add(square);
        }
    }

    private void aiMove() {
        aiTurnClicks = 0;
        GameSquare validSquare = generateValidSquare();
        validSquare.hasBeenClicked = true;
        if (!validSquare.isShip) {
            SetTurns.setTurnState(Turns.PLAYER_TURN);
            turnState = "Player's turn";
            aiTimer.stop();
        } else {
            aiTurnClicks++;
            aiTimer.stop();
            smartAiMove(validSquare);
        }
    }

    private void smartAiMove(GameSquare shipSquare) {
        HashMap<String, GameSquare> playerMap = buildSquareMap(playerSquares);
        int[][] directions = getAllDirections();
        int[] validDirection = getValidDirection(shipSquare, directions, playerMap);

        if (difficulty.equals("EASY")) {
            delayTimer(this::aiMove);
            return;
        }

        traverseFirstPath(shipSquare, validDirection, playerMap, () -> {
            if (difficulty.equals("MEDIUM")) {
                delayTimer(this::aiMove);
                return;
            }

            int[] oppositeDirection = new int[]{-validDirection[0], -validDirection[1]};

            traverseSecondPath(shipSquare, oppositeDirection, playerMap, () -> {
                if (allShipsSunk(playerShips)) {
                    GameManager.setGameState(GameState.GAME_OVER);
                    return;
                }
                delayTimer(this::aiMove);
            });
        });


    }

    private int[] getValidDirection(GameSquare shipSquare, int[][] directions, HashMap<String, GameSquare> playerMap) {
        int[] validDirection = new int[2];

        for (int[] direction : directions) {
            int nextX = shipSquare.x + (direction[0] * 40);
            int nextY = shipSquare.y + (direction[1] * 40);
            GameSquare nextSquare = playerMap.get(nextX + "," + nextY);

            if (nextSquare == null || !nextSquare.isShip || nextSquare.hasBeenClicked) {
                continue;
            }

            validDirection = direction;


        }

        return validDirection;
    }

    private void traverseFirstPath(GameSquare shipSquare, int[] validDirection, HashMap<String, GameSquare> playerMap,
                                   Runnable onComplete) {
        ArrayList<GameSquare> squaresToClick = new ArrayList<>();
        int count = 1;

        while (true) {
            int nextX = shipSquare.x + (validDirection[0] * 40 * count);
            int nextY = shipSquare.y + (validDirection[1] * 40 * count);
            GameSquare nextSquare = playerMap.get(nextX + "," + nextY);

            if (nextSquare == null || !nextSquare.isShip) {
                break;
            }

            squaresToClick.add(nextSquare);
            count++;

        }

        clickSquaresWithDelay(squaresToClick, 0, onComplete);
    }

    private void traverseSecondPath(GameSquare shipSquare, int[] oppositeDirection, HashMap<String, GameSquare>
            playerMap, Runnable onComplete) {
        ArrayList<GameSquare> squaresToClick = new ArrayList<>();
        int count = 1;

        while (aiTurnClicks < shipSquare.parentShip.length) {
            int nextX = shipSquare.x + (oppositeDirection[0] * 40 * count);
            int nextY = shipSquare.y + (oppositeDirection[1] * 40 * count);
            GameSquare nextSquare = playerMap.get(nextX + "," + nextY);

            if (nextSquare == null || !nextSquare.isShip) {
                break;
            }

            squaresToClick.add(nextSquare);
            count++;

        }

        clickSquaresWithDelay(squaresToClick, 0, onComplete);
    }

    private void clickSquaresWithDelay(ArrayList<GameSquare> squares, int index, Runnable onComplete) {
        if (index >= squares.size()) {
            onComplete.run();
            return;
        }

        delayTimer(() -> {
            squares.get(index).hasBeenClicked = true;
            aiTurnClicks++;

            clickSquaresWithDelay(squares, index + 1, onComplete);
        });
    }

    private int[][] getAllDirections() {
        int xDirection1 = 0;
        int yDirection1 = 0;
        int xDirection2 = 0;
        int yDirection2 = 0;

        for (ShipDirection direction : ShipDirection.values()) {
            if (direction == ShipDirection.UP) {
                yDirection1 = -1;
            } else if (direction == ShipDirection.DOWN) {
                yDirection2 = 1;
            } else if (direction == ShipDirection.LEFT) {
                xDirection1 = -1;
            } else if (direction == ShipDirection.RIGHT) {
                xDirection2 = 1;
            }
        }

        return new int[][] {{0, yDirection1}, {0, yDirection2}, {xDirection1, 0}, {xDirection2, 0}};
    }



    private GameSquare generateValidSquare() {
        int radomIndex;
        GameSquare square;

        while (true) {
            radomIndex = random.nextInt(0, playerSquares.size());
            square = playerSquares.get(radomIndex);

            if (!square.hasBeenClicked) {
                return square;
            }
        }
    }

    private HashMap<String, GameSquare> buildSquareMap(ArrayList<GameSquare> squares) {
        // The hashmap contains all the squares in the game board. Allows O(1) lookups and cleaner code
        HashMap<String, GameSquare> squareMap = new HashMap<>();
        for (GameSquare square: squares) {
            squareMap.put(square.x + "," + square.y, square);
        }

        return squareMap;
    }

    private boolean canShipBePlaced(int startX, int startY, int width, int height, int length, int[] direction,
                                    HashMap<String, GameSquare> squareMap) {

        for (int i = 0; i < length; i++) {
            int checkX = startX + (direction[0] * i * width);
            int checkY = startY + (direction[1] * i * height);

            if (!isPositionValid(checkX, checkY, width, height, squareMap)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPositionValid(int checkX, int checkY, int width, int height,
                                    HashMap<String, GameSquare> squareMap) {

        GameSquare square = squareMap.get(checkX + "," + checkY);

        if (square == null || square.isShip) { // Returns false if outside board or already a ship square
            return false;
        }

        // Checks all 8 surrounding squares of a given ship square
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                if (offsetX == 0 && offsetY == 0) {
                    continue;
                }
                int adjacentX = checkX + offsetX * width;
                int adjacentY = checkY + offsetY * height;

                GameSquare adjacentSquare = squareMap.get(adjacentX + "," + adjacentY);

                // Can only be a ship square if it is in the board and no adjacent square is a ship
                // Ensures all ships are at least one square apart
                if (adjacentSquare != null && adjacentSquare.isShip) {
                    return false;
                }
            }
        }
        return true;
    }

    private void placeShip(int startX, int startY, int width, int height, int length, int[] direction,
                           HashMap<String, GameSquare> squareMap, ArrayList<Ship> ships) {

        Ship ship = new Ship(length);

        for (int i = 0; i < length; i++) {
            int placeX = startX + (direction[0] * i * width);
            int placeY = startY + (direction[1] * i * height);

            GameSquare square = squareMap.get(placeX + "," + placeY);

            if (square != null) {
                square.isShip = true;
                square.shipColour = shipColours.get(length);
                square.parentShip = ship;
                ship.setSquare(i, square);
            }
        }
        ships.add(ship);
    }

    private int[] getDirection() {
       ShipDirection randomDirection = directions[random.nextInt(0, directions.length)];
       int xDirection = 0;
       int yDirection = 0;

       switch (randomDirection) {
           case UP -> yDirection = -1;
           case DOWN -> yDirection = 1;
           case LEFT -> xDirection = -1;
           case RIGHT -> xDirection = 1;
           default -> System.out.println("How did we get here");
       }

       return new int[] {xDirection, yDirection};
    }

    public void generatePlayerShips() {
        HashMap<String, GameSquare> playerSquareMap = buildSquareMap(playerSquares);

        for (int length: shipLengths) {
            boolean shipPlaced = false;
            int shipAttempts = 0;

            while (!shipPlaced && shipAttempts < 1000) {
                GameSquare startSquare = playerSquares.get(random.nextInt(0, playerSquares.size()));
                int startX = startSquare.x;
                int startY = startSquare.y;
                int width = startSquare.width;
                int height = startSquare.height;
                int[] direction = getDirection();

                if (canShipBePlaced(startX, startY, width, height, length, direction, playerSquareMap)) {
                    placeShip(startX, startY, width, height, length, direction, playerSquareMap, playerShips);
                    shipPlaced = true;
                }

                shipAttempts++;
            }
        }
    }

    public void generateEnemyShips() {
        HashMap<String, GameSquare> enemySquareMap = buildSquareMap(botSquares);

        for (int length: shipLengths) {
            boolean shipPlaced = false;
            int shipAttempts = 0;

            while (!shipPlaced && shipAttempts < 1000) {
                GameSquare startSquare = botSquares.get(random.nextInt(0, botSquares.size()));
                int startX = startSquare.x;
                int startY = startSquare.y;
                int width = startSquare.width;
                int height = startSquare.height;
                int[] direction = getDirection();

                if (canShipBePlaced(startX, startY, width, height, length, direction, enemySquareMap)) {
                    placeShip(startX, startY, width, height, length, direction, enemySquareMap, botShips);
                    shipPlaced = true;
                }

                shipAttempts++;
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGame(g);

        if (GameManager.getGameState() == GameState.GAME_PAUSED) {
            drawPauseMenu(g);
        } else if (GameManager.getGameState() == GameState.GAME_OVER) {
            drawGameOver(g);
        }
    }

    public void drawGame(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        FontMetrics textFM = g2d.getFontMetrics(textFont);

        g2d.setColor(backgroundColour);
        g2d.fillRect(0, 0, GAME_BOARD_WIDTH, GAME_BOARD_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(playerBoard.x - 1, playerBoard.y - 1, GameBoard.width + 1, GameBoard.height + 1);
        g2d.drawRect(botBoard.x - 1, botBoard.y - 1, GameBoard.width + 1, GameBoard.height + 1);

        g2d.setFont(textFont);
        g2d.drawString(turnState, (GAME_BOARD_WIDTH - textFM.stringWidth(turnState)) / 2, 50);
        g2d.drawString(powerUpText, (GAME_BOARD_WIDTH - textFM.stringWidth(powerUpText)) / 2, 90);


        for (GameSquare square : playerSquares) {
            if (square.isShip) {
                g2d.setColor(square.shipColour);
            } else if (square.hasBeenClicked){
                g2d.setColor(square.hoverColour);
            } else {
                g2d.setColor(square.squareColour);
            }
            g2d.fillRect(square.x, square.y, square.width, square.height);
        }

        for (GameSquare square : botSquares) {
            if (square.hasBeenClicked) {
                g2d.setColor(square.hoverColour);
            } else {
                g2d.setColor(square.squareColour);
            }
            g2d.fillRect(square.x, square.y, square.width, square.height);
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));

        for (Ship ship : playerShips) {
            for (GameSquare square : ship.squares) {
                if (square != null && square.hasBeenClicked) {
                    g2d.drawLine(square.x, square.y, square.x + square.width, square.y + square.height);
                    g2d.drawLine(square.x + square.width, square.y, square.x, square.y + square.height);
                }
            }
        }

        for (Ship ship : botShips) {
            if (ship.isSunk()) {
                g2d.setColor(ship.getShipColour(ship.length));

                for (GameSquare square : ship.squares) {
                    if (square != null) {
                        g2d.fillRect(square.x, square.y, square.width, square.height);
                    }
                }
            } else {
                g.setColor(Color.RED);

                for (GameSquare square : ship.squares) {
                    if (square != null && square.hasBeenClicked) {
                        g2d.drawLine(square.x, square.y, square.x + square.width, square.y + square.height);
                        g2d.drawLine(square.x + square.width, square.y, square.x, square.y + square.height);
                    }
                }
            }
        }

        g2d.setStroke(new BasicStroke(1));

        Font newFont = textFont.deriveFont(20f);
        FontMetrics textFM2 = g2d.getFontMetrics(newFont);
        HashMap<Point, PowerUp> textPositions = calculateGamePowerPositions(textFM2);
        powerRectangles = getGamePowerRect(textPositions, textFM2);
        g2d.setFont(newFont);
        g2d.setColor(backgroundColour);

        for (Rectangle rect : powerRectangles) {
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20);
        }

        g2d.setColor(Color.WHITE);
        for (var entry : textPositions.entrySet()) {
            g2d.drawString(entry.getValue().stats.name(), entry.getKey().x, entry.getKey().y);
            g2d.drawString(String.valueOf(entry.getValue().usagesRemaining),
                    entry.getKey().x + textFM2.stringWidth(entry.getValue().stats.name()), entry.getKey().y);
        }
    }

    public void drawPauseMenu(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(transparentBackground);
        g2d.fillRect(3 * GAME_BOARD_WIDTH / 10, 5 * GAME_BOARD_HEIGHT / 16, 400, 300);

        FontMetrics textFM = g2d.getFontMetrics(textFont);
        Point[] textPositions = calculatePauseTextPositions(textFM);

        int adjustedResumeY = textPositions[1].y - 30;
        int adjustedLeaveY = textPositions[2].y - 30;

        resumeRect = new Rectangle(textPositions[1].x, adjustedResumeY, textFM.stringWidth("Resume"), 50);
        leaveRect = new Rectangle(textPositions[2].x, adjustedLeaveY, textFM.stringWidth("Main menu"), 50);

        g2d.setColor(resumeRectColour);
        g2d.fillRoundRect(resumeRect.x, resumeRect.y, resumeRect.width, resumeRect.height, 50, 50);

        g2d.setColor(leaveRectColour);
        g2d.fillRoundRect(leaveRect.x, leaveRect.y, leaveRect.width, leaveRect.height, 50, 50);

        g2d.setFont(textFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Game paused", textPositions[0].x, textPositions[0].y);
        g2d.drawString("Resume", textPositions[1].x, textPositions[1].y);
        g2d.drawString("Main menu", textPositions[2].x, textPositions[2].y);
    }

    public void drawGameOver(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(transparentBackground);
        g2d.fillRect(3 * GAME_BOARD_WIDTH / 10, 5 * GAME_BOARD_HEIGHT / 16, 400, 300);

        FontMetrics textFm = g2d.getFontMetrics(textFont);
        Point[] textPositions = calculateGameOverPositions(textFm);

        g2d.setFont(textFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString(gameOverText, textPositions[0].x, textPositions[0].y);

    }

    public void startGame() {
        gameLoop.start();
    }

    public void stopGame() {
        gameLoop.stop();
    }

    public void checkHovering(MouseEvent e) {
        if (resumeRect == null || leaveRect == null) {
            return;
        }

        if (resumeRect.contains(e.getPoint()) && !resumeHovering) {
            resumeHovering = true;
            resumeRectColour = hoverColour;
        } else if (!resumeRect.contains(e.getPoint()) && resumeHovering) {
            resumeHovering = false;
            resumeRectColour = transparentBackground;
        }

        if (leaveRect.contains(e.getPoint()) && !leaveHovering) {
            leaveHovering = true;
            leaveRectColour = hoverColour;
        } else if (!leaveRect.contains(e.getPoint()) && leaveHovering) {
            leaveHovering = false;
            leaveRectColour = transparentBackground;
        }
    }

    public HashMap<Point, PowerUp> calculateGamePowerPositions(FontMetrics textFM) {
        PowerUp[] powerUps = PowerUp.values();
        HashMap<Point, PowerUp> pointMap = new HashMap<>();

        for(int i = 0; i < powerUps.length; i++) {
            int x = botBoard.x + i * textFM.stringWidth(powerUps[i].name());
            int y = botBoard.y + GameBoard.height + 40;

            Point point = new Point(x, y);
            pointMap.put(point, powerUps[i]);
        }

        return pointMap;

    }

    public ArrayList<Rectangle> getGamePowerRect(HashMap<Point, PowerUp> textPositions, FontMetrics textFm) {
        ArrayList<Rectangle> rects = new ArrayList<>();

        for (var entry : textPositions.entrySet()) {
            int x = entry.getKey().x;
            int y = entry.getKey().y - 20;
            int width = textFm.stringWidth(entry.getValue().stats.name());
            int height = 20;
            Rectangle rect = new Rectangle(x, y, width, height);
            entry.getValue().rect = rect;
            rects.add(rect);
        }

        return rects;
    }

    public Point[] calculatePauseTextPositions(FontMetrics textFM) {
        int gamePausedX = 3 * GAME_BOARD_WIDTH / 10 + (400 - textFM.stringWidth("Game paused")) / 2;
        int gamePausedY = 5 * GAME_BOARD_HEIGHT / 16 + 300 / 4;

        int resumeX = 3 * GAME_BOARD_WIDTH / 10 + (400 - textFM.stringWidth("Resume")) / 2;
        int resumeY = 5 * GAME_BOARD_HEIGHT / 16 + 2 * 300 / 4;

        int leaveX = 3 * GAME_BOARD_WIDTH / 10 + (400 - textFM.stringWidth("Main menu")) / 2;
        int leaveY = 5 * GAME_BOARD_HEIGHT / 16 + 3 * 300 / 4;

        Point gamePausedPoint = new Point(gamePausedX, gamePausedY);
        Point resumePoint = new Point(resumeX, resumeY);
        Point leavePoint = new Point(leaveX, leaveY);

        return new Point[]{gamePausedPoint, resumePoint, leavePoint};
    }

    public Point[] calculateGameOverPositions(FontMetrics textFm) {
        if (playerWon) {
            gameOverText = "You won! ENTER to restart";
        } else {
            gameOverText = "You lost. ENTER to restart";
        }

        int gameOverX = 3 * GAME_BOARD_WIDTH / 10 + (400 - textFm.stringWidth(gameOverText)) / 2;
        int gameOverY = 5 * GAME_BOARD_HEIGHT / 16 + 300 / 4;

        Point gameOverPoint = new Point(gameOverX, gameOverY);

        return new Point[]{gameOverPoint};
    }

    public void setMenuListener(MenuListener listener) {
        this.listener = listener;
    }

    public void pauseGame() {
        GameManager.setGameState(GameState.GAME_PAUSED);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void checkPauseMenuButtons(MouseEvent e) {
        if (resumeRect == null || leaveRect == null) {
            return;
        }

        if (resumeRect.contains(e.getPoint())) {
            GameManager.setGameState(GameState.GAME_RUNNING);
        } else if (leaveRect.contains(e.getPoint()) && listener != null) {
            this.stopGame();
            clearShipsBoard();
            listener.returnToMenu();
        }
    }

    public boolean allShipsSunk(ArrayList<Ship> ships) {
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public void clearShipsBoard() {
        playerShips.clear();
        botShips.clear();
        playerSquares.clear();
        botSquares.clear();
    }

    public void generateShipsBoard() {
        generatePlayerBoard();
        generateEnemyBoard();
        generatePlayerShips();
        generateEnemyShips();
    }

    public void delayTimer(Runnable task) {
        Timer delayTimer = new Timer(1000, e -> {
            task.run();
            ((Timer)e.getSource()).stop();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    public void checkPowers(MouseEvent e) {
        if (powerRectangles == null) {
            return;
        }

        for (Rectangle rectangle : powerRectangles) {
            if (rectangle.contains(e.getPoint())) {
                for (PowerUp powerUp : powerUps) {
                    if (rectangle == powerUp.rect && powerUp.usagesRemaining > 0) {
                        powerUpText = "You have selected " + powerUp.stats.name();
                        selectedPowerUp = powerUp;
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            pauseGame();
        }

        if (GameManager.getGameState() == GameState.GAME_OVER && e.getKeyCode() == KeyEvent.VK_ENTER) {
            playerWon = false;

            SetTurns.setTurnState(Turns.PLAYER_TURN);
            turnState = "Player's turn";

            if (aiTimer != null && aiTimer.isRunning()) {
                aiTimer.stop();
            }

            clearShipsBoard();
            generateShipsBoard();
            resetPowerUps();
            startGame();

            GameManager.setGameState(GameState.GAME_RUNNING);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        checkPauseMenuButtons(e);
        checkPowers(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }


    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private static class GameBoard {
        int x;
        int y;
        static int width = 400;
        static int height = 600;

        GameBoard(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Rectangle getBounds() {
            return new Rectangle(this.x, this.y, width, height);
        }
    }

    public class GameSquare extends Rectangle implements MouseMotionListener {
        private boolean squareHovering = false;
        private boolean hasBeenClicked = false;
        private boolean isShip = false;
        private Color squareColour = backgroundColour;
        private Color shipColour;
        private final Color hoverColour = (squareColour.brighter()).brighter();
        private Ship parentShip = null;

        GameSquare(int x, int y) {
            super(x, y, 40, 40);
        }

        public void setHover() {
            addMouseMotionListener(this); // Gives each square a listener
        }


        public void setClick() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!(GameManager.getGameState() == GameState.GAME_RUNNING)) {
                        return;
                    }

                    if (SetTurns.getTurn() == Turns.AI_TURN) {
                        return;
                    }

                    if (GameSquare.this.contains(e.getPoint()) && SetTurns.getTurn() == Turns.PLAYER_TURN) {
                        // Fixes square disappearing issue. Don't know why it only works inside this if statement.
                        // Breaks otherwise
                        if (!GameSquare.this.isShip && !GameSquare.this.hasBeenClicked) {
                            SetTurns.setTurnState(Turns.AI_TURN);
                            turnState = "Bot's turn";
                            aiTimer.start();
                        }

                        if (selectedPowerUp != null) {
                            HashMap<String, GameSquare> botMap = buildSquareMap(botSquares);
                            selectedPowerUp.apply(botMap, GameSquare.this);
                            powerUpText = "";
                            selectedPowerUp = null;
                        } else {
                            hasBeenClicked = true;
                            squareColour = hoverColour;
                        }

                    }

                    if (allShipsSunk(botShips)) {
                        playerWon = true;
                        GameManager.setGameState(GameState.GAME_OVER);
                    }


                }
            });
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!(GameManager.getGameState() == GameState.GAME_RUNNING)) {
                return;
            }

            if (SetTurns.getTurn() == Turns.AI_TURN) {
                return;
            }

            if (hasBeenClicked) {
                return;
            }

            if (this.contains(e.getPoint()) && !squareHovering) {
                squareHovering = true;
                squareColour = hoverColour;
            } else if (!this.contains(e.getPoint()) && squareHovering) {
                squareHovering = false;
                squareColour = backgroundColour;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }

    }

    public class Ship {
        private final GameSquare[] squares;
        private final int length;

        Ship(int length) {
            this.length = length;
            this.squares = new GameSquare[length];

        }

        public void setSquare(int index, GameSquare square) {
            if (index >= 0 && index < length) {
                squares[index] = square;
            }
        }

        public Color getShipColour(int length) {
            return shipColours.get(length);
        }

        public boolean isSunk() {
            for (GameSquare square: squares) {
                if (!square.hasBeenClicked) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class SetTurns {
        private static Turns turn = Turns.PLAYER_TURN;

        public static void setTurnState(Turns newTurn) {
            turn = newTurn;
        }

        public static Turns getTurn() {
            return turn;
        }
    }

    private enum ShipDirection {
        LEFT, RIGHT, UP, DOWN
    }

    public enum Turns {
        PLAYER_TURN, AI_TURN
    }

    public enum PowerUp {
        BOMB(new Stats(1, "Bomb")) {
            @Override
            public void applyEffect(HashMap<String, GameSquare> squares, GameSquare square) {
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        int adjacentX = square.x + offsetX * square.width;
                        int adjacentY = square.y + offsetY * square.height;

                        GameSquare adjacentSquare = squares.get(adjacentX + "," + adjacentY);

                        if (adjacentSquare == null || adjacentSquare.hasBeenClicked) {
                            continue;
                        }
                        adjacentSquare.hasBeenClicked = true;

                    }
                }

            }
        },

        MISSILE(new Stats(2, "Missile")) {
            @Override
            public void applyEffect(HashMap<String, GameSquare> squares, GameSquare square) {
                for (GameSquare sq : squares.values()) {
                    if (sq.y == square.y) {
                        sq.hasBeenClicked = true;
                    }
                }
            }
        };

        public record Stats(int maxUses, String name) {}

        public final Stats stats;
        private Rectangle rect;
        private int usagesRemaining;

        PowerUp(Stats stats) {
            this.stats = stats;
            this.usagesRemaining = stats.maxUses();
        }

        public final void apply(HashMap<String, GameSquare> squares, GameSquare square) {
            if (usagesRemaining > 0) {
                applyEffect(squares, square);
                usagesRemaining--;
            }
        }

        protected abstract void applyEffect(HashMap<String, GameSquare> squares, GameSquare square);
    }

    public void resetPowerUps() {
        for (PowerUp powerUp: PowerUp.values()) {
            powerUp.usagesRemaining = powerUp.stats.maxUses();
        }
    }

}
