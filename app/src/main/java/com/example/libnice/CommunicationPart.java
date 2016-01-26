package com.example.libnice;


import com.via.libnice;
public class CommunicationPart implements libnice.ReceiveCallback {
	int COMMUNICATION_COMPONENT_ID = -1;
	MainActivity mAct = null;
	libnice mNice = null;
	String loggingMessage = "";

	public CommunicationPart(MainActivity ma,libnice nice,int compId) {
		mAct = ma;
		mNice = nice;
		COMMUNICATION_COMPONENT_ID = compId;
	}

	public void onMessage(byte[] buf) {
		String tmp = new String(buf);
		loggingMessage += tmp + "\n";
		if(tmp.startsWith("VIDEOSTART")) {
			
		}
	}

	public void sendMessage(String msg) {
		mNice.sendMsg(msg, COMMUNICATION_COMPONENT_ID);
	}
}
