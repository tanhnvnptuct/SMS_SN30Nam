package com.vnp.sms.thread;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.ReceiveMessage;
import com.vnp.util.SubmitMessage;
import com.vnp.util.Util;

public class ProcessMessageServiceSale extends ManageableThreadEx{
	
	private int queueSize;
	private String supportCommands = "";
	private String codeResponse = "";
	private String mstrTestMode = "";
	private String acceptIsdn = "";
	private String sms_sai_cu_phap = "";
	private String sms_sai_thong_tin_thue_bao = "";
	private String sms_sai_cu_phap_dang_ky = "";
	private String sms_sai_thong_tin_thue_bao_eload = "";
	private String numberic = "[0-9]+";
	private CallableStatement stmt;
	
	@Override
	protected void beforeSession() throws Exception {
		mcnMain = AppServer.getConnection();
		super.beforeSession();
		initPreparedStatement();
	}
	
	@Override
	public void initPreparedStatement() throws Exception{
		stmt = mcnMain.prepareCall("{call ENQUEUE_SMS_RECEIVER(?,?,?)}");
	}
	
	@Override
	protected void processSession() throws Exception {
		getThreadManagerEx().queueReceiver.setMaxQueueSize(queueSize);
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			if (!loadFromQueue()) {
				fillLogFile();
				Thread.sleep(30);
			}
		}		
	}
	
	@Override
	public void closeDBObject() {
		super.closeDBObject();
		Database.closeObject(stmt);
		Database.closeObject(mcnMain);		
	}
	
	private boolean loadFromQueue() throws AppException, Exception{
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
		logMonitor("queue size: " + getThreadManagerEx().queueReceiver.getQueueSize());
//		while (queueSize >= getThreadManagerEx().queueReceiver.getQueueSize()){
		while (getThreadManagerEx().queueReceiver.getQueueSize() > 0){
			ReceiveMessage receiveMessage = (ReceiveMessage) getThreadManagerEx().queueReceiver.detach(prtDeliverSM);
			if (receiveMessage != null){
				DeliverSM deliverSM = (DeliverSM) receiveMessage.getReceiveSM();
				if (deliverSM != null && deliverSM.getShortMessage() != null){
					exists = true;
					String messageContent = deliverSM.getShortMessage().toUpperCase().trim();
					String callingNumber = deliverSM.getSourceAddr().getAddress();
					String calledNumber = deliverSM.getDestAddr().getAddress();
					logMonitor("Decrypting --> " + callingNumber + "|" + messageContent + "|" + calledNumber);
					if(mstrTestMode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + callingNumber + "|") < 0){
						logMonitor("Test Mode: msisdn " + callingNumber + " not allowed");
					}else if(messageContent.startsWith("Y") || messageContent.startsWith("N")){
						String id = messageContent.substring(1);		
						if(id.trim().equals("") || id.matches(numberic)){
							//Process cu phap xac nhan/tu choi
							executeProcessMessage(callingNumber, calledNumber, messageContent);
						}else{
							//Send sms sai cu phap xac nhan/tu choi
							Send_sms_resp(callingNumber, "Sai cu phap xac nhan hoac tu choi.", codeResponse);
						}
					}else if(messageContent.contains(" ")){
						String[] arr = messageContent.split(" ");
						if(arr.length > 3){
							//Send sms sai cu phap
							Send_sms_resp(callingNumber, sms_sai_cu_phap, codeResponse);
						}else if(arr.length == 2){
							//Eload nhan tin dky cho khach hang
							String invite = arr[1];	
							if(!Util.validIsdn(invite)){
								//Send sms sai thong tin thue bao khach hang
								Send_sms_resp(callingNumber, sms_sai_thong_tin_thue_bao, codeResponse);
							}else{
								executeProcessMessage(callingNumber, calledNumber, messageContent);
							}							
						}else{
							//Khach hang nhan tin dang ky dich vu
							if(!arr[1].equals("DL")){
								//Send sms sai cu phap dang ky
								Send_sms_resp(callingNumber, sms_sai_cu_phap_dang_ky, codeResponse);
							}
							String eload = arr[2];
							if(!Util.validIsdn(eload)){
								//Send sms sai thong tin thue bao eload
								Send_sms_resp(callingNumber, sms_sai_thong_tin_thue_bao_eload, codeResponse);
							}else{
								executeProcessMessage(callingNumber, calledNumber, messageContent);
							}
						}
					}else{
						//Send sms sai cu phap
						Send_sms_resp(callingNumber, sms_sai_cu_phap, codeResponse);
					}
				}
			}
		}
		return exists;
	}
	
	private boolean executeProcessMessage(String callingNumber, String calledNumber, String smsContent){
		try {
			stmt.setString(1, callingNumber);
			stmt.setString(2, calledNumber);
			stmt.setString(3, smsContent);
			stmt.execute();
			//mcnMain.commit();
			return true;
		}catch (SQLException e) {
			logMonitor("Error when push sms to DB: " + e.getMessage() + e.getStackTrace());
			return false;
		}
	}
	
	public void Send_sms_resp(String msisdn, String Strtxt, String owner) {
		try {

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
	
	public void fillParameter() throws AppException {
//		mstrSmsResponse = loadMandatory("Response");
		supportCommands = loadMandatory("support-commands");
		sms_sai_cu_phap = loadMandatory("sms-sai-cu-phap");
		sms_sai_thong_tin_thue_bao = loadMandatory("sms-sai-thong-tin-thue-bao");
		sms_sai_cu_phap_dang_ky = loadMandatory("sms-sai-cu-phap-dang-ky");
		sms_sai_thong_tin_thue_bao_eload = loadMandatory("sms-sai-thong-tin-thue-bao-eload");
		queueSize = loadUnsignedInteger("queue-size");
		codeResponse = loadMandatory("code-response");
		acceptIsdn = loadMandatory("AcceptIsdn");
		mstrTestMode = loadMandatory("TestMode");
//		Object obj = getParameter("Response");
//		if (obj != null && obj instanceof Vector) {
//			responseParam = (Vector) obj;
//		} else {
//			responseParam = new Vector();
//		}
//
//		if (responseParam == null) {
//			responseParam = new Vector();
//		}
		super.fillParameter();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
//		vtReturn.addElement(ParameterUtil.createParameterDefinition("load-interval", "", ParameterType.PARAM_TEXTBOX_MASK, "999",""));
//		vtReturn.addElement(ParameterUtil.createParameterDefinition("dump-interval", "", ParameterType.PARAM_TEXTBOX_MASK, "99999",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("queue-size", "", ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("support-commands", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("sms-sai-cu-phap", "", ParameterType.PARAM_TEXTBOX_MAX, "500",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("sms-sai-thong-tin-thue-bao", "", ParameterType.PARAM_TEXTBOX_MAX, "500",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("sms-sai-cu-phap-dang-ky", "", ParameterType.PARAM_TEXTBOX_MAX, "500",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("sms-sai-thong-tin-thue-bao-eload", "", ParameterType.PARAM_TEXTBOX_MAX, "500",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("code-response", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "99999", ""));
		
		Vector vtValue2 = new Vector();
		vtValue2.addElement("Y");
		vtValue2.addElement("N");
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_COMBOBOX, vtValue2, ""));
		
//		Vector vtValue3 = new Vector();
//		vtValue3.addElement(ParameterUtil.createParameterDefinition("command", "", ParameterType.PARAM_TEXTBOX_MAX, "50",""));
//		vtValue3.addElement(ParameterUtil.createParameterDefinition("sms_response", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
//		vtValue3.addElement(ParameterUtil.createParameterDefinition("mdt_campaign", "", ParameterType.PARAM_TEXTBOX_MAX, "20", ""));		
//		
//		Vector vtValue4 = new Vector();
//		vtValue4.addElement("Total");
//		vtValue4.addElement("Detail");
//		
//		vtValue3.addElement(createParameterDefinition("CheckType", "", ParameterType.PARAM_COMBOBOX, vtValue4, ""));
//		
//		vtReturn.addElement(createParameterDefinition("Response", "", ParameterType.PARAM_TABLE, vtValue3, ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
