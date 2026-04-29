package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.vnp.util.VasProApi;

public class ProcessMessage9029 extends ManageableThreadEx{
	int queueSize;
	
	private Connection conn = null;
	private Statement stmt = null;
	
	private String codeResponse = "";
	private String sms_sai_cu_phap = "";
	private String supportCommands = "";
	private String sms_loi_he_thong = "";
	private String sms_tu_choi = "";
	private String test_mode = "";
	private String acceptIsdn = "";
	
	public void fillParameter() throws AppException {
		queueSize = loadUnsignedInteger("queue-size");
		codeResponse = loadMandatory("code-response");
		sms_sai_cu_phap = loadMandatory("sms_sai_cu_phap");
		sms_loi_he_thong = loadMandatory("sms_loi_he_thong");
		sms_tu_choi = loadMandatory("sms_tu_choi");
		supportCommands = loadMandatory("supportCommands");
		test_mode = loadMandatory("TestMode");
		acceptIsdn = loadMandatory("AcceptIsdn");
		super.fillParameter();
	}
	
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(ParameterUtil
				.createParameterDefinition("queue-size", "",
						ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"supportCommands", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"code-response", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_loi_he_thong", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_tu_choi", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_sai_cu_phap", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"TestMode", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));

		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
	
	@Override
	public void beforeSession() throws Exception {
		super.beforeSession();
		initPreparedStatement();
	}
	
	public void initPreparedStatement() throws Exception {
		//get connection
		conn = AppServer.getConnection();
		conn.setAutoCommit(true);
		
		//create/prepare statement
		stmt = conn.createStatement();
	}
	
	@Override
	protected void processSession() throws Exception {		
		//run process
		getThreadManagerEx().queueReceiver.setMaxQueueSize(queueSize);
		while (miThreadCommand != ThreadConstant.THREAD_STOP) {
			if (!loadFromQueue()) {
				fillLogFile();
				Thread.sleep(30);
			}
		}
	}
	
	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(conn);
		Database.closeObject(stmt);
	}
	
	private boolean loadFromQueue() throws AppException, Exception {
		// logMonitor("Process msg from queue");
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
		logMonitor("process queue");

		while (queueSize >= getThreadManagerEx().queueReceiver.getQueueSize()) {
			ReceiveMessage receiveMessage = (ReceiveMessage) getThreadManagerEx().queueReceiver
															.detach(prtDeliverSM);			
			if (receiveMessage != null) {
				DeliverSM deliverSM = (DeliverSM) receiveMessage.getReceiveSM();

				if (deliverSM != null && deliverSM.getShortMessage() != null) {
					exists = true;
					String messageContent = deliverSM.getShortMessage()
							.toUpperCase().trim();
					String callingNumber = deliverSM.getSourceAddr()
							.getAddress();
					String calledNumber = deliverSM.getDestAddr().getAddress();
					logMonitor("Decrypting --> " + callingNumber + "|"
							+ messageContent + "|" + calledNumber);
					if ((supportCommands.indexOf("|" + messageContent + "|")) >= 0){
						String cmd = messageContent;
						if (cmd.equalsIgnoreCase("HUY_KM")){
							try{
								String black_list_chk = "select * from blacklist_9029 where msisdn =  '" + callingNumber + "'";
								ResultSet blackItem = stmt.executeQuery(black_list_chk);
								if(!blackItem.next()){
									String insert_black_list = "insert into blacklist_9029 (msisdn, status) values ('" + callingNumber + "',1)";
									stmt.execute(insert_black_list);
									conn.commit();									
								}
								Send_sms_resp(callingNumber, sms_tu_choi, codeResponse);
							}catch(Exception e){
								Send_sms_resp(callingNumber, sms_loi_he_thong, codeResponse);
							}
						}
					}else {
						Send_sms_resp(callingNumber, sms_sai_cu_phap, codeResponse);					
					}
				}
			}
		}
		conn.commit();
		return exists;
	}
	
	public void Send_sms_resp(String msisdn, String Strtxt, String owner) {
		try {
			if(test_mode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + msisdn + "|") < 0){
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
}
