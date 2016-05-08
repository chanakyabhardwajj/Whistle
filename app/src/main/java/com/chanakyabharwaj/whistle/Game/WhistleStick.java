package com.chanakyabharwaj.whistle.Game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;

public class WhistleStick extends Shape {
    public float dampFactor = 0.95f;
    public boolean configured = false;
    public float minLength = 100;
    public float length = 100;

    public PointF end = new PointF();

    private Paint helperStickPaint;
    private int helperStickLength = 1000;
    private PointF helperStickEnd = new PointF();

    private Paint helperCirclePaint;

    public double angle = Math.PI / 2;

    public void configure(Canvas c) {
        if (configured) {
            return;
        }

        pos.x = c.getWidth() / 2;
        pos.y = c.getHeight() - 100;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.parseColor("#02C39A"));
        paint.setShadowLayer(4.0f, 0.0f, 2.0f, 0xFF000000);
        paint.setStrokeWidth(6);

        helperStickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        helperStickPaint.setStyle(Paint.Style.STROKE);
        helperStickPaint.setColor(Color.parseColor("#ff9f1c"));
        helperStickPaint.setStrokeWidth(2);
        helperStickPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

        helperCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        helperCirclePaint.setStyle(Paint.Style.FILL);
        helperCirclePaint.setColor(Color.parseColor("#02C39A"));
        helperCirclePaint.setAlpha(50);
        configured = true;
    }

    @Override
    public void draw(Canvas c) {
        if (!configured) {
            configure(c);
        }

        end.x = pos.x + (float) (length * Math.cos(angle));
        end.y = pos.y - (float) (length * Math.sin(angle));
        helperStickEnd.x = pos.x + (float) (helperStickLength * Math.cos(angle));
        helperStickEnd.y = pos.y - (float) (helperStickLength * Math.sin(angle));

        c.drawLine(pos.x, pos.y, helperStickEnd.x, helperStickEnd.y, helperStickPaint);
        c.drawCircle(pos.x, pos.y, length, helperCirclePaint);
        c.drawLine(pos.x, pos.y, end.x, end.y, paint);
        c.drawCircle(pos.x, pos.y, 10, paint);
    }

    public void adaptToPitch(float pitch) {
        if (pitch < 600) {
            length = length <= minLength ? length : length * dampFactor;
        } else {
            length = (int) Math.floor(pitch / 4);
        }
    }

    public boolean collidesWith(EnemyCircle e) {
        PointF stickStart = new PointF(pos.x, pos.y);
        PointF stickEnd = new PointF(end.x, end.y);
        PointF enemyCenter = new PointF(e.pos.x, e.pos.y);

        PointF closestToCenter = closestPointOnLine(stickStart.x, stickStart.y, stickEnd.x, stickEnd.y, enemyCenter.x, enemyCenter.y);

        float perpDist = distance(enemyCenter, closestToCenter);
        if (perpDist < e.radius) { //line will go thru cicle
            if (is_between(stickStart, closestToCenter, stickEnd)) {
                return true; //line is thru the enemy center
            } else if (distance(stickEnd, enemyCenter) < e.radius) {
                return true; //single poke. line end hasn't crossed the enemies center though
            } else {
                return false;
            }
        }

        return false;
    }

    private float distance(PointF a, PointF b) {
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private boolean is_between(PointF a, PointF c, PointF b) {
        return distance(a, c) + distance(c, b) == distance(a, b);
    }

    PointF closestPointOnLine(float lx1, float ly1, float lx2, float ly2, float x0, float y0) {
        float A1 = ly2 - ly1;
        float B1 = lx1 - lx2;
        double C1 = (ly2 - ly1) * lx1 + (lx1 - lx2) * ly1;
        double C2 = -B1 * x0 + A1 * y0;
        double det = A1 * A1 - -B1 * B1;
        double cx = 0;
        double cy = 0;
        if (det != 0) {
            cx = (float) ((A1 * C1 - B1 * C2) / det);
            cy = (float) ((A1 * C2 - -B1 * C1) / det);
        } else {
            cx = x0;
            cy = y0;
        }
        return new PointF((float) cx, (float) cy);
    }
}