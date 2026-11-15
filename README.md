# ahorita — proyecto inicial Android

Configuración elegida:
- Java
- XML
- minSdk 26 (Android 8.0)
- compileSdk 36
- Orientación vertical
- Sin dependencias externas en esta primera base

## Abrir
1. Descomprime el ZIP.
2. En Android Studio selecciona **Open** y elige la carpeta `Ahorita`.
3. Usa JDK 17 para Gradle.
4. Instala Android SDK Platform 36 desde SDK Manager.
5. Espera la sincronización de Gradle y ejecuta en un emulador o dispositivo.

## Estado de esta primera entrega
- Inicio: estructura visual inicial y navegación a nueva tarea/temporizador.
- Nueva tarea: captura de título y configuración visual preliminar.
- Temporizador: cuenta regresiva funcional durante la sesión en primer plano.
- Widget: proveedor y layout inicial de demostración, todavía con contenido fijo.

## Pendiente
- Ajustar con precisión las medidas y detalles visuales usando las páginas originales del PDF.
- Hacer persistentes tareas y estados (Room/SQLite).
- Implementar fechas límite reales, avisos/notificaciones y permisos.
- Hacer que el temporizador sobreviva al cierre de la app y a reinicios.
- Conectar el widget al mismo repositorio de tareas y actualizarlo correctamente.
- Implementar la pantalla de resumen y rachas.
- Completar la selección de duración y avisos en el formulario.

Los textos, horas y actividades de ejemplo están marcados en Java/XML como contenido temporal.
