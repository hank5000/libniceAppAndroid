package com.via.p2p;


import android.util.Log;

public class CommunicationPart implements libnice.ReceiveCallback {
	int COMMUNICATION_COMPONENT_ID = -1;
	libnice mNice = null;
	String loggingMessage = "";

	public CommunicationPart(libnice nice,int compId) {
		mNice = nice;
		COMMUNICATION_COMPONENT_ID = compId;
	}

	public void onMessage(byte[] buf) {
		String tmp = new String(buf);
		Log.d("Hank", tmp);

		loggingMessage += tmp + "\n";
	}

	public void sendMessage(String msg) {
		mNice.sendMsg(msg, COMMUNICATION_COMPONENT_ID);
	}
}
