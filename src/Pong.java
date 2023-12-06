import javax.swing.*;
import java.awt.*;

public class Pong extends JFrame {
    public final static int WIDTH = 700, HEIGHT = 450;
    private final PongPanel panel;

    public Pong() {
        setSize(WIDTH, HEIGHT);
        setTitle("Pong");
        setBackground(Color.WHITE);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        panel = new PongPanel(this);
        add(panel);
    }

    public PongPanel getPanel() {
        return panel;
    }
}