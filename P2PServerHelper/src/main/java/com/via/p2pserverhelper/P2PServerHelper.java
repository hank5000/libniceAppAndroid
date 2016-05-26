package com.via.p2pserverhelper;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.via.p2p.DefaultSetting;
import com.via.p2p.libnice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

/**
 * Created by HankWu_Office on 2015/11/26.
 */
public class P2PServerHelper extends Thread {
    libnice mNice;
    String TAG = "P2PServerHelper";
    String remoteSdp = "";
    String localSdp = "";
    Context application_ctx = null;
    private Socket mSocket = null;
    libnice.ComponentListener[] callbacks = new libnice.ComponentListener[5];

    private String username = null;
    private String password = null;

    final int MessageChannelNumber = 4;

    public void release() {
        if(mSocket!=null) {
            mSocket.disconnect();
            mSocket.close();
            mSocket=null;
        }
        if(mNice!=null) {
            mNice.release();
            mNice=null;
        }
    }

    public P2PServerHelper(Context ctx) throws URISyntaxException {
        try {
            application_ctx = ctx;
            setSocketIOAndConnect();
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    public void prepare() {
        mNice = new libnice();
        if(initNice(mNice)) {
            Log.d("HANK","Init libnice success!!");
            mSocket.emit("add user", username);

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
        // TODO: return stream id
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
        callbacks[i] = new CommunicationPart(nice,i+1);
        nice.setComponentHandler(libnice.ComponentIndex.Component1,callbacks[i]);
        i++;
        callbacks[i] = new CommunicationPart(nice,i+1);
        nice.setComponentHandler(libnice.ComponentIndex.Component2,callbacks[i]);
        i++;
        callbacks[i] = new CommunicationPart(nice,i+1);
        nice.setComponentHandler(libnice.ComponentIndex.Component3,callbacks[i]);
        i++;
        callbacks[i] = new CommunicationPart(nice,i+1);
        nice.setComponentHandler(libnice.ComponentIndex.Component4,callbacks[i]);
        i++;
        callbacks[i] = new CommunicationPart(nice,i+1);
        nice.setComponentHandler(libnice.ComponentIndex.Component5,callbacks[i]);


        // register a state Observer to catch stream/component state change
        nice.setOnStateChangeListener(new ServerOnStateChangeListener(nice));
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
            Log.d("P2PServerHelper","onGetSdp!!");
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
            TODO: Kill Process
         */
    }

    public void reset() {
        mNice.restartStream();
        localSdp = "";
        localSdp = mNice.getLocalSdp();
    }

    public String getLocalSdp() {
        return localSdp;
    }

    public void setRemoteSDP(String sdp) {
        mNice.setRemoteSdp(sdp);
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

    public class ServerOnStateChangeListener implements libnice.OnStateChangeListener {
        private libnice mNice;

        public ServerOnStateChangeListener(libnice nice) {
            mNice = nice;
        }

        @Override
        public void candiateGatheringDone() {
            localSdp= mNice.getLocalSdp();
            mSocket.emit("set local sdp",localSdp);
        }

        @Override
        public void componentStateChanged(int componentId, String stateName) {
            Log.d("componentStateChanged_s","Component["+componentId+"]:"+stateName);
        }
    }

    public void sendMessage(String mesg) {
        Log.d("P2PServerHelper","SendMesg:"+mesg);
        ((CommunicationPart)callbacks[MessageChannelNumber]).sendMessage(mesg);
    }

    public static String byteArrayToHexString(byte[] b) {
        int len = b.length;
        String data = new String();

        for (int i = 0; i < len; i++){
            data += Integer.toHexString((b[i] >> 4) & 0xf);
            data += Integer.toHexString(b[i] & 0xf);
        }
        return data;
    }



    int W = 0;
    int H = 0;
    String mime = "";
    String sps = "";
    String pps = "";
    long startTime = 0;
    long firstTime = -1;
    final private int splitSize = 5000;
    boolean bSplitPackageMode = true;
    byte[] sps_b = null;
    byte[] pps_b = null;


    public void sendVideo() {
        final MediaExtractor me = new MediaExtractor();
        firstTime = -1;

        try {
//            me.setDataSource("/mnt/usbdisk/usbdisk2/fo-20000101_080611.mp4");
            me.setDataSource("/sdcard/Download/DemoPTZ.mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < me.getTrackCount(); i++) {
            MediaFormat format = me.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                me.selectTrack(i);
                W = format.getInteger(MediaFormat.KEY_WIDTH);
                H = format.getInteger(MediaFormat.KEY_HEIGHT);
                Log.d("HANK","WxH="+W+"x"+H);

                ByteBuffer sps_bb = format.getByteBuffer("csd-0");
                ByteBuffer pps_bb = format.getByteBuffer("csd-1");

                sps = byteArrayToHexString(sps_bb.array());
                pps = byteArrayToHexString(pps_bb.array());
                break;
            }
        }


        while(-1==((CommunicationPart)callbacks[0]).sendMessage("Video:"+mime+":"+W+":"+H+":"+sps+":"+pps+":")) {
            Log.d("HANK","Sending again!!!!");
        }

        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 1024* 2);
        int size = 0;

        while(true) {
            size = 0;
            buf.clear();
            if(firstTime==-1) {
                startTime = System.currentTimeMillis();
                firstTime = me.getSampleTime()/1000;
            }


            size = me.readSampleData(buf,0);
            if(size>0) {
                if ((System.currentTimeMillis() - startTime) > (me.getSampleTime() / 1000 - firstTime)) {
                    if(isImportantFrame(buf)) {
                        ((CommunicationPart) callbacks[0]).sendDataReliableWithTimeout(buf, size, 9999);
                    } else {
                        ((CommunicationPart) callbacks[0]).sendDataReliableWithTimeout(buf, size, 300);
                    }
                    me.advance();
                }
            } else {
                me.seekTo(0,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
        }
    }

    private int getBit(Byte b,int index) {
        if((b&(1<<index))!=0) {
            return 1;
        } else {
            return 0;
        }
    }

    private boolean isImportantFrame(ByteBuffer b) {
        //IDR SPS PPS
        if((b.get(4) & 0X1F)==5 && (b.get(4) & 0X1F)==7 && (b.get(4) & 0X1F)==8) {
            return true;
        }
        //SLICE => get slice type
        if((b.get(4) & 0X1F)==1) {
//            int j = 5;
//            int zeroCounter = 0;
//            for(int i=0;i<8;i++) {
//                if(getBit(b.get(j), 0)==0) {
//                    zeroCounter++;
//                }
//            }

        }
        return false;
    }

}
