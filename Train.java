public import kareltherobot.*;
import java.awt.Color;

public class Train extends Robot implements Runnable {
    private final String linea;
    private final boolean reversa; // si el tren va en sentido opuesto
    private final Color color;

    public Train(int street, int avenue, Direction dir, int beepers, Color color, String linea, boolean reversa) {
        super(street, avenue, dir, beepers);
        this.linea = linea;
        this.reversa = reversa;
        this.color = color;
        this.setColor(color);
        World.setupThread(this);
    }

    @Override
    public void run() {
        if (linea.equals("A")) {
            if (reversa) {
                lineaA_EstrellaToNiquia();
            } else {
                lineaA_NiquiaToEstrella();
            }
        } else if (linea.equals("B")) {
            lineaB_SanJavierToSanAntonio();
        }
    }

    // ---------- Movimiento para Línea A (de norte a sur) ----------
    private void lineaA_NiquiaToEstrella() {
        // Ejemplo de movimiento simple con delays entre estaciones
        try {
            moveTo(5, 16); stopAtStation();
            moveTo(8, 16); stopAtStation();
            moveTo(11, 16); stopAtStation();
            moveTo(14, 16); stopAtStation(); // San Antonio A
            // Línea C uso exclusivo
            moveTo(14, 15); useLineaC(); moveTo(14, 14);
            moveTo(14, 13); stopAtStation();
            moveTo(14, 11); stopAtStation();
            moveTo(14, 9); stopAtStation(); // La Estrella
            turnOff();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- Movimiento para Línea A (de sur a norte) ----------
    private void lineaA_EstrellaToNiquia() {
        try {
            moveTo(14, 9); stopAtStation();
            moveTo(14, 11); stopAtStation();
            moveTo(14, 13); stopAtStation();
            moveTo(14, 14); useLineaC(); moveTo(14, 15);
            moveTo(14, 16); stopAtStation(); // San Antonio A
            moveTo(11, 16); stopAtStation();
            moveTo(8, 16); stopAtStation();
            moveTo(5, 16); stopAtStation();
            turnOff();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- Movimiento para Línea B (San Javier a San Antonio B) ----------
    private void lineaB_SanJavierToSanAntonio() {
        try {
            moveTo(11, 6); stopAtStation();
            moveTo(13, 6); stopAtStation();
            moveTo(14, 6); stopAtStation(); // San Antonio B
            turnAround();
            moveTo(13, 6); stopAtStation();
            moveTo(11, 6); stopAtStation();
            turnOff();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- Utilidades ----------
    private void stopAtStation() throws InterruptedException {
        Thread.sleep(3000); // Espera de 3 segundos
    }

    private void useLineaC() {
        MetroSimulator.lineaC.lock();
        try {
            // Simula paso por línea C
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            MetroSimulator.lineaC.unlock();
        }
    }

    private void moveTo(int street, int
 {
    
}
