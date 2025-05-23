# MetroMed - Simulador de Sistema de Metro

## 📋 Descripción del Proyecto

MetroMed es un simulador de sistema de transporte metro desarrollado en Java usando Karel the Robot. El proyecto simula la operación de dos líneas de metro (A, B y C) con múltiples trenes operando concurrentemente en un mundo virtual.

## 🚇 Características del Sistema

### Líneas de Metro
- **Línea A (Azul)**: Opera entre estaciones NIQUIA y ESTRELLA
  - Hasta 22 trenes en calle 34, mirando hacia el Este
  - Los trenes van de NIQUIA → ESTRELLA → NIQUIA (ciclo continuo)

- **Línea B (Verde)**: Opera entre estaciones SAN_JAVIER y SAN_ANTONIO_B  
  - Hasta 10 trenes en calle 35, mirando hacia el Oeste
  - Los trenes van de SAN_JAVIER → SAN_ANTONIO_B → SAN_JAVIER (ciclo continuo)

### Horarios de Operación
- **Inicio**: Simulación de 4:30 AM (primer ENTER)
- **Fin**: Simulación de 11:00 PM (segundo ENTER)
- **Fuera de horario**: Los trenes permanecen en sus posiciones iniciales

## ✅❌ Implementación vs Reglas del Sistema Metro

### ✅ **REGLAS IMPLEMENTADAS CORRECTAMENTE:**

1. **✅ Colores de líneas**: Línea A (azul) y Línea B (verde)
2. **✅ Prevención de choques**: Sistema robusto que evita dos trenes en la misma posición
3. **✅ Tiempo en estaciones**: 3 segundos de espera en cada estación (beepers)
4. **✅ Límite de trenes**: Línea B máximo 10 trenes (respetado)
5. **✅ Señal de finalización**: Comando por teclado (ENTER) para terminar operaciones
6. **✅ Trenes no infinitos**: Control de cantidad máxima de trenes


### 🔄 **SIMPLIFICACIONES REALIZADAS:**
- **Inicio directo**: Los trenes aparecen en sus líneas sin trayecto desde taller
- **Horarios simulados**: Se usan señales manuales (ENTER) en lugar de tiempo real
- **Navegación simplificada**: Algoritmo básico de pathfinding sin restricciones específicas de estaciones
- **Fin de operaciones**: Todos los trenes van al taller al mismo tiempo, sin lógica de último tren

## 🚀 Cómo Ejecutar el Proyecto

### Prerrequisitos
- Java JDK 8 o superior
- Karel the Robot JAR (`karel.jar`)
- Archivo de mundo `MetroMed.kwld`

### Comandos de Ejecución

```bash
# Compilar el proyecto
javac -cp ".:karel.jar" MetroMed.java

# Ejecutar en Linux/Mac
java -cp ".:karel.jar" MetroMed

# Ejecutar en Windows
java -cp ".;karel.jar" MetroMed
```

### Flujo de Ejecución
1. **Inicialización**: Los trenes se posicionan en calles 34-35
2. **Primer ENTER**: Inicia operaciones a las 4:30 AM

## 🧵 Manejo de Hilos y Concurrencia

### Arquitectura de Hilos
El proyecto implementa un sistema multihilo robusto:

```java
// Cada tren opera en su propio hilo
public class Train extends Robot implements Runnable {
    @Override
    public void run() {
        // Lógica de operación del tren
    }
}
```

### Sincronización con synchronized

#### 1. Control de Posiciones Ocupadas
```java
synchronized (MetroMed.Estado.lock) {
    // Verificar y actualizar posiciones ocupadas
    if (!MetroMed.Estado.positionsOcupadas.contains(positionActual)) {
        MetroMed.Estado.positionsOcupadas.add(positionActual);
    }
}
```

#### 2. Prevención de Colisiones
```java
private static final Object positionLock = new Object();

synchronized (positionLock) {
    if (isPositionFree(nextPos)) {
        move();
        return true;
    }
    return false;
}
```

### Patrón Wait/Notify
El sistema usa `wait()` y `notifyAll()` para coordinación:

```java
while (MetroMed.Estado.positionsOcupadas.contains(positionFrente)) {
    try {
        MetroMed.Estado.lock.wait(); // Esperar hasta que se libere la posición
        positionFrente = getPositionFrente();
    } catch (InterruptedException e) {
        // Manejo de interrupción
    }
}
```

## ⚠️ Gestión de Problemas de Concurrencia

### Prevención de Deadlocks

#### Problema Potencial
```java
// PELIGRO: Posible deadlock si dos hilos adquieren locks en orden diferente
synchronized(lock1) {
    synchronized(lock2) { /* operación */ }
}
```

#### Solución Implementada
1. **Orden consistente de locks**: Siempre se adquiere `Estado.lock` antes que `positionLock`
2. **Timeouts en wait()**: Evita esperas indefinidas
3. **Liberación automática**: Los locks se liberan automáticamente al salir del bloque

### Estrategias Anti-Colisión

#### Comportamiento Inteligente
- **Tren vs Tren**: Espera pacientemente hasta que el otro tren se mueva
- **Tren vs Pared**: Cambia de dirección usando `adjustDirection()`

```java
if (tryMove()) {
    // Movimiento exitoso
} else {
    if (!frontIsClear()) {
        adjustDirection(); // Solo cambia dirección si hay pared
    }
    // Si hay otro tren, simplemente espera
}
```

## 🗺️ Estructura de Clases

### Clase Principal: MetroMed
- **Estado estático**: Controla el estado global del sistema
- **Posiciones ocupadas**: HashSet thread-safe para tracking de posiciones
- **Control de operaciones**: Flags para horarios y estado operativo

### Clase Train (extends Robot)
- **Identificación**: Nombre único y línea asignada
- **Navegación**: Algoritmos de pathfinding inteligente
- **Sincronización**: Manejo de recursos compartidos

### Clase SimplePoint
- **Inmutable**: Representa coordenadas (x, y)
- **Thread-safe**: Implementación segura para entornos concurrentes

## 📊 Estaciones y Coordenadas

| Estación | Coordenadas | Línea |
|----------|-------------|-------|
| NIQUIA | (35, 19) | A |
| ESTRELLA | (1, 11) | A |
| SAN_JAVIER | (16, 1) | B |
| SAN_ANTONIO_B | (14, 12) | B |
| TALLER | (1, 1) | Ambas |

## 🔧 Mejoras Sugeridas

### 📋 **Para Implementar Reglas Faltantes:**

#### 1. Sistema de Taller Completo
```java
// Implementar salida escalonada desde taller
private void scheduleTrainDeparture(Train train, long delayMs) {
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
        @Override
        public void run() {
            train.start();
        }
    }, delayMs);
}
```

#### 2. Horarios Reales con Timestamps
```java
// Sistema de tiempo real
private static final long START_TIME = LocalTime.of(4, 20).toSecondOfDay() * 1000;
private static final long END_TIME = LocalTime.of(23, 0).toSecondOfDay() * 1000;

private boolean isOperationalTime() {
    long currentTime = System.currentTimeMillis() % (24 * 60 * 60 * 1000);
    return currentTime >= START_TIME && currentTime <= END_TIME;
}
```

#### 3. Lógica de San Antonio B (Una Plataforma)
```java
private static final Object sanAntonioPlatform = new Object();

private void handleSanAntonioB() {
    synchronized (sanAntonioPlatform) {
        // Entrar a la plataforma
        waitAtStation();
        turnAround(); // Giro obligatorio
        waitAtStation();
        // Salir de la plataforma
    }
}
```

#### 4. Línea C de Conexión
```java
private static final Object lineaCLock = new Object();
private static boolean lineaCOccupied = false;

private boolean tryEnterLineaC() {
    synchronized (lineaCLock) {
        if (!lineaCOccupied) {
            lineaCOccupied = true;
            return true;
        }
        return false;
    }
}
```

### 🚀 **Mejoras Adicionales de Performance**

### 1. Performance y Escalabilidad
```java
// Usar ConcurrentHashMap en lugar de HashMap + synchronized
private static final ConcurrentHashMap<String, SimplePoint> positions = new ConcurrentHashMap<>();

// Pool de hilos para mejor gestión de recursos
private static final ExecutorService threadPool = Executors.newFixedThreadPool(32);
```

### 2. Algoritmos de Navegación
- **A* Pathfinding**: Rutas más eficientes
- **Predictive Collision Avoidance**: Anticipar conflictos
- **Dynamic Load Balancing**: Distribuir trenes según demanda

### 3. Monitoreo y Logging
```java
// Sistema de logging robusto
private static final Logger logger = LoggerFactory.getLogger(Train.class);

// Métricas de performance
- Tiempo promedio de viaje
- Número de colisiones evitadas
- Eficiencia del sistema
```

### 4. Manejo de Errores Avanzado
```java
// Circuit Breaker pattern para fallos
private volatile boolean emergencyStop = false;

// Retry mechanism con backoff exponencial
private void moveWithRetry(int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        try {
            move();
            return;
        } catch (Exception e) {
            Thread.sleep(Math.pow(2, i) * 100); // Exponential backoff
        }
    }
}
```

### 5. Configuración Externa
```properties
# config.properties
line.a.max.trains=22
line.b.max.trains=10
operation.start.time=04:30
operation.end.time=23:00
collision.check.delay=100
station.wait.time=3000
```

## 🐛 Debugging y Troubleshooting

### Problemas Comunes

1. **Trenes no se mueven**: Verificar que `isOperating()` retorne `true`
2. **Deadlock aparente**: Revisar orden de adquisición de locks
3. **Memory leaks**: Asegurar limpieza de threads al terminar
4. **Race conditions**: Validar uso correcto de `synchronized`

### Herramientas de Debug
```java
// Logging detallado
System.out.println("Tren " + nombre + " en posición " + getPosition() + 
                   " intentando ir a " + getPositionFrente());

// Contador de hilos activos
Thread.getAllStackTraces().keySet().stream()
    .filter(t -> t.getName().startsWith("Train"))
    .count();
```
## 🎯 Casos de Uso

1. **Simulación de tráfico urbano**
2. **Análisis de sistemas de transporte**
3. **Estudio de algoritmos de sincronización**
4. **Educación en programación concurrente**

## 📈 Logros del Proyecto

### ✅ **Aspectos Exitosos:**
- **Concurrencia Robusta**: Sistema multihilo estable sin deadlocks
- **Prevención de Colisiones**: Algoritmo efectivo para evitar choques
- **Sincronización Eficiente**: Uso correcto de `synchronized`, `wait()` y `notify()`
- **Comportamiento Inteligente**: Diferenciación entre obstáculos (trenes vs paredes)
- **Escalabilidad**: Maneja múltiples trenes simultáneamente
- **Control de Estado**: Gestión centralizada del sistema

### ⚠️ **Limitaciones Reconocidas:**
- **Falta de Realismo**: No hay horarios reales ni lógica de estaciones complejas
- **Navegación Básica**: Pathfinding simple sin optimizaciones avanzadas
- **Sin Línea C**: Conexión entre líneas no implementada

### 🎯 **Valor Educativo Logrado:**
- **Programación Concurrente**: Excelente ejemplo de manejo de hilos
- **Sincronización**: Implementación práctica de patrones de concurrencia
- **Resolución de Problemas**: Estrategias para evitar deadlocks y race conditions
- **Arquitectura Software**: Diseño orientado a objetos con responsabilidades claras

*Profe tenganos piedad, muchas gracias por todo, lo queremos mucho.*

---
