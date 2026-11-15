package com.ahorita.app.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.ahorita.app.R;

/**
 * Widget inicial de demostración.
 * TODO: leer la actividad y el tiempo restante desde el repositorio compartido de tareas.
 * TODO: actualizar con AlarmManager/WorkManager según la frecuencia y límites de Android.
 */
public class AhoritaWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_ahorita);
            manager.updateAppWidget(id, views);
        }
    }
}
