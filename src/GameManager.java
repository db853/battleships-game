public class GameManager {
    private static GameState gameState = GameState.START_MENU;

    public static GameState getGameState() {
        return gameState;
    }

    public static void setGameState(GameState newState) {
        gameState = newState;
    }
}
