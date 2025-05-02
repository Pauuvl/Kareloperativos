import kareltherobot.*;
import java.awt.Color;
import java.util.concurrent.*;
import java.util.*;

public class MetroSimulation implements Directions {
    public static void main(String[] args) {
        // Cargar el mundo del metro
        World.readWorld("MetroMedellin.kwld");
        World.setVisible(true);
        World.setDelay(50); // Velocidad de animación
        
        // Tamaño del mundo (ajustar según tu mapa)
        World.setSize(36, 21); // 36 calles (norte-sur), 21 avenidas (este-oeste)
        
        // Iniciar simulación
        ControladorMetro controlador = new ControladorMetro();
        controlador.iniciarSimulacion();
    }
}

class ControladorMetro {
    // Semáforos y locks para control de concurrencia
    private final Semaphore lineaCSemaphore = new Semaphore(1, true); // Línea C (un solo carril)
    private final Object tallerLock = new Object();
    private final Object estacionSanAntonioBLock = new Object();
    
    // Contadores para control de capacidad
    private int trenesEnLineaB = 0;
    private final int MAX_TRENES_B = 10;
    private final int CAPACIDAD_TALLER = 32;
    
    // Estado de la simulación
    private boolean esHoraPico = false;
    private boolean finDeOperacion = false;
    private int trenesEnTaller = 0;
    
    public void iniciarSimulacion() {
        System.out.println("=== INICIANDO SIMULACIÓN DEL METRO DE MEDELLÍN ===");
        
        // 1. Crear flota inicial de trenes en el taller
        List<Tren> flota = new ArrayList<>();
        
        // Trenes línea A (color azul)
        for(int i = 1; i <= 8; i++) {
            synchronized(tallerLock) {
                if(trenesEnTaller < CAPACIDAD_TALLER) {
                    Tren tren = new Tren(1, i, North, 0, "A"+i, "A", Color.BLUE, this);
                    flota.add(tren);
                    trenesEnTaller++;
                }
            }
        }
        
        // Trenes línea B (color verde)
        for(int i = 1; i <= 5; i++) {
            synchronized(tallerLock) {
                if(trenesEnTaller < CAPACIDAD_TALLER) {
                    Tren tren = new Tren(2, i, North, 0, "B"+i, "B", Color.GREEN, this);
                    flota.add(tren);
                    trenesEnTaller++;
                }
            }
        }
        
        System.out.println("Trenes creados: " + flota.size() + " (Taller: " + trenesEnTaller + "/" + CAPACIDAD_TALLER + ")");
        
        // 2. Iniciar ejecución de todos los trenes
        ExecutorService executor = Executors.newFixedThreadPool(flota.size());
        for(Tren tren : flota) {
            executor.execute(tren);
        }
        
        // 3. Simular el paso del tiempo y eventos
        simularJornadaLaboral(executor);
    }
    
    private void simularJornadaLaboral(ExecutorService executor) {
        // Simular inicio a las 4:20 am
        System.out.println("[4:20 AM] Inicio de operaciones");
        
        // Simular hora pico (a los 30 segundos de simulación)
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                esHoraPico = true;
                System.out.println("[7:00 AM] Hora pico: Aumentando frecuencia de trenes");
            }
        }, 30000); // 30 segundos
        
        // Simular fin de operación a las 11 pm (a los 2 minutos de simulación)
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                finDeOperacion = true;
                System.out.println("[11:00 PM] Fin de operaciones - Todos los trenes deben regresar al taller");
                
                executor.shutdown();
                try {
                    if(!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
        }, 120000); // 2 minutos
    }
    
    // Métodos para control de recursos compartidos
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
            // San Antonio B solo puede atender un tren a la vez
            Thread.sleep(3000); // 3 segundos en estación
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
    
    public Tren(int calle, int avenida, Direction direccion, int beepers, 
               String id, String linea, Color color, ControladorMetro controlador) {
        super(calle, avenida, direccion, beepers);
        this.controlador = controlador;
        this.linea = linea;
        this.id = id;
        this.setColor(color);
        World.setupThread(this);
    }
    
    @Override
    public void run() {
        try {
            System.out.println("Tren " + id + " (" + linea + ") iniciando operación");
            
            // Esperar inicio de operaciones (4:20 am)
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Tren " + id + " finalizando operación");
            turnOff();
        }
    }
    
    private void operarLineaA() throws InterruptedException {
        // Ruta Niquía -> La Estrella
        irAEstacion(36, 21); // Niquía
        controlador.esperarEnEstacion();
        
        // Viaje hacia sur por la línea A
        while(avenue() > 10 && enOperacion && !controlador.isFinDeOperacion()) {
            moverSeguro();
        }
        
        // Llegada a San Antonio A
        if(enPosicion(14, 16)) {
            controlador.esperarEnEstacion();
            
            // Cambiar a línea B si es necesario
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
        
        // Continuar a La Estrella
        irAEstacion(1, 10); // La Estrella
        controlador.esperarEnEstacion();
    }
    
    private void operarLineaB() throws InterruptedException {
        // Ruta San Javier -> San Antonio B
        irAEstacion(18, 1); // San Javier
        controlador.esperarEnEstacion();
        
        // Viaje hacia San Antonio B
        while(!enPosicion(13, 13) && enOperacion && !controlador.isFinDeOperacion()) {
            moverSeguro();
        }
        
        // Espera en San Antonio B
        controlador.esperarEnSanAntonioB();
        
        // Regreso a San Javier
        turnAround();
        while(!enPosicion(18, 1) && enOperacion && !controlador.isFinDeOperacion()) {
            moverSeguro();
        }
        
        controlador.esperarEnEstacion();
    }
    
    private void moverSeguro() throws InterruptedException {
        if(frontIsClear()) {
            move();
            Thread.sleep(100); // Pequeña pausa entre movimientos
        } else {
            // Buscar ruta alternativa
            if(nextToARobot()) {
                // Evitar colisión
                Thread.sleep(500);
            } else {
                girarAlternativo();
            }
        }
    }
    
    private void girarAlternativo() {
        // Estrategia de giro para evitar obstáculos
        turnLeft();
        if(frontIsClear()) {
            try { move(); } catch (Exception e) {}
        } else {
            turnLeft();
            turnLeft();
            if(frontIsClear()) {
                try { move(); } catch (Exception e) {}
            } else {
                turnLeft();
            }
        }
    }
    
    private void irAEstacion(int calleObj, int avenidaObj) throws InterruptedException {
        while(!enPosicion(calleObj, avenidaObj) && enOperacion && !controlador.isFinDeOperacion()) {
            // Navegación básica hacia la estación objetivo
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
                // Reorientarse si no está mirando en la dirección correcta
                if(random.nextBoolean()) turnLeft();
                else turnRight();
            }
        }
    }
    
    private void turnRight() {
        turnLeft();
        turnLeft();
        turnLeft();
    }
    
    private void turnAround() {
        turnLeft();
        turnLeft();
    }
    
    private boolean enPosicion(int calle, int avenida) {
        return street() == calle && avenue() == avenida;
    }
    
    private void regresarAlTaller() throws InterruptedException {
        System.out.println("Tren " + id + " regresando al taller");
        irAEstacion(1, 1); // Posición del taller
        controlador.regresarAlTaller();
        turnOff();
    }
    
    public void finalizarOperacion() {
        this.enOperacion = false;
    }
}