import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class Game extends JPanel implements KeyListener, ActionListener {
    // AI modifies HashMap ("LEFT" / "RIGHT") in order to control Paddle
    static HashSet<String> keys = new HashSet<>();
    // 0 = Nothing, -1 = Penalty, 1 = Reward
    static int result;
    static boolean done = false;
    public final int padW = 40;
    public final double ballSize = 20;
    private final int padH = 10;
    private final int inset = 10;
    // ==================== MODIFY THESE VALUES FOR OPTIMAL RESULTS ====================
    double learning_rate = 0.8;
    double gamma = 0.85;
    double max_epsilon = 1.0;
    double min_epsilon = 0.05;
    double decay_rate = 0.0000005;

    // ==================== MODIFY THESE VALUES FOR OPTIMAL RESULTS ====================

    int gameSpeed = 1;
    int n_training_episodes = 10000;
    double epsilon = 0;
    double[][] qTable;
    JFrame qTableFrame = new JFrame("Q-Table");
    DefaultTableModel qTableModel = new DefaultTableModel();
    JTable qTableDisplay = new JTable(qTableModel);
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

    public Game() {
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        first = true;
        int delay = 5 / gameSpeed;

        Timer t = new Timer(delay, this);
        t.setInitialDelay(100);
        t.start();

        // Add a cool motherfucking slider sheesh (it gets jankier the higher we go so I cap at 20 hehe)
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, gameSpeed);
        speedSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                // Update the game speed when the slider value changes
                gameSpeed = source.getValue();
            }
        });
        this.add(speedSlider);

        // Q-Table Frame to better watch
        qTableFrame.setSize(400, 400);
        qTableFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        qTableFrame.add(new JScrollPane(qTableDisplay));
        qTableFrame.setVisible(true);
    }

    // ============================== UTIL FUNCTIONS ==============================
    public static double findMaxInColumns(double[][] arr, int cols) {
        double max = 0;
        int index = Math.abs(cols) % arr.length;
        for (double i = 0; i < arr[index].length; i++) {
            if (arr[index][(int) i] > max) max = arr[index][(int) i];
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

    // Ignore this, nerdy stuff
    public void updateQTableDisplay() {
        SwingUtilities.invokeLater(() -> {
            Object[][] qTableObjects = new Object[qTable.length][qTable[0].length];
            for (int i = 0; i < qTable.length; i++) {
                for (int j = 0; j < qTable[0].length; j++) {
                    qTableObjects[i][j] = qTable[i][j];
                }
            }
            qTableModel.setDataVector(qTableObjects, new String[qTable[0].length]);
        });
    }

    public void debugger(String msg) {
        System.out.println(msg);
    }

    // Method to add left or right to the list of keys and remove it when the key is released
    public void keyModifier(int action) {
        switch (action) {
            case 0 -> keys.add("LEFT");
            case 1 -> keys.add("RIGHT");
        }
        // wait for 100ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        switch (action) {
            case 0 -> keys.remove("LEFT");
            case 1 -> keys.remove("RIGHT");
        }
    }

    // ============================== UTIL FUNCTIONS ==============================

    // get and return gameState
    public GameState getGameState() {
        return new GameState(ballX, ballY, bottomPadX);
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
        String gameSpeedText = "GS: " + gameSpeed;
        g2d.drawString(gameSpeedText, width / 2 - 20, 40);
        g2d.drawString(scoreB, 10, height / 2);
        g2d.drawString(scoreT, width - 50, height / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // side walls (left / right): change direction on collision
        if (ballX < 0 || ballX > width - ballSize) {
            velX = -velX;
        }
        // top / down walls (top / bottom): change direction on collision and update score
        // Top Wall
        if (ballY < 0) {
            velY = -velY;
            ++scoreBottom;

            // Reward
            done = true;
        }
        // Bottom Wall
        if (ballY + ballSize > height) {
            velY = -velY;
            ++scoreTop;
            done = true;

            // Penalty
            result = -10 * (int) (1+Math.pow(bottomPadX-ballX, 2));
        }

        // Q-Table AI change direction on collision
        if (ballY + ballSize >= height - padH - inset && velY > 0)
            if (ballX + ballSize >= bottomPadX && ballX <= bottomPadX + padW) {
                velY = -velY;
                done = true;

                // Reward
                result = 10;
            }


        // Pre-Made AI change direction on collision
        if (ballY <= padH + inset && velY < 0) if (ballX + ballSize >= topPadX && ballX <= topPadX + padW) velY = -velY;

        // update ball position
        ballX += velX * gameSpeed;
        ballY += velY * gameSpeed;

        // Our Q-Table AI
        int SPEED = gameSpeed;
        if (keys.size() == 1) {
            if (keys.contains("LEFT")) {
                // Prevent pad from going out of bounds
                bottomPadX -= (bottomPadX > 0) ? SPEED : 0;
            } else if (keys.contains("RIGHT")) {
                // Prevent pad from going out of bounds
                bottomPadX += (bottomPadX < width - padW) ? SPEED : 0;
            }
        }

        // Opponent AI - Pre-made
        double delta = ballX - topPadX;
        if (delta > 0) {
            topPadX += (topPadX < width - padW) ? SPEED : 0;
        } else if (delta < 0) {
            topPadX -= (topPadX > 0) ? SPEED : 0;
        }

        repaint();
    }


    /**
     * Initialize Q-Table with 0 and train it
     *
     * @param rows    Number of rows
     * @param columns Number of columns
     */
    public void initQTable(int rows, int columns) {
        qTable = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                qTable[i][j] = 0;
            }
        }
        updateQTableDisplay();
        debugger("QTable initialized");
        training();
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
    public double epsilon_greedy_policy(double[][] qTable, GameState state, double epsilon) {

        // Create random number between 0 and 1
        double rand = ThreadLocalRandom.current().nextDouble(0, 1);
        double action = 0.0;
        int index = Math.abs(state.hashCode()) % qTable.length;

        if (rand > epsilon) {
            // get max of QTable in one column
            double cMax = 0;
            for (int i = 0; i < qTable[index].length; i++) {
                if (qTable[index][i] > cMax) cMax = qTable[index][i];
            }

        } else {
            // action is set to random action
            action = ThreadLocalRandom.current().nextDouble(0, 2);
        }

        return action;
    }

    /**
     * Using Bellman's equation to update Q-Table and train it
     */
    public void training() {
        double[] episodeRewards = new double[n_training_episodes];

        // Loop through episodes
        for (int episode = 0; episode < n_training_episodes; episode++) {
            result = 0;
            done = false;

            GameState state = getGameState();

            // Reduce epsilon (because we need less and less exploration)
            epsilon = min_epsilon + ( max_epsilon - min_epsilon ) * Math.exp( -decay_rate * episode );
            double action = epsilon_greedy_policy( qTable, state, epsilon );

            // Add action to list of actions
            keyModifier( (int) action );

            // Hash code of state to find index in Q-Table (modulo to prevent out of bounds error, idk if this works btw)
            int index = Math.abs( state.hashCode() ) % qTable.length;

            // Update Q-Table with Bellman's equation (weighted sum of current Q-value and learned value)
            qTable[index][(int) action] = qTable[index][(int) action] + learning_rate * ( result + gamma * findMaxInColumns( qTable, index ) - qTable[index][(int) action] );

            updateQTableDisplay();

            // Add reward
            episodeRewards[episode] += result;

            // Show episode and reward only if reward is not 0
            if (result != 0) System.out.println("Episode: " + episode + ", Reward: " + result);

            if (done) {
                // Do evaluation
                double avg = calculateAverage(episodeRewards);
                double sd = calculateSD(episodeRewards);
                System.out.println("Episode: " + episode + ", Average Reward: " + avg + ", Standard Deviation: " + sd);

                // Reset game
                done = false;
                debugger("Game reset");
            }
        }
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
