package com.via.p2pclienthelper;

import android.content.Context;
import android.util.Log;
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
    CommunicationPart[] cps = new CommunicationPart[5];

    private String username = null;
    private String password = null;

    public void release() {
        mNice.release();
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
        mNice = new libnice();
        initNice(mNice);
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
        int useReliable = 0;
        nice.createAgent(useReliable);
        nice.setStunAddress(DefaultSetting.stunServerIp[1], DefaultSetting.stunServerPort[1]);
		/*
			0 => controlling
			1 => controlled
		 */
        int controllMode = 0;
        nice.setControllingMode(controllMode);
        String streamName = "P2PStream";
        int numberOfComponent = 5;
        // TODO: return stream id
		/*
			ret = 0 => Fail.
				= 1 => Success.
				= 2 => It has been added.
		 */
        if(nice.addStream(streamName,numberOfComponent)!=1) {
            return false;
        }

//         register a receive Observer to get byte array from jni side to java side.
        for(int compIndex=0;compIndex<5;compIndex++) {
            cps[compIndex] = new CommunicationPart(nice, compIndex);
            nice.registerReceiveCallback(cps[compIndex],compIndex);
        }

        // register a state Observer to catch stream/component state change
        nice.registerStateObserver(new NiceStateObserver(nice));
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
            mSocket = IO.socket(DefaultSetting.serverUrl);
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
        if(mNice!=null) {
            mNice = null;
        }
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

    public class NiceStateObserver implements libnice.StateObserver {
        private libnice mNice;

        public NiceStateObserver(libnice nice) {
            mNice = nice;
        }
        public void cbComponentStateChanged(final int stream_id, final int component_id,
                                            final int state) {
            Log.d("cbComponentStateChanged","Stream["+stream_id+"]["+component_id+"]:"+libnice.StateObserver.STATE_TABLE[state]);
        }
        public void cbCandidateGatheringDone(int stream_id) {
            localSdp= mNice.getLocalSdp();

            mSocket.emit("add user", username==null?DefaultSetting.sourcePeerUsername:username);
            mSocket.emit("set local sdp",localSdp);
        }
    }

}
