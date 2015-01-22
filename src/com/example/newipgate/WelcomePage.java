package com.example.newipgate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.string;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomePage extends Activity {
	private Encrypt encrypt;
	private final String currentVersion = "1.0";
	private String responseBody = "";
	private String newVersion = "";
	private String description = "";
	private String downloadUrl = "";
	final private Handler refreshHandler = new Handler(){
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	        case 0 :
	        	new AlertDialog.Builder(WelcomePage.this) 
			 	.setTitle("有可用的更新"+newVersion)
			 	.setMessage(description)
			 	.setPositiveButton("是", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Uri uri = Uri.parse(downloadUrl);
						Intent it = new Intent(Intent.ACTION_VIEW, uri);
						startActivity(it);
					}
				})
			 	.setNegativeButton("否", null)
			 	.show();
	            break;
	        case 1 :
	        	chooseNextActivity();
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
		
	}
	
	private void chooseNextActivity(){
		String username = null;
		SharedPreferences sharedPre = this.getSharedPreferences("config", MODE_PRIVATE); 
		username = sharedPre.getString("username", "");		
		String password = sharedPre.getString("password", "");
		encrypt = new Encrypt(this);
		String undecryptedPassword = encrypt.decrypt(password);
		PublicObjects.setPassword(undecryptedPassword);
		System.out.println("welcomepage, user is "+username + " and passwd is "+ undecryptedPassword);
		
		if(username != null && !username.equals("") && undecryptedPassword != null && !undecryptedPassword.equals(""))
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
		System.out.println("check version is called");
		new Thread() {
			public void run() {
				 final int REQUEST_TIMEOUT = 2*1000;
				 final int SO_TIMEOUT = 2*1000; 
				String getaddr = "http://162.105.146.35:9000/assets/update/android.json";
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
				JSONObject jsonObject = null;
				try {
					jsonObject = new JSONObject(responseBody);
	                newVersion = jsonObject.getString("version");
	                description=  jsonObject.getString("description");
	                downloadUrl = jsonObject.getString("url");
	                
	                
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				if(!newVersion.equals(currentVersion) && !newVersion.equals("")){
					System.out.println("there is a new version");
					refreshHandler.sendEmptyMessage(0);	
				}
				else{
					System.out.println("current version is up-to-date");
					refreshHandler.sendEmptyMessage(1);	
				}
			
				
			}
		}.start();

	}
	
	
}
