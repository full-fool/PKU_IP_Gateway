package com.example.newipgate;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
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
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.newipgate.WebSocketPart.WebSocketConnection;
import com.example.newipgate.WebSocketPart.WebSocketConnectionHandler;
import com.example.newipgate.WebSocketPart.WebSocketException;


import android.util.Log;


import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

@SuppressLint("CommitPrefEdits")
public class MainActivity extends Activity {

	
	private static final String TAG = null;
	//private Encrypt encrypt;
	private String username;
	private String password;
	private Switch charge;
	private Switch remember;
	private TextView infoStr;
	private Encrypt encrypt;
	private boolean websocketConnected = false; 
	private final WebSocketConnection wsc = new WebSocketConnection();
	private DeviceInfo thisDeviceInfo = new DeviceInfo();
	private DeviceInfo[] otherDevices = new DeviceInfo[4];
	private int otherDeviceNum = 0;
	Handler heartBeatHandler  = new Handler();
	Runnable sendHeartBeat = new Runnable() {
		
		public void run() {
			if(websocketConnected)
			{
				System.out.println("send out heartbeat information");
				JSONObject requestInfo = new JSONObject();
				try {
					requestInfo.put("type", 2);
					requestInfo.put("content", thisDeviceInfo.status);
					wsc.sendTextMessage(requestInfo.toString());

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				System.out.println("the websocket is not established, so the heartbeat is not sent out");
			}
			heartBeatHandler.postDelayed(sendHeartBeat, 300000);  

		}
	};



	private DefaultHttpClient client;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		encrypt = new Encrypt(this);
		InputStream ins = this.getResources().openRawResource(R.raw.ca);
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
		
		
		
		//encrypt = new Encrypt(this);
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE); 
		username = sharedPre.getString("username", "");		
		String p = sharedPre.getString("password", "");
		System.out.println("in onCreate, the encrypted password is " + p);
		if(p.equals(""))
		{
			password = "";
		}
		else {
			password = encrypt.decrypt(p);
			//password = "";
		}
		//password = p.equals("") ? p : encrypt.decrypt(p);
		
		System.out.println("The stored username is " + username + " and the password is " + password);
		((EditText)findViewById(R.id.usname)).setText(username);
		((EditText)findViewById(R.id.passwd)).setText(password);
		
		infoStr = (TextView)findViewById(R.id.info);
		
		
	}


	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	private void changeOtherDevice(String DeviceID, int newStatus)
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 4);
				JSONObject deviceInfo = new JSONObject();
				deviceInfo.put("device_id", DeviceID);
				deviceInfo.put("status", newStatus);
				requestInfo.put("content", deviceInfo);
				wsc.sendTextMessage(requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
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

	private void saveUserInfo(String u, String p)
	{
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE);  
		Editor editor = sharedPre.edit(); 
		editor.putString("username", u);
		String encryptedPassword = encrypt.encrypt(p);
		editor.putString("password", encryptedPassword);
		System.out.println("in save, username is " + u + " the password is " + encryptedPassword);
		editor.commit();
		System.out.println("after commit, username is " + sharedPre.getString("username", "") + 
				" password is " + sharedPre.getString("password", ""));
	}
	
	
	private void updatePassword(String newPassword)
	{
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE);  
		Editor editor = sharedPre.edit(); 
		editor.remove("password");
		String newEncryptedPassword = encrypt.encrypt(newPassword);
		System.out.println("in update password, new password is "  + newEncryptedPassword);
		editor.putString("password", newEncryptedPassword);
		editor.commit();
	}
	
	
	private void updateOtherDevice()
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
	
	private void startWebSocket()
	{
		try {
            
            wsc.connect("ws://162.105.146.140:9000/login?student_id=1100012950&password=11223344433&type=1&status=3", new WebSocketConnectionHandler(){

                    @Override
                    public void onBinaryMessage(byte[] payload) {
                            System.out.println("onBinaryMessage size="+payload.length);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                            System.out.println("onClose code = " + code + " reason="+reason);
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
                            
                            //wsc.sendTextMessage("{\"type\":3, \"content\":null}");
                            //wsc.disconnect();
                    }

                    @Override
                    public void onRawTextMessage(byte[] payload) {
                            System.out.println("onRawTextMessage size="+payload.length);
                            
                            
                    }

                    @Override
                    public void onTextMessage(String payload) {
                    		System.out.println("onTextMessage"+payload);
                            if(Pattern.matches("[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+", payload))
                            {
                            	thisDeviceInfo.device_id = payload;
                            	thisDeviceInfo.type = 1;
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
								
								//add new device
								if(infoType == 102)
								{
									try {
										JSONObject connectionInfo = jsonObject.getJSONObject("content");
										otherDevices[otherDeviceNum].device_id = connectionInfo.getString("device_id");
										otherDevices[otherDeviceNum].status = connectionInfo.getInt("status");
										otherDevices[otherDeviceNum].type = connectionInfo.getInt("type");
										otherDevices[otherDeviceNum].ip = connectionInfo.getString("ip");
										otherDevices[otherDeviceNum].location = connectionInfo.getString("location");
										otherDeviceNum++;
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
										for(int i=0; i<4; i++)
										{
											if(deletedDeviceId.equals(otherDevices[i].device_id))
											{
												otherDevices[i].device_id = null;
												otherDeviceNum--;
												hasFind = true;
												break;
											}
										}
										if(!hasFind)
											System.out.println("the delete device error");
										
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
											if(otherDeviceId.equals(otherDevices[i].device_id))
											{
												otherDevices[i].status = otherDeviceStatus;
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
										thisDeviceInfo.status = newStatus;
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
											requestInfo.put("content", thisDeviceInfo.status);
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
										updatePassword(newPassword);
										
									} catch (JSONException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
								
								
                                
							}
                            
                    }
            });
            
    } catch (WebSocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    }

	}
	
	
	public void connectButton(View view)
	{
		charge = (Switch)findViewById(R.id.charge);
		remember = (Switch)findViewById(R.id.remember);
		final boolean charge_or_not = charge.isChecked();
		final boolean remember_or_not = remember.isChecked();
		username = ((EditText)findViewById(R.id.usname)).getText().toString();
		password = ((EditText)findViewById(R.id.passwd)).getText().toString();
		if(remember_or_not)
		{
			System.out.println("User chooses to store username " + username + " and password " + password);
			saveUserInfo(username, password);
		}
		if(charge_or_not)
		{
			connect(2);
		}
		else {
			connect(1);
		}
	}
	
	//connectType equals 1 when this is a free connect, else equals 2.
	private void connect(int connectType) 
	{
		final int ConnectType = connectType;
		new Thread() {
			public void run() {
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=open&type=";
				int random = Math.abs((new Random()).nextInt() % 1000);
				if(ConnectType == 2)
				{
					getaddr += "fee&fr=0&sid=" + random;
					thisDeviceInfo.status = 4;
				}
				else {
					getaddr += "free&fr=0&sid=701";
					thisDeviceInfo.status = 3;
				}
				String response = cmd(getaddr);
				if(response.contains("wrong password")){
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("密码错误");
						}
					});
				}
				else if(response.contains("重新输入")) {

					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("重新输入");
						}
					});
				} else if(response.contains("达到设定值")) {
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("当前连接超过预定值");
						}
					});
				} else if(response.contains("不在申请访问服务的范围内")) {
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("不在申请访问服务的范围内");
						}
					});
				} else if(response.contains("ipgw_open_Failed")) {
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("ipgw_open_Failed");
						}
					});
				} else if(response.contains("连接成功"))
				{
					//System.out.println("in 连接成功, the response is " + response);
					heartBeatHandler.post(sendHeartBeat);
					String [] resultStrings = response.split(">[012]个<", 0);
					
					if(resultStrings.length > 0)
					{
						int indexOfNext = response.indexOf(resultStrings[1]);
						final String connectionNum = response.substring(indexOfNext-3, indexOfNext-1);
						infoStr.post(new Runnable(){
							public void run() {
								infoStr.setText("连接成功" + "\n当前连接：" + connectionNum);
							}
						});
						startWebSocket();	
					}
					else 
					{
						infoStr.post(new Runnable(){
							public void run() {
								infoStr.setText("连接成功" + "\n未知错误，无法获取连接数");
							}
						});
					}
				}
				else {
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("未知错误");
						}
					});
				}
			}
		}.start();
	}
	
	
	public void disconnectAllButton(View view)
	{
		disconnectAll();
	}
	
	
	
	private void disconnectAll() {
		new Thread() {
			public void run() {
				int random = Math.abs((new Random()).nextInt() % 1000);
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=close&type=allconn&fr=0&sid" + random;
				username = ((EditText)findViewById(R.id.usname)).getText().toString();
				password = ((EditText)findViewById(R.id.passwd)).getText().toString();
				String response = cmd(getaddr);
				System.out.println("in disconnect all, the response is " + response);
				if(response.contains("断开成功")) {
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("断开全部连接成功");
						}
					});
				}else{
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("unknown error");
						}
					});
				}
			}
		}.start();

	}
	
	public void disconnectThisButton(View view)
	{
		disconnectThis();
	}
	
	
	private void disconnectThis() {
		new Thread() {
			public void run() {
				double random = (new Random()).nextDouble() * 100;
				String getaddr = "http://its.pku.edu.cn/disconnetwireless.do?t=" + random;
				HttpGet get = new HttpGet(getaddr);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String responseBody = "";
				try {
					responseBody = client.execute(get, responseHandler);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("the disconnect this response is  " + responseBody);
				
				if(responseBody.contains("SUCCESS=YES")) {
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("网络断开成功");
						}
					});
				} else{
					
					infoStr.post(new Runnable(){
						public void run() {
							infoStr.setText("unknown error");
						}
					});
				}
			}
		}.start();

	}

	
	
	public void checkState(View veiw) {
		/*
		new Thread() {
			public void run() {
				HttpGet get = new HttpGet("http://www.baidu.com");
				try {
					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					String responseBody = client.execute(get, responseHandler);
					System.out.println("the baidu is " + responseBody.substring(0, 100));
					
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					get.abort();
				}

				
			}
		}.start();
		*/
		
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
		 System.out.println("this device id is " + thisDeviceInfo.device_id);
		
	}

	
}
