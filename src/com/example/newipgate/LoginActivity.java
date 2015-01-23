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
import android.os.Message;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("CommitPrefEdits")
public class LoginActivity extends Activity{

	
	private String username;
	private String password;
	private Switch charge;
	private Switch remember;

	private Encrypt encrypt;
	private TextView infoStr;
	private ITSClient itsClient;
	private Boolean hasbind = false;
	//private ProgressDialog progressDialog = null;
	private CustomProgressDialog customProgressDialog = null;
	private static boolean changeUser = false;
	private static boolean isTrying2ConnectServer = false;
	//private Interaction interaction;
	

	final private Handler saveInfoHint = new Handler(){
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	        case 0 :
				Toast.makeText(LoginActivity.this, "保存密码成功！", Toast.LENGTH_LONG).show();
	            break;
	        default :
	            break;
	        }
	    }
	};
	
	
	

	Handler AutoLoginHandler  = new Handler();
	Runnable AutoLogin = new Runnable() {
		
		public void run() {
			if(hasbind){
				loginServer(getCurrentFocus());
				//Finish();
				return;
			}
			else{
				AutoLoginHandler.postDelayed(AutoLogin, 10);  
			}

		}
	};
	

	Handler loginHintHandler  = new Handler();
	Runnable loginHint = new Runnable() {
		
		public void run() {
			if(!itsClient.isWebsocketConnected()){
				//System.out.println("in login hint");
				if(customProgressDialog != null && customProgressDialog.isShowing()){
					customProgressDialog.dismiss();
					Toast.makeText(LoginActivity.this, "未能连接服务器，请稍候再试", Toast.LENGTH_SHORT).show();
				}
				//ShowInfo("未能连接服务器，请稍候再试");
			}
			return;
		}
	};
	
	public static void setIsTrying2ConnectServer(Boolean isTrying){
		isTrying2ConnectServer = isTrying;
	}
	
	public static void setChangeUser(){
		changeUser = true;
	}
	
	public static boolean getChangeUser(){
		return changeUser;
	}
	
	private ServiceConnection mConn = new ServiceConnection()

	{
				public void onServiceConnected(ComponentName name,
						IBinder service) {
					
					itsClient=((ITSClient.MyBinder)service).getService();
					hasbind = true;
					
				}
				@Override
				public void onServiceDisconnected(ComponentName name) {
					itsClient = null;
					hasbind = false;
				}  
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		setContentView(R.layout.login_page);
		
		final ActionBar bar = getActionBar();
		Drawable actionBarBGDrawable = getResources().getDrawable(R.drawable.actionbarbg); 
		bar.setBackgroundDrawable(actionBarBGDrawable);
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle("北大北门@PKU");

		
		PublicObjects.setCurrentActivity(1);
		PublicObjects.setCurrentLoginActivity(LoginActivity.this);
		PublicObjects.setThisDeviceStatus(5);
		
		//check the connection status
		System.out.println("mainactivity oncreate");
		Intent intent = new Intent(this,ITSClient.class);
		bindService(intent, mConn, Context.BIND_AUTO_CREATE); 
		ITSClient.setLoginActivity(LoginActivity.this);
		
		
		//this piece of code is intentded to keep the menu bar in overflow
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
		//infoStr = (TextView)findViewById(R.id.info);
		
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
		System.out.println("The stored username is " + username + " and the password is " + password);
		((EditText)findViewById(R.id.usname)).setText(username);
		((EditText)findViewById(R.id.passwd)).setText(password);
		
		/*
		if(username != null && !username.equals("") && p != null && !p.equals("") && !changeUser)
		{
			PublicObjects.setThisDeviceStatus(5);
			//checkStatus();
			//loginServer(getCurrentFocus());
			AutoLoginHandler.post(AutoLogin);
		}
		*/

	
		
	}
	
	protected void onResume(){
		
		
		PublicObjects.setCurrentActivity(1);
		System.out.println("login activity onResume");
		super.onResume();
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE); 
		String p = sharedPre.getString("password", "");
		if(username != null && !username.equals("") && p != null && !p.equals("") && !changeUser)
		{
			PublicObjects.setThisDeviceStatus(5);
			AutoLoginHandler.post(AutoLogin);
		}
		
		
		
	}

	protected void onRestart(){
		
		System.out.println("login activity onRestart");
		super.onRestart();
	}
	
	protected void onStart(){
		
		System.out.println("login activity onStart");
		super.onStart();
	}
	

	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	        moveTaskToBack(true);
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	 protected void onDestroy() {  
	        unbindService(mConn);  
	        System.out.println("loginactivity destroyed");
	        super.onDestroy();  
	    }  
	  

	
	public void loginServer(View view){
		if(PublicObjects.getThisDeviceStatus() == -1){
			Toast.makeText(LoginActivity.this, "暂未获取此设备联网状态，请稍候再试", Toast.LENGTH_SHORT).show();
			return;
		}
		else if(!itsClient.isWebsocketConnected() && !isTrying2ConnectServer){
			isTrying2ConnectServer = true;
			//progressDialog = ProgressDialog.show(MainActivity.this, "提示", "正在登录……");
			customProgressDialog = CustomProgressDialog.createDialog(this);
            customProgressDialog.setMessage("Loading...");
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();
			itsClient.startWebSocket();
			loginHintHandler.postDelayed(loginHint, 7000);
			return;	
		}
		else if(itsClient.isWebsocketConnected()){
			
			changeActivity();
		}
		else{
			return;
		}
	}
	
	public void saveInfo(View v){
		System.out.println("save user info selected");
		saveUserInfo(getUsername(), getPassword());
	}
	
	public void changeActivity(){
		Intent intent = new Intent(LoginActivity.this, AllConnections.class);
		if(customProgressDialog != null && customProgressDialog.isShowing()){
			customProgressDialog.dismiss();
		}
		//用来处理连接路由器时一台设备断开了，另外的设备都会断开。
		itsClient.updateConnectionStatus();
		changeUser = false;
		startActivity(intent);
		//finish();
		
		
	}
	
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mainmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		  // Handle presses on the action bar items
		  return super.onOptionsItemSelected(item);

		//return true;
		
	}
	

	
	public String getUsername(){
		return 		((EditText)findViewById(R.id.usname)).getText().toString();

	}
	
	public String getPassword(){
		String returnPassword = ((EditText)findViewById(R.id.passwd)).getText().toString(); 
		System.out.println("in getpassword, the password is " + returnPassword);
		return 	returnPassword;

	}

	
	

	private void saveUserInfo(String u, String p)
	{
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE);  
		Editor editor = sharedPre.edit(); 
		editor.putString("username", u);
		System.out.println("in save, before encryption, the password is " + p);
		PublicObjects.setPassword(p);
		String encryptedPassword = encrypt.encrypt(p);
		editor.putString("password", encryptedPassword);
		System.out.println("in save, username is " + u + " the password is " + encryptedPassword);
		editor.commit();
		System.out.println("after commit, username is " + sharedPre.getString("username", "") + 
				" password is " + sharedPre.getString("password", ""));
		saveInfoHint.sendEmptyMessage(0);
		
	}
	
	
	
	public void connect(int type)
	{
		itsClient.connect(type);
	}
	
	public void disconnectAll()
	{
		itsClient.disconnectAll();
	}
	
	
	
	public void disconnectThis()
	{
		itsClient.disconnectThis();

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
	
	
	
	
}
