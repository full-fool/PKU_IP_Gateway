package com.example.newipgate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.json.JSONTokener;



import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;


public class AllConnections extends Activity{
	//private ListView lv;
	private GridView gv;
	private ListView lv;
	private final int CHARGE = 4;
	private final int FREE = 3;
	private final int DISCONNECTTHIS = 1;
	private final int DISCONNECTALL = 2;
	private final int ANDROID = 1;
	private final int IPHONE = 2;
	private boolean hasRefreshed = false;
	private boolean isBusy = false;
	private int selectedItem = 0;
	//private ProgressDialog progressDialog = null;
	private CustomProgressDialog customProgressDialog = null;




	private Adapter adapter;
	int selectedOperation = 0; 
	private ITSClient itsClient;


	private ArrayList<HashMap<String, Object>> items=new ArrayList<HashMap<String, Object>>();
	
	
	Handler hasStatusChangedHandler = new Handler();
	Runnable hasStatusChanged = new Runnable() {
		
		public void run() {
			if(customProgressDialog != null && customProgressDialog.isShowing()){
				customProgressDialog.dismiss();
				Toast.makeText(AllConnections.this, "更改状态失败，请稍后再试", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	
	final private Handler refreshHandler = new Handler(){
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	        case 0 :
	        	refresh();
	            break;
	        default :
	            break;
	        }
	    }
	};
	
	private ServiceConnection mConn = new ServiceConnection()

	{
				public void onServiceConnected(ComponentName name,
						IBinder service) {
					itsClient=((ITSClient.MyBinder)service).getService();
					//itsClient.getOtherDevices();
					
				}
				@Override
				public void onServiceDisconnected(ComponentName name) {
					itsClient = null;					
				}  
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.all_connections);
		setContentView(R.layout.control);
		System.out.println("allConnections onCreate");
		final ActionBar bar = getActionBar();
		Drawable actionBarBGDrawable = getResources().getDrawable(R.drawable.actionbarbg); 
		bar.setBackgroundDrawable(actionBarBGDrawable);
		//bar.setIcon(R.drawable.logo);
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle("北大北门@PKU");
		Intent intent = new Intent(this,ITSClient.class);

		PublicObjects.setCurrentActivity(2);
		PublicObjects.setCurrentAllconnections(AllConnections.this);
		bindService(intent, mConn, Context.BIND_AUTO_CREATE); 
		
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
		
		refresh();
		
		//things happen after the user click on one of these items 
		
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view,
			  int position, long id) {
				
				
				//only when the websocket is established, then the item will be clicked
			if(!itsClient.isWebsocketConnected()){
				System.out.println("the websocket is not established");
				return;
				
			}
			
			
			 GridView gridView = (GridView)parent;
			 final int selectedPosition = position;  
			 HashMap<String, Object> map = (HashMap<String, Object>) gridView.getItemAtPosition(position);


			 //device_id只有在连接建立之后才有
			 final String device_id = map.get("device_id").toString();
			 selectedOperation = 0;
			 final String[] arrayFruit = new String[] { "连接免费网址", "连接收费网址", "断开连接"}; 
		        Dialog alertDialog = new AlertDialog.Builder(AllConnections.this). 
		                setTitle("请选择将要执行的操作"). 
		                setIcon(R.drawable.ic_launcher) 
		                .setSingleChoiceItems(arrayFruit, 0, new DialogInterface.OnClickListener() { 
		  
		                    @Override 
		                    public void onClick(DialogInterface dialog, int which) { 
		                        selectedOperation = which; 
		                    } 
		                }). 
		                
		                setNegativeButton("确认", new DialogInterface.OnClickListener() { 
		 
		                    @Override 
		                    public void onClick(DialogInterface dialog, int which) { 

		                    //connect free	
		                    
		                    customProgressDialog = CustomProgressDialog.createDialog(AllConnections.this);
		                    customProgressDialog.setMessage("loading...");
		                    customProgressDialog.setCancelable(false);
		                    customProgressDialog.show();
		    				hasStatusChangedHandler.postDelayed(hasStatusChanged, 5000);  


		                    if(selectedOperation == 0)
		                    {
		                    	//if the device is itself, execute the method and update status
		                    	if(selectedPosition== 0){
		                    		itsClient.connect(1);
		                    	}
		                    	//if the device is not itself, then updateOtherDeviceStatus
		                    	else {
		                    		itsClient.changeOtherDevice(device_id, FREE);
		                    		itsClient.getOtherDevices();
								}		                    	
		                    }
		                    //connect charge
		                    else if (selectedOperation == 1){
		                    	//if the device is itself, execute the method and update status
		                    	if(selectedPosition== 0){
		                    		itsClient.connect(2);
		                    	}
		                    	//if the device is not itself, then updateOtherDeviceStatus
		                    	else {
		                    		itsClient.changeOtherDevice(device_id, CHARGE);
		                    		itsClient.getOtherDevices();
								}	
							}
		                    else {
		                    	//if the device is itself, execute the method and update status
		                    	if(selectedPosition== 0){
		                    		itsClient.disconnectThis();
		                    	}
		                    	//if the device is not itself, then updateOtherDeviceStatus
		                    	else {
		                    		System.out.println("the device id is " + device_id + " is to be disconnected");
		                    		itsClient.changeOtherDevice(device_id, DISCONNECTTHIS);
		                    		itsClient.getOtherDevices();
								}	
							}
		                    hasRefreshed = false;
		                    //refreshHandler.post(activeRefresh);
		                    } 
		                }). setPositiveButton("取消", new DialogInterface.OnClickListener() { 
		 		                public void onClick(DialogInterface dialog, int which) { 
		                    } 
		                }). 
		                create(); 
		        alertDialog.show(); 
		    } 
		    
		    });
	    
		    
	}
	
	
	
	protected void onResume(){
		PublicObjects.setCurrentActivity(2);
		super.onResume();  
		checkStatus();
		
	}	
	

	
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			Intent intent= new Intent(Intent.ACTION_MAIN); 
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	 protected void onDestroy() {  
	        unbindService(mConn);  
	        System.out.println("allconnections destroyed");
	        super.onDestroy();  
	    }  
	  
	
	public void showInfo(String content){
		Toast.makeText(this, content, Toast.LENGTH_SHORT).show();

	}
	
	
	public void goBack()
	{
		Intent intent = new Intent(AllConnections.this, LoginActivity.class);
		startActivity(intent);
	}
	public void refresh(){
		//PublicObjects.printOtherDevices();
		if(customProgressDialog != null && customProgressDialog.isShowing()){
			customProgressDialog.dismiss();
		}
		items.clear();
		//lv = (ListView) findViewById(R.id.list);
		HashMap<String, Object> map = new HashMap<String, Object>();		
		map.put("device_id", PublicObjects.thisDeviceInfo.device_id);
		if(PublicObjects.getThisDeviceStatus() == DISCONNECTTHIS || PublicObjects.getThisDeviceStatus() == DISCONNECTALL)
		{
			map.put("status", "(本机)未连接");
			map.put("icon", getResources().getDrawable(R.drawable.androidoff));

		}
		else if(PublicObjects.getThisDeviceStatus() == FREE) {
			map.put("status", "(本机)已连接免费地址");
			map.put("icon", getResources().getDrawable(R.drawable.android));

		}
		else if(PublicObjects.getThisDeviceStatus() == CHARGE){
			map.put("status", "(本机)已连接收费地址");
			map.put("icon", getResources().getDrawable(R.drawable.android));

		}
		else if(PublicObjects.getThisDeviceStatus() == 5){
			map.put("status", "(本机)暂未获取状态");
			map.put("icon", getResources().getDrawable(R.drawable.androidoff));

		}
		else{
			map.put("status", "(本机)状态错误" + PublicObjects.thisDeviceInfo.status);
			map.put("icon", getResources().getDrawable(R.drawable.androidoff));

		}
		
		
		items.add(map);

		
		
		for (int i=0; i<PublicObjects.otherDeviceNum; i++) {
			
			HashMap<String, Object> newMap = new HashMap<String, Object>();
			if(PublicObjects.otherDevices[i].type == IPHONE)
			{
				newMap.put("icon", getResources().getDrawable(R.drawable.iphone));
			}
			else {
				if(PublicObjects.otherDevices[i].status == DISCONNECTTHIS || PublicObjects.otherDevices[i].status == DISCONNECTALL
						|| PublicObjects.otherDevices[i].status == 5){
					newMap.put("icon", getResources().getDrawable(R.drawable.androidoff));
				}
				else{
					newMap.put("icon", getResources().getDrawable(R.drawable.android));
				}
			}
			
			newMap.put("device_id", PublicObjects.otherDevices[i].device_id);
			//System.out.println("in refresh, the device_id is " + PublicObjects.otherDevices[i].device_id);
			
			if(PublicObjects.otherDevices[i].status == DISCONNECTTHIS || PublicObjects.otherDevices[i].status == DISCONNECTALL)
			{
				newMap.put("status", "未连接");
			}
			else if(PublicObjects.otherDevices[i].status == FREE) {
				newMap.put("status", "已连接免费地址");
			}
			else if(PublicObjects.otherDevices[i].status == CHARGE){
				newMap.put("status", "已连接收费地址");
			}
			else if(PublicObjects.otherDevices[i].status == 5){
				newMap.put("status", "暂未获取状态");
			}
			else{
				newMap.put("status", "状态错误" + PublicObjects.otherDevices[i].status);
			}
			items.add(newMap);
			

		}
		
		
		adapter = new Adapter(this, items, R.layout.app_item, new String[] {
				"icon"}, new int[] { R.id.icon});
		lv = (ListView)findViewById(R.id.listview);
		lv.setAdapter(adapter);
		//gv = (GridView) findViewById(R.id.gridview);
		//gv.setNumColumns(2);
		//gv.setAdapter(adapter);
	}
	
	
	public void checkStatus(){
		//itsClient.updateOtherDevice();
		System.out.println("check status called");
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
					if(PublicObjects.getThisDeviceStatus() != 1){
						PublicObjects.setThisDeviceStatus(1);
						itsClient.updateConnectionStatus();
						refreshHandler.sendEmptyMessage(0);	
					}
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
						if(PublicObjects.getThisDeviceStatus() != 3){
							PublicObjects.setThisDeviceStatus(3);
							itsClient.updateConnectionStatus();
							refreshHandler.sendEmptyMessage(0);	
							
						}
					}
					else{
						//ShowInfo("当前连接状态：收费网址");
						if(PublicObjects.getThisDeviceStatus() != 4){
							PublicObjects.setThisDeviceStatus(4);
							itsClient.updateConnectionStatus();
							refreshHandler.sendEmptyMessage(0);		
						}
					
					}
					
				}
				
			}
		}.start();

	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.allconnectionmenu, menu);
		return super.onCreateOptionsMenu(menu);
		//return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		  // Handle presses on the action bar items
		if(item.getItemId() == R.id.action_diconnectall){
			itsClient.disconnectAll();
		}
		else if(item.getItemId() == R.id.action_update){
			itsClient.updateOtherDevice();
		}
		else if(item.getItemId() == R.id.change_user){
			LoginActivity.setChangeUser();
			Intent intent = new Intent(AllConnections.this, LoginActivity.class);
			startActivity(intent);
			
		}
		
		  //return super.onOptionsItemSelected(item);
  
		return true;
		
	}
	

}
