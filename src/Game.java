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
    // KI modifiziert HashMap ("LEFT" / "RIGHT") um Paddle zu steuern
    static HashSet<String> keys = new HashSet<>();
    // 0 = Nichts, -1 = Strafe, 1 = Belohnung

    static boolean debug = true;
    // ALAAAARM: AUF FALSE, WENN Q-TABLE > 1000 STATES
    static boolean qTableFrameShow = false;
    static int result;
    static boolean done = false;
    static int n_training_episodes = 10000;
    public final int padW = 40;
    public final double ballSize = 20;
    private final int padH = 10;
    private final int inset = 10;
    // ==================== DIESE WERTE FÜR OPTIMIERUNG ANPASSEN ====================
    double learning_rate = 0.8;
    double gamma = 0.85;
    double max_epsilon = 1.0;
    double min_epsilon = 0.05;

    // ==================== DIESE WERTE FÜR OPTIMIERUNG ANPASSEN ====================
    double decay_rate = 0.0000005;
    int gameSpeed = 1;
    double epsilon = 0;
    double[][] qTable;
    JFrame qTableFrame = new JFrame("Q-Table");
    DefaultTableModel qTableModel = new DefaultTableModel();
    JTable qTableDisplay = new JTable(qTableModel);
    int currentEpisode = 0;
    private int height, width;
    private boolean first;
    private int bottomPadX, topPadX;
    private double ballX;
    private double ballY;
    private double velX = 1;
    private double velY = 1;
    private int scoreTop, scoreBottom;

    /**
     * Konstruktor der Game Klasse
     */
    public Game() {
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        first = true;
        int delay = 5 / gameSpeed;

        Timer t = new Timer(delay, this);
        t.setInitialDelay(100);
        t.start();

        // Slider um die Spielgeschwindigkeit zu ändern
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, gameSpeed);
        speedSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                // Geschwindigkeit wird geändert wenn der Slider nicht mehr bewegt wird
                gameSpeed = source.getValue();
            }
        });
        this.add(speedSlider);

        // Q-Table Visualisierung
        if (qTableFrameShow) {
            qTableFrame.setSize(400, 400);
            qTableFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            qTableFrame.add(new JScrollPane(qTableDisplay));
            qTableFrame.setVisible(true);
        }
    }

    // ============================== UTIL FUNKTIONEN ==============================

    /**
     * Findet das Maximum in einer Spalte eines 2D Arrays
     *
     * @param arr  2D Array
     * @param cols Spalte
     * @return Maximalwert in einer Spalte
     */
    public static double findMaxInColumns(double[][] arr, int cols) {
        double max = 0;
        int index = Math.abs(cols) % arr.length;
        for (double i = 0; i < arr[index].length; i++) {
            if (arr[index][(int) i] > max) max = arr[index][(int) i];
        }
        return max;
    }

    /**
     * Berechnet die Standardabweichung eines Arrays
     *
     * @param numArray Array mit Zahlen
     * @return Standardabweichung des Arrays
     */
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

    /**
     * Berechnet den Durchschnitt eines Arrays
     *
     * @param array Array mit Zahlen
     * @return Durchschnitt des Arrays
     */
    public static double calculateAverage(double[] array) {
        return Arrays.stream(array).average().orElse(Double.NaN);
    }

    /**
     * Aktualisiert die Q-Table Visualisierung mit einem separaten Thread
     */
    public void updateQTableDisplay() {
        if (qTableFrameShow) {
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
    }

    /**
     * Debugging Funktion um Nachrichten in der Konsole auszugeben
     *
     * @param msg Nachricht
     */
    public void debugger(String msg) {
        if (debug) System.out.println(msg);
    }


    /**
     * Fügt "LEFT" oder "RIGHT" zur Liste der Tasten hinzu und entfernt es, wenn die Taste losgelassen wird
     * Paddle bewegt sich nur, wenn die Taste im HashSet ist
     *
     * @param action 0 = LEFT, 1 = RIGHT
     */
    public void keyModifier(int action) {
        switch (action) {
            case 0 -> keys.add("LEFT");
            case 1 -> keys.add("RIGHT");
        }
        // 100ms Wartezeit um die Tasten nicht zu schnell zu drücken
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

    /**
     * Gibt den aktuellen GameState zurück
     *
     * @return Aktueller GameState
     */
    public GameState getGameState() {
        return new GameState(ballX, ballY, bottomPadX);
    }

    // ============================== UTIL FUNKTIONEN ==============================

    /**
     * Zeichnet die Komponenten des Spiels (Paddles, Ball, Scores)
     *
     * @param g Graphics Objekt (wird automatisch übergeben)
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        height = getHeight();
        width = getWidth();

        // Initialisierung der Positionen
        if (first) {
            bottomPadX = width / 2 - padW / 2;
            topPadX = bottomPadX;
            ballX = (double) width / 2 - ballSize / 2;
            ballY = (double) height / 2 - ballSize / 2;
            first = false;
        }

        // Unteres Paddle
        Rectangle2D bottomPad = new Rectangle(bottomPadX, height - padH - inset, padW, padH);
        g2d.fill(bottomPad);

        // Oberes Paddle
        Rectangle2D topPad = new Rectangle(topPadX, inset, padW, padH);
        g2d.fill(topPad);

        // Ball
        Ellipse2D ball = new Ellipse2D.Double(ballX, ballY, ballSize, ballSize);
        g2d.fill(ball);

        // Punkte Anzeige und Geschwindigkeit des Spiels
        String scoreB = "Unten: " + scoreBottom;
        String scoreT = "Oben: " + scoreTop;
        String gameSpeedText = "GS: " + gameSpeed;
        String episode = "EP: " + currentEpisode;
        g2d.drawString(gameSpeedText, width / 2 - 20, 40);
        g2d.drawString(episode, width / 2 - 20, 60);
        g2d.drawString(scoreB, 10, height / 2);
        g2d.drawString(scoreT, width - 50, height / 2);
    }

    /**
     * Bearbeitet die Events (Kollisionen, Punkte, etc.)
     * Runtime Methode
     *
     * @param e ActionEvent Objekt (wird automatisch übergeben)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Seitenwände (links / rechts): Richtung des Balls bei Kollision ändern
        if (ballX < 0 || ballX > width - ballSize) {
            velX = -velX;
        }
        // Wände Oben/Unten: Richtung des Balls bei Kollision ändern und Punkte aktualisieren
        // obere Wand
        if (ballY < 0) {
            velY = -velY;
            ++scoreBottom;

            // Reward
            done = true;
        }
        // Untere Wand
        if (ballY + ballSize > height) {
            velY = -velY;
            ++scoreTop;
            done = true;

            // Strafe
            result = -10 * (int) (1 + Math.abs(bottomPadX - ballX));
        }


        // Q-Table KI; ändert Richtung des Balls bei Kollision
        if (ballY + ballSize >= height - padH - inset && velY > 0)
            if (ballX + ballSize >= bottomPadX && ballX <= bottomPadX + padW) {
                velY = -velY;
                done = true;


                // Belohnung
                result = 100;
            }


        // Vordefinierte KI; ändert Richtung des Balls bei Kollision
        if (ballY <= padH + inset && velY < 0) if (ballX + ballSize >= topPadX && ballX <= topPadX + padW) velY = -velY;

        // Aktualisiert Position des Balls
        ballX += velX * gameSpeed;
        ballY += velY * gameSpeed;

        // Q-Table KI; ändert Position des Paddles
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

        // Vordefinierte KI; ändert Position des Paddles
        double delta = ballX - topPadX;
        if (delta > 0) {
            topPadX += (topPadX < width - padW) ? SPEED : 0;
        } else if (delta < 0) {
            topPadX -= (topPadX > 0) ? SPEED : 0;
        }

        repaint();
    }


    /**
     * Initialisiert die Q-Table mit den gegebenen Parametern
     *
     * @param rows    Anzahl der Zeilen
     * @param columns Anzahl der Spalten
     */
    public void initQTable(int rows, int columns) {
        qTable = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                qTable[i][j] = 0;
            }
        }
        updateQTableDisplay();
        debugger("QTable initialisiert");
        training();
    }

    /**
     * Epsilon-Greedy Policy um die beste Aktion in einem gegebenen Zustand zu finden (mit der Wahrscheinlichkeit Epsilon)
     * Ausgleich zwischen Exploration und Exploitation
     *
     * @param qTable  Q-Table
     * @param state   Aktueller GameState
     * @param epsilon Wahrscheinlichkeit für Exploration
     * @return Beste Aktion für den gegebenen GameState
     */
    public double epsilon_greedy_policy(double[][] qTable, GameState state, double epsilon) {

        // Zufallszahl zwischen 0 und 1
        double rand = ThreadLocalRandom.current().nextDouble(0, 1);
        double action = 0.0;
        int index = Math.abs(state.hashCode()) % qTable.length;

        if (rand > epsilon) {
            // Aktion des höchsten Q-Wertes ist die beste Aktion (Exploitation)
            double cMax = 0;
            for (int i = 0; i < qTable[index].length; i++) {
                if (qTable[index][i] > cMax) cMax = qTable[index][i];
            }

        } else {
            // Aktion wird zufällig gewählt (Exploration)
            action = ThreadLocalRandom.current().nextDouble(0, 2);
        }

        return action;
    }

    /**
     * Trainiert die KI mit Bellman's equation und epsilon-greedy policy
     * Bellman's Formel benutzt den aktuellen Q-Wert und den maximalen Q-Wert des nächsten Zustandes und aktualisiert den Q-Wert des aktuellen Zustandes
     */
    public void training() {
        double[] episodeRewards = new double[n_training_episodes];


        // Trainiere für n Trainingsepisoden (eine Episode ist bis der Ball die untere Wand berührt oder das Paddle)
        for (int episode = 0; episode < n_training_episodes; episode++) {
            result = 0;
            done = false;
            currentEpisode = episode;

            GameState state = getGameState();

            // Epsilon wird mit jedem Trainingsschritt kleiner (exploration wird weniger, da die KI mehr lernt)
            epsilon = min_epsilon + (max_epsilon - min_epsilon) * Math.exp(-decay_rate * episode);
            double action = epsilon_greedy_policy(qTable, state, epsilon);

            // Führe Aktion aus
            keyModifier((int) action);

            // ein eindeutiger Integer Wert des nächsten GameStates wird berechnet (Hashcode Index)
            // |x| wird benutzt, um negative indexe für die Q-Table zu vermeiden und Modulo wird benutzt, um den Index nicht zu groß werden zu lassen (zws. 0 und qTable.length - 1)
            int index = Math.abs(state.hashCode()) % qTable.length;

            // Aktualisiere Q-Table mit Bellman's equation
            qTable[index][(int) action] = qTable[index][(int) action] + learning_rate * (result + gamma * findMaxInColumns(qTable, index) - qTable[index][(int) action]);

            updateQTableDisplay();

            // Speichere Reward der Episode
            episodeRewards[episode] += result;

            // Debugging von Rewards, nur wenn Reward != 0
            if (result != 0) debugger("Episode: " + episode + ", Belohnung: " + result);

            if (done) {
                // Evaluation von Durchschnitt und Standardabweichung der Rewards
                // Durchschnitt der Belohnungen zeigt, wie gut die KI ist (Je höher, desto besser)
                // Standardabweichung der Belohnungen zeigt wie Konsistent die KI ist (Je niedriger, desto konstanter)
                debugger("Episode: " + episode + ", Durchschnittliche Belohnung: " + calculateAverage(episodeRewards) + ", Standardabweichung: " + calculateSD(episodeRewards));

                // "Reset" des Spiels
                done = false;
                debugger("Game resetted");
            }
        }

    }


    // ==================== USER-INPUT METHODEN ====================

    /**
     * Kann ignoriert werden
     *
     * @param e -
     */
    @Override
    public void keyTyped(KeyEvent e) {
        // Necessary for KeyListener class. DO NOT DELETE!
    }

    /**
     * Fügt "LEFT" oder "RIGHT" zur Liste der Tasten hinzu
     *
     * @param e KeyEvent Objekt (wird automatisch übergeben)
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT -> keys.add("LEFT");
            case KeyEvent.VK_RIGHT -> keys.add("RIGHT");
        }
    }

    /**
     * Entfernt "LEFT" oder "RIGHT" von Liste der Tasten, wenn die Taste losgelassen wird
     *
     * @param e KeyEvent Objekt (wird automatisch übergeben)
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT -> keys.remove("LEFT");
            case KeyEvent.VK_RIGHT -> keys.remove("RIGHT");
        }
    }
}
