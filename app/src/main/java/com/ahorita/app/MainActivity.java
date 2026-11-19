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
import android.app.DatePickerDialog;


public class MainActivity extends Activity {
    // DATOS TEMPORALES: sustituir por repositorio/base de datos cuando exista backend.
    private CountDownTimer countDownTimer;
    private long remainingMillis;   // milisegundos que faltan en la pantalla de cuenta regresiva
    private Task activeTask;        // tarea que se está cronometrando
    private long customDeadlineMillis = 0;
    private boolean showAllTasks = false;
    private static final int HOME_TASK_LIMIT = 4;
    private final List<String> pendingSteps = new ArrayList<>();
    private TextView countdownUnit;

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
            label.setText("Proximo  ·  vence " + formatDeadline(urgent.deadlineMillis));
            title.setText(urgent.title);
            time.setText(formatRemaining(urgent.deadlineMillis - now));
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
                pickDeadline(customTime, durations, false);
            } else if (checkedId == R.id.rbDate) {
                pickDeadline(customTime, durations, true);
            } else {
                customTime.setVisibility(View.GONE);
            }
        });
        customTime.setOnClickListener(v ->
                pickDeadline(customTime, durations,
                        durations.getCheckedRadioButtonId() == R.id.rbDate));

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

            if (checkedId == R.id.rbCustom || checkedId == R.id.rbDate) {

                if (customDeadlineMillis == 0) {
                    Toast.makeText(this, "Elige una fecha y hora de vencimiento",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                deadline = customDeadlineMillis;
                duration = deadline - now;

                if (duration <= 0) {
                    Toast.makeText(this, "La fecha debe ser futura",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

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
    // Flujo de elección: si pickDate es true, primero abre el calendario y luego la hora
    private void pickDeadline(TextView label, RadioGroup group, boolean pickDate) {
        Calendar today = Calendar.getInstance();
        if (pickDate) {
            DatePickerDialog dateDialog = new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar date = Calendar.getInstance();
                date.set(year, month, day);
                pickTimeFor(date, label, group, true);
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

            // No permite fechas pasadas
            dateDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

            dateDialog.setOnCancelListener(d -> {
                if (customDeadlineMillis == 0) group.check(R.id.rb15);
            });
            dateDialog.show();
        } else {
            pickTimeFor(today, label, group, false);
        }
    }

    // Abre el selector de hora sobre el día indicado y guarda el resultado
    private void pickTimeFor(Calendar day, TextView label, RadioGroup group, boolean dateChosen) {
        Calendar now = Calendar.getInstance();
        int initialHour = dateChosen ? 9 : now.get(Calendar.HOUR_OF_DAY);
        int initialMinute = dateChosen ? 0 : now.get(Calendar.MINUTE);

        TimePickerDialog timeDialog = new TimePickerDialog(this, (view, hour, minute) -> {
            Calendar chosen = (Calendar) day.clone();
            chosen.set(Calendar.HOUR_OF_DAY, hour);
            chosen.set(Calendar.MINUTE, minute);
            chosen.set(Calendar.SECOND, 0);
            chosen.set(Calendar.MILLISECOND, 0);

            if (chosen.getTimeInMillis() <= System.currentTimeMillis()) {
                if (dateChosen) {
                    // Con fecha explícita no movemos nada: avisamos y pedimos otra hora
                    Toast.makeText(this, "Esa hora ya pasó. Elige una hora futura.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Con "Elegir hora" se toma como la de mañana
                chosen.add(Calendar.DAY_OF_MONTH, 1);
            }

            customDeadlineMillis = chosen.getTimeInMillis();
            label.setText("Vence " + formatDeadline(customDeadlineMillis));
            label.setVisibility(View.VISIBLE);
        }, initialHour, initialMinute, false);

        timeDialog.setOnCancelListener(d -> {
            if (customDeadlineMillis == 0) group.check(R.id.rb15);
        });
        timeDialog.show();
    }

    // Muestra solo la hora si vence hoy; si no, agrega el día, por ejemplo "jue 3 dic, 3:30 p. m."
    private String formatDeadline(long millis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();

        boolean isToday = target.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        String pattern = isToday ? "h:mm a" : "EEE d MMM, h:mm a";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
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
        countdownUnit = findViewById(R.id.tvCountdownUnit);
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
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days >= 1) {
            // Más de un día: número de días y las horas restantes debajo
            view.setText(String.valueOf(days));
            countdownUnit.setText("días · " + hours + " h");
        } else if (totalSeconds >= 3600) {
            // Entre 1 y 24 horas: horas y minutos
            view.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    hours, minutes));
            countdownUnit.setText("horas : minutos");
        } else {
            // Menos de una hora: el formato original
            view.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    minutes, seconds));
            countdownUnit.setText("minutos : segundos");
        }
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
            ((TextView) card.findViewById(R.id.tvTaskInfo))
                    .setText(formatInfo(task.deadlineMillis - now, task.deadlineMillis));
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


    // Texto de la tarjeta: "13 d 17 h  ·  vence Wed 2 Dec, 6:00 PM"
    private String formatInfo(long millisLeft, long deadlineMillis) {
        return formatRemaining(millisLeft) + "  ·  vence " + formatDeadline(deadlineMillis);
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

    // Tiempo restante legible: "19 d 17 h", "5 h 12 min" o "42 min"
    private String formatRemaining(long millis) {
        if (millis <= 0) return "vencida";
        long totalMinutes = millis / 60000L;
        long days = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long minutes = totalMinutes % 60;

        if (days >= 1) {
            return days + " d " + hours + " h";
        } else if (hours >= 1) {
            return hours + " h " + String.format(Locale.getDefault(), "%02d", minutes) + " min";
        } else {
            return Math.max(1, minutes) + " min";
        }
    }
}
