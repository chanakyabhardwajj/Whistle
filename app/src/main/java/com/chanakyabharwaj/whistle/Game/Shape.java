package com.chanakyabharwaj.whistle.Game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

public abstract class Shape {
    public PointF pos = new PointF();
    public Paint paint;
    abstract public void draw(Canvas c);
}
