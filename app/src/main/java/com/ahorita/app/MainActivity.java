package com.ahorita.app;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.widget.CheckBox;


public class MainActivity extends Activity {
    // DATOS TEMPORALES: sustituir por repositorio/base de datos cuando exista backend.
    private CountDownTimer countDownTimer;
    private long remainingMillis;   // milisegundos que faltan en la pantalla de cuenta regresiva
    private Task activeTask;        // tarea que se está cronometrando
    private long customDeadlineMillis = 0;
    private boolean showAllTasks = false;
    private static final int HOME_TASK_LIMIT = 4;
    private final List<String> pendingSteps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        setContentView(R.layout.activity_home);

        TextView label = findViewById(R.id.tvUrgentLabel);
        TextView title = findViewById(R.id.tvUrgentTask);
        TextView time = findViewById(R.id.tvUrgentTime);
        Button start = findViewById(R.id.btnStartTask);

        Task urgent = getMostUrgent();
        if (urgent == null) {
            // Sin tareas: mensaje de estado vacío y sin botón de inicio
            label.setText("AHORA");
            title.setText("No tienes tareas pendientes");
            time.setText("—");
            start.setVisibility(View.GONE);
        } else {
            long now = System.currentTimeMillis();
            String hour = new SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(new Date(urgent.deadlineMillis));
            label.setText("AHORA  ·  vence " + hour);
            title.setText(urgent.title);
            time.setText(formatMinutes(urgent.deadlineMillis - now));
            start.setVisibility(View.VISIBLE);
            start.setOnClickListener(v -> showCountdown(urgent));
        }

        findViewById(R.id.btnNewTask).setOnClickListener(v -> showNewTask());
        findViewById(R.id.btnStreaks).setOnClickListener(v ->
                Toast.makeText(this, "La sección de rachas se implementará después.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnSeeAll).setOnClickListener(v -> {
            showAllTasks = !showAllTasks;
            renderTasks();
        });

        renderTasks();
    }

    // La tarea que vence primero. TaskRepository.getAll() ya viene ordenada por vencimiento.
    private Task getMostUrgent() {
        List<Task> all = TaskRepository.getAll();
        return all.isEmpty() ? null : all.get(0);
    }

    private void showNewTask() {
        setContentView(R.layout.activity_new_task);
        customDeadlineMillis = 0;
        pendingSteps.clear(); // cada vez empieza sin pasos

        EditText input = findViewById(R.id.etTaskTitle);
        RadioGroup durations = findViewById(R.id.rgDuration);
        TextView customTime = findViewById(R.id.tvCustomTime);
        EditText stepInput = findViewById(R.id.etStep);
        LinearLayout stepsContainer = findViewById(R.id.containerNewSteps);

        durations.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCustom) {
                pickCustomTime(customTime, durations);
            } else {
                customTime.setVisibility(View.GONE);
            }
        });
        customTime.setOnClickListener(v -> pickCustomTime(customTime, durations));

        // NUEVO: agrega un paso a la lista temporal
        findViewById(R.id.btnAddStep).setOnClickListener(v -> {
            String step = stepInput.getText().toString().trim();
            if (step.isEmpty()) {
                stepInput.setError("Escribe un paso");
                return;
            }
            pendingSteps.add(step);
            addStepRow(stepsContainer, step);
            stepInput.setText("");
        });

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

            // CAMBIO: ahora también se pasan los pasos
            TaskRepository.add(value, duration, deadline, new ArrayList<>(pendingSteps));
            showHome();
        });
        findViewById(R.id.btnCancelTask).setOnClickListener(v -> showHome());
    }

    // Muestra un paso ya agregado como una línea de texto
    private void addStepRow(LinearLayout container, String text) {
        TextView row = new TextView(this);
        row.setText("•  " + text);
        row.setTextColor(getResources().getColor(R.color.ah_navy, getTheme()));
        row.setTextSize(16);
        row.setPadding(0, 6, 0, 6);
        container.addView(row);
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

    private void showCountdown(Task task) {
        activeTask = task;
        setContentView(R.layout.activity_countdown);

        TextView taskView = findViewById(R.id.tvCountdownTask);
        TextView timerView = findViewById(R.id.tvCountdown);
        ArcProgressView arc = findViewById(R.id.arcCountdown);

        taskView.setText(task.title);
        renderSteps(findViewById(R.id.containerSteps), findViewById(R.id.tvStepsTitle), task.steps);

        // El tiempo restante sale de la hora límite, no de un número guardado aparte
        remainingMillis = Math.max(0, task.deadlineMillis - System.currentTimeMillis());
        updateTimerText(timerView, remainingMillis);
        updateArc(arc);

        findViewById(R.id.btnDone).setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            TaskRepository.remove(activeTask.id); // la tarea terminada deja de aparecer
            Toast.makeText(this, "¡Actividad completada!", Toast.LENGTH_SHORT).show();
            showHome();
        });
        findViewById(R.id.btnAddFive).setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            long fiveMin = 5 * 60 * 1000L;
            activeTask.deadlineMillis += fiveMin;  // la hora límite se corre 5 min
            activeTask.durationMillis += fiveMin;  // el total crece igual, para que el arco no pase de lleno
            remainingMillis = Math.max(0, activeTask.deadlineMillis - System.currentTimeMillis());
            startTimer(timerView, arc);
        });
        findViewById(R.id.btnBackHome).setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            showHome();
        });

        startTimer(timerView, arc);
    }

    private void startTimer(TextView timerView, ArcProgressView arc) {
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
        if (activeTask == null || activeTask.durationMillis <= 0) return;
        arc.setProgress((float) remainingMillis / activeTask.durationMillis);
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


    private void renderTasks() {
        LinearLayout container = findViewById(R.id.containerSiguientes);
        Button seeAll = findViewById(R.id.btnSeeAll);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        long now = System.currentTimeMillis();
        List<Task> all = TaskRepository.getAll();
        int limit = showAllTasks ? all.size() : Math.min(HOME_TASK_LIMIT, all.size());

        for (int i = 0; i < limit; i++) {
            Task task = all.get(i);
            View card = inflater.inflate(R.layout.item_task, container, false);
            long minutesLeft = task.minutesLeft(now);

            ((TextView) card.findViewById(R.id.tvTaskTitle)).setText(task.title);
            ((TextView) card.findViewById(R.id.tvTaskInfo)).setText(formatInfo(minutesLeft, task.deadlineMillis));
            applyUrgency(card, minutesLeft);

            container.addView(card);
        }


        if (all.size() > HOME_TASK_LIMIT) {
            seeAll.setVisibility(View.VISIBLE);
            seeAll.setText(showAllTasks ? "Ver menos" : "Ver todo");
        } else {
            seeAll.setVisibility(View.GONE);
        }
    }


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
    private void renderSteps(LinearLayout container, TextView title, List<String> steps) {
        container.removeAllViews();
        if (steps.isEmpty()) {
            title.setVisibility(View.GONE);
            return;
        }
        title.setVisibility(View.VISIBLE);
        for (String step : steps) {
            CheckBox box = new CheckBox(this);
            box.setText(step);
            container.addView(box);
        }
    }
}
