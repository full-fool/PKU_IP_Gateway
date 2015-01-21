package com.example.newipgate;

import java.lang.reflect.Field;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.TextView;

public class WelcomePage extends Activity {
	private Encrypt encrypt;
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
		
		
		
	}
	
}
