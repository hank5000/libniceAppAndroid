package com.via.serverside;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.via.p2p.DefaultSetting;
import com.via.p2pserverhelper.P2PServerHelper;

import java.net.URISyntaxException;

public class MainActivity extends Activity {

    P2PServerHelper p2PServerHelper = null;
    P2PServerHelper p2pThread2 = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = (Button)findViewById(R.id.start_p2p);
        Button stopBtn  = (Button)findViewById(R.id.stop_p2p);
        Button resetBtn  = (Button)findViewById(R.id.reset_p2p);



        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p2PServerHelper.reset();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
            }
        });
    }

    void start() {
        if(p2PServerHelper ==null) {
            try {
                p2PServerHelper = new P2PServerHelper(this);
                p2PServerHelper.setUsername(DefaultSetting.sourcePeerUsername);
                p2PServerHelper.prepare();
                p2PServerHelper.start();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

    }

    void stop() {
        if(p2PServerHelper !=null) {
            p2PServerHelper.release();
            p2PServerHelper = null;
        }
    }






}
