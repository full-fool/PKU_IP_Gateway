package com.example.newipgate;
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

	
	private String username;
	private String password;
	private Switch charge;
	private Switch remember;

	private Encrypt encrypt;
	private TextView infoStr;
	private ITSClient itsClient;
	private Interaction interaction;

	Handler heartBeatHandler  = new Handler();
	Runnable sendHeartBeat = new Runnable() {
		
		public void run() {
			interaction.sendHeartBeat();
			heartBeatHandler.postDelayed(sendHeartBeat, 300000);  

		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		encrypt = new Encrypt(this);

		itsClient = new ITSClient(this);
		
		interaction = new Interaction(this);

		
		
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
	
	public void ShowInfo(String content)
	{
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
	
	public void startHeartBeat(){
		heartBeatHandler.post(sendHeartBeat);
	}
	
	public void startWebSocket()
	{
		interaction.startWebSocket();
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
			//itsClient.connect(2);
			connect(2);
		}
		else {
			//itsClient.connect(1);
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


	
}
