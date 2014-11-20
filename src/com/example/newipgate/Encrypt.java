package com.example.newipgate;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

public class Encrypt {
	private Context context;
	public Encrypt(Context con) {
		context = con;
	}
	private static final String TAG = "Encrypt";
	
	public String encrypt(String text) {
		byte[] encryptText = null;
		try {
			SecretKeySpec sks = new SecretKeySpec(getKey(), "AES");
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, sks);
			encryptText = c.doFinal(text.getBytes("UTF-8"));
			return Base64.encodeToString(encryptText, Base64.CRLF);
		} catch(Exception e) {
			return null;
		}
	}
	public String decrypt(String encryptText) {
		byte[] text = null;
		try{
			SecretKeySpec sks = new SecretKeySpec(getKey(), "AES");
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, sks);
			text = c.doFinal(Base64.decode(encryptText, Base64.DEFAULT));
			return new String(text, "UTF-8");
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	private byte[] getKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
		TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		byte[] seed = telephonyManager.getDeviceId().getBytes();
		if(seed == null) {
			Log.i(TAG, "can't get IMEI");
			seed = "this is my imei number".getBytes();
		}
		KeyGenerator kg;
		
		/*
		int iterationCount = 1000;
		  int saltLength = 8; // bytes; 64 bits
		  int keyLength = 256;
		    SecureRandom random = new SecureRandom();
		  byte[] salt = new byte[saltLength];
		      random.nextBytes(salt);
		  KeySpec keySpec = new PBEKeySpec(seed.toString().toCharArray(), salt,
		      iterationCount, keyLength);
		  SecretKeyFactory keyFactory = SecretKeyFactory
		      .getInstance("PBKDF2WithHmacSHA1");
		  byte[] raw = keyFactory.generateSecret(keySpec).getEncoded();
		  
		  */
		  
		
		try {
			kg = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			Log.i(TAG, "no AES algorithm");
			return null;
		}
		SecureRandom sr;
		try {
			//sr = SecureRandom.getInstance("SHA1PRNG");
		    sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");

		} catch (NoSuchAlgorithmException e) {
			Log.i(TAG, "no SHA1PRNG algorithm");
			return null;
		}
		sr.setSeed(seed);
		kg.init(128, sr);
		SecretKey sk = kg.generateKey();
		byte[] raw = sk.getEncoded();
		
		return raw;
	}
}
