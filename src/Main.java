import javax.swing.*;


public class Main {

    public static void main(String[] args) {
        JFrame frm = new JFrame();
        frm.setTitle("Pong");
        Game g = new Game();
        frm.setContentPane(g);
        int width = 300;
        int height = 700;
        frm.setSize(width, height);
        frm.setResizable(false);
        frm.setVisible(true);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Number of states
        int ballSize = (int) g.ballSize;
        int padW = g.padW;

        // Factoring in when ball hits wall we get this (commented out because of heapspace):
//        int numStates = (width - ballSize) * (height - ballSize) * (width - padW);

        g.initQTable(1000, 3);
    }

}