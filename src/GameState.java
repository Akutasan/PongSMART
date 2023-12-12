import java.util.Objects;

/**
 * Klasse, die den Zustand des Spiels zu einem bestimmten Zeitpunkt darstellt.
 * Wird verwendet, um den Zustand des Spiels in einer HashMap zu speichern, sodass die KI
 * Entscheidungen auf der Grundlage des aktuellen Spielzustands treffen kann.
 */
public class GameState {
    private final double ballX;
    private final double ballY;
    private final int bottomPadX;

    /**
     * Konstruktor für GameState
     *
     * @param ballX      x-Koordinate des Balls
     * @param ballY      y-Koordinate des Balls
     * @param bottomPadX x-Koordinate des unteren Schlägers (Q-Table KI)
     */
    public GameState(double ballX, double ballY, int bottomPadX) {
        // Standard gamestate definiert als 0,0,0
        // gibt 0,0,0 weiter, wenn der Ball sich auf den Gegner zubewegt

        this.ballX = ballX % .1f;
        this.ballY = ballY % .1f;
        this.bottomPadX = bottomPadX;
    }

    /**
     * Gibt Hashcode des aktuellen GameState zurück
     *
     * @return Hashcode des aktuellen GameState
     */
    @Override
    public int hashCode() {
        return Objects.hash(ballX, ballY, bottomPadX);
    }
}
