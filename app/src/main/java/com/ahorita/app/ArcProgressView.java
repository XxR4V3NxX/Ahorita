package com.ahorita.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ArcProgressView extends View {

    private static final float START_ANGLE = 135f; // inicia en la esquina inferior izquierda
    private static final float FULL_SWEEP = 270f;  // 270° de recorrido, deja abierta la parte de abajo

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private float progress = 1f; // 1 = lleno, 0 = vacío

    public ArcProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float stroke = 14 * context.getResources().getDisplayMetrics().density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(stroke);
        trackPaint.setStrokeCap(Paint.Cap.BUTT);
        trackPaint.setColor(0xFFE2E6EE); // gris claro de fondo

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeCap(Paint.Cap.BUTT);
        progressPaint.setColor(context.getResources().getColor(R.color.ah_orange, context.getTheme()));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float stroke = trackPaint.getStrokeWidth();
        float size = Math.min(w, h) - stroke; // diámetro del círculo, dejando espacio para el grosor
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        arcBounds.set(left, top, left + size, top + size);
    }

    /** Recibe un valor de 0 a 1 con la proporción de tiempo restante. */
    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(1f, value));
        invalidate(); // pide redibujar la vista
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(arcBounds, START_ANGLE, FULL_SWEEP, false, trackPaint);
        canvas.drawArc(arcBounds, START_ANGLE, FULL_SWEEP * progress, false, progressPaint);
    }
}