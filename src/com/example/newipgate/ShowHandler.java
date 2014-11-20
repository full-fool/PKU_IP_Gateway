package com.example.newipgate;
import java.lang.ref.WeakReference;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.Toast;


public class ShowHandler extends Handler{
	//WeakReference<PKUer> pkuer;
	/*
	public static final int DIALOG= 1;
	public static final int TOAST = 2;
	
	//public ShowHandler(PKUer service) {
		//pkuer = new WeakReference<PKUer>(service);
	//}
	@Override
	public void handleMessage(Message msg) {
		
		PKUer service = pkuer.get();
		if(service == null) {
			return;
		}
		switch(msg.what) {
		case DIALOG:
			String[] info = ((String)msg.obj).split("@split@");
			String title = info[0];
			String message = info[1];
			AlertDialog.Builder builder = new Builder(service);
			builder.setTitle(title);
			builder.setMessage(message);
			builder.setPositiveButton("OK", null);
			final AlertDialog dialog = builder.create();
			dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			dialog.show();
			break;
		case TOAST:
			message = (String)msg.obj;
			Toast.makeText(service, message, Toast.LENGTH_LONG).show();
			break;
		}
	}
*/	
}