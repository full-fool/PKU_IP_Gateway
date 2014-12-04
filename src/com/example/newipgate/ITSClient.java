package com.example.newipgate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
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
import android.util.Log;





public class ITSClient{
	
	
	private static final String TAG = null;
	
	private DefaultHttpClient client;

	private static MainActivity mainActivity;
	
	private final WebSocketConnection wsc = new WebSocketConnection();
	
	private boolean websocketConnected = false; 
	
	
	public ITSClient(MainActivity thisActivity){
		mainActivity = thisActivity;
		
		InputStream ins = mainActivity.getResources().openRawResource(R.raw.ca);
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
		
		//infoStr = (TextView)findViewById(R.id.info);

		
	}
	
	public static  void setMainActivity(MainActivity thisActivity){
		mainActivity = thisActivity;
	}
	
	private int login() {
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
			String username = mainActivity.getUsername();
			String password = mainActivity.getPassword();
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
			if(response == null) 
				return -1;
			Header newLocation = response.getFirstHeader("Location");
			response.getEntity().consumeContent();
			while(newLocation != null)
			{
				String newLocationUrl = newLocation.getValue();
				System.out.println("the redirect url is " + newLocationUrl);
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
			
			//System.out.println("the itsresult is " + client.execute(get, responseHandler));
			//Log.i(TAG, responseBody);
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
					mainActivity.ShowInfo("密码错误");
					System.out.println("密码错误");
				}
				else if(response.contains("重新输入")) {
					mainActivity.ShowInfo("重新输入");
					System.out.println("重新输入");

				} else if(response.contains("达到设定值")) {
					mainActivity.ShowInfo("当前连接超过预定值");
					System.out.println("当前连接超过预定值");
				} else if(response.contains("不在申请访问服务的范围内")) {
					mainActivity.ShowInfo("不在申请访问服务的范围内");
					System.out.println("不在申请访问服务的范围内");
				} else if(response.contains("ipgw_open_Failed")) {
					mainActivity.ShowInfo("ipgw_open_Failed");
					System.out.println("ipgw_open_Failed");
				} else if(response.contains("连接成功"))
				{
					String [] resultStrings = response.split(">[012]个<", 0);
					if(ConnectType == 2){
						PublicObjects.changeThisDeviceStatus(4);

					}else {
						PublicObjects.changeThisDeviceStatus(3);
					}
					mainActivity.tryStartWebsocket();	
					if(resultStrings.length > 0)
					{
						int indexOfNext = response.indexOf(resultStrings[1]);
						final String connectionNum = response.substring(indexOfNext-3, indexOfNext-1);
						mainActivity.ShowInfo("连接成功" + "\n当前连接：" + connectionNum);
						System.out.println("连接成功" + "\n当前连接：" + connectionNum);
					}
					else 
					{
						mainActivity.ShowInfo("连接成功" + "\n未知错误，无法获取连接数");
						System.out.println("连接成功" + "\n未知错误，无法获取连接数");
					}
					//mainActivity.startHeartBeat();
					updateConnectionStatus();
				}
				else {
					mainActivity.ShowInfo("未知错误");
					System.out.println("未知错误");
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
				System.out.println("in disconnect all, the response is " + response);
				if(response.contains("断开成功") || response.contains("断开全部连接成功")) {
					PublicObjects.setThisDeviceStatus(2);
					mainActivity.ShowInfo("断开全部连接成功");
					System.out.println("断开全部连接成功");
					updateConnectionStatus();
				}else{
					mainActivity.ShowInfo("unknown error");					
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
				login();
				System.out.println("in disconnect this, the response is " + response);
				if(response.contains("断开成功")) {
					PublicObjects.setThisDeviceStatus(1);
					mainActivity.ShowInfo("网络断开成功");
					System.out.println("网络断开成功");
					updateConnectionStatus();
				}else{
					mainActivity.ShowInfo("unknown error");					
					System.out.println("unknown error");
				}
			}
		}.start();

	}

	
	//webSocket part
	

	
	public Boolean isWebsocketConnected(){
		return websocketConnected;
	}
	
	public void updateConnectionStatus(){
		if(websocketConnected){ 
		try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 1);
				requestInfo.put("content", PublicObjects.getThisDeviceStatus());
				wsc.sendTextMessage(requestInfo.toString());
				System.out.println("updateconnectionstatus successfully, the sent string is " + requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			System.out.println("send out heartbeat information");
			JSONObject requestInfo = new JSONObject();
			try {
				requestInfo.put("type", 2);
				requestInfo.put("content", PublicObjects.getThisDeviceStatus());
				wsc.sendTextMessage(requestInfo.toString());

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			System.out.println("the websocket is not established, so the heartbeat is not sent out");
		}

	}

	//initiate the websocket connection
	public void startWebSocket(int IPtype)
	{
		if(!websocketConnected){
		try {
			//System.out.println("in start websocket, the status is disconnect");
            String url = "";
            String MD5password = "";
			if(IPtype == 1){
            	url = "ws://[2001:da8:201:1146:6d69:95e2:9746:1d81]:9000/";
            }
			else{
				url = "ws://162.105.146.140:9000/";
			}
			
			try {
				MD5password = URLEncoder.encode(md5(mainActivity.getPassword()), "utf-8");
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			System.out.println("in startwebsocket, the user name is " + mainActivity.getUsername() + " password is " + MD5password +
					" status " + mainActivity.getIntendedStatus());
			wsc.connect(url + "login?student_id=" + mainActivity.getUsername() + "&password=" + MD5password + "&type=1&status=" + mainActivity.getIntendedStatus(), 
					new WebSocketConnectionHandler(){

                    @Override
                    public void onBinaryMessage(byte[] payload) {
                            System.out.println("onBinaryMessage size="+payload.length);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                            System.out.println("onClose code = " + code + " reason=" + reason);
                    }

                    @Override
                    public void onOpen() {
                    		websocketConnected = true;
                            System.out.println("onOpen");
                            try {
        						JSONObject requestInfo = new JSONObject();
        						requestInfo.put("type", 3);
        						requestInfo.put("content", JSONObject.NULL);
        						System.out.println("the send json string is " + requestInfo.toString());
        						
        						wsc.sendTextMessage(requestInfo.toString());
        						
        					} catch (JSONException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					}
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
	
	//change other device's status
	public void changeOtherDevice(String DeviceID, int newStatus)
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 4);
				JSONObject deviceInfo = new JSONObject();
				deviceInfo.put("device_id", DeviceID);
				deviceInfo.put("status", newStatus);
				requestInfo.put("content", deviceInfo.toString());
				System.out.println("in change other device, the sent string is " + requestInfo.toString());
				wsc.sendTextMessage(requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public void getOtherDevices(){
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 3);
				requestInfo.put("content", JSONObject.NULL);
				System.out.println("the send json string is " + requestInfo.toString());
				
				wsc.sendTextMessage(requestInfo.toString());
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	}
	
	
	//change the password
	private void sendChangePassword(String newPassword)
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 7);
				requestInfo.put("content", newPassword);
				wsc.sendTextMessage(requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	//update the information of other device
	public void updateOtherDevice()
	{
		try {
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", 6);
			requestInfo.put("content", JSONObject.NULL);
			wsc.sendTextMessage(requestInfo.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			
			if(infoType == 3){
				try {
					String connectionString = jsonObject.getString("content");
					//JSONObject connectionInfo = new JSONObject(connectionString);
					if(connectionString.startsWith("\ufeff")){
						System.out.println("starts with bom");
						connectionString = connectionString.substring(1);
					}
					JSONArray contents = new JSONArray(connectionString);
					System.out.println("there are " + contents.length() + "more devices");

					for(int i=0; i<contents.length(); i++)
					{
						PublicObjects.otherDevices[i].device_id = ((JSONObject)contents.get(i)).getString("device_id");
						PublicObjects.otherDevices[i].status = ((JSONObject)contents.get(i)).getInt("status");
						PublicObjects.otherDevices[i].type = ((JSONObject)contents.get(i)).getInt("type");
						PublicObjects.otherDevices[i].ip = ((JSONObject)contents.get(i)).getString("ip");
						PublicObjects.otherDevices[i].location = ((JSONObject)contents.get(i)).getString("location");
						PublicObjects.otherDevices[i].owner = ((JSONObject)contents.get(i)).getString("owner");
					}
					PublicObjects.otherDeviceNum = contents.length();
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
			//add new device
			else if(infoType == 102)
			{
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
					
					
					
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
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
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			
			//update other device's status
			else if(infoType == 101)
			{
				try {
					Boolean hasFind = false;
					JSONObject otherDeviceInfo = jsonObject.getJSONObject("content");
					String otherDeviceId = otherDeviceInfo.getString("device_id");
					int otherDeviceStatus = otherDeviceInfo.getInt("status");
					for(int i=0; i<4; i++)
					{
						if(otherDeviceId.equals(PublicObjects.otherDevices[i].device_id))
						{
							PublicObjects.otherDevices[i].status = otherDeviceStatus;
							hasFind = true;
							break;
						}
					}
					if(!hasFind)
						System.out.println("the update other device's status error");
					
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
					PublicObjects.setThisDeviceStatus(newStatus);
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
			
			else if(infoType == 107)
			{
				try {
					String newPassword = jsonObject.getString("content");
					mainActivity.updatePassword(newPassword);
						
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
            
		}
	}

}
