import java.util.Objects;

public class GameState {
    private final double ballX;
    private final double ballY;
    private final int bottomPadX;

    public GameState(double ballX, double ballY, int bottomPadX) {
        // default gamestate defined as 0,0,0
        // passes 0,0,0 when ball is moving towards oppentent

        this.ballX = ballX %.1f;
        this.ballY = ballY %.1f;
        this.bottomPadX = bottomPadX;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        return Double.compare(gameState.ballX, ballX) == 0 && Double.compare(gameState.ballY, ballY) == 0 && bottomPadX == gameState.bottomPadX;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ballX, ballY, bottomPadX);
    }
}
