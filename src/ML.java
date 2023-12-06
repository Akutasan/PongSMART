public class ML {
    public static double distance = 0;

    public static void main(String[] args) {
        Pong activeGame = new Pong();
    }

    public static boolean bestAction(double position) {

        // Actions: Go up, Go down
        if (distance - position > 0) {
            return true;
        }
        else {
            return false;
        }
    }
}
