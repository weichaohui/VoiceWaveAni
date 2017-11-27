package com.example.weichaohui.voicewaveani;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    VoiceWaveView mWaveView = null;
    private Handler uiHandler = new Handler();
    private Random mRandom = new Random(System.currentTimeMillis());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWaveView = (VoiceWaveView) findViewById(R.id.wave);

        sendDBInterval();
    }

    private void sendDBInterval(){
        mWaveView.waveOccurd(mRandom.nextInt(21));
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendDBInterval();
            }
        }, 500);
    }
}
