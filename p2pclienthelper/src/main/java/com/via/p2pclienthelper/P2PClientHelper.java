package com.via.p2pclienthelper;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.via.p2p.DefaultSetting;
import com.via.p2p.libnice;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by HankWu_Office on 2015/11/26.
 */
public class P2PClientHelper extends Thread {
    libnice mNice;
    String TAG = "P2PClientHelper";
    String remoteSdp = "";
    String localSdp = "";
    Context application_ctx = null;
    private Socket mSocket;
    libnice.ComponentListener[] componentListeners = new libnice.ComponentListener[5];
    SurfaceView[] surfaceViews = null;

    private String username = null;
    private String password = null;

    boolean bReadyToPairing = false;
    final int MessageChannelNumber = 4;

    public void setSurfaceViews(SurfaceView[] svs) {
        surfaceViews = svs;

    }

    public void release() {
        if(mSocket!=null) {
            mSocket.disconnect();
            mSocket.close();
            mSocket=null;
        }
        bReadyToPairing = false;
        stopAllProcess();
        if(mNice!=null) {
            mNice.release();
            mNice = null;
        }
    }

    public P2PClientHelper(Context ctx) throws URISyntaxException {
        try {
            application_ctx = ctx;
            setSocketIOAndConnect();
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    public void prepare() {
        bReadyToPairing = false;
        mNice = new libnice();
        if(initNice(mNice)) {
            Log.d("HANK","Init libnice success!!");
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    boolean initNice(libnice nice) {
		/*
			0 => Fail
			1 => Success
		 */
        if(nice.init()==0) {
            return false;
        }
		/*
			If useReliable = 1, the libnice will send the few small packages which is separated by user giving package
						   = 0, the libnice will send the original package.
		 */
        int useReliable = 1;
        nice.createAgent(useReliable);
        nice.setStunAddress(DefaultSetting.stunServerIp[1], DefaultSetting.stunServerPort[1]);
		/*
			0 => controlling
			1 => controlled
		 */
        int controllMode = 0;
        nice.setControllingMode(controllMode);
        String streamName = "P2PStream";
		/*
			ret = 0 => Fail.
				= 1 => Success.
				= 2 => It has been added.
		 */
        if(nice.addStream(streamName)!=1) {
            return false;
        }

//         register a receive Observer to get byte array from jni side to java side.
        int i = 0;
        componentListeners[i] = new VideoRecvCallback(surfaceViews[0]);
        nice.setComponentHandler(libnice.ComponentIndex.Component1,componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaceViews[0]);
        nice.setComponentHandler(libnice.ComponentIndex.Component2,componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaceViews[0]);
        nice.setComponentHandler(libnice.ComponentIndex.Component3,componentListeners[i]);

        i++;
        componentListeners[i] = new VideoRecvCallback(surfaceViews[0]);
        nice.setComponentHandler(libnice.ComponentIndex.Component4,componentListeners[i]);

        i++;
        componentListeners[i] = new CommunicationPart(nice,i+1);
        nice.setComponentHandler(libnice.ComponentIndex.Component5,componentListeners[i]);


        // register a state Observer to catch stream/component state change
        nice.setOnStateChangeListener(new NiceStateObserver(nice));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(nice.gatheringCandidate()==1) {
            showToast("D","gathering Candidate Success, please wait gathering done then getLocalSDP");
        } else {
            showToast("D","gathering Candidate fail");
            return false;
        }

        return true;
    }

    public String getSDP() {
        return localSdp;
    }

    public void setSDP(String sdp) {
        mNice.setRemoteSdp(sdp);
    }

    @Override
    public void run() {
        super.run();
    }

    private void setSocketIOAndConnect() throws URISyntaxException {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew=true;
            mSocket = IO.socket(DefaultSetting.serverUrl,opts);
            mSocket.on("response", onResponse);
            mSocket.on("get sdp", onGetSdp);
            mSocket.on("restart stream", onRestartStream);
            mSocket.connect();
        } catch (URISyntaxException e) {
            showToast("I","Server is offline, please contact your application provider!");
            throw new RuntimeException(e);
        }
    }

    private Emitter.Listener onResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String message;
            try {
                message = data.getString("message");
                showToast("D","onRespone:" + message);

            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onGetSdp = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("P2PClientHelper","onGetSdp!!");
            JSONObject data = (JSONObject) args[0];
            String SDP;
            try {
                remoteSdp = data.getString("SDP");
                mNice.setRemoteSdp(remoteSdp);
                showToast("D","GetSDP:" + remoteSdp);
            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onRestartStream = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            showToast("I","on Restart Stream");
            stopAllProcess();

            mNice.restartStream();
            localSdp = mNice.getLocalSdp();
            mSocket.emit("set local sdp", localSdp);
        }
    };

    private void stopAllProcess() {
        /*
            TODO: Add process killer.
         */

    }

    public void pairing() {
        Log.d("P2PClientHelper","Pairing!!");
        mSocket.emit("get remote sdp",username+":"+localSdp);
    }

    public void triggerSocketio() {
        mSocket.emit("trigger","abcd");
    }

    public void setContext(Context app_ctx) {
        application_ctx = app_ctx;
    }

    public void showToast(String level, final String tmp) {

        if(level.equalsIgnoreCase("D") && DefaultSetting.printLevelD) {
            Log.d(DefaultSetting.WTAG+"/"+TAG, tmp);
            if(application_ctx!=null)
                Toast.makeText(application_ctx, tmp, Toast.LENGTH_SHORT).show();
        }

        if(level.equalsIgnoreCase("I") && DefaultSetting.printLevelI) {
            Log.d(DefaultSetting.WTAG+"/"+TAG, tmp);
            if(application_ctx!=null)
                Toast.makeText(application_ctx, tmp, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isReadyToPairing() {
        return bReadyToPairing;
    }

    public String getLocalSdp() {
        return localSdp;
    }

    public void setRemoteSDP(String sdp) {
        mNice.setRemoteSdp(sdp);
    }

    public class NiceStateObserver implements libnice.OnStateChangeListener {
        private libnice mNice;

        public NiceStateObserver(libnice nice) {
            mNice = nice;
        }

        @Override
        public void candiateGatheringDone() {
            localSdp= mNice.getLocalSdp();
            bReadyToPairing = true;
        }

        @Override
        public void componentStateChanged(int componentId, String stateName) {
            Log.d("componentStateChanged_c","Component["+componentId+"]:"+stateName);
        }

    }

    public void sendMessage(String msg) {
        Log.d("P2PClientHelper","SendMesg:"+msg);
        ((CommunicationPart)componentListeners[MessageChannelNumber]).sendMessage(msg);
    }

}
