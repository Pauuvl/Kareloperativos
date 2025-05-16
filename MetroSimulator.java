import kareltherobot.*;
import java.awt.Color;
import java.util.concurrent.locks.ReentrantLock;

public class MetroSimulator implements Directions {

    static final ReentrantLock lineaC = new ReentrantLock(); // Exclusividad de Línea C
    static final ReentrantLock[][] posiciones = new ReentrantLock[30][30]; // Control de colisiones

    public static void main(String[] args) {
        World.readWorld("MetroMed.kwld");
        World.setVisible(true);
        World.setDelay(20);

        // Inicializar matriz de bloqueos por coordenadas
        for (int i = 0; i < posiciones.length; i++) {
            for (int j = 0; j < posiciones[i].length; j++) {
                posiciones[i][j] = new ReentrantLock();
            }
        }

        // Crear trenes desde el taller
        Thread trenA1 = new Thread(new Train(21, 9, North, 0, Color.BLUE, "A", false));
        Thread trenA2 = new Thread(new Train(22, 9, North, 0, Color.BLUE, "A", true)); // sentido contrario
        Thread trenB1 = new Thread(new Train(23, 9, North, 0, Color.GREEN, "B", false));

        trenA1.start();
        trenA2.start();
        trenB1.start();
    }
}

