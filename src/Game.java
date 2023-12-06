import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.*;


public class Game extends JPanel implements KeyListener, ActionListener {

    // TODO: AI modifies HashMap ("LEFT" / "RIGHT") in order to control Paddle
    private final HashSet<String> keys = new HashSet<>();

    private final int padH = 10;
    private final int padW = 40;
    private final int inset = 10;
    private final double ballSize = 20;
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
        Timer t = new Timer(5, this);
        t.setInitialDelay(100);
        t.start();
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
        // side walls (left / right): change direction on collision
        if (ballX < 0 || ballX > width - ballSize) {
            velX = -velX;
        }
        // top / down walls (top / bottom): change direction on collision and update score
        if (ballY < 0) {
            velY = -velY;
            ++scoreBottom;
        }
        if (ballY + ballSize > height) {
            velY = -velY;
            ++scoreTop;
        }

        // bottom pad (player) change direction on collision
        if (ballY + ballSize >= height - padH - inset && velY > 0)
            if (ballX + ballSize >= bottomPadX && ballX <= bottomPadX + padW)
                velY = -velY;

        // top pad (AI) change direction on collision
        if (ballY <= padH + inset && velY < 0)
            if (ballX + ballSize >= topPadX && ballX <= topPadX + padW)
                velY = -velY;
                // TODO: Set reward

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

        getBestAction();

        repaint();
    }

    /**
     * TODO: Implement Q-Learning algorithm
     */

    // TODO: Initalize Q-Table: (3x12) Double Array

    // TODO: Define Epsilon-greedy policy

    // Do EXPLORATION with chance (Epsilon) and EXPLOITATION with chance of (1-Epsilon)
    public double epsilon_greedy_policy(double[][] Qtable, double state, double epsilon){

        // Create random number between 0 and 1
        double rand = ThreadLocalRandom.current().nextDouble(0, 1);
        double action;

        if (rand > epsilon){
            // action is set to max of QTable

            //action = Qtable[State][]
        } else {
            // action is set to random action?!?
            //action = somn like a sample
        }

        return action;
    }

    // TODO: Define Greedy-Policy

    // TODO: Define Hyperparameters

    // TODO: Model Training

    // TODO: OTHER STEPS THAT MIRO HAD NO BRAIN POWER TO WRITE DOWN ANYMORE

    public void getBestAction(){
        //Bellman's equation
        //Q(s,a) = Q(s,a) + α * (r + γ * max(Q(s',a')) - Q(s,a))
        //
        //The equation breaks down as follows:
        //
        //Q(s, a) represents the expected reward for taking action a in state s.
        //The actual reward received for that action is referenced by r while s' refers to the next state.
        //The learning rate is α and γ is the discount factor.
        //The highest expected reward for all possible actions a' in state s' is represented by max(Q(s', a')).
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Necessary for KeyListener class. DO NOT DELETE!
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT:
                keys.add("LEFT");
                break;
            case KeyEvent.VK_RIGHT:
                keys.add("RIGHT");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT:
                keys.remove("LEFT");
                break;
            case KeyEvent.VK_RIGHT:
                keys.remove("RIGHT");
                break;
        }
    }
}