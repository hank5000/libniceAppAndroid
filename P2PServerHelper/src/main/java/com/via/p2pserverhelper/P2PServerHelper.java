package com.via.p2pserverhelper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by HankWu_Office on 2015/11/26.
 */
public class P2PServerHelper extends Thread {
    libnice mNice;
    String TAG = "P2PServerHelper";
    String remoteSdp = "";
    String localSdp = "";
    Context application_ctx = null;
    private Socket mSocket;
    String STUN_IP 	= "74.125.204.127";
    int    STUN_PORT= 19302;
    CommunicationPart cp1 = null;

    public void release() {
        mNice.release();
    }

    public P2PServerHelper(Context ctx) throws URISyntaxException {
        try {
            application_ctx = ctx;
            setSocketIOAndConnect();
            mNice = new libnice();
            initNice(mNice);
        } catch (URISyntaxException e) {
            throw e;
        }
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
        int forComponentIndex = 1;
        nice.registerReceiveCallback(new CommunicationPart(nice, forComponentIndex),forComponentIndex);
        forComponentIndex = 2;
        nice.registerReceiveCallback(new CommunicationPart(nice, forComponentIndex),forComponentIndex);
        forComponentIndex = 3;
        nice.registerReceiveCallback(new CommunicationPart(nice, forComponentIndex),forComponentIndex);
        forComponentIndex = 4;
        nice.registerReceiveCallback(new CommunicationPart(nice, forComponentIndex),forComponentIndex);
        forComponentIndex = 5;
        cp1 = new CommunicationPart(nice, forComponentIndex);
        nice.registerReceiveCallback(cp1, forComponentIndex);

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

//            if((new String(Base64.decode(localSdp,Base64.DEFAULT))).contains("220")) {
//                //showToast("D","The stun server is alive!!!");
//                //Log.d("D","The stun server is alive!!!");
//
//            }

            mSocket.emit("add user", DefaultSetting.sourcePeerUsername);
            mSocket.emit("set local sdp",localSdp);
        }
    }




//    public void createLocalVideoSendingThread(int Stream_id, int onChannel, String path) {
//        showToast("D","create Local Video Sending Thread");
//
//        if (sendingThreads[onChannel - 1] == null) {


//            showToast("D","create Sending Thread");
//            Log.d("hank", "Create sending Thread");
//            sendingThreads[onChannel - 1] = new SendingLocalVideoThread(mNice, Stream_id, onChannel, path);
//            sendingThreads[onChannel - 1].start();
//        }
//    }
//
//    public void createLiveViewSendingThread(int Stream_id, int onChannel, String ip) {
//        if (sendingLiveThreads[onChannel - 1] == null) {
//            showToast("D","create live view thread");
//            sendingLiveThreads[onChannel - 1] = new SendingLiveViewThread(mNice, Stream_id, onChannel, ip);
//            sendingLiveThreads[onChannel - 1].start();
//        }
//    }
//
//    public void stopSendingThread(int Stream_id, int onChannel) {
//        if (sendingThreads[onChannel - 1] != null) {
//            sendingThreads[onChannel - 1].setStop();
//            sendingThreads[onChannel - 1].interrupt();
//            try {
//                sendingThreads[onChannel - 1].join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            sendingThreads[onChannel - 1] = null;
//            showToast("D","Stop sending Thread " + onChannel);
//
//        }
//
//        if (sendingLiveThreads[onChannel - 1] != null) {
//            sendingLiveThreads[onChannel - 1].setStop();
//            sendingLiveThreads[onChannel - 1].interrupt();
//            try {
//                sendingLiveThreads[onChannel - 1].join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            sendingLiveThreads[onChannel - 1] = null;
//            showToast("D","Stop sending Thread " + onChannel);
//        }
//    }
}
