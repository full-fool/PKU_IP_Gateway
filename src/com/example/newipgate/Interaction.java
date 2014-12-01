package com.example.newipgate;

import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.newipgate.WebSocketPart.WebSocketConnection;
import com.example.newipgate.WebSocketPart.WebSocketConnectionHandler;
import com.example.newipgate.WebSocketPart.WebSocketException;

public class Interaction {
	
	
	
	private final WebSocketConnection wsc = new WebSocketConnection();
	private boolean websocketConnected = false; 
	private static MainActivity mainActivity;
	
	
	
	
	public Interaction(MainActivity thisActivity){
		
		mainActivity = thisActivity;
	}
	
	public static void setMainActivity(MainActivity thisActivity){
		mainActivity = thisActivity;
	}
	
	public void testSend(){
		if(websocketConnected){
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
	}
	
	public Boolean isWebsocketConnected(){
		return websocketConnected;
	}
	
	public void updateConnectionStatus(){
		if(websocketConnected){ 
		try {
				System.out.println("updateconnectionstatus successfully");
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 1);
				requestInfo.put("content", PublicObjects.getThisDeviceStatus());
				wsc.sendTextMessage(requestInfo.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			System.out.println("in updateconnectionstatus, the websocket is already closed");
		}
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
				requestInfo.put("content", PublicObjects.getThisDeviceStatus());
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
		if(!websocketConnected){
		try {
			System.out.println("in start websocket, the status is disconnect");
            
            wsc.connect("ws://162.105.146.140:9000/login?student_id=1100012950&password=11223344433&type=1&status=3", 
            		new WebSocketConnectionHandler(){

                    @Override
                    public void onBinaryMessage(byte[] payload) {
                            System.out.println("onBinaryMessage size="+payload.length);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                            System.out.println("onClose code = " + code + " reason=" + reason);
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
		else{
			System.out.println("in start websocket, the connection is already established");
		}

	}
	
	//change other device's status
	public void changeOtherDevice(String DeviceID, int newStatus)
	{
		 try {
				JSONObject requestInfo = new JSONObject();
				requestInfo.put("type", 4);
				JSONObject deviceInfo = new JSONObject();
				deviceInfo.put("device_id", DeviceID);
				deviceInfo.put("status", newStatus);
				requestInfo.put("content", deviceInfo);
				System.out.println("in change other device, the sent string is " + requestInfo.toString());
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
        	PublicObjects.setThisDeviceDeviceID(payload);
        	PublicObjects.setThisDeviceType(1);
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
			
			if(infoType == 3){
				try {
					String connectionString = jsonObject.getString("content");
					//JSONObject connectionInfo = new JSONObject(connectionString);
					if(connectionString.startsWith("\ufeff")){
						System.out.println("starts with bom");
						connectionString = connectionString.substring(1);
					}
					JSONArray contents = new JSONArray(connectionString);
					System.out.println("there are " + contents.length() + "more devices");

					for(int i=0; i<contents.length(); i++)
					{
						PublicObjects.otherDevices[i].device_id = ((JSONObject)contents.get(i)).getString("device_id");
						PublicObjects.otherDevices[i].status = ((JSONObject)contents.get(i)).getInt("status");
						PublicObjects.otherDevices[i].type = ((JSONObject)contents.get(i)).getInt("type");
						PublicObjects.otherDevices[i].ip = ((JSONObject)contents.get(i)).getString("ip");
						PublicObjects.otherDevices[i].location = ((JSONObject)contents.get(i)).getString("location");
						PublicObjects.otherDevices[i].owner = ((JSONObject)contents.get(i)).getString("owner");
					}
					PublicObjects.otherDeviceNum = contents.length();
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
			//add new device
			else if(infoType == 102)
			{
				try {
					String connectionString = jsonObject.getString("content");
					JSONObject connectionInfo = new JSONObject(connectionString);
					//JSONObject connectionInfo = jsonObject.getJSONObject("content");
					if(!PublicObjects.getThisDeviceID().equals(connectionInfo.getString("device_id"))){

						System.out.println("the device_id is different, old id is " + PublicObjects.getThisDeviceID() + " and new id is " 
								+ connectionInfo.getString("device_id"));
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].device_id = connectionInfo.getString("device_id");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].status = connectionInfo.getInt("status");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].type = connectionInfo.getInt("type");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].ip = connectionInfo.getString("ip");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].location = connectionInfo.getString("location");
						PublicObjects.otherDevices[PublicObjects.otherDeviceNum].owner = connectionInfo.getString("owner");
						PublicObjects.otherDeviceNum++;
					}
					
					
					
					
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
					
					//disconnect this device
					if(deletedDeviceId.equals(PublicObjects.getThisDeviceID())){
						System.out.println("in 103, the deleted device is itself");
						mainActivity.disconnectThis();
					}
					//delete other device's info
					else{
					for(int i=0; i<PublicObjects.otherDeviceNum; i++)
					{
						//move the last device to this slot
						if(deletedDeviceId.equals(PublicObjects.otherDevices[i].device_id))
						{
							PublicObjects.otherDevices[i].device_id = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].device_id;
							PublicObjects.otherDevices[i].status = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].status;
							PublicObjects.otherDevices[i].type = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].type;
							PublicObjects.otherDevices[i].ip = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].ip;
							PublicObjects.otherDevices[i].location = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].location;
							PublicObjects.otherDevices[i].owner = PublicObjects.otherDevices[PublicObjects.otherDeviceNum-1].owner;
							PublicObjects.otherDeviceNum--;
							hasFind = true;
							break;
						}
					}
					if(!hasFind)
						System.out.println("the deleted device not found");
					}
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
						if(otherDeviceId.equals(PublicObjects.otherDevices[i].device_id))
						{
							PublicObjects.otherDevices[i].status = otherDeviceStatus;
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
					//thisDeviceInfo.status = newStatus;
					PublicObjects.setThisDeviceStatus(newStatus);
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
						requestInfo.put("content", PublicObjects.getThisDeviceStatus());
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
}
