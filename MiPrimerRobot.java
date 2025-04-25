import kareltherobot.*;
        import java.awt.Color;

public class MiPrimerRobot implements Directions {
    public static void main(String[] args) {
        // Usamos el archivo que creamos del mundo
        World.readWorld("Mundo.kwld");
        World.setVisible(true);

        // Coloca el robot Karel en la posición inicial (1,1), mirando al Este, sin beepers
        Robot Karel = new Robot(1, 1, East, 0);

        // Coloca el robot azul en (1,2), mirando al Este, sin beepers
        Robot karel2 = new Robot(1, 1, East, 0, Color.BLUE);

        // Karel se mueve 4 pasos
        Karel.move();
        karel2.move();
        Karel.move();
        karel2.move();
        Karel.move();
        karel2.move();
        Karel.move();
        karel2.move();

        // Recoge 5 beepers
        Karel.pickBeeper();
        Karel.pickBeeper();
        Karel.pickBeeper();
        Karel.pickBeeper();
        Karel.pickBeeper();

        // Gira a la izquierda y se mueve 2 veces
        Karel.turnLeft();
        karel2.turnLeft();
        Karel.move();
        karel2.move();
        Karel.move();

        // Deja los beepers
        Karel.putBeeper();
        Karel.putBeeper();
        Karel.putBeeper();
        Karel.putBeeper();
        Karel.putBeeper();

        // Se mueve y se apaga
        Karel.move();
        karel2.move();
        karel2.turnOff();
        Karel.turnOff();


    }
}