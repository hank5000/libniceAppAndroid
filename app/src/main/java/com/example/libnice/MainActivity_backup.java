//package com.example.libnice;
//
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.media.MediaExtractor;
//import android.media.MediaFormat;
//import android.media.MediaPlayer;
//import android.os.Bundle;
//import android.os.Handler;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.SurfaceView;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.via.p2p.CommunicationPart;
//import com.via.p2p.QueryToServer;
//import com.via.p2p.VideoRecvCallback;
//import com.via.p2p.libnice;
//
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.nio.ByteBuffer;
//// avoid gstreamer_android cannot find issue.
//
//public class MainActivity_backup extends Activity {
//	boolean selfLoop = true;
//
//	libnice nice = new libnice();
//	libnice nice2= new libnice();
//	String STUN_IP 	= "74.125.204.127";
//	int    STUN_PORT= 19302;
//	String sdp = "";
//	String sdp2= "";
//	CommunicationPart cp1;
//	CommunicationPart cp2;
//
//	Handler handler = new Handler();
//	Button initBtn = null;
//	Button getBtn  = null;
//	Button setBtn = null;
//	Button sendBtn = null;
//	TextView resultView = null;
//	MainActivity_backup instance = this;
//	ImageView qrView = null;
//	SurfaceView qrSfView = null;
//	SurfaceView videoSurfaceView = null;
//	SurfaceView videoSurfaceView2 = null;
//	SurfaceView videoSurfaceView3 = null;
//	SurfaceView videoSurfaceView4 = null;
//	int MESSAGE_CHANNEL = 5;
//
//	boolean bSourceSide = false;
//	Handler handle = new Handler();
//	String remoteSdp = "";
//
//	Runnable serverTask = new Runnable(){
//		public void run() {
//			try {
//				String method = "Server";
//				String postParameters = "register="+URLEncoder.encode("FALSE", "UTF-8")
//						       +"&username="+URLEncoder.encode("HankWu","UTF-8");
//				String remoteSDP = QueryToServer.excutePost(method, postParameters);
//				if(remoteSDP.equals("NOBODY")) {
//					showToast("No Remote SDP");
//				} else {
//					showToast("Get remote SDP "+remoteSDP);
//					remoteSdp = remoteSDP;
//				}
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	};
//
//	Runnable clientTask = new Runnable(){
//		public void run() {
//			try {
//				String method = "Client";
//				String findusername = "HankWu";
//				String postParameters = "findusername="+URLEncoder.encode(findusername,"UTF-8") + "&SDP="+URLEncoder.encode(sdp,"UTF-8");
//				String remoteSDP = QueryToServer.excutePost(method, postParameters);
//
//				if (remoteSDP.equals("")) {
//					showToast("Server is offline");
//				} else if(remoteSDP.equals("OFFLINE")) {
//					showToast(findusername+"is OFFLINE");
//				} else {
//					showToast("Get remote SDP "+remoteSDP);
//					remoteSdp = remoteSDP;
//				}
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	};
//
//
//	boolean initNice(libnice nice,int i) {
//		/*
//			0 => Fail
//			1 => Success
//		 */
//		if(nice.init()==0) {
//			return false;
//		}
//		/*
//			If useReliable = 1, the libnice will send the few small packages which is separated by user giving package
//						   = 0, the libnice will send the original package.
//		 */
//		int useReliable = 0;
//		nice.createAgent(useReliable);
//		nice.setStunAddress(STUN_IP, STUN_PORT);
//		/*
//			0 => controlling
//			1 => controlled
//		 */
//		int controllMode = 0;
//		nice.setControllingMode(controllMode);
//		String streamName = "P2PStream";
//		int numberOfComponent = 5;
//		// TODO: return stream id
//		/*
//			ret = 0 => Fail.
//				= 1 => Success.
//				= 2 => It has been added.
//		 */
//		if(nice.addStream(streamName,numberOfComponent)!=1) {
//			return false;
//		}
//
//		// register a receive Observer to get byte array from jni side to java side.
//		int forComponentIndex = 1;
//		nice.registerReceiveCallback(new VideoRecvCallback(videoSurfaceView),forComponentIndex);
//		forComponentIndex = 2;
//		nice.registerReceiveCallback(new VideoRecvCallback(videoSurfaceView2),forComponentIndex);
//		forComponentIndex = 3;
//		nice.registerReceiveCallback(new VideoRecvCallback(videoSurfaceView3),forComponentIndex);
//		forComponentIndex = 4;
//		nice.registerReceiveCallback(new VideoRecvCallback(videoSurfaceView4),forComponentIndex);
//		forComponentIndex = 5;
//		if(i==1) {
//			cp1 = new CommunicationPart(nice,forComponentIndex);
//			nice.registerReceiveCallback(cp1, forComponentIndex);
//		} else {
//			cp2 = new CommunicationPart(nice,forComponentIndex);
//			nice.registerReceiveCallback(cp2, forComponentIndex);
//		}
//
//		// register a state Observer to catch stream/component state change
//        nice.registerStateObserver(new NiceStateObserver(nice,i));
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        // TODO: add stream id, each stream has self SDP.
//		if(nice.gatheringCandidate()==1) {
//			showToast("gathering Candidate Success, please wait gathering done then getLocalSDP");
//		} else {
//			showToast("gathering Candidate fail");
//			return false;
//		}
//
//		return true;
//	}
//
//
//	OnClickListener initListener = new OnClickListener() {
//
//        public void onClick(View v) {
//
//			if (initNice(nice,1) && initNice(nice2,2)) {
//				showToast("nice nice2 create success!");
//			}
//
//
//
//			Thread a = new Thread(new Runnable() {
//				public void run() {
//					if(bSourceSide) {
//						// Send SDP to Server
//						String method = "Server";
//						String postParameters;
//						try {
//							postParameters = "register="+URLEncoder.encode("TRUE", "UTF-8")
//									       +"&username="+URLEncoder.encode("HankWu","UTF-8")
//									       +"&SDP="+URLEncoder.encode(sdp,"UTF-8");
//							QueryToServer.excutePost(method, postParameters);
//						} catch (UnsupportedEncodingException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				}
//			});
//			a.start();
//		}
//	};
//
//	OnClickListener getListener = new OnClickListener(){
//
//		public void onClick(View v) {
//			if(bSourceSide) {
//				(new Thread(serverTask)).start();
//			} else {
//				(new Thread(clientTask)).start();
//			}
//		}
//	};
//
//
//	OnClickListener setListener = new OnClickListener(){
//
//		public void onClick(View v) {
//
//			   AlertDialog.Builder editDialog = new AlertDialog.Builder(MainActivity_backup.this);
//			   editDialog.setTitle("--- send remote sdp ---");
//
//			   editDialog.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
//			    // do something when the button is clicked
//			    public void onClick(DialogInterface arg0, int arg1) {
//					if(selfLoop) {
//						nice.setRemoteSdp(sdp2);
//						nice2.setRemoteSdp(sdp);
//					} else {
//						nice.setRemoteSdp(remoteSdp);
//					}
//			    }
//			    });
//			   editDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//			          // do something when the button is clicked
//			    public void onClick(DialogInterface arg0, int arg1) {
//
//			    }
//			    });
//			   editDialog.show();
//		}
//	};
//
//    ByteBuffer naluBuffer = ByteBuffer.allocateDirect(1024*1024);
//
//	int DEFAULT_DIVIDED_SIZE = 1024*1024;
//
//	OnClickListener sendListener = new OnClickListener(){
//
//
//		public void onClick(View v) {
//			AlertDialog.Builder editDialog = new AlertDialog.Builder(MainActivity_backup.this);
//			   editDialog.setTitle("--- send message ---");
//
//			   final EditText editText = new EditText(MainActivity_backup.this);
//			   editDialog.setView(editText);
//
//			   editDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//			    // do something when the button is clicked
//			    public void onClick(DialogInterface arg0, int arg1) {
//			    	String sendmsg = editText.getText().toString();
//					//nice.sendMsg(sendmsg,1);
//					cp1.sendMessage("Halo i'm cp1");
//					cp2.sendMessage("Halo i'm cp2");
//
//					//AddTextToChat("Me:"+sendmsg);
//			    }
//			    });
//			   editDialog.setNeutralButton("play video", new DialogInterface.OnClickListener() {
//
//
//				public void onClick(DialogInterface dialog, int which) {
//					final String path = editText.getText().toString();
//
//					Thread a = new Thread(new Runnable(){
//
//						public void run() {
//							int counter = 0;
//							if(!bInit) {
//								initMediaExtractor(path);
//								bInit = true;
//							}
//							for(;;){
//								// TODO Auto-generated method stub
//								int naluSize = me.readSampleData(naluBuffer, 0);
//
//								LOGD("Sent naluSize : "+naluSize);
//								int divideSize = DEFAULT_DIVIDED_SIZE;
//								int sentSize = 0;
//								//nice.sendMsg("NALU", 1);
//
//								//for(;;) {
//								if(naluSize > 0)
//								{
//									for(;;) {
//										if((naluSize-sentSize) < divideSize) {
//											divideSize = naluSize-sentSize;
//										}
//
//										naluBuffer.position(sentSize);
//										naluBuffer.limit(divideSize+sentSize);
//										// Reliable mode : if send buffer size bigger than MTU, the destination side will received data partition which is divided by 1284.
//										// Normal mode   : if send buffer size bigger than MTU, the destination side will received all data in once receive.
//										nice.sendDataDirect(naluBuffer.slice(),divideSize,1);
//										nice.sendDataDirect(naluBuffer.slice(),divideSize,2);
//										nice.sendDataDirect(naluBuffer.slice(),divideSize,3);
//										nice.sendDataDirect(naluBuffer.slice(),divideSize,4);
//
//										naluBuffer.limit(naluBuffer.capacity());
//
//					                    sentSize += divideSize;
//										if(sentSize >= naluSize) {
//											break;
//										}
//									}
//									me.advance();
//
//									try {
//										Thread.sleep(33);
//									} catch (InterruptedException e) {
//										// TODO Auto-generated catch block
//										e.printStackTrace();
//									}
//
//								} else {
//									me.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
//								}
//							}
//						}
//					});
//					a.start();
//				}
//			});
//
//			   editDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//			          // do something when the button is clicked
//			    public void onClick(DialogInterface arg0, int arg1) {
//
//			    }
//			    });
//			   editDialog.show();
//
//		}
//	};
//
//
//
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		initBtn = (Button) findViewById(R.id.initBtn);
//		getBtn = (Button) findViewById(R.id.getBtn);
//		setBtn = (Button) findViewById(R.id.setBtn);
//		sendBtn = (Button) findViewById(R.id.sendBtn);
//
////		qrSfView = (SurfaceView) findViewById(R.id.QRCodeSurfaceView);
//		videoSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
//		videoSurfaceView2 = (SurfaceView) findViewById(R.id.surfaceView2);
//		videoSurfaceView3 = (SurfaceView) findViewById(R.id.surfaceView3);
//		videoSurfaceView4 = (SurfaceView) findViewById(R.id.surfaceView4);
//
////		resultView = (TextView) findViewById(R.id.textView2);
//		initBtn.setOnClickListener(initListener);
//		getBtn.setOnClickListener(getListener);
//		setBtn.setOnClickListener(setListener);
//		sendBtn.setOnClickListener(sendListener);
//
//	}
//
//	public void AddTextToChat(final String msg) {
//		runOnUiThread(new Runnable() {
//
//
//			public void run() {
//				//resultView.setText(msg +"\n"+ resultView.getText().toString());
//			}
//
//		});
//	}
//
//
//
//	public boolean onCreateOptionsMenu(Menu menu) {
//
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
//
//
//	private void LOGD(String msg) {
//		Log.d("Libnice-java",msg);
//	}
//
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			nice.init();
//
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}
//
//	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
//		if(requestCode == 1) {
//			if(resultCode==RESULT_OK) {
//				final String contents = intent.getStringExtra("SCAN_RESULT");
//				//TextView tv = (TextView) findViewById(R.id.textView1);
//				//tv.setText(contents);
//				//remoteSdp = contents;
//			}
//		}
//	}
//
//	public class NiceStateObserver implements libnice.StateObserver {
//			private libnice mNice;
//			int index = -1;
//			public NiceStateObserver(libnice nice,int i) {
//				mNice = nice;
//				index = i;
//			}
//			public void cbComponentStateChanged(final int stream_id, final int component_id,
//												final int state) {
//				Log.d("cbComponentStateChanged","Index["+index+"]"+"Stream["+stream_id+"]["+component_id+"]:"+libnice.StateObserver.STATE_TABLE[state]);
//			}
//			public void cbCandidateGatheringDone(int stream_id) {
//				/*
//					if candidate gathering done, then it will getLocalSDP automatically.
//				 */
//
//				if(index==1) {
//					sdp =  mNice.getLocalSdp();
////                    if(selfLoop)
////                        nice2.setRemoteSdp(sdp);
//				} else {
//					sdp2= mNice.getLocalSdp();
////                    if(selfLoop)
////                        nice.setRemoteSdp(sdp2);
//
//				}
//				showToast("get sdp : "+sdp);
//			}
//	}
//
//	MediaPlayer mMediaPlayer;
//
//	MediaExtractor me = new MediaExtractor();
//	boolean bInit = false;
//	void initMediaExtractor(String path) {
//		try {
//			me.setDataSource(path);
//			MediaFormat mf = null;
//			String mime = null;
//			String videoMsg = "Video";
//			int w = 0;
//			int h = 0;
//			String s_sps = null;
//			String s_pps = null;
//
//			for(int i=0;i<me.getTrackCount();i++) {
//				mf = me.getTrackFormat(i);
//				mime = mf.getString(MediaFormat.KEY_MIME);
//
//
//				if(mime.startsWith("video")) {
//					me.selectTrack(i);
//					mime = mf.getString(MediaFormat.KEY_MIME);
//
//					w = mf.getInteger(MediaFormat.KEY_WIDTH);
//					h = mf.getInteger(MediaFormat.KEY_HEIGHT);
//
//					ByteBuffer sps_b = mf.getByteBuffer("csd-0");
//			        byte[] sps_ba = new byte[sps_b.remaining()];
//			        sps_b.get(sps_ba);
//			        s_sps = bytesToHex(sps_ba);
//
//			        mf.getByteBuffer("csd-1");
//			        ByteBuffer pps_b = mf.getByteBuffer("csd-1");
//			        byte[] pps_ba = new byte[pps_b.remaining()];
//			        pps_b.get(pps_ba);
//			        s_pps = bytesToHex(pps_ba);
//
//			        videoMsg = videoMsg + ":" + mime + ":" + w + ":" + h + ":" + s_sps + ":" + s_pps + ":";
//
//			        nice.sendMsg(videoMsg, 1);
//					nice.sendMsg(videoMsg, 2);
//			        nice.sendMsg(videoMsg, 3);
//					nice.sendMsg(videoMsg, 4);
//					break;
//				}
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
//
//	  public static String bytesToHex(byte[] bytes) {
//		    char[] hexChars = new char[bytes.length * 2];
//		    for ( int j = 0; j < bytes.length; j++ ) {
//		      int v = bytes[j] & 0xFF;
//		      hexChars[j * 2] = hexArray[v >>> 4];
//		      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//		    }
//		    return new String(hexChars);
//		  }
//
//	  public void showToast(final String tmp) {
//		  runOnUiThread(new Runnable(){
//
//			public void run() {
//				Toast.makeText(instance, tmp, Toast.LENGTH_SHORT).show();
//			}
//		  });
//	  }
//
//
//}
