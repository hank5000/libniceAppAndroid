package com.via.p2p;
import java.nio.ByteBuffer;

public class libnice {
	static {
		System.loadLibrary("gstreamer_android");
		System.loadLibrary("nice4android");
	}
	/*
		get agentCtx and gloop handle
	 */
	private long agentCtxHandle = 0;
	private long gloopLong = 0;
	/*
		it will get one stream id at one class
	 */
	private int  streamId = -1;
	private String localSdp = "";
	/*
		One NiceAgent will get one stream id currently
		because i don't know what scenario will use a lot stream
	 */
	private native long initNative();
	private native long /*agent handle*/ createAgentNative(long gloop,int useReliable);
	private native int setStunAddressNative(long agentHandle,String stun_ip,int stun_port);
	private native int setControllingModeNative(long agentHandle,int controllingMode);
	private native int /*stream id*/ addStreamNative(long agentHandle,String streamName, int numberOfComponent); // return stream id which is signed by libnice

	private native int /*0: fail 1:success*/ gatheringCandidateNative(long agentHandle,int stream_id);
	/*
		it need to wait for gathering complete and then call getLocalSdpNative
	 */
	private native String /*sdp which is encoded by base 64*/ getLocalSdpNative(long agentHandle,int stream_id);
	private native int setRemoteSdpNative(long agentHandle,String jremoteSdp,long Size);
	private native int sendMsgNative(long agentHandle,String data,int streamId,int compId);
	private native int sendDataNative(long agentHandle,byte[] data,int len ,int streamId,int compId);
	private native int sendDataDirectNative(long agentHandle,ByteBuffer data, int len ,int streamId,int compId);
	private native int setDirectBufferIndexNative(long agentHandle,ByteBuffer data, int index);
	private native int sendDataDirectByIndexNative(long agentHandle,ByteBuffer data, int len ,int index,int streamId,int compId);
	private native int restartStreamNative(long agentHandle,int streamId);
	
	public int restartStream() {
		localSdp = "";
		return restartStreamNative(agentCtxHandle, streamId);
	}

	public int sendDataDirectByIndex(ByteBuffer data,int len,int index,int streamId,int CompId) {
		return sendDataDirectByIndexNative(agentCtxHandle, data, len, index, streamId, CompId);
	}
	
	public int setDirectBufferIndex(ByteBuffer data,int index) {
		return setDirectBufferIndexNative(agentCtxHandle,data,index);
	}
	
	private native void mainLoopStart(long gloop);
	private native void mainLoopStop(long gloop);
	// register callback function for stream[streamId],component[compId]
	private native void registerReceiveCallbackNative(long agentHandle,libnice.ReceiveCallback recv_cb_obj,int streamId,int compId);
	private native void registerStateObserverNative(long agentHandle,libnice.StateObserver obs);
	private native void destroyAgentNative(long agentHandle);


	Thread mainLoopThread = new Thread(new Runnable(){
		public void run() {
			// Just use it to run gloop
			mainLoopStart(gloopLong);
		}
	});

	public int init() {
		int ret = 0;
		gloopLong = initNative();
		if(gloopLong!=0) {
			mainLoopThread.start();
			ret = 1;
		}
		return ret;
	}

	public void release() {
		//destroyAgentNative(agentCtxHandle);
		mainLoopStop(gloopLong);
	}


	public int createAgent(int useReliable) {
		agentCtxHandle = createAgentNative(gloopLong, useReliable);
		return 1;
	}

	public int setStunAddress(String stun_ip, int stun_port) {
		return setStunAddressNative(agentCtxHandle, stun_ip, stun_port);
	}

	public int setControllingMode(int controllingMode) {
		return setControllingModeNative(agentCtxHandle, controllingMode);
	}

	public int addStream(String streamName, int numberOfComponent) {
		int ret = 0;
		if(streamId<0) {
			streamId = addStreamNative(agentCtxHandle, streamName, numberOfComponent);
			if(streamId>0) {
				ret = 1;
			} else {
				ret = 0;
			}
		} else {
			ret = 2;
		}
		return ret;
	}

	public String getLocalSdp() {
		localSdp = getLocalSdpNative(agentCtxHandle, streamId);
		return localSdp;
	}

	public int gatheringCandidate() {
		return gatheringCandidateNative(agentCtxHandle, streamId);
	}

	public void setRemoteSdp(String remoteSdp) {
		setRemoteSdpNative(agentCtxHandle,remoteSdp,remoteSdp.length());
	}

	public void sendData(byte[] buf, int len, int compId) {
		sendDataNative(agentCtxHandle,buf,len,streamId,compId);
	}

	public void sendDataDirect(ByteBuffer buf, int len, int compId) {
		sendDataDirectNative(agentCtxHandle,buf,len,streamId,compId);
	}

	public void sendMsg(String msg, int compId) {
		sendMsgNative(agentCtxHandle,msg,streamId,compId);
	}

	public void registerReceiveCallback(libnice.ReceiveCallback obs, int compId) {
		this.registerReceiveCallbackNative(agentCtxHandle,obs,streamId,compId);
	}

	public void registerStateObserver(libnice.StateObserver stateObserver) {
		this.registerStateObserverNative(agentCtxHandle,stateObserver);
	}

	public interface ReceiveObserver{
		void obCallback(byte[] msg);
	}
	
	public interface StateObserver {
		String[] STATE_TABLE = {"disconnected", "gathering", "connecting",
                "connected", "ready", "failed"};
		void cbCandidateGatheringDone(int stream_id);
		void cbComponentStateChanged(int stream_id, int component_id, int state);
	}
	
	public interface ReceiveCallback {
		void onMessage(byte[] buf);
	}
}
