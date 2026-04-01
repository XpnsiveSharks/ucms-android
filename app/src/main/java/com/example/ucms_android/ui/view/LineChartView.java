package com.example.ucms_android.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LineChartView extends View {

    private List<DataPoint> dataPoints = new ArrayList<>();
    private Paint linePaint;
    private Paint pointPaint;
    private Paint fillPaint;
    private Paint labelPaint;
    private float maxVal = 0;

    public static class DataPoint {
        public String label;
        public long value;

        public DataPoint(String label, long value) {
            this.label = label;
            this.value = value;
        }
    }

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFF2196F3); // Primary color
        linePaint.setStrokeWidth(dpToPx(3));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.WHITE);
        pointPaint.setStyle(Paint.Style.FILL);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF888888);
        labelPaint.setTextSize(dpToPx(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    private int gridLines = 4;
    private long displayMax = 10;

    public void setData(List<DataPoint> points) {
        this.dataPoints = points;
        long realMax = 0;
        for (DataPoint p : points) {
            if (p.value > realMax) realMax = p.value;
        }
        
        // Calculate nice scale
        if (realMax <= 4) {
            gridLines = realMax == 0 ? 1 : (int) realMax;
            displayMax = gridLines;
        } else {
            gridLines = 4;
            long stepSize = (long) Math.ceil(realMax / 4.0);
            // Make stepSize a multiple of 2, 5, or 10 if large
            if (stepSize > 2 && stepSize % 2 != 0) stepSize++;
            displayMax = stepSize * 4;
        }
        this.maxVal = displayMax;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints == null || dataPoints.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float paddingLeft = dpToPx(48);
        float paddingRight = dpToPx(16);
        float paddingTop = dpToPx(24);
        float paddingBottom = dpToPx(32);
        
        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        // Draw Y-axis labels and grid lines
        labelPaint.setTextAlign(Paint.Align.LEFT);
        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x1A000000);
        gridPaint.setStrokeWidth(dpToPx(1));

        for (int i = 0; i <= gridLines; i++) {
            float y = paddingTop + chartHeight - (chartHeight * i / gridLines);
            long val = Math.round((double)displayMax * i / gridLines);
            // Center text vertically on the line
            float textOffset = (labelPaint.descent() + labelPaint.ascent()) / 2;
            canvas.drawText(String.valueOf(val), 0, y - textOffset, labelPaint);
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
        }

        float stepX = chartWidth / (dataPoints.size() - 1);
        
        Path linePath = new Path();
        Path fillPath = new Path();

        List<PointF> points = new ArrayList<>();
        labelPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < dataPoints.size(); i++) {
            float x = paddingLeft + (i * stepX);
            float y = paddingTop + chartHeight - (chartHeight * (dataPoints.get(i).value / maxVal));
            points.add(new PointF(x, y));

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, paddingTop + chartHeight);
                fillPath.lineTo(x, y);
            } else {
                float prevX = points.get(i - 1).x;
                float prevY = points.get(i - 1).y;
                float cx = (prevX + x) / 2;
                linePath.quadTo(prevX, prevY, cx, (prevY + y) / 2);
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }

            // Draw Label (Day name)
            canvas.drawText(dataPoints.get(i).label, x, height - dpToPx(8), labelPaint);
        }

        fillPath.lineTo(points.get(points.size() - 1).x, paddingTop + chartHeight);
        fillPath.close();

        // Gradient for fill
        fillPaint.setShader(new LinearGradient(0, paddingTop, 0, paddingTop + chartHeight, 
            0x442196F3, 0x002196F3, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Draw Line
        canvas.drawPath(linePath, linePaint);

        // Draw Points
        for (PointF p : points) {
            pointPaint.setColor(0xFF2196F3);
            canvas.drawCircle(p.x, p.y, dpToPx(5), pointPaint);
            pointPaint.setColor(Color.WHITE);
            canvas.drawCircle(p.x, p.y, dpToPx(3), pointPaint);
        }
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
