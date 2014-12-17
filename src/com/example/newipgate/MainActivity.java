package com.example.newipgate;



import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("CommitPrefEdits")
public class MainActivity extends Activity{

	
	private String username;
	private String password;
	private Switch charge;
	private Switch remember;

	private Encrypt encrypt;
	private TextView infoStr;
	private ITSClient itsClient;
	private ProgressDialog progressDialog = null;
	private static boolean isTrying2ConnectServer = false;
	//private Interaction interaction;

	Handler heartBeatHandler  = new Handler();
	Runnable sendHeartBeat = new Runnable() {
		
		public void run() {
			itsClient.sendHeartBeat();
			heartBeatHandler.postDelayed(sendHeartBeat, 300000);  

		}
	};
	

	Handler loginHintHandler  = new Handler();
	Runnable loginHint = new Runnable() {
		
		public void run() {
			if(!itsClient.isWebsocketConnected()){
				if(progressDialog != null && progressDialog.isShowing()){
					progressDialog.dismiss();
					Toast.makeText(MainActivity.this, "未能连接服务器，请稍候再试", Toast.LENGTH_SHORT).show();
				}
				//ShowInfo("未能连接服务器，请稍候再试");
			}
			return;
		}
	};
	
	public static void setIsTrying2ConnectServer(Boolean isTrying){
		isTrying2ConnectServer = isTrying;
	}

	/*
	Handler startWebsocketHandler = new Handler();
	Runnable startWebsocket = new Runnable() {
		
		public void run() {
			if(tryTimes == 0 && !itsClient.isWebsocketConnected()){
				System.out.println("try ivp6");
				itsClient.startWebSocket(1);
				//hasStartedTry = true;
				tryTimes = 1;
				startWebsocketHandler.postDelayed(startWebsocket, 8000);  
			}
				
			else if(tryTimes == 1 && !itsClient.isWebsocketConnected()){
				System.out.println("try ipv4");
				itsClient.startWebSocket(2);
				//hasStartedTry = false;
				tryTimes = 2;
				startWebsocketHandler.postDelayed(startWebsocket, 8000);  
				return;
			}
			else {
				tryTimes = 0;
				return;
			}

		}
	};
	
	
	
	public void setTrytimes(int times){
		tryTimes = times;
	}
	
	*/
	
	
	private ServiceConnection mConn = new ServiceConnection()

	{
				public void onServiceConnected(ComponentName name,
						IBinder service) {
					itsClient=((ITSClient.MyBinder)service).getService();
					
				}
				@Override
				public void onServiceDisconnected(ComponentName name) {
					itsClient = null;					
				}  
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_page);
		//setContentView(R.layout.activity_main);
		
		PublicObjects.setCurrentActivity(1);
		PublicObjects.setCurrentMainActivity(MainActivity.this);
		PublicObjects.setThisDeviceStatus(-1);
		
		//check the connection status
		
		
		System.out.println("mainactivity oncreate");
		Intent intent = new Intent(this,ITSClient.class);
		bindService(intent, mConn, Context.BIND_AUTO_CREATE); 
		ITSClient.setMainActivity(MainActivity.this);
		
		
		
		 try {
	         ViewConfiguration config = ViewConfiguration.get(this);
	         Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	         if(menuKeyField != null) {
	             menuKeyField.setAccessible(true);
	             menuKeyField.setBoolean(config, false);
	         }
	     } catch (Exception e) {
	         e.printStackTrace();
	     }
	
		encrypt = new Encrypt(this);
		infoStr = (TextView)findViewById(R.id.info);
		

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
		}
		//password = p.equals("") ? p : encrypt.decrypt(p);
		
		System.out.println("The stored username is " + username + " and the password is " + password);
		((EditText)findViewById(R.id.usname)).setText(username);
		((EditText)findViewById(R.id.passwd)).setText(password);
		
		
	}
	
	protected void onResume(){
		super.onResume();  
		PublicObjects.setThisDeviceStatus(-1);
		checkStatus(getCurrentFocus());
		
	}		

	
	public void loginServer(View view){
		if(PublicObjects.getThisDeviceStatus() == -1){
			Toast.makeText(MainActivity.this, "暂未获取此设备联网状态，请稍候再试", Toast.LENGTH_SHORT).show();
			return;
		}
		else if(!itsClient.isWebsocketConnected() && !isTrying2ConnectServer){
			isTrying2ConnectServer = true;
			progressDialog = ProgressDialog.show(MainActivity.this, "提示", "正在登录……");
			itsClient.startWebSocket();
			loginHintHandler.postDelayed(loginHint, 70000);
			return;	
		}
		else if(itsClient.isWebsocketConnected()){
			changeActivity();
		}
		else{
			return;
		}
	}
	
	public void changeActivity(){
		Intent intent = new Intent(MainActivity.this, AllConnections.class);
		if(progressDialog != null && progressDialog.isShowing()){
			progressDialog.dismiss();
		}
		startActivity(intent);
		
	}
	
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mainmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		  // Handle presses on the action bar items
		if(item.getItemId() == R.id.save_userinfo){
			System.out.println("refresh selected");
			saveUserInfo(getUsername(), getPassword());
		}
		  //return super.onOptionsItemSelected(item);

		return true;
		
	}
	
	/*
	public void ShowInfo(String content)
	{
		//infoStr = (TextView)findViewById(R.id.info);

		final String showContent = content;
		infoStr.post(new Runnable(){
			public void run() {
				infoStr.setText(showContent);
			}
		});

		//Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
	}
	*/

	
	public String getUsername(){
		return 		((EditText)findViewById(R.id.usname)).getText().toString();

	}
	
	public String getPassword(){
		return 		((EditText)findViewById(R.id.passwd)).getText().toString();

	}

	public void startHeartBeat(){
		heartBeatHandler.post(sendHeartBeat);
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
	
	
	public void updatePassword(String newPassword)
	{
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE);  
		Editor editor = sharedPre.edit(); 
		editor.remove("password");
		String newEncryptedPassword = encrypt.encrypt(newPassword);
		System.out.println("in update password, new password is "  + newEncryptedPassword);
		editor.putString("password", newEncryptedPassword);
		editor.commit();
	}
	
	
	public void connect(int type)
	{
		itsClient.connect(type);
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

	public void disconnectAll()
	{
		itsClient.disconnectAll();
	}
	
	public void disconnectAllButton(View view)
	{
		disconnectAll();
	}
	
	public void disconnectThis()
	{
		itsClient.disconnectThis();

	}
	
	public void disconnectThisButton(View view)
	{
		disconnectThis();
	}

	/*
	public void checkAllConnections(View view){
		if(!itsClient.isWebsocketConnected()){
			ShowInfo("服务器连接暂未建立，请稍候再试");
			if(tryTimes == 0){
				tryStartWebsocket();
			}
			return;
		}
		Intent intent = new Intent(MainActivity.this, AllConnections.class);
		startActivity(intent);
	}
	*/
	
	
	public void changePasswordToServer(View view){
		if(!itsClient.isWebsocketConnected()){
			//ShowInfo("服务器连接未建立，请稍候再试");
			return;
		}
		itsClient.sendChangePassword(getPassword());
	}
	
	/*
	public void refresh(View view){
		if(!itsClient.isWebsocketConnected()){
			ShowInfo("服务器连接未建立，请稍候再试");
			return;
		}
		itsClient.updateOtherDevice();
	}
	
	*/
	
	
	
	public void checkStatus(View view){
		//itsClient.updateOtherDevice();
		new Thread() {
			public void run() {
				 final int REQUEST_TIMEOUT = 2*1000;
				 final int SO_TIMEOUT = 2*1000; 
				String getaddr = "http://www.baidu.com";
				HttpGet get = new HttpGet(getaddr);
				BasicHttpParams httpParams = new BasicHttpParams();  
			    HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);  
			    HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);  
			    HttpClient client = new DefaultHttpClient(httpParams);  
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
				//未连接网关
				if(responseBody.equals("")){
					//ShowInfo("当前连接状态：未连接");
					PublicObjects.setThisDeviceStatus(1);
				}
				else {
					getaddr = "http://www.stackoverflow.com";
					HttpGet get2 = new HttpGet(getaddr);
					responseBody = "";
					try {
						responseBody = client.execute(get2, responseHandler);
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(responseBody.equals("")){
						//ShowInfo("当前连接状态：免费网址");
						PublicObjects.setThisDeviceStatus(3);
					}
					else{
						//ShowInfo("当前连接状态：收费网址");
						PublicObjects.setThisDeviceStatus(4);
					}
					
				}				
			}
		}.start();
	}
}
