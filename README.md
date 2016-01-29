# libniceAppAndroid (Android Studio)

libniceAppAndroid is using for testing libnice JNI development.


## How to use libnice.java + libnice.so (it is neccessary copy with libgstream_android.so + gstreamer.java...)
libnice nice = new libnice();
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
nice.setStunAddress(STUN_IP, STUN_PORT);
/*
	0 => controlling
	1 => controlled
 */
int controllMode = 0;
nice.setControllingMode(controllMode);
String streamName = "P2PStream";
int numberOfComponent = 1;
// TODO: return stream id
/*
	ret = 0 => Fail.
		= 1 => Success.
		= 2 => It has been added.
 */
if(nice.addStream(streamName,numberOfComponent)!=1) {
	return false;
}

// register a receive Observer to get byte array from jni side to java side.
int forComponentIndex = 1;
nice.registerReceiveCallback(new libnice.ReceiveCallback(),forComponentIndex);


// register a state Observer to catch stream/component state change
nice.registerStateObserver(new NiceStateObserver(nice,i));

// TODO: add stream id, each stream has self SDP.
if(nice.gatheringCandidate()==1) {
	showToast("gathering Candidate Success, please wait gathering done then getLocalSDP");
} else {
	showToast("gathering Candidate fail");
	return false;
}
...

you can see detail in MainActivity.
