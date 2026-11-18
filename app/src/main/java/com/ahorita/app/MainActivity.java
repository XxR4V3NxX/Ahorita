package com.ahorita.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.TimePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import java.util.Locale;

public class MainActivity extends Activity {
    // DATOS TEMPORALES: sustituir por repositorio/base de datos cuando exista backend.
    private String taskTitle = "Salir para la clase de Diseño";
    private long taskDurationMillis = 42 * 60 * 1000L;
    private long totalMillis = taskDurationMillis;
    private CountDownTimer countDownTimer;
    private long remainingMillis = taskDurationMillis;
    private long customDeadlineMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        setContentView(R.layout.activity_home);

        // Tarea urgente: todavía fija. La conectaremos a TaskRepository en el siguiente paso.
        TextView title = findViewById(R.id.tvUrgentTask);
        TextView time = findViewById(R.id.tvUrgentTime);
        title.setText(taskTitle);
        time.setText(formatMinutes(remainingMillis));

        Button start = findViewById(R.id.btnStartTask);
        start.setOnClickListener(v -> showCountdown());

        renderTasks();

        findViewById(R.id.btnNewTask).setOnClickListener(v -> showNewTask());
        findViewById(R.id.btnSeeAll).setOnClickListener(v ->
                Toast.makeText(this, "Aquí se mostrará la lista completa de tareas.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnStreaks).setOnClickListener(v ->
                Toast.makeText(this, "La sección de rachas se implementará después.", Toast.LENGTH_SHORT).show());
    }

    private void showNewTask() {
        setContentView(R.layout.activity_new_task);
        customDeadlineMillis = 0; // cada vez que se abre la pantalla empieza sin hora elegida

        EditText input = findViewById(R.id.etTaskTitle);
        RadioGroup durations = findViewById(R.id.rgDuration);
        TextView customTime = findViewById(R.id.tvCustomTime);

        // Si eligen "Elegir hora", abrimos el selector. Si eligen otra opción, ocultamos la hora
        durations.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCustom) {
                pickCustomTime(customTime, durations);
            } else {
                customTime.setVisibility(View.GONE);
            }
        });
        // Tocar la hora mostrada permite cambiarla
        customTime.setOnClickListener(v -> pickCustomTime(customTime, durations));

        findViewById(R.id.btnCreateTask).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                input.setError("Escribe qué tienes que hacer");
                return;
            }

            long now = System.currentTimeMillis();
            int checkedId = durations.getCheckedRadioButtonId();
            long duration;
            long deadline;

            if (checkedId == R.id.rbCustom) {
                if (customDeadlineMillis == 0) {
                    Toast.makeText(this, "Elige la hora de vencimiento", Toast.LENGTH_SHORT).show();
                    return;
                }
                deadline = customDeadlineMillis;
                duration = deadline - now;
            } else {
                duration = presetDurationMillis(checkedId);
                deadline = now + duration;
            }

            TaskRepository.add(value, duration, deadline);
            showHome();
        });
        findViewById(R.id.btnCancelTask).setOnClickListener(v -> showHome());
    }

    // Abre el selector de hora y guarda la hora elegida
    private void pickCustomTime(TextView label, RadioGroup group) {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(Calendar.HOUR_OF_DAY, hour);
            chosen.set(Calendar.MINUTE, minute);
            chosen.set(Calendar.SECOND, 0);
            chosen.set(Calendar.MILLISECOND, 0);


            if (chosen.getTimeInMillis() <= System.currentTimeMillis()) {
                chosen.add(Calendar.DAY_OF_MONTH, 1);
            }

            customDeadlineMillis = chosen.getTimeInMillis();
            label.setText("Vence " + new SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(new Date(customDeadlineMillis)));
            label.setVisibility(View.VISIBLE);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);

        // Si cancelan sin haber elegido nunca una hora, regresamos a la opción de 15 min
        dialog.setOnCancelListener(d -> {
            if (customDeadlineMillis == 0) {
                group.check(R.id.rb15);
            }
        });
        dialog.show();
    }


    private long presetDurationMillis(int checkedId) {
        if (checkedId == R.id.rb30) return 30 * 60000L;
        if (checkedId == R.id.rb60) return 60 * 60000L;
        if (checkedId == R.id.rb120) return 120 * 60000L;
        return 15 * 60000L; // rb15 es el valor por defecto
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

    // Crea una tarjeta por cada tarea guardada y la agrega a la sección "Siguientes"
    private void renderTasks() {
        LinearLayout container = findViewById(R.id.containerSiguientes);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        long now = System.currentTimeMillis();

        for (Task task : TaskRepository.getAll()) {
            View card = inflater.inflate(R.layout.item_task, container, false);
            long minutesLeft = task.minutesLeft(now);

            ((TextView) card.findViewById(R.id.tvTaskTitle)).setText(task.title);
            ((TextView) card.findViewById(R.id.tvTaskInfo)).setText(formatInfo(minutesLeft, task.deadlineMillis));
            applyUrgency(card, minutesLeft);

            container.addView(card);
        }
    }

    // Texto de la tarjeta, por ejemplo: "1 h 18 min  ·  vence 9:10 a. m."
    private String formatInfo(long minutesLeft, long deadlineMillis) {
        String left;
        if (minutesLeft < 0) {
            left = "vencida";
        } else if (minutesLeft >= 60) {
            left = (minutesLeft / 60) + " h "
                    + String.format(Locale.getDefault(), "%02d", minutesLeft % 60) + " min";
        } else {
            left = minutesLeft + " min";
        }
        String hour = new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(new Date(deadlineMillis));
        return left + "  ·  vence " + hour;
    }
}
