package com.vnp.sms.thread;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;
import com.vnp.util.TripleDES;
import com.vnp.util.Util;

public class ProcessRefundEload extends ManageableThreadEx {

	private String select_command = "";
	private String update_command = "";
	private String eload_url = "";
	private String eload_msisdn = "";
	private String eload_user = "";
	private String eload_sha_password = "";
	private String eload_mpin = "";
	private String sms_success = "";
	private int counter = 0;
	private String mstrTestMode = "";
	private String acceptIsdn = "";
	private CallableStatement stmt;
	private PreparedStatement psReadQueue;
	private PreparedStatement psUpdateQueue;
	private String eloadSesssionId = "";
	private Map<String, String> params = new HashMap<String, String>();

	@Override
	protected void beforeSession() throws Exception {
		mcnMain = AppServer.getConnection();
		super.beforeSession();
		initPreparedStatement();
	}

	@Override
	public void initPreparedStatement() throws Exception {
		psReadQueue = mcnMain.prepareStatement(select_command);
		psUpdateQueue = mcnMain.prepareStatement(update_command);
		params = createSoapPamram();
		openEloadConnection();
	}

	private Map<String, String> createSoapPamram() {
		Map<String, String> res = new HashMap<String, String>();
		res.put("url", eload_url);
		res.put("user_name", eload_user);
		res.put("sha_password", eload_sha_password);
		res.put("source_msisdn", eload_msisdn);
		return res;
	}

	@Override
	protected void processSession() throws Exception {
		if(eloadSesssionId.trim().equals("")){
			logMonitor("Session Id null.");
			return;
		}
		TripleDES.key = TripleDES.createKey(eloadSesssionId);
		String mstrEncryptPIN = TripleDES.encrypt(eload_mpin);
		ResultSet rs = psReadQueue.executeQuery();
		while (rs.next()) {
			if(counter > 100000){
				counter = 1;
			}
			String rowId = rs.getString("row_id");			
			String targetEload = rs.getString("target_eload");
			String amount = rs.getString("amount");
			logMonitor("process row id: " + rowId + "|" + targetEload + "|" + amount + " |counter: " + counter);
			params.put("service", "EloadTransfer");
			params.put("SOAPAction", "stock_transfer");
			params.put("target_msisdn", "0" + Util.normalizeIsdn(targetEload));
			params.put("encrypt_password", mstrEncryptPIN);
			params.put("amount", amount);
			params.put("counter", String.valueOf(counter));
			String transferRes = Util.post(params).replace("<?xml version=\"1.0\" encoding=\"utf-8\"?>", "").trim();
			String status = Util.getXmlText(transferRes, "//*[name()='status']");
			String transId = Util.getXmlText(transferRes, "//*[name()='transId']");
			if(transId == null || transId.trim().equals("")) transId = "0";
			if (status != null && status.equals("0")){
				psUpdateQueue.setInt(1, 1);
				psUpdateQueue.setString(2, "Success");
				String sms = sms_success.replace("<amount>", amount);
				Send_sms_resp(Util.formatMsisdn(targetEload), sms, "1543");
			}else{
				String message = Util.getXmlText(transferRes, "//*[name()='message']").trim();
				logMonitor("tra thuong loi. message: " + message);
				psUpdateQueue.setInt(1, 9);
				psUpdateQueue.setString(2, message);
			}
			psUpdateQueue.setString(3, transId);
			psUpdateQueue.setString(4, rowId);
			psUpdateQueue.execute();
			counter++;
		}
		logMonitor("Process complete. New counter: " + counter);
		setParameter("counter", String.valueOf(counter));
		storeConfig();
	}

	private void openEloadConnection() throws Exception {
		try {
			params.put("service", "EloadLogIn");
			params.put("SOAPAction", "login");
			String logInRes = Util.post(params).replace("<?xml version=\"1.0\" encoding=\"utf-8\"?>", "").trim();
			if (!logInRes.equals("")) {
				String status = Util.getXmlText(logInRes, "//*[name()='status']");
				logMonitor("log in status:" + status);
				if (status != null && status.equals("0")) {
					eloadSesssionId = Util.getXmlText(logInRes, "//*[name()='sessionid']");
					logMonitor("Login success. Session id: " + eloadSesssionId);
				} else {
					String message = Util.getXmlText(logInRes, "//*[name()='message']");
					logMonitor("Log in false with status " + status + " with message: " + message);
					eloadSesssionId = "";
				}
			} else{
				eloadSesssionId = "";
			}
		}
		catch (Exception e) {
			logMonitor(e.getMessage());
		}
	}
	
	public void Send_sms_resp(String msisdn, String Strtxt, String owner) {
		try {
			if(mstrTestMode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + msisdn + "|") < 0){
				logMonitor("Test Mode: msisdn " + msisdn + " not allowed to send sms");
				return;
			}
			SubmitSM submitSM = new SubmitSM();
			submitSM.setShortMessage(Strtxt);
			submitSM.getSourceAddr().setAddress(owner);
			submitSM.getDestAddr().setAddress(msisdn);
			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
			logMonitor("[SEND_SMS]" + msisdn + " with content: " + Strtxt);
			getThreadManagerEx().queueSubmit.enqueue(submitMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void closeDBObject() {
		super.closeDBObject();
		Database.closeObject(stmt);
		Database.closeObject(psReadQueue);
		Database.closeObject(psUpdateQueue);
		Database.closeObject(mcnMain);
	}

	public void fillParameter() throws AppException {
		select_command = loadMandatory("select-command");
		update_command = loadMandatory("update-command");
		eload_url = loadMandatory("eload-url");
		eload_msisdn = loadMandatory("eload-msisdn");
		eload_user = loadMandatory("eload-user");
		eload_sha_password = loadMandatory("eload-sha-password");
		eload_mpin = loadMandatory("eload-mpin");
		sms_success = loadMandatory("sms-success");
		counter = loadInteger("counter");
		acceptIsdn = loadMandatory("AcceptIsdn");
		mstrTestMode = loadMandatory("TestMode");
		super.fillParameter();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(ParameterUtil.createParameterDefinition("select-command", "",
				ParameterType.PARAM_TEXTBOX_MASK, "10000", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("update-command", "",
				ParameterType.PARAM_TEXTBOX_MAX, "10000", ""));
		vtReturn.addElement(
				ParameterUtil.createParameterDefinition("eload-url", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("eload-msisdn", "", ParameterType.PARAM_TEXTBOX_MAX,
				"500", ""));
		vtReturn.addElement(
				ParameterUtil.createParameterDefinition("eload-user", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("eload-sha-password", "",
				ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("eload-mpin", "",
				ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(
				ParameterUtil.createParameterDefinition("counter", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("sms-success", "",
				ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "99999", ""));
		Vector vtValue2 = new Vector();
		vtValue2.addElement("Y");
		vtValue2.addElement("N");
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_COMBOBOX, vtValue2, ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
