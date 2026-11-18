package com.ahorita.app;

public class Task {
    public final long id;
    public final String title;
    public final long durationMillis;  // duración total que eligió el usuario
    public final long deadlineMillis;  // momento exacto en que vence

    public Task(long id, String title, long durationMillis, long deadlineMillis) {
        this.id = id;
        this.title = title;
        this.durationMillis = durationMillis;
        this.deadlineMillis = deadlineMillis;
    }

    /** Minutos que faltan desde ahora. Puede ser negativo si ya venció. */
    public long minutesLeft(long nowMillis) {
        return (deadlineMillis - nowMillis) / 60000L;
    }
}