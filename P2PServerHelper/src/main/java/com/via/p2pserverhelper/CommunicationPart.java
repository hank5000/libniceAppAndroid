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

	public int sendMessage(String msg) {
		return mNice.sendMsg(msg, COMMUNICATION_COMPONENT_ID);
	}

	public int sendData(ByteBuffer bb,int length) {
		return mNice.sendDataDirect(bb,length,COMMUNICATION_COMPONENT_ID);
	}

	private int splitSize = 1024;

	public void sendDataReliable(ByteBuffer buf, int size) {
		int sended = 0;
		int split = 0;
		while (sended < size) {
			if ((size - sended) >= splitSize) {
				split = splitSize;
			} else {
				split = size - sended;
			}
			int limit = buf.limit();
			buf.position(sended);
			buf.limit(sended + split);
			int sendByte = sendData(buf.slice(), size);

			if(sendByte==-1) {
				split = 0;
			} else {
				split = sendByte;
			}

			sended += split;
			buf.limit(limit);
		}
	}

    public boolean sendMessageReliable(String msg,long timeout) {
        long startTime = System.currentTimeMillis();
        while(-1==sendMessage(msg)) {
            //until != -1
            if((System.currentTimeMillis()-startTime)>timeout) {
                return false;
            }
        }
        return true;
    }

    public void sendDataReliableWithTimeout(final ByteBuffer buf,final int size, long timeout) {
        SendThreadReliable sendThreadReliable = new SendThreadReliable(buf,size,timeout);
        sendThreadReliable.start();
        try {
            sendThreadReliable.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class SendThreadReliable extends Thread {
        long timeout = 5000;
        long startTime = 0;
        ByteBuffer buf = null;
        int size = 0;
        SendThreadReliable(ByteBuffer bb,int s, long timeoutMs) {
            timeout = timeoutMs;
            buf = bb;
            size = s;
        }
        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            int sended = 0;
            int split = 0;
            while ( sended < size ) {
                if((System.currentTimeMillis()-startTime)>timeout) {
                    break;
                }

                if ((size - sended) >= splitSize) {
                    split = splitSize;
                } else {
                    split = size - sended;
                }
                int limit = buf.limit();
                buf.position(sended);
                buf.limit(sended + split);
                int sendByte = sendData(buf.slice(), size);

                if(sendByte==-1) {
                    split = 0;
                } else {
                    split = sendByte;
                }
                sended += split;
                buf.limit(limit);
            }
        }
    }


}
