package com.ahorita.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskRepository {
    // TODO: reemplazar esta lista por Room/SQLite para que las tareas sobrevivan al cerrar la app.
    private static final List<Task> tasks = new ArrayList<>();
    private static long nextId = 1;

    /** Devuelve las tareas ordenadas por hora de vencimiento (la más cercana primero). */
    public static List<Task> getAll() {
        Collections.sort(tasks, Comparator.comparingLong(t -> t.deadlineMillis));
        return tasks;
    }

    public static void add(String title, long durationMillis, long deadlineMillis) {
        tasks.add(new Task(nextId++, title, durationMillis, deadlineMillis));
    }
}