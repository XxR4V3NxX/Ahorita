package com.ahorita.app;

import java.util.Collections;
import java.util.List;

public class Task {
    public final long id;
    public final String title;
    public long durationMillis;  // duración total de la tarea (se actualiza con "+5 min")
    public long deadlineMillis;  // momento exacto en que vence (se actualiza con "+5 min")
    public final List<String> steps; // NUEVO: pasos pequeños que desglosan la tarea

    public Task(long id, String title, long durationMillis, long deadlineMillis) {
        this(id, title, durationMillis, deadlineMillis, Collections.emptyList());
    }

    public Task(long id, String title, long durationMillis, long deadlineMillis, List<String> steps) {
        this.id = id;
        this.title = title;
        this.durationMillis = durationMillis;
        this.deadlineMillis = deadlineMillis;
        this.steps = steps;
    }

    /** Minutos que faltan desde ahora. Puede ser negativo si ya venció. */
    public long minutesLeft(long nowMillis) {
        return (deadlineMillis - nowMillis) / 60000L;
    }
}