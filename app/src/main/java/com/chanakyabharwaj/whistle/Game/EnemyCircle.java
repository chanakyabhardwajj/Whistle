package com.chanakyabharwaj.whistle.Game;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class EnemyCircle extends Circle {
    public static int minRadius = 20;
    public static long movementInterval = 1000;
    private long lastMovedTime = 0;
    public static int spawnInterval = 2000;
    private static int moveDelta = 15;
    public boolean killed = false;
    public long killedAt;
    public int timeToRemoval = 300;

    public static void levelUpdatedTo(int gameLevel) {
        if (gameLevel == 0) {
            movementInterval = 1000;
            spawnInterval = 1000;
            moveDelta = 15;
            minRadius = 20;
            return;
        }
        movementInterval = movementInterval > 300 ? movementInterval - (50 * gameLevel) : movementInterval;
        spawnInterval = spawnInterval > 300 ? spawnInterval - (50 * gameLevel) : spawnInterval;
        moveDelta = moveDelta < 50 ? moveDelta + (2 * gameLevel) : moveDelta;
        minRadius = minRadius > 10 ? minRadius - gameLevel : minRadius;
    }

    private Paint helperCirclePaint;

    public EnemyCircle(float x, float r) {
        pos.x = x;
        pos.y = 10;
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

    @Override
    public void draw(Canvas c) {
        long now = System.currentTimeMillis();
        if (now - lastMovedTime > movementInterval) {
            pos.y += moveDelta;
            lastMovedTime = now;
        }
        c.drawCircle(pos.x, pos.y, radius * 2f, helperCirclePaint);
        c.drawCircle(pos.x, pos.y, radius, paint);
    }

    public void kill() {
        if (killed) {
            return;
        }

        paint.setAlpha(0);
        helperCirclePaint.setColor(Color.parseColor("#ffcc01"));
        helperCirclePaint.setAlpha(80);
        killed = true;
        killedAt = System.currentTimeMillis();
    }
}
