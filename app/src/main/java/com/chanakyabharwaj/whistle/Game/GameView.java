package com.chanakyabharwaj.whistle.Game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.chanakyabharwaj.whistle.R;

import java.util.ArrayList;
import java.util.Iterator;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class GameView extends SurfaceView implements Runnable {
    //Game play
    private final ArrayList<EnemyCircle> enemies;
    private int gameScore = 0;
    private int gameLevel = 0;
    private long levelDuration = 0;
    private long lastRenderStamp;
    private long levelRunningFor = 0;

    //Game state
    private boolean isGameOver = false;
    private boolean isGamePaused;
    private GameState pausedGameState = null;
    public int lastHighScore;

    //Dimensions for game
    private float canvasWidth = 0;
    private float canvasHeight = 0;
    private float canvasMinX;
    private float canvasMinY;
    private float canvasMaxX;
    private float canvasMaxY;
    private int topMargin = 40;
    private int leftMargin = 40;

    private volatile float pitch;
    private double lastX = 0;

    //Game rendering
    private volatile boolean running = false;
    private SurfaceHolder holder;
    private Thread renderThread = null;
    private WhistleStick stick;
    private Vibrator vibrator;
    private AudioDispatcher dispatcher;

    private OnGameOverListener gameOverListener;

    public void setOnGameOverListener(OnGameOverListener l) {
        gameOverListener = l;
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        stick = new WhistleStick();
        enemies = new ArrayList<>();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public GameState getGameState() {
        if (isGameOver) {
            return null;
        }
        GameState state = new GameState();
        state.enemiesState = new ArrayList<>();

        for (EnemyCircle e : enemies) {
            state.enemiesState.add(e.getState());
        }
        state.stickState = stick.getState();
        state.gameLevel = gameLevel;
        state.gameScore = gameScore;
        state.levelRunningFor = levelRunningFor;
        return state;
    }

    public void resume(GameState state) {
        restoreGameState(state);
        startGamePipeLine();
    }

    public void pause() {
        if (isGamePaused) {
            return;
        }
        stopGamePipeLine();
    }

    public void restoreGameState(GameState state) {
        isGameOver = false;
        gameLevel = state == null ? 0 : state.gameLevel;
        gameScore = state == null ? 0 : state.gameScore;

        EnemyCircle.levelUpdatedTo(gameLevel);
        if (state == null) {
            addRandomEnemies(gameLevel);
        } else {
            synchronized (enemies) {
                enemies.clear();
                for (EnemyCircleState es : state.enemiesState) {
                    enemies.add(new EnemyCircle(es.x, es.y, es.r));
                }
            }
        }

        levelDuration = 1000 * (gameLevel < 10 ? 20 - gameLevel : 10);
        levelRunningFor = state == null ? 0 : state.levelRunningFor;
        if (state != null) stick.setState(state.stickState);
    }

    private void startLevel(int level) {
        EnemyCircle.levelUpdatedTo(level);
        addRandomEnemies(level);

        levelDuration = 1000 * (gameLevel < 10 ? 20 - gameLevel : 10);
        levelRunningFor = 0;
    }

    private void addRandomEnemies(int level) {
        int numOfEnemies = 2 * (1 + level);
        synchronized (enemies) {
            enemies.clear();
            for (int i = 0; i < numOfEnemies; i++) {
                float x = canvasMinX + (int) (Math.random() * ((canvasMaxX - canvasMinX) + 1));
                float y = canvasMinY + (int) (Math.random() * ((canvasMaxY - canvasMinY) + 1));
                float r = (float) (EnemyCircle.minRadius * (1 + Math.random()));
                enemies.add(new EnemyCircle(x, y, r));
            }
        }
    }

    private void startGamePipeLine() {
        lastRenderStamp = System.currentTimeMillis();
        startAudio();
        running = true;
        renderThread = new Thread(this);
        renderThread.start();
    }

    private void stopGamePipeLine() {
        stopAudio();
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
        if (isGameOver) {
            return;
        }

        if (isGamePaused) {
            resume(pausedGameState);
            isGamePaused = false;
            Toast.makeText(getContext(), "Play", Toast.LENGTH_SHORT).show();
        } else {
            pause();
            isGamePaused = true;
            pausedGameState = getGameState();
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

            long now = System.currentTimeMillis();

            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(getResources().getColor(R.color.background));

            if (canvasHeight == 0) {
                canvasHeight = canvas.getHeight();
                canvasWidth = canvas.getWidth();
                canvasMinX = 0.2f * canvasWidth;
                canvasMinY = 0.2f * canvasHeight;
                canvasMaxX = 0.8f * canvasWidth;
                canvasMaxY = 0.8f * canvasHeight;
            }

            if (isGameOver) {
                drawGameOverMessage(canvas);
            } else {
                stick.draw(canvas);

                synchronized (enemies) {
                    for (Iterator<EnemyCircle> iterator = enemies.iterator(); iterator.hasNext(); ) {
                        EnemyCircle e = iterator.next();

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
                                gameScore++;
                            }
                        }
                    }

                    if (enemies.size() == 0) {
                        startLevel(++gameLevel);
                    }
                }
                drawScore(canvas);
                drawLevel(canvas);
                drawLevelTime(canvas);

                levelRunningFor += now - lastRenderStamp;
                lastRenderStamp = now;
                if (levelRunningFor > levelDuration) {
                    synchronized (enemies) {
                        if (enemies.size() > 0) {
                            finishGame();
                        } else {
                            startLevel(++gameLevel);
                        }
                    }
                }
            }
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawMultilineText(String label, int value, int x, int y, Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.white));
        paint.setTextSize(20);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y, paint);

        paint.setColor(getResources().getColor(R.color.yellow));
        paint.setTextSize(30);
        canvas.drawText(value + "", x, y + 50, paint);
    }

    private void drawLevelTime(Canvas canvas) {
        drawMultilineText("Time", Math.round((levelDuration - levelRunningFor) / 1000), (int) canvasWidth / 2, topMargin, canvas);
    }

    private void drawScore(Canvas canvas) {
        drawMultilineText("Score", gameScore, (int) canvasWidth - leftMargin, topMargin, canvas);
    }

    private void drawLevel(Canvas canvas) {
        drawMultilineText("Level", gameLevel, (int) leftMargin, topMargin, canvas);
    }

    private void drawGameOverMessage(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.white));
        paint.setTextSize(50);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawColor(getResources().getColor(R.color.background));

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

        canvas.drawText("GAME OVER", xPos, yPos, paint);

        Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setStyle(Paint.Style.FILL);
        scorePaint.setColor(getResources().getColor(R.color.yellow));
        scorePaint.setTextSize(30);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        int xPos2 = (canvas.getWidth() / 2);
        int yPos2 = yPos + 40;

        canvas.drawText("Score - " + gameScore, xPos2, yPos2, scorePaint);
        canvas.drawText("Highest score - " + lastHighScore, xPos2, yPos2 + 40, scorePaint);
    }

    private void finishGame() {
        isGameOver = true;
        isGamePaused = false;
        vibrator.vibrate(300);

        if (gameScore > lastHighScore) {
            lastHighScore = gameScore;
        }

        if (gameOverListener != null)
            gameOverListener.OnGameOverListener(this, gameScore);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                double xDelta = event.getX() - lastX;
                double angleDelta = ((xDelta * Math.PI)) / (2 * canvasWidth);
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