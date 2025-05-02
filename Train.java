import kareltherobot.*;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class MetroMed implements Directions {
    private static boolean isOperating = true;
    private static final Semaphore lineCSemaphore = new Semaphore(1);

    public static void main(String[] args) {
        World.readWorld("MetroMed.kwld");
        World.setVisible(true);
        World.setDelay(50);

        // Posiciones de los talleres

        // Creación de trenes en sus talleres
        Train trenA1 = new Train(35, 1, South, 0, Color.BLUE, "Taller_Niquia", "A");
        Train trenA2 = new Train(35, 2, North, 0, Color.BLUE, "Taller_Estrella", "A");
        Train trenB1 = new Train(35, 3, South, 0, Color.GREEN, "Taller_SanJavier", "B");

        new Thread(trenA1).start();
        new Thread(trenA2).start();
        new Thread(trenB1).start();

        // Control de cierre
        new Thread(() -> {
            new Scanner(System.in).nextLine();
            isOperating = false;
        }).start();
    }

    public static boolean isOperating() {
        return isOperating;
    }

    public static Semaphore getLineCSemaphore() {
        return lineCSemaphore;
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

    private static final int STATION_WAIT = 3000;
    private static final int COLLISION_CHECK_DELAY = 100;

    // Posiciones clave
    private static final SimplePoint NIQUIA = new SimplePoint(36, 1);
    private static final SimplePoint ESTRELLA = new SimplePoint(3, 1);
    private static final SimplePoint SAN_JAVIER = new SimplePoint(36, 2);
    private static final SimplePoint SAN_ANTONIO = new SimplePoint(3, 2);
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

    @Override
    public void run() {
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

        // Retorno al taller
        returnToDepot();
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

    private void safeMove() {
        while (true) {
            SimplePoint nextPos = calculateNextPosition();

            synchronized (positionLock) {
                boolean positionFree = isPositionFree(nextPos);
                boolean stationFree = isStationFree(nextPos);

                if (positionFree && stationFree && frontIsClear()) {
                    move();
                    positions.put(nombre, nextPos);
                    break;
                }
            }

            try {
                Thread.sleep(COLLISION_CHECK_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isPositionFree(SimplePoint pos) {
        for (Map.Entry<String, SimplePoint> entry : positions.entrySet()) {
            if (!entry.getKey().equals(nombre)) {
                if (entry.getValue().equals(pos)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isStationFree(SimplePoint pos) {
        if (pos.equals(NIQUIA) || pos.equals(ESTRELLA) ||
                pos.equals(SAN_JAVIER) || pos.equals(SAN_ANTONIO) ||
                pos.equals(TRANSFER_C)) {

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
            Thread.sleep(STATION_WAIT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            releaseStation();
        }
    }

    private void routeToNiquia() {
        while (getCurrentStreet() > 1 && MetroMed.isOperating()) {
            safeMove();
        }
    }

    private void routeToEstrella() {
        while (getCurrentStreet() < 36 && MetroMed.isOperating()) {
            safeMove();
        }
    }

    private void routeToSanJavier() {
        while (getCurrentStreet() > 1 && MetroMed.isOperating()) {
            safeMove();
        }
    }

    private void routeNiquiaToEstrella() {
        turnAround();
        while (getCurrentStreet() > 3 && MetroMed.isOperating()) {
            safeMove();
            if (getCurrentStreet() % 3 == 0) {
                waitAtStation();
            }
        }
    }

    private void routeEstrellaToNiquia() {
        turnAround();
        while (getCurrentStreet() < 36 && MetroMed.isOperating()) {
            safeMove();
            if (getCurrentStreet() % 3 == 0) {
                waitAtStation();
            }
        }
    }

    private void routeSanJavierToSanAntonio() {
        turnAround();
        while (getCurrentStreet() > 3 && MetroMed.isOperating()) {
            safeMove();
            if (getCurrentStreet() == 18) {
                try {
                    MetroMed.getLineCSemaphore().acquire();
                    turnRight();
                    safeMove();
                    turnRight();
                    MetroMed.getLineCSemaphore().release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (getCurrentStreet() % 3 == 0) {
                waitAtStation();
            }
        }
    }

    private void routeSanAntonioToSanJavier() {
        turnAround();
        while (getCurrentStreet() < 36 && MetroMed.isOperating()) {
            safeMove();
            if (getCurrentStreet() == 18) {
                try {
                    MetroMed.getLineCSemaphore().acquire();
                    turnLeft();
                    safeMove();
                    turnLeft();
                    MetroMed.getLineCSemaphore().release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (getCurrentStreet() % 3 == 0) {
                waitAtStation();
            }
        }
    }

    private void returnToDepot() {
        if (linea.equals("A")) {
            if (getCurrentAvenue() == 1) {
                turnAround();
                while (getCurrentStreet() < 36 && MetroMed.isOperating()) {
                    safeMove();
                }
            }
        } else if (linea.equals("B")) {
            if (getCurrentAvenue() == 2) {
                turnAround();
                while (getCurrentStreet() < 36 && MetroMed.isOperating()) {
                    safeMove();
                }
            }
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