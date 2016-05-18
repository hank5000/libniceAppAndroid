package com.via.serverclienttester;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.via.p2p.DefaultSetting;
import com.via.p2pclienthelper.P2PClientHelper;
import com.via.p2pserverhelper.P2PServerHelper;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    P2PClientHelper client = null;
    P2PServerHelper server = null;
    Context c = this;

    Button start_s = null;
    Button stop_s = null;
    Button start_c = null;
    Button stop_c = null;
    Button pair_c = null;
    EditText editText_s = null;
    EditText editText_c = null;

    boolean bLocalPairMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start_s = (Button) findViewById(R.id.start_s);
        stop_s = (Button) findViewById(R.id.stop_s);
        start_c = (Button) findViewById(R.id.start_c);
        stop_c = (Button) findViewById(R.id.stop_c);
        pair_c = (Button) findViewById(R.id.pair_c);

        editText_s = (EditText) findViewById(R.id.editText_s);
        editText_c = (EditText) findViewById(R.id.editText_c);


        editText_s.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String s = editText_s.getText().toString();
                    server.sendMessage(s);
                    // Perform action on key press
                    return true;
                }
                return false;
            }
        });

        editText_c.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String s = editText_s.getText().toString();
                    //client.sendMessage(s);
                    // Perform action on key press
                    return true;
                }
                return false;
            }
        });


        start_s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(server==null) server = new P2PServerHelper(null);
                    server.setUsername(DefaultSetting.sourcePeerUsername);
                    server.prepare();
                    server.start();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });

        stop_s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(server!=null) server.release();
                server = null;
            }
        });

        start_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(client==null) client = new P2PClientHelper(null);
                    client.setUsername(DefaultSetting.sourcePeerUsername);
                    client.prepare();
                    client.start();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });

        stop_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(client!=null) client.release();
                client = null;
            }
        });

        pair_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(client!=null) {
                    if(client.isReadyToPairing()) {
                        if(bLocalPairMode) {
                            String sdp_s = server.getLocalSdp();
                            String sdp_c = client.getLocalSdp();
                            server.setSDP(sdp_c);
                            client.setSDP(sdp_s);
                        } else {
                            client.pairing();
                        }

                    }
                }
            }
        });


    }
}
