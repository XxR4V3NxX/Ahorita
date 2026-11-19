package com.ahorita.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class TaskRepository {
    // TODO: reemplazar por Room/SQLite para que tareas y racha sobrevivan al cerrar la app.
    private static final List<Task> tasks = new ArrayList<>();
    private static long nextId = 1;
    private static int currentStreak = 0;
    private static int bestStreak = 0;

    /** Tareas ordenadas por vencimiento (la más cercana primero). */
    public static List<Task> getAll() {
        Collections.sort(tasks, Comparator.comparingLong(t -> t.deadlineMillis));
        return tasks;
    }

    public static void add(String title, long durationMillis, long deadlineMillis, List<String> steps) {
        tasks.add(new Task(nextId++, title, durationMillis, deadlineMillis, steps));
    }

    /**
     * Marca la tarea como terminada.
     * Si se completa antes de su hora límite, suma 1 a la racha.
     * Si ya había vencido, la racha se reinicia.
     */
    public static void complete(long id, long nowMillis) {
        Task task = find(id);
        if (task == null) return;

        if (nowMillis < task.deadlineMillis) {
            currentStreak++;
            if (currentStreak > bestStreak) bestStreak = currentStreak;
        } else {
            currentStreak = 0;
        }
        removeById(id);
    }

    /** Quita las tareas que ya vencieron sin completarse y reinicia la racha. */
    public static void expireOverdue(long nowMillis) {
        Iterator<Task> it = tasks.iterator();
        while (it.hasNext()) {
            if (it.next().deadlineMillis <= nowMillis) {
                currentStreak = 0;
                it.remove();
            }
        }
    }

    public static int getCurrentStreak() {
        return currentStreak;
    }

    public static int getBestStreak() {
        return bestStreak;
    }

    private static Task find(long id) {
        for (Task t : tasks) {
            if (t.id == id) return t;
        }
        return null;
    }

    private static void removeById(long id) {
        Iterator<Task> it = tasks.iterator();
        while (it.hasNext()) {
            if (it.next().id == id) it.remove();
        }
    }
}