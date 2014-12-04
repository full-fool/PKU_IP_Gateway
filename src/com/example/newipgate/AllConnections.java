package com.example.newipgate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;


public class AllConnections extends Activity{
	private ListView lv;
	private final int CHARGE = 4;
	private final int FREE = 3;
	private final int DISCONNECTTHIS = 1;
	private final int DISCONNECTALL = 2;
	private final int ANDROID = 1;
	private final int IPHONE = 2;


	private Adapter adapter;
	int selectedOperation = 0; 
	private ITSClient itsClient;


	private ArrayList<HashMap<String, Object>> items=new ArrayList<HashMap<String, Object>>();
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.all_connections);
		
		
		System.out.println("All Connections oncreate");
		itsClient = PublicObjects.getItsClient();
		
		
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
			 ListView listView = (ListView)parent;
			 final int selectedPosition = position;  
			 HashMap<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
			 //String deviceStatus = map.get("status").toString();  
			 //device_id只有在连接建立之后才有
			 final String device_id = map.get("device_id").toString();
			 //System.out.println("the touched device_id is " + device_id);
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
	
	/*
	protected void onDestroy(){
		super.onDestroy();
		System.out.println("allConnections destroied");
	}
	*/
	
	public void goBack(View view)
	{
		Intent intent = new Intent(AllConnections.this, MainActivity.class);
		startActivity(intent);
	}
	
	private void refresh(){
		
		itsClient.getOtherDevices();
		items.clear();
		lv = (ListView) findViewById(R.id.list);
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("icon", getResources().getDrawable(R.drawable.android));
		map.put("device_id", PublicObjects.thisDeviceInfo.device_id);
		if(PublicObjects.thisDeviceInfo.status == DISCONNECTTHIS || PublicObjects.thisDeviceInfo.status == DISCONNECTALL)
		{
			map.put("status", "(本机)未连接");
		}
		else if(PublicObjects.thisDeviceInfo.status == FREE) {
			map.put("status", "(本机)已连接免费地址");
		}
		else if(PublicObjects.thisDeviceInfo.status == CHARGE){
			map.put("status", "(本机)已连接收费地址");
		}
		else{
			map.put("status", "(本机)状态错误" + PublicObjects.thisDeviceInfo.status);
		}
		
		items.add(map);

		for (int i=0; i<PublicObjects.otherDeviceNum; i++) {
			
			HashMap<String, Object> newMap = new HashMap<String, Object>();
			if(PublicObjects.otherDevices[i].type == IPHONE)
			{
				newMap.put("icon", getResources().getDrawable(R.drawable.iphone));
			}
			else {
				newMap.put("icon", getResources().getDrawable(R.drawable.android));
			}
			
			newMap.put("device_id", PublicObjects.otherDevices[i].device_id);
			
			if(PublicObjects.otherDevices[i].status == DISCONNECTTHIS )
			{
				newMap.put("status", "未连接");
			}
			else if(PublicObjects.otherDevices[i].status == FREE) {
				newMap.put("status", "已连接免费地址");
			}
			else if(PublicObjects.otherDevices[i].status == CHARGE){
				newMap.put("status", "已连接收费地址");
			}
			else{
				newMap.put("status", "状态错误" + PublicObjects.otherDevices[i].status);
			}
			items.add(newMap);

		}
		
		adapter = new Adapter(this, items, R.layout.app_item, new String[] {
				"icon", "status", "device_id"}, new int[] { R.id.icon,R.id.connectionState, R.id.deviceID});
		
		lv.setAdapter(adapter);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		  // Handle presses on the action bar items
		  refresh();
		  return true;
		
	}
	

}
