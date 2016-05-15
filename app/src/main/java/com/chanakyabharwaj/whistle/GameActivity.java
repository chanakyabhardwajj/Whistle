package com.chanakyabharwaj.whistle;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.chanakyabharwaj.whistle.Game.GameState;
import com.chanakyabharwaj.whistle.Game.GameView;
import com.chanakyabharwaj.whistle.Game.OnGameOverListener;
import com.google.gson.Gson;

public class GameActivity extends Activity {
    private GameView whistleView;
    SharedPreferences mPrefs;
    String gameStateKey = "game_state";
    String gameStateDefault = "no game saved";
    String gameHighScoreKey = "high_score";
    int gameHighScoreDefault = 0;
    private OnGameOverListener gameOverListener;
    private ImageButton pauseButton;
    private TextView newGameButton;

    public void saveHighScore(int score) {
        int lastHighScore = mPrefs.getInt(gameHighScoreKey, gameHighScoreDefault);
        if (score > lastHighScore) {
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putInt(gameHighScoreKey, score);
            prefsEditor.commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game);

        mPrefs = getPreferences(MODE_PRIVATE);
        whistleView = (GameView) findViewById(R.id.whistle_view);
        pauseButton = (ImageButton) findViewById(R.id.pause_button);
        newGameButton = (TextView) findViewById(R.id.new_game_button);

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whistleView.togglePause();
            }
        });

        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whistleView.pause();
                whistleView.resume(null);
                showPauseButton();
            }
        });

        gameOverListener = new OnGameOverListener() {
            public void OnGameOverListener(GameView v, int score) {
                hidePauseButton();
                saveHighScore(score);
            }
        };
        whistleView.setOnGameOverListener(gameOverListener);
    }

    private void hidePauseButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pauseButton.setVisibility(View.GONE);
            }
        });

    }

    private void showPauseButton() {
        pauseButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Gson gson = new Gson();
        String json = mPrefs.getString(gameStateKey, gameStateDefault);

        whistleView.lastHighScore = mPrefs.getInt(gameHighScoreKey, gameHighScoreDefault);

        if (json.equals(gameStateDefault)) {
            whistleView.resume(null);
        } else {
            GameState gamestate = gson.fromJson(json, GameState.class);
            whistleView.resume(gamestate);

            //Remove the previously stored state
            mPrefs.edit().remove(gameStateKey).commit();
        }
        showPauseButton();
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        GameState state = whistleView.getGameState();
        if (state == null) {
            prefsEditor.putString(gameStateKey, null);
        } else {
            String json = gson.toJson(state);
            prefsEditor.putString(gameStateKey, json);
            saveHighScore(state.gameScore);
        }

        prefsEditor.commit();
        whistleView.pause();
    }
}
