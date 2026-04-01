package com.example.ucms_android.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ThreeDBarView extends View {

    private int baseColor = Color.parseColor("#2196F3");
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public ThreeDBarView(Context context) {
        super(context);
    }

    public ThreeDBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setBarColor(int color) {
        this.baseColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float depth = w / 3f;
        if (h <= depth) depth = h / 2f; // Prevent depth from being larger than height

        // 1. Top Face (Lighter)
        paint.setColor(adjustColor(baseColor, 1.15f));
        path.reset();
        path.moveTo(0, depth);
        path.lineTo(depth, 0);
        path.lineTo(w, 0);
        path.lineTo(w - depth, depth);
        path.close();
        canvas.drawPath(path, paint);

        // 2. Side Face (Right, Darker)
        paint.setColor(adjustColor(baseColor, 0.85f));
        path.reset();
        path.moveTo(w - depth, depth);
        path.lineTo(w, 0);
        path.lineTo(w, h - depth);
        path.lineTo(w - depth, h);
        path.close();
        canvas.drawPath(path, paint);

        // 3. Front Face (Base Color)
        paint.setColor(baseColor);
        canvas.drawRect(0, depth, w - depth, h, paint);
    }

    private int adjustColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.max(0, (int) (Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, (int) (Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, (int) (Color.blue(color) * factor)));
        return Color.argb(a, r, g, b);
    }
}
