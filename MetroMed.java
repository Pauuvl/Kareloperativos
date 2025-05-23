import kareltherobot.*;
import java.awt.Color;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class MetroMed implements Directions {
    public static class Estado {
        public static final Set<String> positionsOcupadas = new HashSet<>();
        public static final Object lock = new Object();
        public static boolean operationalTime = false;
    }

    private static boolean isOperating = false;
    private static final int MAX_AVENUE = 14;

    public static void main(String[] args) {
        World.readWorld("MetroMed.kwld");
        World.setVisible(true);
        World.setDelay(10);

        System.out.println("Sistema inicializado. Trenes en calles 34-35, avenidas 1-14");

        crearTrenesEnCalles34y35();

        new Thread(() -> {
            System.out.println("Presione ENTER para iniciar operaciones (4:30 am)...");
            new Scanner(System.in).nextLine();
            isOperating = true;
            Estado.operationalTime = true;
            System.out.println("=== INICIO DE OPERACIONES 4:30 AM ===");

            System.out.println("Presione ENTER para finalizar operaciones (11 pm)...");
            new Scanner(System.in).nextLine();
            isOperating = false;
            Estado.operationalTime = false;
            synchronized (Estado.lock) {
                Estado.lock.notifyAll();
            }
            System.out.println("=== CIERRE DE OPERACIONES 11 PM ===");
            System.out.println("Trenes regresando al taller...");
        }).start();
    }

    private static void crearTrenesEnCalles34y35() {
        // Línea A (azul) en calle 34 mirando Este
        for (int i = 0; i < 22 && (i+1) <= MAX_AVENUE; i++) {
            String nombre = "A" + (i + 1);
            new Train(34, 1 + i, East, 0, Color.BLUE, nombre, "A").start();
        }

        // Línea B (verde) en calle 35 mirando Oeste
        for (int i = 0; i < 10 && (i+1) <= MAX_AVENUE; i++) {
            String nombre = "B" + (i + 1);
            new Train(35, 1 + i, West, 0, Color.GREEN, nombre, "B").start();
        }
    }

    public static boolean isOperating() { return isOperating; }
    public static boolean isOperationalTime() { return Estado.operationalTime; }
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
    private static final int STATION_WAIT = 3000;
    private static final int COLLISION_CHECK_DELAY = 100;

    private static final SimplePoint NIQUIA = new SimplePoint(35, 19);
    private static final SimplePoint ESTRELLA = new SimplePoint(1, 11);
    private static final SimplePoint SAN_JAVIER = new SimplePoint(16, 1);
    private static final SimplePoint SAN_ANTONIO_B = new SimplePoint(14, 12);
    private static final SimplePoint TALLER = new SimplePoint(1, 1);

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
    public void move() {
        String positionFrente = getPositionFrente();
        String positionActual = getPosition();

        synchronized (MetroMed.Estado.lock) {
            if (!MetroMed.Estado.positionsOcupadas.contains(positionActual)) {
                MetroMed.Estado.positionsOcupadas.add(positionActual);
            }

            while (MetroMed.Estado.positionsOcupadas.contains(positionFrente)) {
                try {
                    MetroMed.Estado.lock.wait();
                    positionFrente = getPositionFrente();
                    if (!MetroMed.isOperating()) {
                        MetroMed.Estado.positionsOcupadas.remove(positionActual);
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    MetroMed.Estado.positionsOcupadas.remove(positionActual);
                    return;
                }
            }

            MetroMed.Estado.positionsOcupadas.remove(positionActual);
            super.move();
            actualizarPosition();
            positionActual = getPosition();
            MetroMed.Estado.positionsOcupadas.add(positionActual);
            MetroMed.Estado.lock.notifyAll();
        }
    }

    private String getPosition() {
        SimplePoint current = positions.get(nombre);
        return current.x + "," + current.y;
    }

    private String getPositionFrente() {
        SimplePoint nextPos = calculateNextPosition();
        return nextPos.x + "," + nextPos.y;
    }

    private void actualizarPosition() {
        SimplePoint current = positions.get(nombre);
        if (facingNorth()) current.x++;
        else if (facingSouth()) current.x--;
        else if (facingEast()) current.y++;
        else if (facingWest()) current.y--;
        positions.put(nombre, current);
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            Thread.sleep((int)(Math.random() * 50));
        } catch (InterruptedException e) {
            return;
        }

        SimplePoint initialPosition = getInitialPosition();
        navigateTo(initialPosition);

        while (!MetroMed.isOperationalTime()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }

        while (MetroMed.isOperating()) {
            if (linea.equals("A")) {
                routeBetween(NIQUIA, ESTRELLA);
                routeBetween(ESTRELLA, NIQUIA);
            } else {
                routeBetween(SAN_JAVIER, SAN_ANTONIO_B);
                turnAround();
                routeBetween(SAN_ANTONIO_B, SAN_JAVIER);
            }
        }

        returnToTaller();
    }

    private SimplePoint getInitialPosition() {
        if (linea.equals("A")) {
            int num = Integer.parseInt(nombre.substring(1));
            return (num % 2 == 0) ? NIQUIA : ESTRELLA;
        } else {
            return SAN_JAVIER;
        }
    }

    private void returnToTaller() {
        System.out.println("Tren " + nombre + " regresando al taller...");
        navigateToTaller(TALLER);
        System.out.println("Tren " + nombre + " llegó al taller.");
    }

    private void navigateToTaller(SimplePoint destination) {
        while (!reachedDestination(destination)) {
            if (tryMoveToTaller()) {
                // No checking beepers when going to taller
            } else {
                // Only change direction if blocked by wall, not by another train
                if (!frontIsClear()) {
                    adjustDirection();
                }
                // If blocked by another train, just wait
            }
            try {
                Thread.sleep(COLLISION_CHECK_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private boolean tryMoveToTaller() {
        if (!frontIsClear()) return false;
        SimplePoint nextPos = calculateNextPosition();
        synchronized (positionLock) {
            if (isPositionFree(nextPos)) {
                moveToTaller();
                return true;
            }
            return false;
        }
    }

    private void moveToTaller() {
        String positionFrente = getPositionFrente();
        String positionActual = getPosition();

        synchronized (MetroMed.Estado.lock) {
            if (!MetroMed.Estado.positionsOcupadas.contains(positionActual)) {
                MetroMed.Estado.positionsOcupadas.add(positionActual);
            }

            while (MetroMed.Estado.positionsOcupadas.contains(positionFrente)) {
                try {
                    MetroMed.Estado.lock.wait(50); // Short wait for taller navigation
                    positionFrente = getPositionFrente();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    MetroMed.Estado.positionsOcupadas.remove(positionActual);
                    return;
                }
            }

            MetroMed.Estado.positionsOcupadas.remove(positionActual);
            super.move(); // Direct call to Robot's move, bypassing our override
            actualizarPosition();
            positionActual = getPosition();
            MetroMed.Estado.positionsOcupadas.add(positionActual);
            MetroMed.Estado.lock.notifyAll();
        }
    }

    private boolean tryMove() {
        if (!frontIsClear()) return false;
        SimplePoint nextPos = calculateNextPosition();
        synchronized (positionLock) {
            if (isPositionFree(nextPos)) {
                move();
                return true;
            }
            return false;
        }
    }

    private void navigateTo(SimplePoint destination) {
        while (!reachedDestination(destination) && MetroMed.isOperating()) {
            if (tryMove()) {
                checkForBeeper();
            } else {
                // Only change direction if blocked by wall, not by another train
                if (!frontIsClear()) {
                    adjustDirection();
                }
                // If blocked by another train, just wait
            }
            try {
                Thread.sleep(COLLISION_CHECK_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void checkForBeeper() {
        if (nextToABeeper()) {
            waitAtStation();
        }
    }

    private void adjustDirection() {
        if (Math.random() > 0.5) turnRight();
        else turnLeft();

        if (!frontIsClear()) {
            turnAround();
            if (!frontIsClear()) {
                turnLeft();
            }
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

    private void routeBetween(SimplePoint start, SimplePoint end) {
        navigateTo(end);
        handleBeeperWait();
    }

    private void handleBeeperWait() {
        if (nextToABeeper()) {
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

    private void turnRight() {
        for (int i = 0; i < 3; i++) turnLeft();
    }

    private void turnAround() {
        turnLeft();
        turnLeft();
    }
}