package com.vnp.util;

import org.json.JSONObject;

public class IParkingApi {
	private MyAPIResponseUtil resUtil;
	private final int timeout = 60000;
	private final String iparking_base_url = "http://127.0.0.1:9556/mapi";
	private final String iparking_login_action = "/login";
	private final String iparking_recharging_action = "/services/RechargingIparkingService";
	private final String iparking_recharging_post = "http://10.149.3.164:8000/ppgw/g";
	private String session_id = "";

	public IParkingApi() {
		super();
		resUtil = new MyAPIResponseUtil();
	}
	
	public int login(String user, String password){
		String request = "{\"username\":\"" + user + "\" , \"password\":\"" + password + "\"}";
		String error_code = "";
		try{
			String return_ = resUtil.getHttpPost(request, iparking_base_url + iparking_login_action, timeout);
			JSONObject json = new JSONObject(return_);
			error_code = json.getString("error_code");
			if(error_code.equals("0")){
				session_id = json.getString("session");
			}
			return Integer.parseInt(error_code);
		}catch(Exception e){
			return -1;
		}		
	}
	
	public int rechargingIparkingService(String msisdn, String command, String transactionAmount, String parkingCode, String transactionID, String carNumberPlate){
//		return 0;
		String request = "{\"msisdn\":\"" + msisdn + "\" , \"command\":\"" + command 
							+ "\" , \"transactionAmount\":\"" + transactionAmount + "\" , \"parkingCode\":\"" + parkingCode 
							+ "\" , \"transactionID\":\"" + transactionID + "\" , \"carNumberPlate\":\"" + carNumberPlate 
							+ "\" , \"session\":\"" + session_id+ "\"}";
		String return_ = "";
		String error_code = "";
		try{
			return_ = resUtil.getHttpPost(request, iparking_base_url + iparking_recharging_action, timeout);
			JSONObject json = new JSONObject(return_);
			error_code = json.getString("error_code");
			return Integer.parseInt(error_code);
		}catch(Exception e){
			return -9999;
		}
	}
	
	public int rechargingIparkingPost(String msisdn, String transactionAmount){
//		return 0;
		String request = "{\"msisdn\":\"" + msisdn + "\" , \"transactionCode\":\"VNPTiParking\" , \"amount\":\"" + transactionAmount + "\" , \"action\":\"D\" , \"service\":\"balanceAdjustment\"}";
		String return_ = "";
		String error_code = "";
		try{
			return_ = resUtil.getHttpPost(request, iparking_recharging_post, timeout);
//			JSONObject json = new JSONObject(return_);
//			error_code = json.getString("error_code");
			return Integer.parseInt(return_.trim().replaceAll("\"", ""));
		}catch(Exception e){
			return -9999;
		}
	}
}
