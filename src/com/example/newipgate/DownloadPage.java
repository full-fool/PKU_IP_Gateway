package com.example.newipgate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

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
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadPage extends Activity {
	private static final int DOWNLOAD_PENDING = 1;
	private static final int DOWNLOAD_SUCCEEDED = 2;
	private static final int DOWNLOAD_FAILED = 3;
	private static int selectedItemPosition = -1;
	
	
	private Adapter adapter;
	private ListView lv;
	private ArrayList<HashMap<String, Object>> items=new ArrayList<HashMap<String, Object>>();


	
	
	
	Handler checkUpdateHandler = new Handler();
	Runnable checkUpdateRunnable = new Runnable() {
		
		@Override
		public void run() {
			if(PublicObjects.getDownloadUpdate()){
				PublicObjects.setDownloadUpdate(false);
				refreshPage();
				checkUpdateHandler.postDelayed(checkUpdateRunnable, 1000);
			}
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_page);
		
		//设置actionBar样式
		final ActionBar bar = getActionBar();
		Drawable actionBarBGDrawable = getResources().getDrawable(R.drawable.actionbarbg); 
		bar.setBackgroundDrawable(actionBarBGDrawable);
		//bar.setIcon(R.drawable.logo);
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle("北大北门@PKU");
		
		lv = (ListView)findViewById(R.id.download_page_listview);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view,
			  int position, long id) {
				//only when the websocket is established, then the item will be clicked
				String tempTaskName = PublicObjects.getDownloadTaskNameWithIndex(position);
				selectedItemPosition = position;
				new AlertDialog.Builder(DownloadPage.this) 
			 	.setTitle("删除任务")
			 	.setMessage("您确定要删除任务 " + tempTaskName + " 吗?")
			 	.setPositiveButton("是", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (selectedItemPosition != -1){
							PublicObjects.deleteDownloadTaskWithIndex(selectedItemPosition);
							refreshPage();
						}
						else{
							System.out.println("the selected position is " + selectedItemPosition);
						}
					}
				})
			 	.setNegativeButton("否", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						return;
					}
				})
			 	.show();
		    	} 
		    });
		
		
		PublicObjects.addDownloadTaskName("testTask1");
		PublicObjects.addDownloadTaskName("testTask2");
		
	}
	
	
	
	//判断是否已经连上websocket服务器使用的是isWebsoketConnected函数
	protected void onResume(){
		refreshPage();
		checkUpdateHandler.post(checkUpdateRunnable);
		PublicObjects.setCurrentActivity(3);
		super.onResume();  
	}	
	
	public void refreshPage(){
		items.clear();
		int downloadTaskNum = PublicObjects.getDownloadTasksNum();
		for (int i=downloadTaskNum-1; i>=0; i--){
			HashMap<String, Object> map = new HashMap<String, Object>();		
			map.put("download_item_name", PublicObjects.getDownloadTaskNameWithIndex(i));
			if(PublicObjects.getDownloadTaskStatusWithIndex(i) == DOWNLOAD_FAILED){
				map.put("download_status_icon", getResources().getDrawable(R.drawable.cross));				
			}
			else if (PublicObjects.getDownloadTaskStatusWithIndex(i) == DOWNLOAD_SUCCEEDED){
				map.put("download_status_icon", getResources().getDrawable(R.drawable.tick));
			}
			else{
				map.put("download_status_icon", getResources().getDrawable(R.drawable.waiting));								
			}
			System.out.println("add map " + map + " status is " +  PublicObjects.getDownloadTaskStatusWithIndex(i));
			items.add(map);
		}
		//items.add(map);
		

		adapter = new Adapter(this, items, R.layout.download_item, new String[] {
		"download_item_name", "download_status_icon"}, new int[] { R.id.download_item_name, R.id.download_item_status});
		
		lv.setAdapter(adapter);
		
	}
	
	
}
