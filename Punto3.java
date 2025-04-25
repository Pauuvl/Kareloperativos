import kareltherobot.*; 
import java.awt.Color; 
public class Punto3 implements Directions 
{ 
public static void main(String [] args) 
{ 
// Usamos el archivo que creamos del mundo 
        World.readWorld("Mundo2.kwld"); 
        World.setVisible(true); 
 
// Coloca el robot en la posición inicial del mundo (1,1), 
// mirando al Este, sin ninguna sirena. 
  Robot Karel = new Robot(10, 10, East, 0);

// Mover el robot 9 pasos 
  Karel.turnLeft();
  Karel.turnLeft();
  Karel.turnLeft();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.turnLeft();
  Karel.turnLeft();
  Karel.turnLeft();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();
  Karel.move();

// Ponerse en otra posición y apagar el robot 
  Karel.move();
  Karel.turnOff();
 } 
} 
 