package com.ahorita.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    // DATOS TEMPORALES: sustituir por repositorio/base de datos cuando exista backend.
    private String taskTitle = "Salir para la clase de Diseño";
    private long taskDurationMillis = 42 * 60 * 1000L;
    private long totalMillis = taskDurationMillis;
    private CountDownTimer countDownTimer;
    private long remainingMillis = taskDurationMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        setContentView(R.layout.activity_home);

        TextView title = findViewById(R.id.tvUrgentTask);
        TextView time = findViewById(R.id.tvUrgentTime);
        title.setText(taskTitle);
        applyUrgency(findViewById(R.id.cardTaskPreview), 78);   // 1 h 18 min → rojo
        applyUrgency(findViewById(R.id.cardMedicine), 188);     // 3 h 08 min → amarillo
        applyUrgency(findViewById(R.id.cardEmail), 360);        // 6 h → verde
        time.setText(formatMinutes(remainingMillis));

        Button start = findViewById(R.id.btnStartTask);
        start.setOnClickListener(v -> showCountdown());

        findViewById(R.id.btnNewTask).setOnClickListener(v -> showNewTask());
        findViewById(R.id.btnSeeAll).setOnClickListener(v ->
                Toast.makeText(this, "Aquí se mostrará la lista completa de tareas.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnStreaks).setOnClickListener(v ->
                Toast.makeText(this, "La sección de rachas se implementará después.", Toast.LENGTH_SHORT).show());
    }

    private void showNewTask() {
        setContentView(R.layout.activity_new_task);

        EditText input = findViewById(R.id.etTaskTitle);
        Button create = findViewById(R.id.btnCreateTask);
        create.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                input.setError("Escribe qué tienes que hacer");
                return;
            }
            taskTitle = value;
            // TODO: guardar la tarea y sus avisos en la base de datos.
            remainingMillis = taskDurationMillis;
            totalMillis = taskDurationMillis; // NUEVO: al crear una tarea nueva, el total vuelve a la duración base
            showHome();
        });
        findViewById(R.id.btnCancelTask).setOnClickListener(v -> showHome());
    }

    private void showCountdown() {
        setContentView(R.layout.activity_countdown);
        TextView task = findViewById(R.id.tvCountdownTask);
        TextView timer = findViewById(R.id.tvCountdown);
        ArcProgressView arc = findViewById(R.id.arcCountdown); // NUEVO
        task.setText(taskTitle);
        updateTimerText(timer, remainingMillis);
        updateArc(arc); // NUEVO: el arco arranca con el progreso correcto

        findViewById(R.id.btnDone).setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            remainingMillis = 0;
            Toast.makeText(this, "¡Actividad completada!", Toast.LENGTH_SHORT).show();
            showHome();
        });
        findViewById(R.id.btnAddFive).setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            remainingMillis += 5 * 60 * 1000L;
            totalMillis += 5 * 60 * 1000L; // NUEVO: el total crece igual que el tiempo, para que el arco no pase de lleno
            startTimer(timer, arc);        // CAMBIA: ahora recibe también el arco
        });
        findViewById(R.id.btnBackHome).setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            showHome();
        });
        startTimer(timer, arc);            // CAMBIA: ahora recibe también el arco
    }

    private void startTimer(TextView timerView, ArcProgressView arc) { // CAMBIA: nuevo parámetro
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateTimerText(timerView, remainingMillis);
                updateArc(arc);
            }
            @Override public void onFinish() {
                remainingMillis = 0;
                updateTimerText(timerView, 0);
                updateArc(arc);
                Toast.makeText(MainActivity.this, "¡Se terminó el tiempo!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void updateArc(ArcProgressView arc) {
        if (totalMillis <= 0) return;
        arc.setProgress((float) remainingMillis / totalMillis);
    }
    private void updateTimerText(TextView view, long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        view.setText(String.format(Locale.getDefault(), "%02d:%02d",
                totalSeconds / 60, totalSeconds % 60));
    }

    private String formatMinutes(long millis) {
        return Math.max(1, millis / 60000) + " min";
    }

    @Override protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }

    private void applyUrgency(View card, long minutesLeft) {
        int background;
        if (minutesLeft < 120) {
            background = R.drawable.bg_urgency_red;     // menos de 2 horas
        } else if (minutesLeft <= 240) {
            background = R.drawable.bg_urgency_yellow;  // de 2 a 4 horas
        } else {
            background = R.drawable.bg_urgency_green;   // más de 4 horas
        }
        card.setBackgroundResource(background);
    }
}
