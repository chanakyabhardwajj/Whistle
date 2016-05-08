package com.chanakyabharwaj.whistle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.chanakyabharwaj.whistle.Game.EnemyCircle;
import com.chanakyabharwaj.whistle.Game.WhistleStick;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;


public class WhistleView extends SurfaceView implements Runnable {
    //Dimensions for game
    static float BOUNDARY = -1;
    float screenWidth;
    float screenHeight;
    volatile float pitch;
    double lastX = 0;

    //Game state
    final ArrayList<EnemyCircle> enemies;
    int totalEnemies = 0;
    int killedEnemies = 0;
    boolean gameOver = false;
    static boolean isGamePaused;
    private SharedPreferences highScoresFile;
    private int lastHighScore;

    //Game controls
    volatile boolean running = false;
    private int gameLevel = -1;
    private int enemiesPerLevel = 10;
    SurfaceHolder holder;
    Thread renderThread = null;
    WhistleStick stick;
    Vibrator vibrator;
    private AudioDispatcher dispatcher;
    private TimerTask enemyTaskTimer;


    public WhistleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        stick = new WhistleStick();
        enemies = new ArrayList<>();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        highScoresFile = PreferenceManager.getDefaultSharedPreferences(context);
        lastHighScore = highScoresFile.getInt("highscore", 0);
    }

    public void resume() {
        if (!isGamePaused) { //new game
            Log.v(">>>>>>>", "starting new game");
            gameLevel = -1;
            incrementGameLevel();
        } else {
            isGamePaused = false;
            Log.v(">>>>>>>", "resuming old game");
        }
        startAudio();
        addEnemyCircles();
        running = true;
        renderThread = new Thread(this);
        renderThread.start();
    }

    public void pause() {
        if (isGamePaused || gameOver) {
            Log.v(">>>>>>>", "in PAUSE() mehtod. game already paused. or game over.");
            return;
        } else {
            Log.v(">>>>>>>", "in PAUSE() mehtod. pausing the game now.");
        }
        stopAudio();
        enemyTaskTimer.cancel();
        running = false;
        boolean retry = true;
        while (retry) {
            try {
                renderThread.join();
                retry = false;
            } catch (InterruptedException e) {
                //retry
            }
        }
    }

    public void togglePause() {
        if (gameOver) {
            return;
        }

        if (isGamePaused) {
            resume();
            isGamePaused = false;
            Toast.makeText(getContext(), "Play", Toast.LENGTH_SHORT).show();
        } else {
            pause();
            isGamePaused = true;
            Toast.makeText(getContext(), "Paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void startAudio() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                pitch = pitchDetectionResult.getPitch();
                stick.adaptToPitch(pitch);
            }
        }));

        new Thread(dispatcher, "Audio Dispatcher").start();
    }

    private void stopAudio() {
        if (dispatcher != null) {
            dispatcher.stop();
        }
    }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid())
                continue; //wait till it becomes valid

            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(getResources().getColor(R.color.background));

            if (gameOver) {
                drawGameOver(canvas);
            } else {
                drawBoundary(canvas);
                stick.draw(canvas);

                synchronized (enemies) {
                    for (Iterator<EnemyCircle> iterator = enemies.iterator(); iterator.hasNext(); ) {
                        EnemyCircle e = iterator.next();

                        //Check if game is over!
                        if (e.pos.y > BOUNDARY) {
                            finishGame(canvas);
                            break;
                        }

                        //Remove previously killed enemies or draw them appropriately!
                        if (e.killed && (System.currentTimeMillis() - e.killedAt > e.timeToRemoval)) {
                            iterator.remove();
                        } else {
                            e.draw(canvas);
                        }

                        //Find new collisions/kills!
                        if (stick.collidesWith(e)) {
                            if (!e.killed) {
                                e.kill();
                                vibrator.vibrate(100);
                                killedEnemies++;
                                if (killedEnemies > 0 && killedEnemies % enemiesPerLevel == 0) {
                                    incrementGameLevel();
                                }
                            }
                        }
                    }
                }
                drawScore(canvas);
                drawLevel(canvas);
                drawStats(canvas);
            }
            holder.unlockCanvasAndPost(canvas);
        }
    }

    public void incrementGameLevel() {
        gameLevel++;
        EnemyCircle.levelUpdatedTo(gameLevel);
    }

    public void addEnemyCircles() {
        enemyTaskTimer = new java.util.TimerTask() {
            @Override
            public void run() {
                synchronized (enemies) {
                    float radius = EnemyCircle.minRadius / 2 + (20 * new Random().nextFloat());
                    enemies.add(new EnemyCircle((float) screenWidth * new Random().nextFloat(), radius));
                    totalEnemies++;
                }
            }
        };
        new java.util.Timer().scheduleAtFixedRate(enemyTaskTimer, 0, EnemyCircle.spawnInterval);
    }

    private void drawScore(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#ffe066"));
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Score : " + killedEnemies, screenWidth - 20, 40, paint);
    }

    private void drawLevel(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#ffe066"));
        paint.setTextSize(30);
        canvas.drawText("Level : " + gameLevel, 20, 40, paint);
    }

    private void drawStats(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#ffe066"));
        paint.setTextSize(30);
        canvas.drawText("Speed : " + EnemyCircle.movementInterval, 20, 140, paint);
    }

    private void drawBoundary(Canvas canvas) {
        if (BOUNDARY == -1) {
            BOUNDARY = canvas.getHeight() - 100;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.parseColor("#ffe066"));
        canvas.drawLine(0, BOUNDARY, screenWidth, BOUNDARY, paint);
    }

    private void drawGameOver(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#ffffff"));
        paint.setTextSize(50);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawColor(getResources().getColor(R.color.background));

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

        canvas.drawText("GAME OVER", xPos, yPos, paint);

        Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setStyle(Paint.Style.FILL);
        scorePaint.setColor(Color.parseColor("#ffe066"));
        scorePaint.setTextSize(30);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        int xPos2 = (canvas.getWidth() / 2);
        int yPos2 = yPos + 40;

        canvas.drawText("Score - " + killedEnemies, xPos2, yPos2, scorePaint);
        canvas.drawText("Highest score - " + lastHighScore, xPos2, yPos2 + 40, scorePaint);
    }

    private void finishGame(Canvas canvas) {
        gameOver = true;
        isGamePaused = false;
        vibrator.vibrate(300);
        enemyTaskTimer.cancel();
        synchronized (enemies) {
            enemies.clear();
        }

        if (killedEnemies > lastHighScore) {
            SharedPreferences.Editor editor = highScoresFile.edit();
            editor.putInt("highscore", killedEnemies);
            editor.apply();
            lastHighScore = killedEnemies;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                double xDelta = event.getX() - lastX;
                double angleDelta = (xDelta * Math.PI) / (2 * screenWidth);
                stick.angle -= angleDelta;
                lastX = event.getX();
                break;

            case MotionEvent.ACTION_UP:
                lastX = 0;
                break;
        }
        return true;
    }
}

