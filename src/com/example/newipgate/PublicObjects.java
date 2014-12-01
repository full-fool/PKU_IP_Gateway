package com.example.newipgate;

import android.R.bool;
import android.widget.TextView;



public class PublicObjects {
	private static ITSClient publicItsClient;
	private static Interaction publicInteraction;
	public static DeviceInfo thisDeviceInfo = new DeviceInfo();
	public static DeviceInfo[] otherDevices = new DeviceInfo[4];
	public static int otherDeviceNum = 0;
	private static TextView infoStr;

	private static boolean isSet = false;

	public PublicObjects(){
		for(int i=0; i<4; i++){
			otherDevices[i] = new DeviceInfo();
		}
		isSet = true;
	}
	
	public static void setInfoStr(TextView v){
		infoStr = v;
	}
	
	public static TextView getInfoStr(){
		return infoStr;
	}
	
	public static boolean isSet(){
		return isSet;
	}
	
	public static void setInteraction(Interaction thisInteraction){
		publicInteraction = thisInteraction;
	}
	
	public static Interaction getInteraction(){
		return publicInteraction;
	}
	
	public static void setItsClient(ITSClient thisClient)
	{
		publicItsClient = thisClient;
	}
	
	public static ITSClient getItsClientwithActivity(MainActivity thisActivity)
	{
		ITSClient.setMainActivity(thisActivity);
		return publicItsClient;
	}
	
	public static Interaction getInteractionwithActivity(MainActivity thisActivity)
	{
		Interaction.setMainActivity(thisActivity);
		return publicInteraction;
	}
	
	public static ITSClient getItsClient()
	{
		return publicItsClient;
	}
	
	public static void setThisDeviceDeviceID(String _device_id)
	{
		thisDeviceInfo.device_id = _device_id;	
	}
	
	public static void setThisDeviceStatus(int _status)
	{
		thisDeviceInfo.status = _status;
	
	}
	
	public static void setThisDeviceType(int _type)
	{
		thisDeviceInfo.type = _type;
	}
	
	public static void setThisDeviceIp(String _ip)
	{
		thisDeviceInfo.ip = _ip;
	}
	
	public static void setThisDeviceLocation(String _location)
	{
		thisDeviceInfo.location = _location;
	
	}
	
	
	
	
	public static void changeThisDeviceStatus(int newStatus)
	{
		thisDeviceInfo.status = newStatus;
	}
	
	public static int getThisDeviceStatus()
	{
		return thisDeviceInfo.status;
	}
	
	public static String getThisDeviceID()
	{
		return thisDeviceInfo.device_id;
	}
	
	


}
