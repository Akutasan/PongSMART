import java.util.ArrayList;
import java.util.Random;

public class QLearning {

    private static int statesCount = 0;
    private static int y_dist;
    private static int x_pos_balls;
    private static int y_pos_racket;
    private static int true_y_pos_racket;
    private static int y_paddle_offset;
    private static Ball ball;
    private static int currentPlayer = 1;
    private final double alpha = 0.1; // Learning rate
    private final double gamma = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future
    private final int reward = 1000;
    private final int penalty = -10;
    private int[][][] R;       // Reward lookup
    private double[][][] Q;    // Q learning


    public static void main(String[] args) {
        QLearning ql = new QLearning();
        Pong activeGame = new Pong();

        y_dist = (activeGame.getPanel().getBall().getY() - activeGame.getPanel().getPlayer(currentPlayer).getY());
        x_pos_balls = activeGame.getPanel().getBall().getX();
        y_pos_racket = activeGame.getPanel().getPlayer(currentPlayer).getY();
        true_y_pos_racket = activeGame.getPanel().getPlayer(currentPlayer).getBounds().height / 2;
        ball = activeGame.getPanel().getBall();
        statesCount = Pong.WIDTH * Pong.HEIGHT;

        ql.init();
        ql.calculateQ();
        ql.printQ();
        ql.printPolicy();
    }

    public void init(PongPanel game) {

        R = new int[statesCount][statesCount][statesCount];
        Q = new double[statesCount][statesCount][statesCount];

        // We will navigate through the reward matrix R using k index
        for (int k = 0; k < statesCount; k++) {
            // We will navigate with i and j through the maze, so we need
            // to translate k into i and j

            // Fill in the reward matrix with -1
            for (int s = 0; s < statesCount; s++) {
                for (int a = 0; a < statesCount; a++) {
                    R[s][a][k] = -1;
                }
            }

            // TODO: Reward 0 when ball does not hit racket
            // Reward 1: when ball hits racket
            // Reward -1: when ball hits wall
            // Reward 100: when ball hits opposite wall
            // Paddle doesn't go OOB

            // Paddle Down
            if (y_pos_racket - true_y_pos_racket > Pong.HEIGHT * -1) { // Can it go down?
                if (ball.checkCollision() == game.getPlayer(currentPlayer) || game.getBall().getX() < 0) { // Is ball hitting racket or opposite wall
                    R[k][y_pos_racket][x_pos_balls] = reward;
                } else {
                    R[k][y_pos_racket][x_pos_balls] = penalty;
                }
            }

            // Paddle Up
            if (y_pos_racket + true_y_pos_racket < Pong.HEIGHT) { // Can it go up?
                if (ball.checkCollision() == game.getPlayer(currentPlayer) || game.getBall().getX() < 0) { // Is ball hitting racket or opposite wall
                    R[k][y_pos_racket][x_pos_balls] = reward;
                } else {
                    R[k][y_pos_racket][x_pos_balls] = penalty;
                }
            }

            // Try to move down in the maze
//                int goDown = i + 1;
//                if (goDown < mazeHeight) {
//                    int target = goDown * mazeWidth + j;
//                    if (maze[goDown][j] == '0') {
//                        R[k][target] = 0;
//                    } else if (maze[goDown][j] == 'F') {
//                        R[k][target] = reward;
//                    } else {
//                        R[k][target] = penalty;
//                    }
//                }
        }

        initializeQ();
        printR(R);
    }

    //Set Q values to R values
    void initializeQ() {
        for (int i = 0; i < statesCount; i++) {
            for (int j = 0; j < statesCount; j++) {
                for (int k = 0; k < statesCount; k++) {
                    Q[i][j][k] = R[i][j][k];
                }
            }
        }
    }

    // Used for debug
    void printR(int[][][] matrix) {
        System.out.printf("%25s", "States: ");
        for (int i = 0; i <= 8; i++) {
            System.out.printf("%4s", i);
        }
        System.out.println();

        for (int i = 0; i < statesCount; i++) {
            System.out.print("Possible states from " + i + " :[");
            for (int j = 0; j < statesCount; j++) {
                System.out.printf("%4s", matrix[i][j]);
            }
            System.out.println("]");
        }
    }

    void calculateQ() {
        Random rand = new Random();

        for (int i = 0; i < 1000; i++) { // Train cycles
            // Select random initial state
            int crtState = rand.nextInt(statesCount);

            while (!isFinalState(crtState)) {
                int[] actionsFromCurrentState = possibleActionsFromState(crtState);

                // Pick a random action from the ones possible
                int index = rand.nextInt(actionsFromCurrentState.length);
                int nextState = actionsFromCurrentState[index];

                // Q(state,action)= Q(state,action) + alpha * (R(state,action) + gamma * Max(next state, all actions) - Q(state,action))
                double q = Q[crtState][nextState][];
                double maxQ = maxQ(nextState);
                int r = R[crtState][nextState][];

                double value = q + alpha * (r + gamma * maxQ - q);
                Q[crtState][nextState] = new double[]{value};

                crtState = nextState;
            }
        }
    }

    boolean isFinalState(int state) {
        // TODO: Implement this

        return false;
    }

    // Returns the best action from the Q matrix
    int[] possibleActionsFromState(int state) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < statesCount; i++) {
            for (int j = 0; j < statesCount; j++) {
                if (R[state][i][j] != -1) {
                    result.add(i);
                }
            }
        }

        return result.stream().mapToInt(i -> i).toArray();
    }

    double maxQ(int nextState) {
        int[] actionsFromState = possibleActionsFromState(nextState);
        //the learning rate and eagerness will keep the W value above the lowest reward
        double maxValue = -10;
        for (int nextAction : actionsFromState) {
            double value = Q[nextState][nextAction][];

            if (value > maxValue)
                maxValue = value;
        }
        return maxValue;
    }

    void printPolicy() {
        System.out.println("\nPrint policy");
        for (int i = 0; i < statesCount; i++) {
            System.out.println("From state " + i + " goto state " + getPolicyFromState(i));
        }
    }

    int getPolicyFromState(int state) {
        int[] actionsFromState = possibleActionsFromState(state);

        double maxValue = Double.MIN_VALUE;
        int policyGotoState = state;

        // Pick to move to the state that has the maximum Q value
        for (int nextState : actionsFromState) {
            double value = Q[state][nextState];

            if (value > maxValue) {
                maxValue = value;
                policyGotoState = nextState;
            }
        }
        return policyGotoState;
    }

    void printQ() {
        System.out.println("Q matrix");
        for (int i = 0; i < Q.length; i++) {
            System.out.print("From state " + i + ":  ");
            for (int j = 0; j < Q[i].length; j++) {
                System.out.printf("%6.2f ", (Q[i][j]));
            }
            System.out.println();
        }
    }
}