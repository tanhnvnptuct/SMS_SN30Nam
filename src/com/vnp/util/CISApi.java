package com.vnp.util;

import java.net.ConnectException;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

public class CISApi {
	private MyAPIResponseUtil resUtil;
	private final int timeout = 30000;
	private final String cis_base_url = "https://vinaphone.iparking.vn/p/cpp/";//"http://45.118.151.70:9556/cpservice/rest/";
	private final String cis_forwardmo_action = "forwardmo";
	private final String cis_submitpayment_action = "submitpayment";
	private final String shortcode = "9556";

	public CISApi() {
		super();
		resUtil = new MyAPIResponseUtil();
	}
	
	public String forwardmo(String msisdn, String smsmo, String username, String password, String transaction_id, String transactionAmount){
//		String transaction_id = String.valueOf(System.currentTimeMillis());
		String hashstring = DigestUtils.sha256Hex(shortcode + msisdn + smsmo + transaction_id + username + password);
		String request = "{\"shortcode\":\""+ shortcode +"\" , \"msisdn\":\"" + msisdn 
				+ "\" , \"smsmo\":\"" + smsmo + "\" , \"transactionid\":\"" + transaction_id 
				+ "\" , \"username\":\"" + username + "\" , \"password\":\"" + password 
				+ "\" , \"hashstring\":\"" + hashstring + "\"}";
		String resultcode = "";
		String amount = "";
		String resultdes = "";
//		return 0;
		try{
			String return_ = resUtil.getHttpPost(request, cis_base_url + cis_forwardmo_action, timeout);
			JSONObject json = new JSONObject(return_);
			resultcode = json.getString("resultcode");
			if(resultcode.equals("0")){
				amount = json.getString("amount");
				return resultcode + "|" + amount;
			}else{
				resultdes = json.getString("resultdes");
				return resultcode + "|" + resultdes;
			}
		}catch(ConnectException ce){
			return "-9999|" + ce.getMessage();
		}catch(Exception e){
			return "-1" + e.getMessage();
		}
	}
	
	public String submitpayment(String msisdn, String smsmo, String amount, String username, String password, String transaction_id){
		String chargeresult = "1";
		String chargedes = "Tru cuoc thanh cong";
		String hashstring = DigestUtils.sha256Hex(msisdn + smsmo + amount + chargeresult + chargedes + transaction_id + username + password);
		String request = "{\"amount\":\"" + amount + "\" , \"msisdn\":\"" + msisdn 
				+ "\" , \"smsmo\":\"" + smsmo
				+ "\" , \"chargeresult\":\"" + chargeresult
				+ "\" , \"chargedes\":\"" + chargedes
				+ "\" , \"transactionid\":\"" + transaction_id 
				+ "\" , \"username\":\"" + username + "\" , \"password\":\"" + password 
				+ "\" , \"hashstring\":\"" + hashstring + "\"}";
		String resultcode = "";
		String serie = "";
//		return 0;
		try{
			String return_ = resUtil.getHttpPost(request, cis_base_url + cis_submitpayment_action, timeout);
			JSONObject json = new JSONObject(return_);
			serie = json.getString("serie");
			resultcode = json.getString("resultcode");
			return resultcode + "|" + serie;
		}catch(ConnectException ce){
			return "-9999|" + ce.getMessage();
		}catch(Exception e){
			return "-1" + e.getMessage();
		}
	}
}
