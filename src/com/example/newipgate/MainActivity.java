package com.example.newipgate;



import java.io.IOException;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.View;
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
	private Boolean hasStartedTry = false;
	//private Interaction interaction;
/*
	Handler heartBeatHandler  = new Handler();
	Runnable sendHeartBeat = new Runnable() {
		
		public void run() {
			itsClient.sendHeartBeat();
			heartBeatHandler.postDelayed(sendHeartBeat, 300000);  

		}
	};
*/
	Handler startWebsocketHandler = new Handler();
	Runnable startWebsocket = new Runnable() {
		
		public void run() {
			if(!hasStartedTry && !itsClient.isWebsocketConnected()){
				itsClient.startWebSocket(1);
				hasStartedTry = true;
				startWebsocketHandler.postDelayed(startWebsocket, 2000);  
			}
			else if(hasStartedTry && !itsClient.isWebsocketConnected()){
				itsClient.startWebSocket(2);
				hasStartedTry = true;
				return;
			}
			else{
				return;
			}
			

		}
	};
	
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
		setContentView(R.layout.activity_main);
		
		PublicObjects.setCurrentMainActivity(MainActivity.this);
		
		System.out.println("mainactivity oncreate");
		Intent intent = new Intent(this,ITSClient.class);
		bindService(intent, mConn, Context.BIND_AUTO_CREATE); 
		ITSClient.setMainActivity(MainActivity.this);
		/*
		if(!PublicObjects.isSet()){
			PublicObjects.initiateOtherDevice();

			itsClient = new ITSClient(this);
			
			PublicObjects.setItsClient(itsClient);
		}
			
		else{
			itsClient = PublicObjects.getItsClientwithActivity(this);
		}
		*/
		encrypt = new Encrypt(this);
		infoStr = (TextView)findViewById(R.id.info);

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
		
		
	}
	
	
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	public void ShowInfo(String content)
	{
		//infoStr = (TextView)findViewById(R.id.info);
		final String showContent = content;
		infoStr.post(new Runnable(){
			public void run() {
				infoStr.setText(showContent);
			}
		});
	}
	

	
	public String getUsername(){
		return 		((EditText)findViewById(R.id.usname)).getText().toString();

	}
	
	public String getPassword(){
		return 		((EditText)findViewById(R.id.passwd)).getText().toString();

	}
	
	public int getIntendedStatus(){
		charge = (Switch)findViewById(R.id.charge);
		if(charge.isChecked()){
			return 4;
		}
		else {
			return 3;
		}
		
	}
	/*
	public void startHeartBeat(){
		heartBeatHandler.post(sendHeartBeat);
	}
	*/
	
	public void tryStartWebsocket(){
		startWebsocketHandler.post(startWebsocket);
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

	public void checkAllConnections(View view){
		if(!itsClient.isWebsocketConnected()){
			ShowInfo("服务器连接未建立，请稍候再试");
			return;
		}
		Intent intent = new Intent(MainActivity.this, AllConnections.class);
		startActivity(intent);
	}
	
	public void changePasswordToServer(View view){
		itsClient.sendChangePassword(getPassword());
	}
	
	public void refresh(View view){
		itsClient.updateOtherDevice();
	}
	
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
					ShowInfo("当前连接状态：未连接");
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
						ShowInfo("当前连接状态：免费网址");
					}
					else{
						ShowInfo("当前连接状态：收费网址");
					}
					
				}
				
				System.out.println("the disconnect this response is  " + responseBody);
				
			}
		}.start();

	
	}

	
	
}
