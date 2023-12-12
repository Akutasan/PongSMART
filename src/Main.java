import javax.swing.*;

/**
 * Erstellt von Enzo, Jakob und Miro
 */
public class Main {

    public static void main(String[] args) {
        // Erstellt ein JFrame und fügt das Game-Objekt hinzu
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


        // Rechnung ist größe des Spielfeldes - größe des Balls * Anzahl der möglichen Positionen des Schlägers um alle möglichen States zu berechnen
        // Auskommentiert da es zu viele States sind und die Q-Table zu groß wird
         int ballSize = (int) g.ballSize;
         int padW = g.padW;
         int numStates = (width - ballSize) * (height - ballSize) * (width - padW);

        // Initialisiert die Q-Table mit (erstmal) 1000 States und 3 Aktionen (links, rechts, nichts)
        // Größere Q-Table = Bessere KI, aber längere Berechnungszeit
        g.initQTable(numStates, 3);
    }

}