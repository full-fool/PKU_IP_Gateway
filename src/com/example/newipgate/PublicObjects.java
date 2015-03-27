package com.example.newipgate;

import android.widget.TextView;



public class PublicObjects {
	private static ITSClient publicItsClient;
	public static DeviceInfo thisDeviceInfo = new DeviceInfo();
	public static DeviceInfo[] otherDevices = new DeviceInfo[4];
	public static int otherDeviceNum = 0;
	private static TextView infoStr;
	public static WelcomePage currentWelcomePageActivity;
	public static LoginActivity currentLoginActivity;
	public static AllConnections currentAllConnections;
	private static final int LOGIN_ACTIVITY = 1;
	private static final int ALL_CONNECTIONS_ACTIVITY = 2;
	private static final int DISCONNECTED = 1;
	private static final int CONNECT_FREE = 3;
	private static final int CONNECT_CHARGE = 4;
	private static final int UNKNOWN = 5;

	private static int currentActivity = LOGIN_ACTIVITY;
	private static String savedUsername = null;
	private static String currentUsername = null;
	private static String savedPassword = null;
	private static String currentPassword = null;

	private static boolean otherDevicesIsInitiated = false;

	public PublicObjects(){
		
	}
	
	
	
	
	
	public static void setCurrentActivity(int activityNum){
		currentActivity = activityNum;
	}
	
	public static int getCurrentActivity(){
		return currentActivity;
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
	
	
	public static void setCurrentWelcomeActivity(WelcomePage thisActivity){
		currentWelcomePageActivity = thisActivity;
		if(!otherDevicesIsInitiated){
			initiateOtherDevice();
			otherDevicesIsInitiated = true;
		}
		
	}
	
	public static void setCurrentLoginActivity(LoginActivity thisActivity){
		currentLoginActivity = thisActivity;
		if(!otherDevicesIsInitiated){
			initiateOtherDevice();
			otherDevicesIsInitiated = true;
		}
		
	}
	
	
	public static WelcomePage getCurrentWelcomeActivity(){
		return currentWelcomePageActivity;
	}
	
	public static LoginActivity getCurrentLoginActivity(){
		return currentLoginActivity;
	}
	
	
	public static void setCurrentAllconnections(AllConnections thisActivity){
		currentAllConnections = thisActivity;
	}
	
	
	public static AllConnections getCurrentAllConnections(){
		return currentAllConnections;
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
	
	public static void setSavedUsername(String newUsername){
		savedUsername = newUsername;
	}
	
	public static String getSavedUsername(){
		return savedUsername;
	}
	
	public static void setSavedPassword(String newPassword){
		savedPassword = newPassword;
	}
	
	public static String getSavedPassword(){
		return savedPassword;
	}
	
	public static void setCurrentUsername(String newUsername){
		currentUsername = newUsername;
	}
	
	public static String getCurrentUsername(){
		return currentUsername;
	}
	
	public static void setCurrentPassword(String newPassword){
		currentPassword = newPassword;
	}
	
	public static String getCurrentPassword(){
		return currentPassword;
	}
	
	
	
	public static void setThisDeviceDeviceID(String _device_id)
	{
		thisDeviceInfo.device_id = _device_id;	
	}
	
	public static void setThisDeviceStatus(int _status)
	{
		System.out.println("this device status is set to " + _status);
		thisDeviceInfo.status = _status;
	}
	
	public static void setAllOtherDeviceStatus(int _status)
	{
		for(int i=0; i<4; i++){
			if(otherDevices[i].device_id != null){
				otherDevices[i].status = _status;
			}
		}
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
	
	public static int getThisDeviceStatus()
	{
		return thisDeviceInfo.status;
	}
	
	public static String getThisDeviceID()
	{
		return thisDeviceInfo.device_id;
	}
	
	


}
