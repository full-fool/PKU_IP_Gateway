package com.example.newipgate;

import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.newipgate.WebSocketPart.WebSocketConnection;
import com.example.newipgate.WebSocketPart.WebSocketConnectionHandler;
import com.example.newipgate.WebSocketPart.WebSocketException;

public class Interaction {
	
	
	
	private final WebSocketConnection wsc = new WebSocketConnection();
	private boolean websocketConnected = false; 
	private static DeviceInfo thisDeviceInfo = new DeviceInfo();
	private static DeviceInfo[] otherDevices = new DeviceInfo[4];
	private int otherDeviceNum = 0;
	private MainActivity mainActivity;
	
	
	
	
	public Interaction(MainActivity thisActivity){
		
		mainActivity = thisActivity;
	}
	
	//change this device's status, called when connection or disconnection happens.
	public static void changeThisDeviceStatus(int newStatus)
	{
		thisDeviceInfo.status = newStatus;
	}
	
	//send heartBeat Information
	public void sendHeartBeat()
	{
		if(websocketConnected)
		{
			System.out.println("send out heartbeat information");
			JSONObject requestInfo = new JSONObject();
			try {
				requestInfo.put("type", 2);
				requestInfo.put("content", thisDeviceInfo.status);
				wsc.sendTextMessage(requestInfo.toString());

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			System.out.println("the websocket is not established, so the heartbeat is not sent out");
		}

	}

	//initiate the websocket connection
	public void startWebSocket()
	{
		try {
            
            wsc.connect("ws://162.105.146.140:9000/login?student_id=1100012950&password=11223344433&type=1&status=3", 
            		new WebSocketConnectionHandler(){

                    @Override
                    public void onBinaryMessage(byte[] payload) {
                            System.out.println("onBinaryMessage size="+payload.length);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                            System.out.println("onClose code = " + code + " reason="+reason);
                    }

                    @Override
                    public void onOpen() {
                    		websocketConnected = true;
                            System.out.println("onOpen");
                            try {
        						JSONObject requestInfo = new JSONObject();
        						requestInfo.put("type", 3);
        						requestInfo.put("content", JSONObject.NULL);
        						System.out.println("the send json string is " + requestInfo.toString());
        						
        						wsc.sendTextMessage(requestInfo.toString());
        						
        					} catch (JSONException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					}
                    }

                    @Override
                    public void onRawTextMessage(byte[] payload) {
                            System.out.println("onRawTextMessage size="+payload.length);
                            
                            
                    }

                    @Override
                    public void onTextMessage(String payload) {
                    	handleReceivedMessage(payload);     
                    }
           
            });
            
    } catch (WebSocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    }

	}
	
	//change other device's status
	private void changeOtherDevice(String DeviceID, int newStatus)
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 4);
				JSONObject deviceInfo = new JSONObject();
				deviceInfo.put("device_id", DeviceID);
				deviceInfo.put("status", newStatus);
				requestInfo.put("content", deviceInfo);
				wsc.sendTextMessage(requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	//change the password
	private void sendChangePassword(String newPassword)
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 7);
				requestInfo.put("content", newPassword);
				wsc.sendTextMessage(requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	//update the information of other device
	private void updateOtherDevice()
	{
		try {
			JSONObject requestInfo = new JSONObject();
			requestInfo.put("type", 6);
			requestInfo.put("content", JSONObject.NULL);
			wsc.sendTextMessage(requestInfo.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//handles all kind of received message
	private void handleReceivedMessage(String payload)
	{
		System.out.println("onTextMessage"+payload);
        if(Pattern.matches("[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+", payload))
        {
        	thisDeviceInfo.device_id = payload;
        	thisDeviceInfo.type = 1;
        }
        else {
        	JSONObject jsonObject = null;
        	int infoType = -1;
			try {
				jsonObject = new JSONObject(payload);
                infoType = jsonObject.getInt("type");  
                
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			//add new device
			if(infoType == 102)
			{
				try {
					JSONObject connectionInfo = jsonObject.getJSONObject("content");
					otherDevices[otherDeviceNum].device_id = connectionInfo.getString("device_id");
					otherDevices[otherDeviceNum].status = connectionInfo.getInt("status");
					otherDevices[otherDeviceNum].type = connectionInfo.getInt("type");
					otherDevices[otherDeviceNum].ip = connectionInfo.getString("ip");
					otherDevices[otherDeviceNum].location = connectionInfo.getString("location");
					otherDeviceNum++;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			//delete device
			else if(infoType == 103)
			{
				try {
					Boolean hasFind = false;
					String deletedDeviceId = jsonObject.getString("content");
					for(int i=0; i<4; i++)
					{
						if(deletedDeviceId.equals(otherDevices[i].device_id))
						{
							otherDevices[i].device_id = null;
							otherDeviceNum--;
							hasFind = true;
							break;
						}
					}
					if(!hasFind)
						System.out.println("the delete device error");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			//update other device's status
			else if(infoType == 101)
			{
				try {
					Boolean hasFind = false;
					JSONObject otherDeviceInfo = jsonObject.getJSONObject("content");
					String otherDeviceId = otherDeviceInfo.getString("device_id");
					int otherDeviceStatus = otherDeviceInfo.getInt("status");
					for(int i=0; i<4; i++)
					{
						if(otherDeviceId.equals(otherDevices[i].device_id))
						{
							otherDevices[i].status = otherDeviceStatus;
							hasFind = true;
							break;
						}
					}
					if(!hasFind)
						System.out.println("the update other device's status error");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//update its own status
			else if(infoType == 104)
			{
				try {
					int newStatus = jsonObject.getInt("content");
					thisDeviceInfo.status = newStatus;
					if(newStatus == 1)
					{
						mainActivity.disconnectThis();
					}
					else if(newStatus == 3)
					{
						mainActivity.connect(1);
					}
					else if(newStatus == 4)
					{
						mainActivity.connect(2);
					}
					else
					{
						System.out.println("when changing status itself, unknown error");
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			else if(infoType == 106)
			{
				 try {
						JSONObject requestInfo = new JSONObject();
						requestInfo.put("type", 1);
						requestInfo.put("content", thisDeviceInfo.status);
						wsc.sendTextMessage(requestInfo.toString());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			else if(infoType == 105)
			{
				mainActivity.disconnectAll();
				try {
					JSONObject requestInfo = new JSONObject();
					requestInfo.put("type", 5);
					requestInfo.put("content", JSONObject.NULL);
					wsc.sendTextMessage(requestInfo.toString());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			else if(infoType == 107)
			{
				try {
					String newPassword = jsonObject.getString("content");
					mainActivity.updatePassword(newPassword);
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			
            
		}
	}

	/*
	public void getDeviceList()
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 3);
				requestInfo.put("content", JSONObject.NULL);
				System.out.println("the send json string is " + requestInfo.toString());
				
				//wsc.sendTextMessage(requestInfo.toString());
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

*/
	
	
}
