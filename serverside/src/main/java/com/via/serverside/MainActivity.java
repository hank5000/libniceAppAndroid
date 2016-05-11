package com.via.serverside;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.via.p2p.P2PThread;

import java.net.URISyntaxException;

public class MainActivity extends Activity {

    P2PThread p2pThread = null;
    P2PThread p2pThread2 = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = (Button)findViewById(R.id.start_p2p);
        Button stopBtn  = (Button)findViewById(R.id.stop_p2p);
        Button pairBtn  = (Button)findViewById(R.id.pair_p2p);



        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

        pairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(p2pThread!=null && p2pThread2!=null) {
                    String sdp1 = p2pThread.getSDP();
                    String sdp2 = p2pThread2.getSDP();
                    p2pThread.setSDP(sdp2);
                    p2pThread2.setSDP(sdp1);
                }
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
        if(p2pThread==null) {
            try {
                p2pThread = new P2PThread(this);
                p2pThread.start();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if(p2pThread2==null) {
            try {
                p2pThread2 = new P2PThread(this);
                p2pThread2.start();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

    }

    void stop() {
        if(p2pThread!=null) {
            p2pThread.release();
            p2pThread = null;
        }

        if(p2pThread2!=null) {
            p2pThread2.release();
            p2pThread2 = null;
        }
    }






}
