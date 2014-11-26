package com.example.newipgate;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;




public class ITSClient {
	
	
	private static final String TAG = null;
	
	private DefaultHttpClient client;

	MainActivity mainActivity;
	
	private int login() {
		HttpPost post = new HttpPost("http://its.pku.edu.cn/cas/login");
		HttpParams httpparams = new BasicHttpParams();
        httpparams.setParameter("http.protocol.handle-redirects", false);
        post.setParams(httpparams);
		String splitwords = "|;kiDrqvfi7d$v0p5Fg72Vwbv2;|";
		try{
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			post.addHeader("User-Agent", 
							"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:28.0) Gecko/20100101 Firefox/28.0");
			post.addHeader("Host", "its.pku.edu.cn");
			post.addHeader("Accept", 
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
			post.addHeader("Accept-Encoding", "gzip, deflate");
			post.addHeader("Referer", "https://its.pku.edu.cn/");
			String username = mainActivity.getUsername();
			String password = mainActivity.getPassword();
			params.add(new BasicNameValuePair("username1", username));
			params.add(new BasicNameValuePair("password", password));
			System.out.println("in login, the username is " + username + " and the password is " + password);
			
			params.add(new BasicNameValuePair("pwd_t", "密码"));
			params.add(new BasicNameValuePair("fwrd", "free"));
			params.add(new BasicNameValuePair("username", username + splitwords + 
					password +splitwords + "12"));
			
			
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");	
			post.setEntity(entity);
			HttpResponse response = client.execute(post);
			if(response == null) 
				return -1;
			Header newLocation = response.getFirstHeader("Location");
			response.getEntity().consumeContent();
			while(newLocation != null)
			{
				String newLocationUrl = newLocation.getValue();
				System.out.println("the redirect url is " + newLocationUrl);
				if(newLocationUrl.contains("e=3"))
					return 1314;
				HttpGet get = new HttpGet(newLocationUrl);
				response = client.execute(get);
				newLocation = response.getFirstHeader("Location");
				response.getEntity().consumeContent();

			}
			
			
			
			
			int statusCode = response.getStatusLine().getStatusCode();
			return statusCode;
		} catch(ConnectTimeoutException e) {
			return -2;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			post.abort();
		}
	}
	
	private String cmd(String cmdaddr) {
		int statusCode = login();
		Log.i(TAG, "login code is " + statusCode);
		if(statusCode != 301 && statusCode != 200
				&& statusCode != 302 && statusCode != 1314) {
			return "error";
		}
		
		if(statusCode == 1314)
			return "wrong password";
		
		//Log.i(TAG, "the itsresult page is " + getPage("http://its.pku.edu.cn/netportal/itsResult.jsp"));
		return getPage(cmdaddr);
	}

	private String getPage(String url) {
		HttpGet get = new HttpGet(url);
		get.setHeader("Referer", "http://its.pku.edu.cn/netportal/netportal_UTF-8.jsp");
		try {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = client.execute(get, responseHandler);
			get.abort();
			get = new HttpGet("http://its.pku.edu.cn/netportal/itsResult.jsp");
			String newResponse = client.execute(get, responseHandler);
			//if(newResponse.contains("达到设定值") || newResponse.contains("连接成功"))
			if(newResponse.contains("达到设定值") || newResponse.contains("连接成功") || newResponse.contains("断开成功"))
				return newResponse;
			
			//System.out.println("the itsresult is " + client.execute(get, responseHandler));
			//Log.i(TAG, responseBody);
			return responseBody;
		} catch(Exception e) {
			e.printStackTrace();
			return "error";
		} finally {
			get.abort();
		}
	}

	
	public ITSClient(MainActivity thisActivity){
		mainActivity = thisActivity;
		
		InputStream ins = mainActivity.getResources().openRawResource(R.raw.ca);
		CertificateFactory cerFactory;
		try {
			cerFactory = CertificateFactory.getInstance("X.509");
			Certificate cer = (Certificate) cerFactory.generateCertificate(ins);
			KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
			keyStore.load(null, null);
			keyStore.setCertificateEntry("trust", cer);
			SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore);
			Scheme sch = new Scheme("https", socketFactory, 443);
			client = new DefaultHttpClient();
			client.getConnectionManager().getSchemeRegistry().register(sch);
			client.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.BEST_MATCH);
			client.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.BROWSER_COMPATIBILITY);
			client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 60000);
		}
		
		catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//infoStr = (TextView)findViewById(R.id.info);

		
	}

	
	public void connect(int connectType) 
	{
		final int ConnectType = connectType;
		new Thread() {
			public void run() {
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=open&type=";
				int random = Math.abs((new Random()).nextInt() % 1000);
				if(ConnectType == 2)
				{
					getaddr += "fee&fr=0&sid=" + random;
					Interaction.changeThisDeviceStatus(4);
				}
				else {
					getaddr += "free&fr=0&sid=701";
					Interaction.changeThisDeviceStatus(3);
				}
				String response = cmd(getaddr);
				if(response.contains("wrong password")){
					mainActivity.ShowInfo("密码错误");
					System.out.println("密码错误");
				}
				else if(response.contains("重新输入")) {
					mainActivity.ShowInfo("重新输入");
					System.out.println("重新输入");
				} else if(response.contains("达到设定值")) {
					mainActivity.ShowInfo("当前连接超过预定值");
					System.out.println("当前连接超过预定值");
				} else if(response.contains("不在申请访问服务的范围内")) {
					mainActivity.ShowInfo("不在申请访问服务的范围内");
					System.out.println("不在申请访问服务的范围内");
				} else if(response.contains("ipgw_open_Failed")) {
					mainActivity.ShowInfo("ipgw_open_Failed");
					System.out.println("ipgw_open_Failed");
				} else if(response.contains("连接成功"))
				{
					String [] resultStrings = response.split(">[012]个<", 0);
					
					if(resultStrings.length > 0)
					{
						int indexOfNext = response.indexOf(resultStrings[1]);
						final String connectionNum = response.substring(indexOfNext-3, indexOfNext-1);
						mainActivity.ShowInfo("连接成功" + "\n当前连接：" + connectionNum);
						System.out.println("连接成功" + "\n当前连接：" + connectionNum);
						mainActivity.startWebSocket();	
					}
					else 
					{
						mainActivity.ShowInfo("连接成功" + "\n未知错误，无法获取连接数");
						System.out.println("连接成功" + "\n未知错误，无法获取连接数");
					}
					mainActivity.startHeartBeat();
				}
				else {
					mainActivity.ShowInfo("未知错误");
					System.out.println("未知错误");
				}
			}
		}.start();
	}
	
	
	public void disconnectAll() {
		new Thread() {
			public void run() {
				int random = Math.abs((new Random()).nextInt() % 1000);
				String getaddr = "http://its.pku.edu.cn/netportal/PKUIPGW?cmd=close&type=allconn&fr=0&sid" + random;
				String response = cmd(getaddr);
				System.out.println("in disconnect all, the response is " + response);
				if(response.contains("断开成功")) {
					mainActivity.ShowInfo("断开全部连接成功");
					System.out.println("断开全部连接成功");
				}else{
					mainActivity.ShowInfo("unknown error");					
					System.out.println("unknown error");
				}
			}
		}.start();

	}
	

	public void disconnectThis() {
		new Thread() {
			public void run() {
				double random = (new Random()).nextDouble() * 100;
				String getaddr = "http://its.pku.edu.cn/disconnetwireless.do?t=" + random;
				HttpGet get = new HttpGet(getaddr);
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
				System.out.println("the disconnect this response is  " + responseBody);
				
				if(responseBody.contains("SUCCESS=YES")) {
					mainActivity.ShowInfo("网络断开成功");
					System.out.println("网络断开成功");
				} else{
					mainActivity.ShowInfo("unknown error");
					System.out.println("unknown error");
				}
			}
		}.start();

	}

}
