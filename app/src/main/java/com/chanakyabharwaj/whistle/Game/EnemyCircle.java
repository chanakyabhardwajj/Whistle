package com.chanakyabharwaj.whistle.Game;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.chanakyabharwaj.whistle.R;


class EnemyCircleState {
    public float x;
    public float y;
    public float r;

    public EnemyCircleState(float _x, float _y, float _r) {
        x = _x;
        y = _y;
        r = _r;
    }
}

public class EnemyCircle extends Circle {
    //Defaults
    static int minRadius = 20;
    static long movementInterval = 1000;
    static int spawnInterval = 2000;

    boolean killed = false;
    long killedAt;
    int timeToRemoval = 300;

    public static void levelUpdatedTo(int gameLevel) {
        if (gameLevel == 0) {
            movementInterval = 1000;
            spawnInterval = 1000;
            minRadius = 20;
            return;
        }
        movementInterval = movementInterval > 300 ? movementInterval - (50 * gameLevel) : movementInterval;
        spawnInterval = spawnInterval > 300 ? spawnInterval - (50 * gameLevel) : spawnInterval;
        minRadius = minRadius > 10 ? minRadius - gameLevel : minRadius;
    }

    private Paint helperCirclePaint;

    public EnemyCircle(float x, float y, float r) {
        pos.x = x;
        pos.y = y;
        radius = r;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#E71D36"));
        paint.setShadowLayer(4.0f, 0.0f, 2.0f, 0xFF000000);

        helperCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        helperCirclePaint.setStyle(Paint.Style.FILL);
        helperCirclePaint.setColor(Color.parseColor("#E71D36"));
        helperCirclePaint.setAlpha(50);
    }

    public EnemyCircleState getState() {
        return new EnemyCircleState(pos.x, pos.y, radius);
    }

    @Override
    public void draw(Canvas c) {
        c.drawCircle(pos.x, pos.y, radius * 2f, helperCirclePaint);
        c.drawCircle(pos.x, pos.y, radius, paint);
    }

    public void kill() {
        if (killed) {
            return;
        }

        paint.setAlpha(0);
        helperCirclePaint.setColor(Color.parseColor("#E71D36"));
        helperCirclePaint.setAlpha(80);
        killed = true;
        killedAt = System.currentTimeMillis();
    }
}
