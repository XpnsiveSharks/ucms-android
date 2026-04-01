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

    private int baseColor = Color.BLUE;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int depth = 20; // depth in pixels

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

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        depth = w / 3; // Dynamic depth based on width

        // 1. Top Face (Lighter)
        paint.setColor(adjustColor(baseColor, 1.2f));
        path.reset();
        path.moveTo(0, depth);
        path.lineTo(depth, 0);
        path.lineTo(w, 0);
        path.lineTo(w - depth, depth);
        path.close();
        canvas.drawPath(path, paint);

        // 2. Side Face (Right, Darker)
        paint.setColor(adjustColor(baseColor, 0.7f));
        path.reset();
        path.moveTo(w - depth, depth);
        path.lineTo(w, 0);
        path.lineTo(w, h - depth);
        path.lineTo(w - depth, h);
        path.close();
        canvas.drawPath(path, paint);

        // 3. Front Face (Base)
        paint.setColor(baseColor);
        canvas.drawRect(0, depth, w - depth, h, paint);
    }

    private int adjustColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }
}
