package com.example.newipgate;

import android.widget.TextView;



public class PublicObjects {
	private static ITSClient publicItsClient;
	public static DeviceInfo thisDeviceInfo = new DeviceInfo();
	public static DeviceInfo[] otherDevices = new DeviceInfo[4];
	public static int otherDeviceNum = 0;
	private static TextView infoStr;
	public static MainActivity currentMainActivity;

	private static boolean otherDevicesIsInitiated = false;

	public PublicObjects(){
		
	}
	
	public static void setCurrentMainActivity(MainActivity thisActivity){
		currentMainActivity = thisActivity;
		if(!otherDevicesIsInitiated){
			initiateOtherDevice();
			otherDevicesIsInitiated = true;
		}
		
	}
	
	public static void printOtherDevices(){
		System.out.println("the device number is " + otherDeviceNum);
		for(int i=0; i<4; i++){
			System.out.println("device " + i);
			if(otherDevices[i].device_id==null){
				System.out.println("null");
			}
			else{
				System.out.println("device id: " + otherDevices[i].device_id + " status: " + otherDevices[i].status );
			}
		}
	}
	
	public static MainActivity getCurrentMainActivity(){
		return currentMainActivity;
	}
	
	
	public static void initiateOtherDevice(){
		for(int i=0; i<4; i++){
			otherDevices[i] = new DeviceInfo();
		}
	}
	
	public static void setInfoStr(TextView v){
		infoStr = v;
	}
	
	public static TextView getInfoStr(){
		return infoStr;
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
