package com.vnp.util;

import org.json.JSONObject;

public class RechargingApi {
	private MyAPIResponseUtil resUtil;
	private final int timeout = 60000;
	private final String base_url = "http://10.149.248.67:81/mapi";
	private final String login_action = "/login";
	private final String check_subs_type = "/services/CheckSubsType";
	private final String transfer_money = "/services/TransferMoney";
	private final String checkAccountBalance = "/services/CheckAccountMainBalance";
	private final String checkAccountKM3Balance = "/services/CheckAccountKM3Balance";
	private String session_id = "";

	public RechargingApi() {
		super();
		resUtil = new MyAPIResponseUtil();
	}
	
	public int login(String user, String password){
		String request = "{\"username\":\"" + user + "\" , \"password\":\"" + password + "\"}";
		String error_code = "";
		try{
			String return_ = resUtil.getHttpPost(request, base_url + login_action, timeout);
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
	
	public int checkSubsType(String subcriber_id, String transactionID){
		String request = "{\"subcriber_id\":\"" + subcriber_id+ "\" , \"transactionID\":\"" + transactionID + "\" , \"session\":\"" + session_id+ "\"}";
		String error_code = "";
		try{
			String return_ = resUtil.getHttpPost(request, base_url + check_subs_type, timeout);
			JSONObject json = new JSONObject(return_);
			error_code = json.getString("error_code");
			return Integer.parseInt(error_code);
		}catch(Exception e){
			return -1;
		}		
	}
	
	public int transferMoney(String subTranfer, String subReceive, String transactionAmount, String transactionExpenditure, String transactionID, String mtr_comment, String service, String pack){
		String request = "{\"subTranfer\":\"" + subTranfer + "\" , \"subReceive\":\"" + subReceive 
							+ "\" , \"transactionAmount\":\"" + transactionAmount + "\" , \"transactionExpenditure\":\"" + transactionExpenditure 
							+ "\" , \"transactionID\":\"" + transactionID + "\" , \"mtr_comment\":\"" + mtr_comment 
							+ "\" , \"package\":\"" + pack + "\" , \"cpservice\":\"" + service 
							+ "\" , \"session\":\"" + session_id+ "\"}";
		String return_ = "";
		String error_code = "";
		try{
			return_ = resUtil.getHttpPost(request, base_url + transfer_money, timeout);
			JSONObject json = new JSONObject(return_);
			error_code = json.getString("error_code");
			return Integer.parseInt(error_code);
		}catch(Exception e){
			return -9999;
		}
	}
	
	public double getAccountMainBalance(String subscriberID, String transactionID){
		String request = "{\"subscriberID\":\"" + subscriberID+ "\" , \"transactionID\":\"" + transactionID + "\" , \"session\":\"" + session_id+ "\"}";
		String error_code = "";
		try{
			String return_ = resUtil.getHttpPost(request, base_url + checkAccountBalance, timeout);
			JSONObject json = new JSONObject(return_);
			error_code = json.getString("error_code");
			if(Integer.parseInt(error_code) == 1){
				return -2; //thue bao tra sau
			}else if(Integer.parseInt(error_code) == 0){
				String balance = error_code = json.getString("balance");
				return Double.parseDouble(balance);
			} else{
				return Integer.parseInt(error_code); //loi khi check thong tin
			}			
		}catch(Exception e){
			return -1;
		}		
	}
	
	public double getAccountKM3Balance(String subscriberID, String transactionID){
		String request = "{\"subscriberID\":\"" + subscriberID+ "\" , \"transactionID\":\"" + transactionID + "\" , \"session\":\"" + session_id+ "\"}";
		String error_code = "";
		try{
			String return_ = resUtil.getHttpPost(request, base_url + checkAccountKM3Balance, timeout);
			JSONObject json = new JSONObject(return_);
			error_code = json.getString("error_code");
			if(Integer.parseInt(error_code) == 1){
				return -2; //thue bao tra sau
			}else if(Integer.parseInt(error_code) == 0){
				String balance = error_code = json.getString("balance");
				return Double.parseDouble(balance);
			} else{
				return Integer.parseInt(error_code); //loi khi check thong tin
			}			
		}catch(Exception e){
			return -1;
		}		
	}
}
