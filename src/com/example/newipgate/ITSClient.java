package com.example.newipgate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.newipgate.WebSocketPart.WebSocketConnection;
import com.example.newipgate.WebSocketPart.WebSocketConnectionHandler;
import com.example.newipgate.WebSocketPart.WebSocketException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;





public class ITSClient extends Service{
	
	
	private static final String TAG = null;
	
	private DefaultHttpClient client;

	private static WelcomePage welcomeActivity;
	
	private static LoginActivity loginActivity;
	
	private final WebSocketConnection wsc = new WebSocketConnection();
	
	private boolean websocketConnected = false; 
	
	private boolean waitForHeartbeatResponse = false;

	private final int CHANGE_OTHER_DEVICE_STATUS = 4;
	private final int CHANGE_PASSWORD = 7;
	private final int HEART_BEAT = 2;
	private final int REFRESH_OTHER_DEVICES_STATUS = 6;
	private final int REQUEST_OTHER_DEVICES_RECOVER = 5;
	private final int GET_DEVICES_LIST = 3;
	private final int UPDATE_SELF_STATUS = 1;


	
	
	Handler heartBeatHandler  = new Handler();
	Runnable sendHeartBeatRunnable = new Runnable() {
		
		public void run() {
			if(websocketConnected){
				System.out.println("sending heartbeat now");
				sendHeartBeat();
				heartBeatHandler.postDelayed(sendHeartBeatRunnable, 300000);
				//heartBeatHandler.postDelayed(sendHeartBeatRunnable, 10000); 	

			}
			else{
				startWebSocket();
				System.out.println("in send heart beat, the socket is not established");
				heartBeatHandler.postDelayed(sendHeartBeatRunnable, 300000); 	
			}
			 

		}
	};
	
	public void startHeartBeat(){
		System.out.println("start to send heartbeat");
		heartBeatHandler.post(sendHeartBeatRunnable);
	}
	
	Handler checkHeartbeatResponseHandler = new Handler();
	Runnable checkHeartbeatResponse = new Runnable() {
		
		@Override
		public void run() {
			if(waitForHeartbeatResponse){
				waitForHeartbeatResponse = false;
				if(websocketConnected)
				{
					stopWebSocket();
				}
				startWebSocket();
			}
		}
	};


	final private Handler timeOutHandler = new Handler(){
	    public void handleMessage(Message msg) {
	       	PublicObjects.getCurrentAllConnections().stopCurrentActionTypeWithServer(msg.what);
			if(PublicObjects.getCurrentActivity() == 2){
				//Toast.makeText(PublicObjects.getCurrentAllConnections(), "网络状况不佳，请稍后再试", Toast.LENGTH_SHORT).show();
			}

	    }
	};

	//这个handler只用来处理本设备网络更改收到its反馈时的情况。因为与its交互的过程在一个线程中实现，因此必须使用handler
	final private Handler refreshUIHandler = new Handler(){
	    public void handleMessage(Message msg) {
		PublicObjects.getCurrentAllConnections().refresh();
	    }
	};


		
	public void onCreate() {
		super.onCreate(); 
		welcomeActivity = PublicObjects.getCurrentWelcomeActivity();
		
		//此处开启https通信机制，并使用本地密钥库
		InputStream ins = welcomeActivity.getResources().openRawResource(R.raw.ca);
		CertificateFactory cerFactory;
		try {
			cerFactory = CertificateFactory.getInstance("X.509");
			Certificate cer = (Certificate) cerFactory.generateCertificate(ins);
			KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
			keyStore.load(null, null);
			keyStore.setCertificateEntry("trust", cer);
			SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore);
			Scheme sch = new Scheme("https", socketFactory, 443);
			client = new DefaultHttpClient();
			client.getConnectionManager().getSchemeRegistry().register(sch);
			client.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.BEST_MATCH);
			client.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.BROWSER_COMPATIBILITY);
			client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 60000);
		}
		
		catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	
	   public void onDestroy() {
	        // Cancel the persistent notification.
		   System.out.println("<----------------ITSClient Service is destroyed!------------->");
		   wsc.disconnect();
		   
	        // Tell the user we stopped.
	    }

	public static  void setLoginActivity(LoginActivity thisActivity){
		loginActivity = thisActivity;
	}
	

	
	
	
	public int login() {
		HttpPost post = new HttpPost("http://its.pku.edu.cn/cas/login");
		HttpParams httpparams = new BasicHttpParams();
        httpparams.setParameter("http.protocol.handle-redirects", false);
        post.setParams(httpparams);
		String splitwords = "|;kiDrqvfi7d$v0p5Fg72Vwbv2;|";
		try{
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			post.addHeader("User-Agent", 
							"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:28.0) Gecko/20100101 Firefox/28.0");
			post.addHeader("Host", "its.pku.edu.cn");
			post.addHeader("Accept", 
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
			post.addHeader("Accept-Encoding", "gzip, deflate");
			post.addHeader("Referer", "https://its.pku.edu.cn/");
			
			SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE); 
			//String username = sharedPre.getString("username", "");
			String username = PublicObjects.getCurrentUsername();
			String password = PublicObjects.getCurrentPassword();
			
			params.add(new BasicNameValuePair("username1", username));
			params.add(new BasicNameValuePair("password", password));
			System.out.println("in login, the username is " + username + " and the password is " + password);
			
			params.add(new BasicNameValuePair("pwd_t", "密码"));
			params.add(new BasicNameValuePair("fwrd", "free"));
			params.add(new BasicNameValuePair("username", username + splitwords + 
					password +splitwords + "12"));
			
			
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");	
			post.setEntity(entity);
			HttpResponse response = client.execute(post);
			if(response == null) {
				System.out.println("response is null");
				return -1;
			}
			Header newLocation = response.getFirstHeader("Location");
			response.getEntity().consumeContent();

			//这个while循环用来判断是否密码错误
			while(newLocation != null)
			{
				String newLocationUrl = newLocation.getValue();
				System.out.println("the redirect url is " + newLocationUrl);
				
				//e=3 means the password is wrong
				if(newLocationUrl.contains("e=3"))
					return 1314;
				HttpGet get = new HttpGet(newLocationUrl);
				response = client.execute(get);
				newLocation = response.getFirstHeader("Location");
				response.getEntity().consumeContent();

			}
			System.out.println("successful login!");
			int statusCode = response.getStatusLine().getStatusCode();
			return statusCode;
		} catch(ConnectTimeoutException e) {
			return -2;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			post.abort();
		}
	}
	
	private String cmd(String cmdaddr) {
		int statusCode = login();
		Log.i(TAG, "login code is " + statusCode);
		if(statusCode != 301 && statusCode != 200
				&& statusCode != 302 && statusCode != 1314) {
			return "error";
		}
		
		if(statusCode == 1314)
			return "wrong password";
		
		//Log.i(TAG, "the itsresult page is " + getPage("http://its.pku.edu.cn/netportal/itsResult.jsp"));
		return getPage(cmdaddr);
	}

	
	public static String md5(String string) {
	    byte[] hash;
	    try {
	        hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException("Huh, MD5 should be supported?", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new RuntimeException("Huh, UTF-8 should be supported?", e);
	    }

	    StringBuilder hex = new StringBuilder(hash.length * 2);
	    for (byte b : hash) {
	        if ((b & 0xFF) < 0x10) hex.append("0");
	        hex.append(Integer.toHexString(b & 0xFF));
	    }
	    return hex.toString();
	}
	
	private String getPage(String url) {
		HttpGet get = new HttpGet(url);
		get.setHeader("Referer", "http://its.pku.edu.cn/netportal/netportal_UTF-8.jsp");
		try {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = client.execute(get, responseHandler);
			get.abort();
			get = new HttpGet("http://its.pku.edu.cn/netportal/itsResult.jsp");
			String newResponse = client.execute(get, responseHandler);
			//if(newResponse.contains("达到设定值") || newResponse.contains("连接成功"))
			if(newResponse.contains("达到设定值") || newResponse.contains("连接成功") || newResponse.contains("断开成功"))
				return newResponse;
			return responseBody;
		} catch(Exception e) {
			e.printStackTrace();
			return "error";
		} finally {
			get.abort();
		}
	}


	//connectType=1, free, connectType=2, charge
	public void connect(int connectType) 
	{
		final int ConnectType = connectType;
		new Thread() {
			public void run() {
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=open&type=";
				int random = Math.abs((new Random()).nextInt() % 1000);
				if(ConnectType == 2)
				{
					getaddr += "fee&fr=0&sid=" + random;
				}
				else {
					getaddr += "free&fr=0&sid=701";
				}
				String response = cmd(getaddr);
				if(response.contains("wrong password")){
					PublicObjects.getCurrentAllConnections().showInfo(4);
					System.out.println("密码错误");
				}
				else if(response.contains("重新输入")) {
					PublicObjects.getCurrentAllConnections().showInfo(6);
					System.out.println("重新输入");

				} else if(response.contains("达到设定值")) {
					PublicObjects.getCurrentAllConnections().showInfo(5);
					System.out.println("当前连接超过预定值");
				} else if(response.contains("不在申请访问服务的范围内")) {
					PublicObjects.getCurrentAllConnections().showInfo(6);
					System.out.println("不在申请访问服务的范围内");
				} else if(response.contains("ipgw_open_Failed")) {
					PublicObjects.getCurrentAllConnections().showInfo(6);
					System.out.println("ipgw_open_Failed");
				} else if(response.contains("连接成功"))
				{
					String [] resultStrings = response.split(">[012]个<", 0);
					if(ConnectType == 2){
						PublicObjects.setThisDeviceStatus(4);
						PublicObjects.getCurrentAllConnections().showInfo(1);
						//PublicObjects.getCurrentAllConnections().refresh();
						refreshUIHandler.sendEmptyMessage(0);

					}else {
						PublicObjects.setThisDeviceStatus(3);
						PublicObjects.getCurrentAllConnections().showInfo(0);
						//PublicObjects.getCurrentAllConnections().refresh();
						refreshUIHandler.sendEmptyMessage(0);



					}
					
					if(resultStrings.length > 0)
					{
						int indexOfNext = response.indexOf(resultStrings[1]);
						final String connectionNum = response.substring(indexOfNext-3, indexOfNext-1);
						System.out.println("连接成功" + "\n当前连接：" + connectionNum);
					}
					else 
					{
						System.out.println("连接成功" + "\n未知错误，无法获取连接数");
					}


					updateConnectionStatus();
				}
				else {
					System.out.println("未知错误, the response is " + response);
				}
			}
		}.start();
	}
	
	
	public void disconnectAll() {
		new Thread() {
			public void run() {
				int random = Math.abs((new Random()).nextInt() % 1000);
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=close&type=allconn&fr=0&sid" + random;
				String response = cmd(getaddr);
				//System.out.println("in disconnect all, the response is " + response);
				if(response.contains("断开成功") || response.contains("断开全部连接成功")) {
					PublicObjects.setThisDeviceStatus(2);
					PublicObjects.getCurrentAllConnections().showInfo(3);
					System.out.println("断开全部连接成功");
					updateConnectionStatus();
					PublicObjects.setAllOtherDeviceStatus(1);
					//PublicObjects.getCurrentAllConnections().refresh();
					refreshUIHandler.sendEmptyMessage(0);


					
				}else{
					PublicObjects.getCurrentAllConnections().showInfo(6);
					System.out.println("unknown error");
				}
			}
		}.start();

	}
	

	public void disconnectThis() {
		
		new Thread() {
			public void run() {
				int random = Math.abs((new Random()).nextInt() % 1000);
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=close&type=self&fr=0&sid=" + random;
				String response = cmd(getaddr);
				//System.out.println("in disconnect this, the response is " + response);
				if(response.contains("断开成功")) {
					PublicObjects.setThisDeviceStatus(1);
					PublicObjects.getCurrentAllConnections().showInfo(2);
					//PublicObjects.getCurrentAllConnections().refresh();
					refreshUIHandler.sendEmptyMessage(0);


					System.out.println("网络断开成功");
					updateConnectionStatus();
				}else if(response.contains("wrong")){
					PublicObjects.getCurrentAllConnections().showInfo(4);
					System.out.println("wrong password");					
				}
				else{
					PublicObjects.getCurrentAllConnections().showInfo(6);
					System.out.println("unknown error");
				}
			}
		}.start();

	}

	
	//webSocket part
	


	
	private String getLocalHostIp()  
    {  
        String ipaddress = "";  
        try  
        {  
            Enumeration<NetworkInterface> en = NetworkInterface  
                    .getNetworkInterfaces();  
            // 遍历所用的网络接口  
            while (en.hasMoreElements())  
            {  
                NetworkInterface nif = en.nextElement();// 得到每一个网络接口绑定的所有ip  
                Enumeration<InetAddress> inet = nif.getInetAddresses();  
                // 遍历每一个接口绑定的所有ip  
                while (inet.hasMoreElements())  
                {  
                    InetAddress ip = inet.nextElement();  
                    // 在这里如果不加isIPv4Address的判断,直接返回,在4.0上获取到的是类似于fe80::1826:66ff:fe23:48e%p2p0的ipv6的地址  
                    if (!ip.isLoopbackAddress())  
                    {  
                        System.out.println("本机ip是 : " + ip.getHostAddress());
                    	return ipaddress = ip.getHostAddress();  
                    }  
                }  
  
            }  
        }  
        catch (SocketException e)  
        {  
            Log.e("feige", "获取本地ip地址失败");  
            e.printStackTrace();  
        }  
        return ipaddress;  
  
    }
	
	private int hasIPv6(){
		String ipAddress = getLocalHostIp();
		System.out.println(ipAddress);
		
		if(ipAddress.contains(":") && !ipAddress.startsWith("fe80")){
			System.out.println("ipv6 and ip is " + ipAddress);
			return 1;
		}
		else if(ipAddress.contains(".")){
			System.out.println("ipv4");
			return 0;
		}
		else {
			System.out.println("other, ip is " + ipAddress);

			return -1;

		}
	}
	
	
	public Boolean isWebsocketConnected(){
		return websocketConnected;
	}
	
	public void updateConnectionStatus(){
		if(websocketConnected){ 
			if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(UPDATE_SELF_STATUS) == -1){
				String sentString = InteractionInfo.formUpdateConnectionStatus();
				PublicObjects.getCurrentAllConnections().setCurrentActionTypeWithServer(UPDATE_SELF_STATUS);
				wsc.sendTextMessage(sentString);
				timeOutHandler.sendEmptyMessageDelayed(UPDATE_SELF_STATUS, 2000);
				System.out.println("in update connection status, the sent string is " + sentString);
			}
			
		}
		else{
			System.out.println("in updateconnectionstatus, the websocket is already closed");
		}
	}
	
	//send heartBeat Information
	public void sendHeartBeat()
	{
		if(websocketConnected)
		{
				wsc.sendTextMessage(InteractionInfo.formHeartBeat());
				System.out.println("in itsclient.sendheartbeat, successful send heartbeat");
				waitForHeartbeatResponse = true;
				checkHeartbeatResponseHandler.postDelayed(checkHeartbeatResponse, 10000);		
		}
		else{
			System.out.println("the websocket is not established, so the heartbeat is not sent out");
		}

	}

	//initiate the websocket connection
	//此处登录时采用的用户名和密码都是系统里本来存储的，而不是主界面上输入的
	public void startWebSocket()
	{
		if(!websocketConnected){
		try {
			System.out.println("in start websocket, the status is disconnect");
            String url = "";
            String MD5password = "";
            
			if(hasIPv6() == 1){
            	url = "ws://[2001:da8:201:1146:2033:44ff:fe55:6677]:9000/";
            }
			else{
				url = "ws://162.105.146.35:9000/";
			}
			
			String socketUsername = PublicObjects.getCurrentUsername();
			String socketPassword = PublicObjects.getCurrentPassword();
			
			try {
				MD5password = URLEncoder.encode(md5(socketPassword), "utf-8");
				System.out.println("passwd before md5 is "+socketPassword + " and after md5 is "+ MD5password);
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			
			System.out.println("in startwebsocket, the user name is " + socketUsername + " password is " + MD5password +
					" status " + PublicObjects.getThisDeviceStatus());
			wsc.connect(url + "login?student_id=" + socketUsername + "&password=" + MD5password + "&type=1&status=" + PublicObjects.getThisDeviceStatus(), 
					new WebSocketConnectionHandler(){

                    @Override
                    public void onBinaryMessage(byte[] payload) {
                            System.out.println("onBinaryMessage size="+payload.length);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                            System.out.println("onClose code = " + code + " reason=" + reason);
                            System.out.println("the websocketConnected is set to false");
                            websocketConnected = false;
                            //LoginActivity.setIsTrying2ConnectServer(false);
                    }

                    @Override
                    public void onOpen() {
                    		System.out.println("the websocketConnected is set to true");
                    		websocketConnected = true;
                            //LoginActivity.setIsTrying2ConnectServer(false);
                            System.out.println("onOpen");
                            if(PublicObjects.getThisDeviceID() != null && !PublicObjects.getThisDeviceID().equals("")){
            					//此处这一行为暂时慢点实行
                            	String sentText = InteractionInfo.formAnnulFormerConnection();
            					wsc.sendTextMessage(sentText);
            					System.out.println("annul former connection, sent string is " + sentText);

                            }else{
                            	System.out.println("no former connection!");
                            }
                            
                            getOtherDevices();
        					startHeartBeat();
                    }

                    @Override
                    public void onRawTextMessage(byte[] payload) {
                            System.out.println("onRawTextMessage size="+payload.length);
                            
                            
                    }

                    @Override
                    public void onTextMessage(String payload) {
                    	handleReceivedMessage(payload);     
                    }
           
            });
            
    } catch (WebSocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    }
		}
		else{
			System.out.println("in start websocket, the connection is already established");
		}

	}
	
	
	public void stopWebSocket(){
		if(websocketConnected){
			wsc.disconnect();			
		}
	}
	//change other device's status
	public void changeOtherDevice(String DeviceID, int newStatus)
	{
		if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(CHANGE_OTHER_DEVICE_STATUS) == -1){
			System.out.println("in change other devices, the device id is " + DeviceID + " and new status is " + newStatus);
			String sentString = InteractionInfo.formChangeOtherDevice(DeviceID, newStatus);
			PublicObjects.getCurrentAllConnections().setCurrentActionTypeWithServer(CHANGE_OTHER_DEVICE_STATUS);
			wsc.sendTextMessage(sentString);
			timeOutHandler.sendEmptyMessageDelayed(CHANGE_OTHER_DEVICE_STATUS, 2000);
			System.out.println("in change other devices, the sent string is " + sentString);			
		}

	}
	
	public void getOtherDevices(){
		if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(GET_DEVICES_LIST) == -1){
			PublicObjects.getCurrentAllConnections().setCurrentActionTypeWithServer(GET_DEVICES_LIST);
			wsc.sendTextMessage(InteractionInfo.formGetOtherDevices());
			timeOutHandler.sendEmptyMessageDelayed(GET_DEVICES_LIST, 2000);			
		}

	}
	
	
	//change the password
	public void sendChangePassword(String newPassword)
	{
		
		PublicObjects.getCurrentAllConnections().setCurrentActionTypeWithServer(CHANGE_PASSWORD);
		wsc.sendTextMessage(InteractionInfo.formSendChangePassword(newPassword));
		timeOutHandler.sendEmptyMessageDelayed(CHANGE_PASSWORD, 2000);
	}

	//update the information of other device
	public void updateOtherDevice()
	{
		if(!websocketConnected){
			startWebSocket();
		}
		else{
			if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(REFRESH_OTHER_DEVICES_STATUS) == -1){
				String sentString = InteractionInfo.formUpdateOtherDevice();
				PublicObjects.getCurrentAllConnections().setCurrentActionTypeWithServer(REFRESH_OTHER_DEVICES_STATUS);
				wsc.sendTextMessage(sentString);
				timeOutHandler.sendEmptyMessageDelayed(REFRESH_OTHER_DEVICES_STATUS, 2000);
				System.out.println("send string: " + sentString);					
			}

		}
		
	}

	//handles all kind of received message
	private void handleReceivedMessage(String payload)
	{
		System.out.println("onTextMessage"+payload);
        if(Pattern.matches("[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+", payload))
        {
        	PublicObjects.setThisDeviceDeviceID(payload);
        	PublicObjects.setThisDeviceType(1);
        	if(PublicObjects.getCurrentActivity() != 2){
            	loginActivity.changeActivity();        		
        	}
        }
        else {
        	JSONObject jsonObject = null;
        	int infoType = -1;
			try {
				jsonObject = new JSONObject(payload);
                infoType = jsonObject.getInt("type");  
                
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			if(infoType == 1){
				try {
					String connectionString = jsonObject.getString("content");
					if(connectionString.equals("ok") && PublicObjects.getCurrentActivity() == 2){
						
						//PublicObjects.getCurrentAllConnections().refresh();
						if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(UPDATE_SELF_STATUS) == 1){
							PublicObjects.getCurrentAllConnections().stopCurrentActionTypeWithServer(UPDATE_SELF_STATUS);
						}
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(infoType == 2){
				try {
					String connectionString = jsonObject.getString("content");
					if(connectionString.equals("ok") ){
						waitForHeartbeatResponse = false;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(infoType == 3){
				try {
					System.out.println("in handlereceived, type 3");
					String connectionString = jsonObject.getString("content");
					if(connectionString.startsWith("\ufeff")){
						System.out.println("starts with bom");
						connectionString = connectionString.substring(1);
					}
					JSONArray contents = new JSONArray(connectionString);
					System.out.println("there are " + contents.length() + " more devices");

					for(int i=0; i<contents.length(); i++)
					{
						PublicObjects.otherDevices[i].device_id = ((JSONObject)contents.get(i)).getString("device_id");
						PublicObjects.otherDevices[i].status = ((JSONObject)contents.get(i)).getInt("status");
						PublicObjects.otherDevices[i].type = ((JSONObject)contents.get(i)).getInt("type");
						PublicObjects.otherDevices[i].ip = ((JSONObject)contents.get(i)).getString("ip");
						PublicObjects.otherDevices[i].location = ((JSONObject)contents.get(i)).getString("location");
						PublicObjects.otherDevices[i].owner = ((JSONObject)contents.get(i)).getString("owner");
						System.out.println("device "+ i + " id is " + PublicObjects.otherDevices[i].device_id);
						
					}
					PublicObjects.otherDeviceNum = contents.length();
					if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(GET_DEVICES_LIST) == 1){
							PublicObjects.getCurrentAllConnections().stopCurrentActionTypeWithServer(GET_DEVICES_LIST);
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
			//add new device
			else if(infoType == 102)
			{
				System.out.println("in deal with 102");
				try {
					String connectionString = jsonObject.getString("content");
					JSONObject connectionInfo = new JSONObject(connectionString);
					//JSONObject connectionInfo = jsonObject.getJSONObject("content");
					if(!PublicObjects.getThisDeviceID().equals(connectionInfo.getString("device_id"))){

						//System.out.println("the device_id is different, old id is " + PublicObjects.getThisDeviceID() + " and new id is " 
							//	+ connectionInfo.getString("device_id"));
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].device_id = connectionInfo.getString("device_id");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].status = connectionInfo.getInt("status");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].type = connectionInfo.getInt("type");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].ip = connectionInfo.getString("ip");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].location = connectionInfo.getString("location");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].owner = connectionInfo.getString("owner");
						PublicObjects.otherDeviceNum++;
					}
					
					if(PublicObjects.getCurrentActivity() == 2){
						PublicObjects.getCurrentAllConnections().refresh();
					}						
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println("after 102 ");
				//PublicObjects.printOtherDevices();
				
			}
			
			//delete device
			else if(infoType == 103)
			{
				try {
					Boolean hasFind = false;
					String deletedDeviceId = jsonObject.getString("content");
					
					//disconnect this device
					if(deletedDeviceId.equals(PublicObjects.getThisDeviceID())){
						System.out.println("in 103, the deleted device is itself");
						disconnectThis();
					}
					//delete other device's info
					else{
					for(int i=0; i<PublicObjects.otherDeviceNum; i++)
					{
						//move the last device to this slot
						if(deletedDeviceId.equals(PublicObjects.otherDevices[i].device_id))
						{
							PublicObjects.otherDevices[i].device_id = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].device_id;
							PublicObjects.otherDevices[i].status = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].status;
							PublicObjects.otherDevices[i].type = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].type;
							PublicObjects.otherDevices[i].ip = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].ip;
							PublicObjects.otherDevices[i].location = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].location;
							PublicObjects.otherDevices[i].owner = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].owner;
							PublicObjects.otherDeviceNum--;
							hasFind = true;
							break;
						}
					}
					if(!hasFind)
						System.out.println("the deleted device not found");
					}
					if(PublicObjects.getCurrentActivity() == 2){
						PublicObjects.getCurrentAllConnections().resetSelectedItem();
						PublicObjects.getCurrentAllConnections().refresh();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			
			//update other device's status
			else if(infoType == 101)
			{
				//System.out.println("before 101");
				//PublicObjects.printOtherDevices();
				try {
					Boolean hasFind = false;
					
					String connectionString = jsonObject.getString("content");
					if(connectionString.startsWith("\ufeff")){
						System.out.println("starts with bom");
						connectionString = connectionString.substring(1);
					}
					JSONObject otherDeviceInfo = new JSONObject(connectionString);
					String otherDeviceId = otherDeviceInfo.getString("device_id");
					int otherDeviceStatus = otherDeviceInfo.getInt("status");
					for(int i=0; i<4; i++)
					{
						if(otherDeviceId.equals(PublicObjects.otherDevices[i].device_id))
						{
							PublicObjects.otherDevices[i].status = otherDeviceStatus;
							if(otherDeviceStatus == 2){
								PublicObjects.getCurrentAllConnections().checkStatus();
							}
							hasFind = true;
							break;
						}
					}
					if(!hasFind)
						System.out.println("the update other device's status error");
					if(PublicObjects.getCurrentActivity() == 2){
						PublicObjects.getCurrentAllConnections().refresh();
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//update its own status
			else if(infoType == 104)
			{
				try {
					int newStatus = jsonObject.getInt("content");
					//thisDeviceInfo.status = newStatus;
					if(newStatus == 1)
					{
						disconnectThis();
					}
					else if(newStatus == 3)
					{
						connect(1);
					}
					else if(newStatus == 4)
					{
						connect(2);
					}
					else
					{
						System.out.println("when changing status itself, unknown error");
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			else if(infoType == 106)
			{
				 try {
						JSONObject requestInfo = new JSONObject();
						PublicObjects.getCurrentAllConnections().checkStatus();
						requestInfo.put("type", 1);
						requestInfo.put("content", PublicObjects.getThisDeviceStatus());
						wsc.sendTextMessage(requestInfo.toString());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			else if(infoType == 105)
			{
				disconnectAll();
				try {
					JSONObject requestInfo = new JSONObject();
					requestInfo.put("type", 5);
					requestInfo.put("content", JSONObject.NULL);
					wsc.sendTextMessage(requestInfo.toString());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			else if(infoType == 107)//比较用户是否是当前的用户
			{
				try {
					String newPassword = jsonObject.getString("content");
					welcomeActivity.updatePassword(newPassword);
						
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(infoType == 6)
			{
				String connectionString;
				try {
					connectionString = jsonObject.getString("content");
					System.out.println("in type 6, the content is " + connectionString);
					//PublicObjects.getCurrentAllConnections().showInfo(connectionString);
					if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(REFRESH_OTHER_DEVICES_STATUS) == 1){
							PublicObjects.getCurrentAllConnections().stopCurrentActionTypeWithServer(REFRESH_OTHER_DEVICES_STATUS);
						}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(infoType == 4)
			{
				
				String connectionString;
				try {
					connectionString = jsonObject.getString("content");
					System.out.println("in type 4, the content is " + connectionString);
					//PublicObjects.getCurrentAllConnections().showInfo(connectionString);
					if(PublicObjects.getCurrentAllConnections().getCurrentActionTypeWithServer(CHANGE_OTHER_DEVICE_STATUS) == 1){
							PublicObjects.getCurrentAllConnections().stopCurrentActionTypeWithServer(CHANGE_OTHER_DEVICE_STATUS);
						}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(PublicObjects.getCurrentActivity() == 2){
						PublicObjects.getCurrentAllConnections().refresh();
				}
					
				
			}
            
		}
	}

	public class MyBinder extends Binder

	{
	      public ITSClient getService()

	     {
	    	  return ITSClient.this;  //返回service对象本身
	     }  
	}
	
	private MyBinder mBinder = new MyBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
}
