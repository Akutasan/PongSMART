import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;


public class Game extends JPanel implements KeyListener, ActionListener {

    // TODO: AI modifies HashMap ("LEFT" / "RIGHT") in order to control Paddle
    static HashSet<String> keys = new HashSet<>();
    // 0 = Nothing, -1 = Penalty, 1 = Reward

    static int result;
    static boolean done = false;
    private final HashMap<Double, String> listOfOptions = new HashMap<>();
    private final int padH = 10;
    private final int padW = 40;
    private final int inset = 10;
    private final double ballSize = 20;
    int n_training_episodes = 10000;
    double learning_rate = 0.7;
    double gamma = 0.95;
    double max_epsilon = 1.0;
    double min_epsilon = 0.05;
    double decay_rate = 0.0005;
    double epsilon = 0;
    private int height, width;
    private boolean first;
    private int bottomPadX, topPadX;
    // ball
    private double ballX;
    private double ballY;
    private double velX = 1;
    private double velY = 1;
    // score
    private int scoreTop, scoreBottom;

    {
        listOfOptions.put(0.0, "LEFT");
        listOfOptions.put(1.0, "RIGHT");
        listOfOptions.put(2.0, "");
    }

    public Game() {
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        first = true;
        Timer t = new Timer(5, this);
        t.setInitialDelay(100);
        t.start();
    }

    // get and return gameState
    double[] gameState = new double[2];
    public double[] getGameState(){
        gameState[0] = ballX;
        gameState[1] = ballY;
        gameState[2] = bottomPadX;

        return gameState;
    }

    public static double findMaxInColumns(double[][] arr, int cols) {
        double max = 0;
        for (double i = 0; i < cols; i++) {
            if (arr[(int) i][cols] > max) max = arr[(int) i][cols];
        }
        return max;
    }

    public static double calculateSD(double[] numArray) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }

    public static double calculateAverage(double[] array) {
        return Arrays.stream(array).average().orElse(Double.NaN);
    }

    public static double findMaxInTable(double[][] arr) {
        double max = 0;
        for (double[] doubles : arr) {
            for (double aDouble : doubles) {
                if (aDouble > max) max = aDouble;
            }
        }
        return max;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        height = getHeight();
        width = getWidth();

        // initial positioning
        if (first) {
            bottomPadX = width / 2 - padW / 2;
            topPadX = bottomPadX;
            ballX = (double) width / 2 - ballSize / 2;
            ballY = (double) height / 2 - ballSize / 2;
            first = false;
        }

        // bottom pad
        Rectangle2D bottomPad = new Rectangle(bottomPadX, height - padH - inset, padW, padH);
        g2d.fill(bottomPad);

        // top pad
        Rectangle2D topPad = new Rectangle(topPadX, inset, padW, padH);
        g2d.fill(topPad);

        // ball
        Ellipse2D ball = new Ellipse2D.Double(ballX, ballY, ballSize, ballSize);
        g2d.fill(ball);

        // scores
        String scoreB = "Bottom: " + scoreBottom;
        String scoreT = "Top: " + scoreTop;
        g2d.drawString(scoreB, 10, height / 2);
        g2d.drawString(scoreT, width - 50, height / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        result = 0;
        done = false;

        // side walls (left / right): change direction on collision
        if (ballX < 0 || ballX > width - ballSize) {
            velX = -velX;
        }
        // top / down walls (top / bottom): change direction on collision and update score
        // Top Wall
        if (ballY < 0) {
            velY = -velY;
            ++scoreBottom;
            done = true;
        }
        // Bottom Wall
        if (ballY + ballSize > height) {
            velY = -velY;
            ++scoreTop;
            done = true;

            // Penalty
            result = -1;
        }

        // bottom pad (player) change direction on collision
        if (ballY + ballSize >= height - padH - inset && velY > 0)
            if (ballX + ballSize >= bottomPadX && ballX <= bottomPadX + padW) {
                velY = -velY;
                done = true;

                // It's like a reward
                result = 1;
            }


        // top pad (AI) change direction on collision
        if (ballY <= padH + inset && velY < 0) if (ballX + ballSize >= topPadX && ballX <= topPadX + padW) velY = -velY;

        // update ball position
        ballX += velX;
        ballY += velY;

        // pressed keys
        // pad
        int SPEED = 1;
        if (keys.size() == 1) {
            if (keys.contains("LEFT")) {
                // Prevent pad from going out of bounds
                bottomPadX -= (bottomPadX > 0) ? SPEED : 0;
            } else if (keys.contains("RIGHT")) {
                // Prevent pad from going out of bounds
                bottomPadX += (bottomPadX < width - padW) ? SPEED : 0;
            }
        }

        // AI
        double delta = ballX - topPadX;
        if (delta > 0) {
            topPadX += (topPadX < width - padW) ? SPEED : 0;
        } else if (delta < 0) {
            topPadX -= (topPadX > 0) ? SPEED : 0;
        }

        repaint();
    }


    // TODO: Initialize Q-Table: (3x12) Double Array

    /**
     * Initialize Q-Table with 0
     *
     * @param rows    Number of rows
     * @param columns Number of columns
     * @return Initialized Q-Table
     */
    public double[][] initQTable(int rows, int columns) {
        double[][] qTable = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                qTable[i][j] = 0;
            }
        }
        return qTable;
    }

    /**
     * Epsilon Greedy Policy is used to find the best action in a given state with a chance of (1-Epsilon)
     *
     * @param qTable  Table of Q-Values
     * @param state   Current state
     * @param epsilon Chance of exploration
     * @return Best action in a given state
     */
    // Do EXPLORATION with chance (Epsilon) and EXPLOITATION with chance of (1-Epsilon)
    public double epsilon_greedy_policy(double[][] qTable, double state, double epsilon) {

        // Create random number between 0 and 1
        double rand = ThreadLocalRandom.current().nextDouble(0, 1);
        double action = 0.0;

        if (rand > epsilon) {
            // get max of QTable in one column
            double cMax = 0;
            for (int i = 0; i < qTable[(int) state].length; i++) {
                if (qTable[(int) state][i] > cMax) cMax = qTable[(int) state][i];
            }

        } else {
            // action is set to random action?!?
            action = ThreadLocalRandom.current().nextDouble(0, 2);
        }

        return action;
    }

    /**
     * Greedy Policy is used to find the best action in a given state
     *
     * @param qTable Table of Q-Values
     * @param state  Current state
     * @return Best action in a given state
     */
    public double greedy_policy(double[][] qTable, double state) {
        double cMax = 0;
        for (int i = 0; i < qTable[(int) state].length; i++) {
            if (qTable[(int) state][i] > cMax) cMax = qTable[(int) state][i];
        }
        return cMax;
    }

    /**
     * Using Bellman's equation to update Q-Table
     *
     * @param qTable Table of Q-Values
     * @return Updated Q-Table with Bellman's equation
     */
    public double[][] training(double[][] qTable) {
        // Loop through episodes
        for (int episode = 0; episode < n_training_episodes; episode++) {

            int state = bottomPadX;

            // Reduce epsilon (because we need less and less exploration)
            epsilon = min_epsilon + (max_epsilon - min_epsilon) * Math.exp(-decay_rate * episode);
            double action = epsilon_greedy_policy(qTable, state, epsilon);

            // Add action to list of actions
            keys.add(listOfOptions.get(action));

            // Update Q-Table with Bellman's equation TODO: WTF IS "QTable, State"
            qTable[state][(int) action] = qTable[state][(int) action] + learning_rate * (result + gamma * findMaxInColumns(qTable, state) - qTable[state][(int) action]);

            if (done) break;
        }
        // print trained QTable
        for (int i = 0; i < qTable.length; i++)
            for (int j = 0; j < qTable[i].length; j++)
                System.out.print(qTable[i][j] + " ");

        // return trained QTable
        return qTable;
    }

    // TODO: Eval Agent

    public double[] eval(double[][] QTable) {
        double[] episode_reward = new double[0];

        // TODO: Define state
        double action = findMaxInColumns(QTable, state);
        int reward = result;

        // Append reward to episode_reward
        episode_reward = Arrays.copyOf(episode_reward, episode_reward.length + 1);
        episode_reward[episode_reward.length - 1] = reward;

        double SD = calculateSD(episode_reward);
        double Avg = calculateAverage(episode_reward);

        return new double[]{SD, Avg};
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Necessary for KeyListener class. DO NOT DELETE!
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT -> keys.add("LEFT");
            case KeyEvent.VK_RIGHT -> keys.add("RIGHT");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT -> keys.remove("LEFT");
            case KeyEvent.VK_RIGHT -> keys.remove("RIGHT");
        }
    }
}