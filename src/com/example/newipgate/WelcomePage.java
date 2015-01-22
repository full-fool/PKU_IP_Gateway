package com.example.newipgate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomePage extends Activity {
	private Encrypt encrypt;
	private final String versionCode = "1.01";
	private String responseBody = "";

	
	
	
	
	final private Handler refreshHandler = new Handler(){
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	        case 0 :
				//Toast.makeText(WelcomePage.this, valueString, Toast.LENGTH_LONG).show();
	        	//AlertDialog.
	        	new AlertDialog.Builder(WelcomePage.this) 
			 	.setTitle("版本检查")
			 	.setMessage(responseBody)
			 	.setPositiveButton("是", null)
			 	.setNegativeButton("否", null)
			 	.show();
	            break;
	        default :
	            break;
	        }
	    }
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_page);
		
		
		final ActionBar bar = getActionBar();
		Drawable actionBarBGDrawable = getResources().getDrawable(R.drawable.actionbarbg); 
		bar.setBackgroundDrawable(actionBarBGDrawable);
		//bar.setIcon(R.drawable.logo);
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle("北大北门@PKU");
		
		
		
		PublicObjects.setCurrentActivity(0);
		PublicObjects.setCurrentWelcomeActivity(WelcomePage.this);
		
		checkVersion();
		
		/*
		String username = null;
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE); 
		username = sharedPre.getString("username", "");		
		String password = sharedPre.getString("password", "");
		encrypt = new Encrypt(this);
		PublicObjects.setPassword(encrypt.decrypt(password));
		
		if(username != null && !username.equals("") && password != null && !password.equals(""))
		{
			System.out.println("change to allconnections activity");
			PublicObjects.setThisDeviceStatus(5);
			Intent intent = new Intent(WelcomePage.this, AllConnections.class);
			startActivity(intent);
		}
		else{
			System.out.println("change to login Activity");
			Intent intent = new Intent(WelcomePage.this, LoginActivity.class);
			startActivity(intent);			
		}
		*/
		
		
		
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

	public void checkVersion(){
		//itsClient.updateOtherDevice();
		System.out.println("check version is called");
		new Thread() {
			public void run() {
				 final int REQUEST_TIMEOUT = 2*1000;
				 final int SO_TIMEOUT = 2*1000; 
				String getaddr = "http://162.105.146.35:9000/assets/update/android.xml";
				HttpGet get = new HttpGet(getaddr);
				BasicHttpParams httpParams = new BasicHttpParams();  
			    HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);  
			    HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);  
			    HttpClient client = new DefaultHttpClient(httpParams);  
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				responseBody = "";
				try {
					responseBody = client.execute(get, responseHandler);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//System.out.println("the response body is " + responseBody);

			
					//valueString = Charset.forName("UTF-8").encode(responseBody);
				//System.out.println(Charset.forName("UTF-8").encode(responseBody));
				
				//System.out.println("the response body is " + valueString);

				int index1 = responseBody.indexOf("<version>");
				int index2 = responseBody.indexOf("</version>");
				System.out.println(index1 + " " + index2);
				String newVersionCode = responseBody.substring(index1+9, index2);
				if(!newVersionCode.equals(versionCode)){
					System.out.println("new version code is " + newVersionCode);
					
				 	
					refreshHandler.sendEmptyMessage(0);	

				}
			
				
			}
		}.start();

	}
	
	
}
