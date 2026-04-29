package com.vnp.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MyAPIResponseUtil {

	public String getHttpPost(String jsonStr, String url, int timeout) throws Exception {		
		URL requestURL = new URL(url);
		HttpURLConnection con = (HttpURLConnection)requestURL.openConnection();
	    con.setDoOutput(true);
	    con.setDoInput(true);

	    con.setRequestProperty("Content-Type", "application/json; charset=utf8");
	    con.setRequestProperty("Accept", "application/json");
	    con.setRequestProperty("Method", "POST");
	    OutputStream os = con.getOutputStream();
	    os.write(jsonStr.getBytes("UTF-8"));
	    os.close();
	    String inputLine;
        StringBuffer out = new StringBuffer();
        try{
        	if(con.getInputStream() != null){
            	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(),"UTF-8"));            
                while ((inputLine = in.readLine()) != null) {
                	out.append(inputLine+"\n");
                }
                in.close();
            }
        }catch(Exception ioex){
        	
        }
        
        try{
        	if(con.getErrorStream() != null){
            	BufferedReader in1 = new BufferedReader(new InputStreamReader(con.getErrorStream(),"UTF-8"));
                while ((inputLine = in1.readLine()) != null) {
                	out.append(inputLine+"\n");
                }
                in1.close();
            }
            
        }catch(Exception ioex){
        	
        }
        return out.toString();
	}
}
