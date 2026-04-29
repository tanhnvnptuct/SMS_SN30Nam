package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.Statement;
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

public class BrandnameSMS extends ManageableThreadEx{
	int queueSize;
	String supportCommands = "";
	
	private Connection conn = null;
	private Statement stmt = null;
	
	String codeResponse = "";
	
	private String sms_sai_cu_phap = "test send sms";//"Cu phap khong dung. QK vui long nhan SP <dau cach> <Thue bao spam> <dau cach> <noi dung spam> de thong bao dau so gui tin spam, chi tiet lien he 9191!";
	private String sms_thanh_cong = "Cam on quy khach da gui thong tin phan hoi ve dau so tin nhan spam.";
	
	public void fillParameter() throws AppException {
		queueSize = loadUnsignedInteger("queue-size");
		supportCommands = loadMandatory("support-commands");
		codeResponse = loadMandatory("code-response");

		super.fillParameter();
	}
	
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(ParameterUtil
				.createParameterDefinition("queue-size", "",
						ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"support-commands", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
//		vtReturn.addElement(ParameterUtil.createParameterDefinition(
//				"sms-response", "", ParameterType.PARAM_TEXTBOX_MAX, "300", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"code-response", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));

		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
	
	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(conn);
		Database.closeObject(stmt);
	}

	@Override
	protected void processSession() throws Exception {
		conn = AppServer.getConnection();
		conn.setAutoCommit(true);
		stmt = conn.createStatement();
		Send_sms_resp("941233764",
				sms_sai_cu_phap, codeResponse);
		getThreadManagerEx().queueReceiver.setMaxQueueSize(queueSize);
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			if (!loadFromQueue()) {
				fillLogFile();
				//Thread.sleep(30);
			}
		}	
	}
	
	private boolean loadFromQueue() throws AppException, Exception {
		// logMonitor("Process msg from queue");
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
		String spamMsisdn = "";
		String content = "";

		// while(queueSize >= getThreadManagerEx().queueRequest.getQueueSize())
		if (queueSize >= getThreadManagerEx().queueReceiver.getQueueSize()) {
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
					
					
//					int spaceIndex = messageContent.indexOf(" ");
//					if(spaceIndex > 0){
//						spamMsisdn = messageContent.substring(0, spaceIndex).trim();
//						content = messageContent.substring(spaceIndex + 1);						
//					}else{
//						spamMsisdn = messageContent;						
//					}
//					if(validSpamIsdn(spamMsisdn)){							
//						String sql_mo = "insert into spam_list(id, content, spam_sub, report_sub)"
//								+ " values(spam_list_id_seq.nextval, '"
//								+ content
//								+ "','"
//								+ Util.formatMsisdn(spamMsisdn)
//								+"', '"
//								+ callingNumber + "')";
//						logMonitor("Chen du lieu vao DB|" + sql_mo);
//						stmt.executeUpdate(sql_mo);
//						Send_sms_resp(callingNumber.substring(2),
//								sms_thanh_cong, codeResponse);
//					}else{
//						Send_sms_resp(callingNumber.substring(2),
//								sms_sai_cu_phap, codeResponse);							
//					}
				}
			}
		}
		return exists;
	}
	
	private boolean validSpamIsdn(String isdn){
		try{
			if(!isdn.matches("[0-9]+")){
				logMonitor("isdn not only content number");
				return false;
			}
			String normalizeIsdn = Util.normalizeIsdn(isdn);
			if(normalizeIsdn.startsWith("9") || normalizeIsdn.startsWith("8")){
				return normalizeIsdn.length() == 9;
			}else if(normalizeIsdn.startsWith("1")){
				return normalizeIsdn.length() == 10;
			}else{
				return false;
			}
			
		}catch(Exception e){
			return false;
		}		
	}

	public void Send_sms_resp(String msisdn, String Strtxt, String owner) {
		try {

			SubmitSM submitSM = new SubmitSM();
			submitSM.setShortMessage(Strtxt);
			submitSM.getSourceAddr().setAddress(owner);
			submitSM.getDestAddr().setAddress("84" + msisdn);
			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
			logMonitor("[SEND_SMS]" + msisdn + " with content: " + Strtxt);

			getThreadManagerEx().queueSubmit.enqueue(submitMessage);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
