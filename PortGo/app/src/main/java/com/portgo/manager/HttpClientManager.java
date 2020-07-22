/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.portgo.manager;

import android.content.Context;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HTTP/HTTPS service.
 */
public class HttpClientManager{
	private static final String TAG = HttpClientManager.class.getCanonicalName();
	public static final String CONTENT_PLAINTEXT = "text/plain; charset=utf-8";
	public static final String CONTENT_JSON = "application/json";
	public static final String CONTENT_RAW = "raw";
	static public boolean stop() {
		return true;
	}

	static public String get(String uri) {
			String result="";
		try{
			URL url=new URL(uri);
			HttpURLConnection conn=(HttpURLConnection)url.openConnection();
			if(conn.getResponseCode()==HttpURLConnection.HTTP_OK){
				InputStream is=conn.getInputStream();
				byte[]data=new byte[1024];
				int len=is.read(data);
				result=new String(data,0,len);
				is.close();
				conn.disconnect();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}

	static public String post(String uri, String contentUTF8, String contentType){
		URL url;
		HttpURLConnection  uRLConnection;
		try {
			url = new URL(uri);
			trustAllHosts();
			uRLConnection = (HttpURLConnection)url.openConnection();
			uRLConnection.setDoInput(true);
			uRLConnection.setDoOutput(true);
			uRLConnection.setRequestMethod("POST");
			uRLConnection.setUseCaches(false);
			uRLConnection.setInstanceFollowRedirects(false);
			uRLConnection.setRequestProperty("Content-Type", contentType);
			uRLConnection.connect();

			DataOutputStream out = new DataOutputStream(uRLConnection.getOutputStream());
			out.writeBytes(contentUTF8);
			out.flush();
			out.close();

			InputStream is = uRLConnection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String response = "";
			String readLine = null;
			while((readLine =br.readLine()) != null){
				//response = br.readLine();
				response = response + readLine;
			}
			is.close();
			br.close();
			uRLConnection.disconnect();
			return response;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
//		return result;
	}

	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };
		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public InputStream getBinary(String uri) {

		return null;
	}

}
