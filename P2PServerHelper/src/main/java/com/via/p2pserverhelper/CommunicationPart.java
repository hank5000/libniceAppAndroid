package com.via.p2pserverhelper;


import android.util.Log;

import com.via.p2p.libnice;

import java.nio.ByteBuffer;

public class CommunicationPart implements libnice.ComponentListener {
	int COMMUNICATION_COMPONENT_ID = -1;
	libnice mNice = null;
	String loggingMessage = "";

	public CommunicationPart(libnice nice,int compId) {
		mNice = nice;
		COMMUNICATION_COMPONENT_ID = compId;
	}

	public void onMessage(byte[] buf) {
		String tmp = new String(buf);
		Log.d("p2pServerHelper", "onMessage:"+tmp);

		loggingMessage += tmp + "\n";
	}

	public void sendMessage(String msg) {
		mNice.sendMsg(msg, COMMUNICATION_COMPONENT_ID);
	}

	public void sendData(byte[] bytes) {
		mNice.sendData(bytes,bytes.length,COMMUNICATION_COMPONENT_ID);
	}

	public void sendData(ByteBuffer bb,int lenght) {
		mNice.sendDataDirect(bb,lenght,COMMUNICATION_COMPONENT_ID);
	}
}
