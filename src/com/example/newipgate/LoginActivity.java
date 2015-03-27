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

import android.net.Uri;
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
	private ITSClient itsClient = null;
	private Boolean hasbind = false;
	//private ProgressDialog progressDialog = null;
	private CustomProgressDialog customProgressDialog = null;
	private static boolean changeUser = false;
	private static boolean isTrying2ConnectServer = false;
	private int loginStatusCode = -100;
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
	
	Handler testNewPasswdHandler  = new Handler();
	Runnable testNewPasswd = new Runnable() {
		public void run() {
			if(!itsClient.isWebsocketConnected()){
				Uri uri = Uri.parse("162.105.146.35:9000/assets/updatepw.html");
				Intent it = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(it);
			}
			return;
		}
	};
	

	Handler loginITSHandler  = new Handler();
	Runnable loginITS = new Runnable() {
		
		public void run() {
			if(loginStatusCode == 1314){
				if(customProgressDialog != null && customProgressDialog.isShowing()){
					customProgressDialog.dismiss();
				}
				Toast.makeText(LoginActivity.this, "密码错误", Toast.LENGTH_LONG).show();
			}
			else if(loginStatusCode == -1 || loginStatusCode == -2 || loginStatusCode == -100){
				if(customProgressDialog != null && customProgressDialog.isShowing()){
					customProgressDialog.dismiss();
				}
				Toast.makeText(LoginActivity.this, "未能连接，请检查您的网络连接状态", Toast.LENGTH_SHORT).show();
			}
			//此处表示登陆成功，
			else{
				//此处用新密码登陆，即输入框里的密码
				itsClient.startWebSocket();
				//testNewPasswdHandler.postDelayed(testNewPasswd, 2000);
				

			}
			return;
		}
	};


////	public static void showInfo(String content){
////		Toast.makeText(LoginActivity.this, content, Toast.LENGTH_SHORT).show();
////	}
//	
	
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
	        //moveTaskToBack(true);
	        //return true;
	        Intent intent= new Intent(Intent.ACTION_MAIN); 
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	 protected void onDestroy() {  
	        unbindService(mConn);  
	        System.out.println("loginactivity destroyed");
	        super.onDestroy();  
	    }  
	  

	
	public void loginServer(View view){
		PublicObjects.setCurrentUsername(getUsername());
		PublicObjects.setCurrentPassword(getPassword());
		if(itsClient != null && !itsClient.isWebsocketConnected() && !isTrying2ConnectServer){
			//isTrying2ConnectServer = true;
			customProgressDialog = CustomProgressDialog.createDialog(this);
            customProgressDialog.setMessage("Loading...");
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();

            new Thread(){
            	public void run(){
        			loginStatusCode = itsClient.login();
            	}
            }.start();

            //itsClient.startWebSocket();
			//loginHintHandler.postDelayed(loginHint, 2000);
            loginITSHandler.postDelayed(loginITS, 2000);
			return;	
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
		return  ((EditText)findViewById(R.id.usname)).getText().toString();

	}
	
	public String getPassword(){
		return  ((EditText)findViewById(R.id.passwd)).getText().toString(); 
	}

	
	

	private void saveUserInfo(String u, String p)
	{
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE);  
		Editor editor = sharedPre.edit(); 
		editor.putString("username", u);
		System.out.println("in save, before encryption, the password is " + p);
		String encryptedPassword = encrypt.encrypt(p);
		editor.putString("password", encryptedPassword);
		System.out.println("in save, username is " + u + " the password is " + encryptedPassword);
		editor.commit();
		PublicObjects.setSavedUsername(u);
		PublicObjects.setSavedPassword(p);
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
