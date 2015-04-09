package com.example.newipgate;

import org.json.JSONException;
import org.json.JSONObject;

public class InteractionInfo {
	private final static int UPDATE_CONNECTION_STATUS = 1;
	private final static int HEART_BEAT = 2;
	private final static int GET_OTHER_DEVICES_TYPE = 3;
	private final static int CHANGE_OTHER_DEVICES = 4;
	private final static int UPDATE_OTHER_DEVICES = 6;	
	private final static int SEND_CHANGE_PASSWORD = 7;
	private final static int ANNUL_FORMER_CONNECTION = 8;
	private final static int ADD_DOWNLOAD_TASK = 9;


	public static String formGetOtherDevices(){
		String returnResult=""; 
		try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", GET_OTHER_DEVICES_TYPE);
				requestInfo.put("content", JSONObject.NULL);
				returnResult = requestInfo.toString();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return returnResult;
	}
	
	public static String formSendChangePassword(String newPassword){
		String returnResult=""; 
		try {
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", SEND_CHANGE_PASSWORD);
			requestInfo.put("content", newPassword);
			returnResult = requestInfo.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
			return returnResult;
	}
	
	public static String formUpdateOtherDevice()
	{
		String returnResult=""; 
		try {
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", UPDATE_OTHER_DEVICES);
			requestInfo.put("content", JSONObject.NULL);
			returnResult = requestInfo.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return returnResult;
	}
	
	public static String formChangeOtherDevice(String DeviceID, int newStatus)
	{
		String returnResult=""; 
		System.out.println("in form change other devices, the device id is " + DeviceID + " and new status is " + newStatus);
		try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", CHANGE_OTHER_DEVICES);
				JSONObject deviceInfo = new JSONObject();
				deviceInfo.put("device_id", DeviceID);
				deviceInfo.put("status", newStatus);
				requestInfo.put("content", deviceInfo.toString());
				returnResult = requestInfo.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		return returnResult;
	}
	
	public static String formHeartBeat(){
		String returnResult=""; 
		try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", HEART_BEAT);
				requestInfo.put("content", PublicObjects.getThisDeviceStatus());
				returnResult = requestInfo.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		return returnResult;
	}
	
	public static String formUpdateConnectionStatus(){
		String returnResult=""; 
		try {
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", UPDATE_CONNECTION_STATUS);
			requestInfo.put("content", PublicObjects.getThisDeviceStatus());
			returnResult = requestInfo.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		return returnResult;
	}
	
	
	public static String formAnnulFormerConnection(){
		if(PublicObjects.getThisDeviceID() == null){
			return "no former connect";
		}
		String returnResult=""; 
		try {
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", ANNUL_FORMER_CONNECTION);
			requestInfo.put("content", PublicObjects.getThisDeviceID());
			returnResult = requestInfo.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		return returnResult;
	}

	public static String formAddDownloadTask(String DeviceID, String url, String fileName){
		String returnResult = "";
		try{
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", ADD_DOWNLOAD_TASK);
			JSONObject downloadInfo = new JSONObject();
			downloadInfo.put("device_id", DeviceID);
			downloadInfo.put("url", url);
			downloadInfo.put("name", fileName);

			requestInfo.put("content", downloadInfo.toString());
			returnResult = requestInfo.toString();
		} catch(JSONException e){
			e.printStackTrace();
		}
		return returnResult;

	}
}
