import kareltherobot.*;
import java.awt.Color;
import java.util.concurrent.*;
import java.util.*;
import java.util.Random;

public class MetroSimulation implements Directions {
    public static void main(String[] args) {
        World.readWorld("MetroMedellin.kwld");
        World.setVisible(true);
        World.setDelay(50);
        World.setSize(36, 21);
        
        ControladorMetro controlador = new ControladorMetro();
        controlador.iniciarSimulacion();
    }
}

class ControladorMetro {
    private final Semaphore lineaCSemaphore = new Semaphore(1, true);
    private final Object tallerLock = new Object();
    private final Object estacionSanAntonioBLock = new Object();
    private int trenesEnLineaB = 0;
    private final int MAX_TRENES_B = 10;
    private final int CAPACIDAD_TALLER = 32;
    private boolean esHoraPico = false;
    private boolean finDeOperacion = false;
    private int trenesEnTaller = 0;
    private final Random random = new Random();
    
    public void iniciarSimulacion() {
        System.out.println("=== INICIANDO SIMULACIÓN DEL METRO DE MEDELLÍN ===");
        
        List<Thread> hilosTrenes = new ArrayList<>();
        
        // Trenes línea A (azules)
        for(int i = 1; i <= 8; i++) {
            synchronized(tallerLock) {
                if(trenesEnTaller < CAPACIDAD_TALLER) {
                    Tren tren = new Tren(1, i, North, 0, "A"+i, "A", Color.BLUE, this);
                    Thread hilo = new Thread(tren);
                    hilosTrenes.add(hilo);
                    trenesEnTaller++;
                }
            }
        }
        
        // Trenes línea B (verdes)
        for(int i = 1; i <= 5; i++) {
            synchronized(tallerLock) {
                if(trenesEnTaller < CAPACIDAD_TALLER) {
                    Tren tren = new Tren(2, i, North, 0, "B"+i, "B", Color.GREEN, this);
                    Thread hilo = new Thread(tren);
                    hilosTrenes.add(hilo);
                    trenesEnTaller++;
                }
            }
        }
        
        System.out.println("Trenes creados: " + hilosTrenes.size());
        
        // Iniciar todos los hilos
        for(Thread hilo : hilosTrenes) {
            hilo.start();
        }
        
        simularJornadaLaboral();
    }
    
    private void simularJornadaLaboral() {
        System.out.println("[4:20 AM] Inicio de operaciones");
        
        // Hora pico a los 30 segundos
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                esHoraPico = true;
                System.out.println("[7:00 AM] Hora pico: Aumentando frecuencia de trenes");
            }
        }, 30000);
        
        // Fin de operación a los 2 minutos
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                finDeOperacion = true;
                System.out.println("[11:00 PM] Fin de operaciones");
            }
        }, 120000);
    }
    
    public void esperarEnEstacion() throws InterruptedException {
        Thread.sleep(3000);
    }
    
    // Resto de métodos del controlador...
    public boolean solicitarSalidaTaller(String linea) {
        synchronized(tallerLock) {
            if(trenesEnTaller > 0) {
                trenesEnTaller--;
                return true;
            }
            return false;
        }
    }
    
    public void regresarAlTaller() {
        synchronized(tallerLock) {
            trenesEnTaller++;
        }
    }
    
    public void entrarLineaC() throws InterruptedException {
        lineaCSemaphore.acquire();
    }
    
    public void salirLineaC() {
        lineaCSemaphore.release();
    }
    
    public boolean puedeEntrarLineaB() {
        synchronized(this) {
            if(trenesEnLineaB < MAX_TRENES_B) {
                trenesEnLineaB++;
                return true;
            }
            return false;
        }
    }
    
    public void salirLineaB() {
        synchronized(this) {
            trenesEnLineaB--;
        }
    }
    
    public void esperarEnSanAntonioB() throws InterruptedException {
        synchronized(estacionSanAntonioBLock) {
            Thread.sleep(3000);
        }
    }
    
    public boolean isFinDeOperacion() {
        return finDeOperacion;
    }
}

class Tren extends Robot implements Runnable {
    private final ControladorMetro controlador;
    private String linea;
    private final String id;
    private boolean enOperacion = true;
    private final Random random = new Random();
    
    public Tren(int calle, int avenida, Direction direccion, int beepers, 
               String id, String linea, Color color, ControladorMetro controlador) {
        super(calle, avenida, direccion, beepers);
        this.controlador = controlador;
        this.linea = linea;
        this.id = id;
        this.setColor(color);
    }
    
    // Implementación de todos los métodos necesarios
    
    @Override
    public void run() {
        try {
            System.out.println("Tren " + id + " (" + linea + ") iniciando operación");
            Thread.sleep(1000);
            
            while(enOperacion && !controlador.isFinDeOperacion()) {
                if(linea.equals("A")) {
                    operarLineaA();
                } else {
                    operarLineaB();
                }
            }
            
            regresarAlTaller();
        } catch (InterruptedException e) {
            System.out.println("Tren " + id + " interrumpido");
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Tren " + id + " finalizando operación");
            turnOff();
        }
    }
    
    // Métodos de movimiento y navegación
    
    private void operarLineaA() throws InterruptedException {
        irAEstacion(36, 21); // Niquía
        controlador.esperarEnEstacion();
        
        while(avenue() > 10 && enOperacion && !controlador.isFinDeOperacion()) {
            moverSeguro();
        }
        
        if(enPosicion(14, 16)) { // San Antonio A
            controlador.esperarEnEstacion();
            
            if(controlador.puedeEntrarLineaB()) {
                controlador.entrarLineaC();
                try {
                    linea = "B";
                    setColor(Color.GREEN);
                    operarLineaB();
                } finally {
                    controlador.salirLineaC();
                    controlador.salirLineaB();
                }
            }
        }
        
        irAEstacion(1, 10); // La Estrella
        controlador.esperarEnEstacion();
    }
    
    private void operarLineaB() throws InterruptedException {
        irAEstacion(18, 1); // San Javier
        controlador.esperarEnEstacion();
        
        while(!enPosicion(13, 13) && enOperacion && !controlador.isFinDeOperacion()) {
            moverSeguro();
        }
        
        controlador.esperarEnSanAntonioB();
        
        turnAround();
        
        while(!enPosicion(18, 1) && enOperacion && !controlador.isFinDeOperacion()) {
            moverSeguro();
        }
        
        controlador.esperarEnEstacion();
    }
    
    private void moverSeguro() throws InterruptedException {
        if(frontIsClear()) {
            move();
            Thread.sleep(100);
        } else {
            if(nextToARobot()) {
                Thread.sleep(500); // Espera si hay otro tren
            } else {
                girarAlternativo();
            }
        }
    }
    
    private void girarAlternativo() {
        turnLeft();
        if(frontIsClear()) {
            try { move(); } catch (Exception e) {}
        } else {
            turnAround();
            if(frontIsClear()) {
                try { move(); } catch (Exception e) {}
            } else {
                turnLeft();
            }
        }
    }
    
    private void irAEstacion(int calleObj, int avenidaObj) throws InterruptedException {
        while(!enPosicion(calleObj, avenidaObj) && enOperacion && !controlador.isFinDeOperacion()) {
            if(street() < calleObj && facingNorth()) {
                if(frontIsClear()) move();
                else girarAlternativo();
            } else if(street() > calleObj && facingSouth()) {
                if(frontIsClear()) move();
                else girarAlternativo();
            } else if(avenue() < avenidaObj && facingEast()) {
                if(frontIsClear()) move();
                else girarAlternativo();
            } else if(avenue() > avenidaObj && facingWest()) {
                if(frontIsClear()) move();
                else girarAlternativo();
            } else {
                // Reorientación aleatoria
                if(random.nextBoolean()) turnLeft();
                else turnRight();
            }
        }
    }
    
    // Métodos auxiliares de movimiento
    
    private void turnRight() {
        for(int i = 0; i < 3; i++) {
            turnLeft();
        }
    }
    
    private void turnAround() {
        turnLeft();
        turnLeft();
    }
    
    // Métodos de verificación de estado
    
    private boolean enPosicion(int calle, int avenida) {
        return street() == calle && avenue() == avenida;
    }
    
    // Métodos heredados de Robot que necesitamos implementar/sobreescribir
    
    @Override
    public void setColor(Color color) {
        super.setColor(color);
    }
    
    @Override
    public boolean frontIsClear() {
        return super.frontIsClear();
    }
    
    @Override
    public boolean nextToARobot() {
        return super.nextToARobot();
    }
    
    @Override
    public int street() {
        return super.street();
    }
    
    @Override
    public int avenue() {
        return super.avenue();
    }
    
    @Override
    public boolean facingNorth() {
        return super.facingNorth();
    }
    
    @Override
    public boolean facingSouth() {
        return super.facingSouth();
    }
    
    @Override
    public boolean facingEast() {
        return super.facingEast();
    }
    
    @Override
    public boolean facingWest() {
        return super.facingWest();
    }
    
    // Métodos de finalización
    
    private void regresarAlTaller() throws InterruptedException {
        System.out.println("Tren " + id + " regresando al taller");
        irAEstacion(1, 1);
        controlador.regresarAlTaller();
        turnOff();
    }
    
    public void finalizarOperacion() {
        this.enOperacion = false;
    }
}