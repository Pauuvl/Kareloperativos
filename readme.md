## Proyecto de Karel

Este proyecto simula un sistema de trenes tipo Metro usando la librería KarelJRobot. Cada tren es un robot que sigue rutas predefinidas, se detiene en estaciones y evita colisiones con otros trenes.

### Estructura del proyecto
- **MetroMed.java**: Código principal que gestiona la simulación y los trenes.
- **KarelJRobot.jar**: Librería necesaria para ejecutar los robots Karel.
- **MetroMed.kwld**: Archivo de mundo para la simulación.
- **lib/**: Carpeta donde se encuentra la librería KarelJRobot.jar.

### Compilación y ejecución desde la terminal

1. **Compilar:**

   ```sh
   javac -cp ".;lib/KarelJRobot.jar" MetroMed.java
   ```
   > En Linux o MacOS usa `:` en vez de `;`:
   >
   > ```sh
   > javac -cp ".:lib/KarelJRobot.jar" MetroMed.java
   > ```

2. **Ejecutar:**

   ```sh
   java -cp ".;lib/KarelJRobot.jar" MetroMed
   ```
   > En Linux o MacOS usa `:` en vez de `;`:
   >
   > ```sh
   > java -cp ".:lib/KarelJRobot.jar" MetroMed
   > ```

### Notas
- No es necesario ejecutar ningún archivo .bat.
- Asegúrate de que el archivo `KarelJRobot.jar` esté en la carpeta `lib`.
- El mundo se visualiza automáticamente al ejecutar el programa.
- Para salir de la ejecución en la consola, presiona **Ctrl+C**.