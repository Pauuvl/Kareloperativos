import kareltherobot.*;
import java.awt.Color;
public class ParalelismoRobots implements Directions {
    public static void main(String[] args) {
        // Cargar el mundo
        World.readWorld("mundo4.kwld");
        World.setVisible(true);

        // Crear el primer robot (negro)
        Racer robot1 = new Racer(1, 1, East, 0, Color.BLACK);

        // Crear el segundo robot (azul)
        Racer robot2 = new Racer(2, 1, East, 0, Color.BLUE);

        // Iniciar ambos robots en hilos independientes
        new Thread(robot1).start();
        new Thread(robot2).start();
    }
}
class Racer extends Robot  {

    public Racer(int street, int avenue, Direction direction, int beepers, Color color) {
        super(street, avenue, direction, beepers, color);
        World.setupThread(this); // Configura este robot para correr en su propio hilo
    }

    // Método que define la carrera del robot
    public void race() {
        // Moverse 4 pasos
        move();
        move();
        move();
        move();

        // Recoger 5 beepers
        pickBeeper();
        pickBeeper();
        pickBeeper();
        pickBeeper();
        pickBeeper();

        // Girar a la izquierda y moverse 2 veces
        turnLeft();
        move();
        move();

        // Dejar 5 beepers
        putBeeper();
        putBeeper();
        putBeeper();
        putBeeper();
        putBeeper();

        // Moverse y apagarse
        move();
        turnOff();
    }

    // Método run requerido por el hilo
    public void run() {
        race();
    }
}
