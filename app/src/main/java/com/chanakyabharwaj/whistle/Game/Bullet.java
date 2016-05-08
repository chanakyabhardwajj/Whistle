package com.chanakyabharwaj.whistle.Game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;


public class Bullet extends Circle {
    private long lastUpdate = 0;
    private PointF velocity = new PointF();

    public Bullet(PointF position, PointF vel) {
        pos = position;
        velocity = vel;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#ffffff"));
        paint.setShadowLayer(4.0f, 0.0f, 2.0f, 0xFF000000);
    }

    @Override
    public void draw(Canvas c) {
        long now = System.currentTimeMillis();
        if (lastUpdate == 0) {
            lastUpdate = now;
            return;
        }

        pos.x += velocity.x * (now - lastUpdate);
        pos.y += velocity.y * (now - lastUpdate);

        lastUpdate = now;


        c.drawCircle(pos.x, pos.y, 20, paint);
    }
}
