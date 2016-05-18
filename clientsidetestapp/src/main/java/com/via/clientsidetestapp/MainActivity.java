package com.via.clientsidetestapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.via.p2p.DefaultSetting;
import com.via.p2pclienthelper.P2PClientHelper;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    P2PClientHelper p2PClientHelper = null;
    Button startBtn = null;
    Button stopBtn = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            p2PClientHelper = new P2PClientHelper(this);
            startBtn = (Button) findViewById(R.id.start);
            stopBtn = (Button) findViewById(R.id.stop);

            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    start();
                }
            });


            stopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stop();
                }
            });

        } catch (Exception e){

        }

    }


    void start() {
        try {
            if(p2PClientHelper==null) {
                p2PClientHelper = new P2PClientHelper(this);
            }

            p2PClientHelper.setUsername(DefaultSetting.sourcePeerUsername);
            p2PClientHelper.prepare();
            p2PClientHelper.start();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    void stop() {
        if(p2PClientHelper !=null) {
            p2PClientHelper.release();
            p2PClientHelper = null;
        }
    }

}
