package com.chanakyabharwaj.whistle;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final int AUDIO_RECORD_PERM_CODE = 1;

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

    @Override
    protected void onResume() {
        super.onResume();
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            beginGame();
        } else {
            requestPermission();
        }
    }

    private void requestPermission() {
        final String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, permissions, AUDIO_RECORD_PERM_CODE);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        AUDIO_RECORD_PERM_CODE);
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AUDIO_RECORD_PERM_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    beginGame();
                } else {
                    Toast.makeText(this, R.string.permission_denied_msg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void beginGame() {
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

    public void saveHighScore(int score) {
        int lastHighScore = mPrefs.getInt(gameHighScoreKey, gameHighScoreDefault);
        if (score > lastHighScore) {
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putInt(gameHighScoreKey, score);
            prefsEditor.commit();
        }
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
}

