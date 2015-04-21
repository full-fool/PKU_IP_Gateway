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
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadPage extends Activity {
	private Adapter adapter;
	private ListView lv;
	private ArrayList<HashMap<String, Object>> items=new ArrayList<HashMap<String, Object>>();



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
		
		
		
	}
	
	
	
	//判断是否已经连上websocket服务器使用的是isWebsoketConnected函数
	protected void onResume(){
		items.clear();
		int downloadTaskNum = PublicObjects.getDownloadTasksNum();
		for (int i=0; i<downloadTaskNum; i++){
			HashMap<String, Object> map = new HashMap<String, Object>();		
			map.put("download_item_name", PublicObjects.getDownloadTaskNameWithIndex(i));
			System.out.println("add map " + map);
			items.add(map);
		}
		//items.add(map);
		

		adapter = new Adapter(this, items, R.layout.download_item, new String[] {
		"download_item_name"}, new int[] { R.id.download_item_name});
		lv = (ListView)findViewById(R.id.download_page_listview);
		lv.setAdapter(adapter);
		super.onResume();  
	}	
	
	
	
	
}
