package com.example.newipgate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

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



import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;


@SuppressLint("NewApi")
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

	//此结构用来存储当前AT与服务器交互的操作类型，每种操作最多发送一次，多余的阻塞。
	private int[] currentActionTypeWithServer = new int[10];
	private Adapter adapter;
	int selectedOperation = 0; 
	private ITSClient itsClient = null;

	//用来在输出提示信息时存储临时参数，例如下载文件名
	private String optionArg = "";


	private ArrayList<HashMap<String, Object>> items=new ArrayList<HashMap<String, Object>>();
	
	
	Handler hasStatusChangedHandler = new Handler();
	Runnable hasStatusChanged = new Runnable() {
		
		public void run() {
			if(customProgressDialog != null && customProgressDialog.isShowing()){
				customProgressDialog.dismiss();
				//Toast.makeText(AllConnections.this, "更改状态失败，请稍后再试", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	//这个handler存在仅仅是为了更新UI，因为会被子线程调用，所以必须使用handler来异步调用
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

	//显示信息的时候已经关掉了loading
	final private Handler showInfoHandler = new Handler(){
	    public void handleMessage(Message msg) {

	    	if(customProgressDialog != null && customProgressDialog.isShowing()){
				customProgressDialog.dismiss();
			}
	        switch (msg.what) {
	        //0:连接免费网址成功，1：连接收费网址成功，2：断开本机成功，3：断开所有成功, 4:密码错误
	        // 5: 超出预定值
	        case 0 :
				Toast.makeText(AllConnections.this, "成功连接免费网址", Toast.LENGTH_SHORT).show();
	            break;
	        case 1:
				Toast.makeText(AllConnections.this, "成功连接收费网址", Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(AllConnections.this, "成功断开本机连接", Toast.LENGTH_SHORT).show();
				break;
			case 3:
				Toast.makeText(AllConnections.this, "成功断开所有连接", Toast.LENGTH_SHORT).show();
				break;
			case 4:
				Toast.makeText(AllConnections.this, "密码错误", Toast.LENGTH_SHORT).show();
				break;
			case 5:
				Toast.makeText(AllConnections.this, "当前连接数超过预定值", Toast.LENGTH_SHORT).show();
				break;
			case 6:
				Toast.makeText(AllConnections.this, "连接错误", Toast.LENGTH_SHORT).show();
				break;
			case 7:
				Toast.makeText(AllConnections.this, optionArg + "下载成功", Toast.LENGTH_SHORT).show();
				break;
			case 8:
				Toast.makeText(AllConnections.this, optionArg + "下载失败", Toast.LENGTH_SHORT).show();
				break;
			case 9:
				Toast.makeText(AllConnections.this, "不在申请访问服务的范围内", Toast.LENGTH_SHORT).show();
				break;
			case 10:
				Toast.makeText(AllConnections.this, "已断开和服务器连接，请重新登录", Toast.LENGTH_SHORT).show();
				break;			
	        default :
	            break;
	        }
	    }
	};



	
	
	//在绑定完成时连接websocket服务器
	private ServiceConnection mConn = new ServiceConnection()

	{
				public void onServiceConnected(ComponentName name,
						IBinder service) {
					System.out.println("itsclient has been bound to allconnections");
					itsClient=((ITSClient.MyBinder)service).getService();
					if(!itsClient.isWebsocketConnected()){
						itsClient.startWebSocket(); 
					}
					checkStatus();

				}
				@Override
				public void onServiceDisconnected(ComponentName name) {
					itsClient = null;					
				}  
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control);
		System.out.println("allConnections onCreate");
		
		for(int i=0; i<8; i++){
			currentActionTypeWithServer[i] = -1;
		}


		PublicObjects.setCurrentActivity(2);
		PublicObjects.setCurrentAllconnections(AllConnections.this);
		
		//set up actionbar 
		final ActionBar bar = getActionBar();
		Drawable actionBarBGDrawable = getResources().getDrawable(R.drawable.actionbarbg); 
		bar.setBackgroundDrawable(actionBarBGDrawable);
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle("北大北门@PKU");
		
		//bind the itsclient service
		Intent intent = new Intent(this,ITSClient.class);
		bindService(intent, mConn, Context.BIND_AUTO_CREATE);
		System.out.println("after the binding of the itsclient service");
		
		//set up the overflow menubar 
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
		//lv是右边的listview
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view,
			  int position, long id) {
				//only when the websocket is established, then the item will be clicked
			if(!itsClient.isWebsocketConnected()){
				System.out.println("the websocket is not established");
				showInfo(10, null);
				return;
				
			}
		     selectedItem = position;
			 refreshMain(position);
		    } 
		    });
		
		
	}
	
	
	//判断是否已经连上websocket服务器使用的是isWebsoketConnected函数
	protected void onResume(){
		PublicObjects.setCurrentActivity(2);
		if(itsClient != null){
			checkStatus();
			if(!itsClient.isWebsocketConnected()){
				itsClient.startWebSocket();
			}
		}
		super.onResume();  
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
	  
	
	public void showInfo(int infoType, String optionalArg){
		//只有在当前界面是AllConnections时才显示相关提示信息
		if(PublicObjects.getCurrentActivity() != 2){
			return;
		}
		optionArg = optionalArg;
		showInfoHandler.sendEmptyMessage(infoType);
	}
	
	
	public void goBack()
	{
		Intent intent = new Intent(AllConnections.this, LoginActivity.class);
		startActivity(intent);
	}
	
	private void startLoading(){
		 customProgressDialog = CustomProgressDialog.createDialog(AllConnections.this);
	     customProgressDialog.setMessage("loading...");
	     customProgressDialog.setCancelable(false);
	     customProgressDialog.show();
	     hasStatusChangedHandler.postDelayed(hasStatusChanged, 10000); 
	}
	
	public void connectFree(View v){
		startLoading();
		if(selectedItem == 0){
    		itsClient.connect(1);
    	}
    	else {
    		HashMap<String, Object> tempHashMap = items.get(selectedItem);
    		String device_id = tempHashMap.get("device_id").toString();
    		itsClient.changeOtherDevice(device_id, FREE);
    		itsClient.getOtherDevices();
		}	
	}
	
	public void connectCharge(View v){
		startLoading();
		if(selectedItem == 0){
    		itsClient.connect(2);
    	}
    	else {
    		HashMap<String, Object> tempHashMap = items.get(selectedItem);
    		String device_id = tempHashMap.get("device_id").toString();
    		itsClient.changeOtherDevice(device_id, CHARGE);
    		itsClient.getOtherDevices();
		}	
	}
	
	
	public void disconnectThis(View v){
		startLoading();
		if(selectedItem == 0){
    		itsClient.disconnectThis();
    	}
    	else {
    		HashMap<String, Object> tempHashMap = items.get(selectedItem);
    		String device_id = tempHashMap.get("device_id").toString();
    		itsClient.changeOtherDevice(device_id, DISCONNECTTHIS);
    		itsClient.getOtherDevices();
		}		
	}
	
	
	public void disconnectAll(View v){
		startLoading();
		itsClient.disconnectAll();	
		
		
	}
	
	public void addDownloadTask(View v){
		 LayoutInflater factory = LayoutInflater.from(this); 
	        final View textEntryView = factory.inflate(R.layout.add_downloadtask, null); 
	        final EditText downloadUrlEditText = (EditText) textEntryView.findViewById(R.id.download_link); 
	        final EditText fileNameEditText = (EditText)textEntryView.findViewById(R.id.file_name); 
	        AlertDialog.Builder ad1 = new AlertDialog.Builder(AllConnections.this); 
	        ad1.setTitle("发起下载任务"); 
	        ad1.setIcon(R.drawable.download_dialog); 
	        ad1.setView(textEntryView); 
	        ad1.setPositiveButton("确定", new DialogInterface.OnClickListener() { 
	            public void onClick(DialogInterface dialog, int i) { 
	            	String downloadUrl = downloadUrlEditText.getText().toString();
	            	String fileName = fileNameEditText.getText().toString();
					String targetDeviceId = PublicObjects.otherDevices[selectedItem -1].device_id;
					System.out.println("the task info\n" + downloadUrl + "\n" + fileName + "\n" + targetDeviceId);
					PublicObjects.addDownloadTaskName(fileName);
					//在此处写入将要和服务器的交互
					itsClient.addDownloadTask(targetDeviceId, downloadUrl, fileName);
					
					
					
	            } 
	        }); 
	        ad1.setNegativeButton("取消", new DialogInterface.OnClickListener() { 
	            public void onClick(DialogInterface dialog, int i) { 
	   
	            } 
	        }); 
	        ad1.show();
	}
	
	public void setCurrentActionTypeWithServer(int actionType){
		currentActionTypeWithServer[actionType] = 1;
	}

	public void stopCurrentActionTypeWithServer(int actionType){
		currentActionTypeWithServer[actionType] = -1;
	}

	public int getCurrentActionTypeWithServer(int actionType){
		return currentActionTypeWithServer[actionType];
	}


	//此函数用来更新UI,所有的数据来源都是本地已存的信息，在publicObjects中
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
			//此处有修改，将第一个iphone的判断去掉了
			// if(PublicObjects.otherDevices[i].type == IPHONE)
			// {
			// 	newMap.put("icon", getResources().getDrawable(R.drawable.iphone));
			// }
			// else 
				if(PublicObjects.otherDevices[i].status == DISCONNECTTHIS || PublicObjects.otherDevices[i].status == DISCONNECTALL
						|| PublicObjects.otherDevices[i].status == 5){
					switch(PublicObjects.otherDevices[i].type){ 
					case 1: newMap.put("icon", getResources().getDrawable(R.drawable.androidoff)); break;
					case 2: newMap.put("icon", getResources().getDrawable(R.drawable.iphoneoff)); break;
					case 3: newMap.put("icon", getResources().getDrawable(R.drawable.winphoneoff)); break;
					case 4: newMap.put("icon", getResources().getDrawable(R.drawable.pcoff)); break;
					case 5: newMap.put("icon", getResources().getDrawable(R.drawable.linuxoff)); break;
					case 6: newMap.put("icon", getResources().getDrawable(R.drawable.macoff)); break;
					default:newMap.put("icon", getResources().getDrawable(R.drawable.pcoff));
					}
				}
				else{
					switch(PublicObjects.otherDevices[i].type){ 
					case 1: newMap.put("icon", getResources().getDrawable(R.drawable.android)); break;
					case 2: newMap.put("icon", getResources().getDrawable(R.drawable.iphone)); break;
					case 3: newMap.put("icon", getResources().getDrawable(R.drawable.winphone)); break;
					case 4: newMap.put("icon", getResources().getDrawable(R.drawable.pc)); break;
					case 5: newMap.put("icon", getResources().getDrawable(R.drawable.linux)); break;
					case 6: newMap.put("icon", getResources().getDrawable(R.drawable.mac)); break;
					default:newMap.put("icon", getResources().getDrawable(R.drawable.pc));
					}
				}
			
			
			newMap.put("device_id", PublicObjects.otherDevices[i].device_id);
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
	
		LinearLayout rightListView= (LinearLayout)findViewById(R.id.listviewlayout);
		if(items.size() == 0){
			rightListView.setPadding(10, 250, 10, 10);
		}
		else if(items.size() == 1){
			rightListView.setPadding(10, 200, 10, 10);
		}
		else if(items.size() == 2){
			rightListView.setPadding(10, 150, 10, 10);
		}
		else{
			rightListView.setPadding(10, 100, 10, 10);
		}
		
	
		adapter = new Adapter(this, items, R.layout.app_item, new String[] {
				"icon"}, new int[] { R.id.icon});
		lv = (ListView)findViewById(R.id.listview);
		lv.setAdapter(adapter);
		refreshMain(selectedItem);
	}
	
	//此函数用来刷新allconnections界面中的中间大界面。
	public void refreshMain(int position){
		ImageView bigIcon = (ImageView)findViewById(R.id.bigicon);
		TextView statusText = (TextView)findViewById(R.id.connectionState);
		//TextView deviceIdText = (TextView)findViewById(R.id.deviceID);
		HashMap<String, Object> tempHashMap = items.get(position);
		statusText.setText(tempHashMap.get("status").toString());
		//deviceIdText.setText(tempHashMap.get("device_id").toString());
		bigIcon.setImageDrawable((Drawable)tempHashMap.get("icon"));
		
		//设置“断开所有连接”的显示
		Button disconnectAllButton = (Button)findViewById(R.id.disconnect_all);
		Button addDownloadTaskButton = (Button)findViewById(R.id.add_downloadtask);
		
		if(position == 0){
			disconnectAllButton.setVisibility(View.VISIBLE);
			addDownloadTaskButton.setVisibility(View.GONE);
		}
		//其他电脑设备
		else if(PublicObjects.otherDevices[position -1].type >= 4) {
			//disconnectAllButton.setVisibility(View.VISIBLE);
			//addDownloadTaskButton.setVisibility(View.VISIBLE);
			//第一版本先不做下载
			disconnectAllButton.setVisibility(View.GONE);
			addDownloadTaskButton.setVisibility(View.GONE);
		}
		//其它手机设备
		else{
			addDownloadTaskButton.setVisibility(View.GONE);
			disconnectAllButton.setVisibility(View.GONE);
		}


	}
	public void resetSelectedItem(){
		selectedItem = 0;
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
		if(item.getItemId() == R.id.action_update){
				itsClient.updateOtherDevice();
				itsClient.getOtherDevices();		
		}
		else if(item.getItemId() == R.id.change_user){
			LoginActivity.setChangeUser();
			itsClient.stopWebSocket();
			Intent intent = new Intent(AllConnections.this, LoginActivity.class);
			startActivity(intent);
			
		}
		//隐藏下载功能
//		else if(item.getItemId() == R.id.check_download){
//			Intent intent = new Intent(AllConnections.this, DownloadPage.class);
//			startActivity(intent);			
//		}
		  //return super.onOptionsItemSelected(item);
		return true;
		
	}
	

}
