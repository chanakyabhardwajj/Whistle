package com.chanakyabharwaj.whistle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final int AUDIO_RECORD_PERM_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        Button startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            fireIntent();
        } else {
            requestPermission();
        }
    }

    private void requestPermission() {
        final String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
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
                    fireIntent();
                } else {
                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Oops!")
                            .setMessage(R.string.permission_denied_msg)
                            .setPositiveButton("Ok", listener)
                            .show();
                }
            }
        }
    }

    private void fireIntent() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
