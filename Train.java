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
    private static final int TOTAL_TRAINS = 32;

    public static void main(String[] args) {
        World.readWorld("MetroMed.kwld");
        World.setVisible(true);
        World.setDelay(10);

        System.out.println("Iniciando operaciones a las 4:20 am");

        // Crear los trenes iniciales
        createInitialTrains();

        // Control de cierre
        new Thread(() -> {
            System.out.println("Presione ENTER para simular las 11 pm...");
            new Scanner(System.in).nextLine();
            isOperating = false;
            System.out.println("Iniciando cierre de operaciones...");
        }).start();
    }

    private static void createInitialTrains() {
        // Trenes de la línea A
        new Train(35, 1, North, 0, Color.BLUE, "Taller_Niquia", "A").start();
        new Train(34, 1, South, 0, Color.GREEN, "Taller_SanJavier", "B").start();

        // Tren de la línea B
        if (canAddTrainB()) {
            new Train(35, 2, West, 0, Color.BLUE, "Taller_Estrella", "A").start();
            incrementTrainBCount();
        }
    }

    public static boolean isOperating() {
        return isOperating;
    }

    public static Semaphore getLineCSemaphore() {
        return lineCSemaphore;
    }

    public static synchronized boolean canAddTrainB() {
        return activeTrainsB < MAX_TRAINS_B;
    }

    public static synchronized void incrementTrainBCount() {
        activeTrainsB++;
    }

    public static synchronized void decrementTrainBCount() {
        activeTrainsB--;
    }
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
    private static final Map<String, Integer> stationCount = new HashMap<>();

    private static final int STATION_WAIT = 3000; // 3 segundos de espera
    private static final int COLLISION_CHECK_DELAY = 100;

    // Posiciones exactas de las estaciones
    private static final SimplePoint NIQUIA = new SimplePoint(45, 1);
    private static final SimplePoint ESTRELLA = new SimplePoint(35, 1);
    private static final SimplePoint SAN_JAVIER = new SimplePoint(34, 2);
    private static final SimplePoint SAN_ANTONIO_A = new SimplePoint(14, 16);
    private static final SimplePoint SAN_ANTONIO_B = new SimplePoint(13, 13);
    private static final SimplePoint TRANSFER_C = new SimplePoint(18, 1);

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
        if (linea.equals("B") && !MetroMed.canAddTrainB()) {
            return;
        }

        // Rutas iniciales desde taller
        if (nombre.equals("Taller_Niquia")) {
            routeToNiquia();
        } else if (nombre.equals("Taller_Estrella")) {
            routeToEstrella();
        } else if (nombre.equals("Taller_SanJavier")) {
            routeToSanJavier();
        }

        // Rutas principales
        while (MetroMed.isOperating()) {
            if (linea.equals("A")) {
                if (nombre.contains("Niquia")) {
                    routeNiquiaToEstrella();
                    routeEstrellaToNiquia();
                } else {
                    routeEstrellaToNiquia();
                    routeNiquiaToEstrella();
                }
            } else if (linea.equals("B")) {
                routeSanJavierToSanAntonio();
                routeSanAntonioToSanJavier();
            }
        }

        returnToDepot();
        if (linea.equals("B")) {
            MetroMed.decrementTrainBCount();
        }
    }

    private int getCurrentStreet() {
        return positions.get(nombre).x;
    }

    private int getCurrentAvenue() {
        return positions.get(nombre).y;
    }

    private String getCurrentDirection() {
        if (facingNorth()) return "North";
        if (facingSouth()) return "South";
        if (facingEast()) return "East";
        if (facingWest()) return "West";
        return "Unknown";
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

    private void navigateTo(SimplePoint destination) {
        while (!reachedDestination(destination) && MetroMed.isOperating()) {
            if (!canMoveForward()) {
                findAlternativePath();
            } else {
                safeMove();

                // Verificación y espera en estaciones (beeper)
                if (nextToABeeper() && isStationPosition()) {
                    waitAtStation();
                }
            }
        }
    }

    private boolean isStationPosition() {
        SimplePoint current = positions.get(nombre);
        return current.equals(NIQUIA) || current.equals(ESTRELLA) ||
                current.equals(SAN_JAVIER) || current.equals(SAN_ANTONIO_A) ||
                current.equals(SAN_ANTONIO_B) || current.equals(TRANSFER_C);
    }

    private boolean canMoveForward() {
        if (!frontIsClear()) {
            return false;
        }

        SimplePoint nextPos = calculateNextPosition();
        synchronized (positionLock) {
            for (Map.Entry<String, SimplePoint> entry : positions.entrySet()) {
                if (!entry.getKey().equals(nombre) && entry.getValue().equals(nextPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean reachedDestination(SimplePoint destination) {
        SimplePoint current = positions.get(nombre);
        return current.x == destination.x && current.y == destination.y;
    }

    private void findAlternativePath() {
        turnRight();
        if (!canMoveForward()) {
            turnAround();
            if (!canMoveForward()) {
                turnLeft();
                if (!canMoveForward()) {
                    try {
                        Thread.sleep(COLLISION_CHECK_DELAY);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void safeMove() {
        SimplePoint nextPos = calculateNextPosition();
        synchronized (positionLock) {
            boolean positionFree = isPositionFree(nextPos);
            boolean stationFree = isStationFree(nextPos);

            if (positionFree && stationFree && frontIsClear()) {
                move();
                positions.put(nombre, nextPos);
            }
        }
    }

    private boolean isPositionFree(SimplePoint pos) {
        for (Map.Entry<String, SimplePoint> entry : positions.entrySet()) {
            if (!entry.getKey().equals(nombre) && entry.getValue().equals(pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean isStationFree(SimplePoint pos) {
        if (pos.equals(NIQUIA) || pos.equals(ESTRELLA) ||
                pos.equals(SAN_JAVIER) || pos.equals(SAN_ANTONIO_A) ||
                pos.equals(SAN_ANTONIO_B) || pos.equals(TRANSFER_C)) {

            String stationKey = pos.x + "," + pos.y + "," + getCurrentDirection();
            synchronized (stationOccupied) {
                if (stationOccupied.containsKey(stationKey) && stationOccupied.get(stationKey)) {
                    return false;
                }
                stationOccupied.put(stationKey, true);
            }
        }
        return true;
    }

    private void releaseStation() {
        SimplePoint current = positions.get(nombre);
        String stationKey = current.x + "," + current.y + "," + getCurrentDirection();
        synchronized (stationOccupied) {
            stationOccupied.put(stationKey, false);
        }
    }

    private void waitAtStation() {
        try {
            Thread.sleep(STATION_WAIT); // Espera exactamente 3 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            releaseStation();
        }
    }

    private void routeToNiquia() {
        navigateTo(NIQUIA);
    }

    private void routeToEstrella() {
        navigateTo(ESTRELLA);
    }

    private void routeToSanJavier() {
        navigateTo(SAN_JAVIER);
    }

    private void routeNiquiaToEstrella() {
        navigateTo(ESTRELLA);
    }

    private void routeEstrellaToNiquia() {
        navigateTo(NIQUIA);
    }

    private void routeSanJavierToSanAntonio() {
        if (getCurrentAvenue() == 2) {
            navigateTo(SAN_ANTONIO_B);
            turnAround();
            waitAtStation();
        } else {
            navigateTo(SAN_ANTONIO_A);
            waitAtStation();
        }
    }

    private void routeSanAntonioToSanJavier() {
        navigateTo(SAN_JAVIER);
        waitAtStation();
    }

    private void returnToDepot() {
        if (linea.equals("A")) {
            if (nombre.contains("Niquia")) {
                navigateTo(new SimplePoint(35, 1));
            } else {
                navigateTo(new SimplePoint(34, 1));
            }
        } else if (linea.equals("B")) {
            navigateTo(new SimplePoint(35, 2));
        }
    }

    private void turnRight() {
        for (int i = 0; i < 3; i++) {
            turnLeft();
        }
    }

    private void turnAround() {
        turnLeft();
        turnLeft();
    }
}
