import kareltherobot.*;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class MetroMed implements Directions {
    private static boolean isOperating = true;
    private static final Semaphore lineCSemaphore = new Semaphore(1);
    private static int activeTrainsB = 0;
    private static final int MAX_TRAINS_B = 10;

    public static void main(String[] args) {
        World.readWorld("MetroMed.kwld");
        World.setVisible(true);
        World.setDelay(10);

        System.out.println("Iniciando operaciones a las 4:20 am");
        createInitialTrains();

        new Thread(() -> {
            System.out.println("Presione ENTER para simular las 11 pm...");
            new Scanner(System.in).nextLine();
            isOperating = false;
            System.out.println("Iniciando cierre de operaciones...");
        }).start();
    }

    private static void createInitialTrains() {
        new Train(35, 1, South, 0, Color.BLACK, "Taller_Niquia", "A").start();
        new Train(35, 2, North, 0, Color.BLUE, "Taller_Estrella", "A").start();

        if (canAddTrainB()) {
            new Train(34, 2, East, 0, Color.GREEN, "Taller_SanJavier", "B").start();
            incrementTrainBCount();
        }
    }

    public static boolean isOperating() { return isOperating; }
    public static Semaphore getLineCSemaphore() { return lineCSemaphore; }
    public static synchronized boolean canAddTrainB() { return activeTrainsB < MAX_TRAINS_B; }
    public static synchronized void incrementTrainBCount() { activeTrainsB++; }
    public static synchronized void decrementTrainBCount() { activeTrainsB--; }
}

class Train extends Robot implements Runnable {
    private String nombre;
    private String linea;
    private static final Object positionLock = new Object();

    private static class SimplePoint {
        public int x, y;
        public SimplePoint(int x, int y) { this.x = x; this.y = y; }
        public boolean equals(Object o) {
            if (!(o instanceof SimplePoint)) return false;
            SimplePoint p = (SimplePoint)o;
            return x == p.x && y == p.y;
        }
    }

    private static final Map<String, SimplePoint> positions = new HashMap<>();
    private static final Map<String, Boolean> stationOccupied = new HashMap<>();

    private static final int STATION_WAIT = 3000;
    private static final int COLLISION_CHECK_DELAY = 100;

    // Coordenadas actualizadas según el mapa
    private static final SimplePoint NIQUIA = new SimplePoint(35, 19);
    private static final SimplePoint ESTRELLA = new SimplePoint(1, 11);
    private static final SimplePoint SAN_JAVIER = new SimplePoint(16, 1);
    private static final SimplePoint SAN_ANTONIO_B = new SimplePoint(14, 12);

    public Train(int street, int avenue, Direction dir, int beepers, Color color, String nombre, String linea) {
        super(street, avenue, dir, beepers, color);
        this.nombre = nombre;
        this.linea = linea;
        World.setupThread(this);
        synchronized (positionLock) {
            positions.put(nombre, new SimplePoint(street, avenue));
        }
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        if (linea.equals("B") && !MetroMed.canAddTrainB()) return;

        // Rutas iniciales
        if (nombre.equals("Taller_Niquia")) routeTo(NIQUIA);
        else if (nombre.equals("Taller_Estrella")) routeTo(ESTRELLA);
        else if (nombre.equals("Taller_SanJavier")) routeTo(SAN_JAVIER);

        // Ciclo principal de operación
        while (MetroMed.isOperating()) {
            if (linea.equals("A")) {
                if (nombre.equals("Taller_Niquia")) {
                    routeBetween(NIQUIA, ESTRELLA);
                    routeBetween(ESTRELLA, NIQUIA);
                } else {
                    routeBetween(ESTRELLA, NIQUIA);
                    routeBetween(NIQUIA, ESTRELLA);
                }
            } else {
                routeBetween(SAN_JAVIER, SAN_ANTONIO_B);
                turnAround();
                routeBetween(SAN_ANTONIO_B, SAN_JAVIER);
            }
        }

        returnToDepot();
        if (linea.equals("B")) MetroMed.decrementTrainBCount();
    }

    private void routeTo(SimplePoint destination) {
        navigateTo(destination);
        handleBeeperWait();
    }

    private void routeBetween(SimplePoint start, SimplePoint end) {
        navigateTo(end);
        handleBeeperWait();
    }

    private void navigateTo(SimplePoint destination) {
        System.out.println(nombre + " navegando a (" + destination.x + "," + destination.y + ")");
        while (!reachedDestination(destination) && MetroMed.isOperating()) {
            if (canMoveSafely()) {
                moveSafely();
                checkForBeeper();
            } else {
                adjustDirection();
            }
        }
    }

    private void checkForBeeper() {
        if (nextToABeeper()) {
            System.out.println(nombre + " encontró beeper - esperando 3s");
            waitAtStation();
        }
    }

    private boolean canMoveSafely() {
        if (!frontIsClear()) return false;
        SimplePoint nextPos = calculateNextPosition();
        synchronized (positionLock) {
            return isPositionFree(nextPos);
        }
    }

    private void moveSafely() {
        SimplePoint nextPos = calculateNextPosition();
        synchronized (positionLock) {
            super.move();
            positions.put(nombre, nextPos);
        }
    }

    private void adjustDirection() {
        turnRight();
        if (!frontIsClear()) {
            turnAround();
            if (!frontIsClear()) {
                turnLeft();
            }
        }
    }

    private void handleBeeperWait() {
        if (nextToABeeper()) {
            System.out.println(nombre + " en estación - esperando 3s");
            waitAtStation();
        }
    }

    private void waitAtStation() {
        try {
            Thread.sleep(STATION_WAIT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SimplePoint calculateNextPosition() {
        SimplePoint current = positions.get(nombre);
        int x = current.x, y = current.y;
        if (facingNorth()) x++;
        else if (facingSouth()) x--;
        else if (facingEast()) y++;
        else if (facingWest()) y--;
        return new SimplePoint(x, y);
    }

    private boolean reachedDestination(SimplePoint destination) {
        SimplePoint current = positions.get(nombre);
        return current.equals(destination);
    }

    private boolean isPositionFree(SimplePoint pos) {
        return positions.values().stream().noneMatch(p -> p.equals(pos));
    }

    private void returnToDepot() {
        SimplePoint taller = nombre.equals("Taller_Niquia") ? new SimplePoint(35, 1) :
                nombre.equals("Taller_Estrella") ? new SimplePoint(34, 1) :
                        new SimplePoint(34, 2);
        navigateTo(taller);
    }

    private void turnRight() { for (int i = 0; i < 3; i++) turnLeft(); }
    private void turnAround() { turnLeft(); turnLeft(); }

    // Métodos auxiliares de posición
    private int getCurrentAvenue() { return positions.get(nombre).y; }
    private int getCurrentStreet() { return positions.get(nombre).x; }
}