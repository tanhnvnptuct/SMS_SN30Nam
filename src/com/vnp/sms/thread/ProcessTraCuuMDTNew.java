package com.vnp.sms.thread;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

public class ProcessTraCuuMDTNew extends ManageableThreadEx{
	
//	private String mstrSmsResponse;
	private String supportCommands = "";
	private int queueSize;
	private String codeResponse = "";
	private String mstrTestMode = "";
	private String acceptIsdn = "";
	@SuppressWarnings("rawtypes")
	protected Vector responseParam = new Vector();
	@SuppressWarnings("rawtypes")
	protected Vector prizeParam = new Vector();
	private String sms_sys_err = "Xin loi QK, dich vu tam thoi gian doan, Quy khach vui long tra cuu lai sau!";
	private String sms_sai_cu_phap = "Cu phap khong dung. QK vui long soan lai cu phap de tra cuu so luong ma du thuong, chi tiet lien he 9191!";
	private String sms_0_mdt = "";
	private Connection conn = null;
	private CallableStatement calstmt = null;
	private CallableStatement detailCalstmt = null;
	private PreparedStatement preStmt = null;

	@Override
	protected void beforeSession() throws Exception {
		conn = AppServer.getConnection();
		conn.setAutoCommit(true);
		
		String sql_mdt = "{ ? = call pkg_inquiry2.fi_MDT_Count_By_CampaignId(?,?)}";
		calstmt = conn.prepareCall(sql_mdt);

		calstmt.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);
		
		String sql_mdt_detail = "{ ? = call pkg_inquiry2.fi_MDT_Detail_By_CampaignId(?,?)}";
		detailCalstmt = conn.prepareCall(sql_mdt_detail);

		detailCalstmt.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);
		
		preStmt = conn.prepareStatement("insert into winner.subs_prize_mo (id, msisdn, sms_content, campaign_id) values (winner.seq_subs_prize_mo.nextval, ?, ?, ?)");
		super.beforeSession();
	}
	
	@Override
	protected void processSession() throws Exception {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(conn);
		Database.closeObject(preStmt);
		Database.closeObject(calstmt);
		Database.closeObject(detailCalstmt);
	}
	
	@SuppressWarnings("rawtypes")
	private boolean loadFromQueue() throws AppException, Exception {
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
//		while (queueSize >= getThreadManagerEx().queueReceiver.getQueueSize()) {
		while (getThreadManagerEx().queueReceiver.getQueueSize() > 0){
			ReceiveMessage receiveMessage = (ReceiveMessage) getThreadManagerEx().queueReceiver
					.detach(prtDeliverSM);
			if (receiveMessage != null) {
				DeliverSM deliverSM = (DeliverSM) receiveMessage.getReceiveSM();

				if (deliverSM != null && deliverSM.getShortMessage() != null) {
					exists = true;
					boolean isTracuu = false;
					String sms_response_1 = "";
					String sms_response_n = "";
					int sms_substring_1 = 0;
					int sms_substring_n = 0;
					String mdt_campaign = "";
					//String checkType = "";
					String messageContent = deliverSM.getShortMessage()
							.toUpperCase().trim().replaceAll("\0","");
					String callingNumber = deliverSM.getSourceAddr()
							.getAddress();
					String calledNumber = deliverSM.getDestAddr().getAddress();
					logMonitor("Decrypting --> " + callingNumber + "|"
							+ messageContent + "|" + calledNumber);

					if(mstrTestMode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + callingNumber + "|") < 0){
						logMonitor("Test Mode: msisdn " + callingNumber + " not allowed");
					}else if ((supportCommands.indexOf("|" + messageContent + "|")) >= 0) {						
						String msisdn = callingNumber.substring(2);
						String cmd = messageContent;
						for(Object obj : responseParam){
							String command = (String) ((Vector) obj).get(0);
							sms_response_1 = (String) ((Vector) obj).get(1);
							sms_response_n = (String) ((Vector) obj).get(2);
							sms_substring_1 = Integer.parseInt( (String) ((Vector) obj).get(3));
							sms_substring_n = Integer.parseInt( (String) ((Vector) obj).get(4));
							mdt_campaign = (String) ((Vector) obj).get(5);
//							sms_0_mdt = (String) ((Vector) obj).get(6);
							//checkType = (String) ((Vector) obj).get(3);
							if(cmd.equalsIgnoreCase(command)){
								isTracuu = true;
								break;
							}
						}
						
						try{
							if(isTracuu){
								logMonitor("Tra cuu detail mdt----");
								detailCalstmt.setInt(2, Integer.parseInt(mdt_campaign));
								detailCalstmt.setString(3, callingNumber);
								detailCalstmt.execute();
								String temp = detailCalstmt.getString(1);
								logMonitor("response tra cuu:" + temp);
								String[] tempArr = temp.split("\\|");
								String lotdate = tempArr[0];
								String mdtCount = tempArr[1];				
								String sms_return = (Integer.parseInt(mdtCount)>1)?sms_response_n:sms_response_1;
								sms_return = sms_return.replaceAll("<date>",lotdate).replaceAll("<mdtcount>", String.format("%02d", Integer.parseInt(mdtCount)));
								
								if(Integer.parseInt(mdtCount) == 0){
									//sms_return = sms_return.replaceAll("<mdt>", "");			
									sms_return=sms_0_mdt;
								}else if(Integer.parseInt(mdtCount) <= 10){
									String tmp = temp.substring(temp.indexOf("|") + 1);
									sms_return = sms_return.replaceAll("<mdt>", tmp.substring(tmp.indexOf("|") + 1).replaceAll("\\|", ","));
								}else{
									String tmp = temp.substring(temp.indexOf("|") + 1);
									sms_return = sms_return.replaceAll("<mdt>", tmp.substring(tmp.indexOf("|") + 1).replaceAll("\\|", ",") + "..");
								}
								
								
								Send_sms_resp(msisdn, sms_return, codeResponse,Integer.parseInt(mdtCount),sms_substring_1,sms_substring_n);
							}	
							
							//bo sung tra cuu cho TTP cho CTKM moi
							else if(cmd.equalsIgnoreCase("TTP")){
								
								CallableStatement TTPstm = conn.prepareCall("{ ? = call pkg_inquiry2.fi_TTP(?)}");
								TTPstm.setString(2, callingNumber);
								TTPstm.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);
								TTPstm.execute();
								
								String temp = TTPstm.getString(1);
								
								logMonitor("response TTP:" + temp);
								
								if (temp.equalsIgnoreCase ("NO DATA") )
									Send_sms_resp(msisdn, sms_0_mdt, codeResponse,0,sms_substring_1,sms_substring_n);
								else
								{
									String sms_return = "Thue bao cua Quy khach thuoc "+temp;
									Send_sms_resp(msisdn, sms_return, codeResponse,0,sms_substring_1,sms_substring_n);
								}
									
								
								

							}
						}catch(Exception e){
							logMonitor("Ex: " + e.getMessage() + "|" + e.getStackTrace());
							Send_sms_resp(msisdn, sms_sys_err, codeResponse,0,0,0);
						}
					} // END COMMAND MESSAGE CONTENT
					else {
						Send_sms_resp(callingNumber.substring(2),
								sms_sai_cu_phap, codeResponse,0,0,0);					
					}
				}
			}
		}
		return exists;
	}

	public void Send_sms_resp(String msisdn, String Strtxt, String owner, int numofcodes, int substring_1, int substring_n) {
		try {
			if (Strtxt.length()>=220){
				int substr = (numofcodes>1)?substring_n:substring_1;
				
				SubmitSM submitSM1 = new SubmitSM();
				submitSM1.setShortMessage(Strtxt.substring(0,substr));
				submitSM1.getSourceAddr().setAddress(owner);
				submitSM1.getDestAddr().setAddress("84" + msisdn);
				SubmitMessage submitMessage1 = new SubmitMessage(submitSM1, false);
				

				getThreadManagerEx().queueSubmit.enqueue(submitMessage1);
				logMonitor("[SEND_SMS1]" + msisdn + " with content: " + Strtxt.substring(0,substr));
				
				SubmitSM submitSM2 = new SubmitSM();
				submitSM2.setShortMessage(Strtxt.substring(substr));
				submitSM2.getSourceAddr().setAddress(owner);
				submitSM2.getDestAddr().setAddress("84" + msisdn);
				SubmitMessage submitMessage2 = new SubmitMessage(submitSM2, false);
				

				getThreadManagerEx().queueSubmit.enqueue(submitMessage2);
				logMonitor("[SEND_SMS2]" + msisdn + " with content: " + Strtxt.substring(substr));
				
			}
			else{
				SubmitSM submitSM = new SubmitSM();
				submitSM.setShortMessage(Strtxt);
				submitSM.getSourceAddr().setAddress(owner);
				submitSM.getDestAddr().setAddress("84" + msisdn);
				SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
				logMonitor("[SEND_SMS]" + msisdn + " with content: " + Strtxt);

				getThreadManagerEx().queueSubmit.enqueue(submitMessage);
				
			}

		

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@SuppressWarnings("rawtypes")
	public void fillParameter() throws AppException {
//		mstrSmsResponse = loadMandatory("Response");
		supportCommands = loadMandatory("support-commands");
		sms_sai_cu_phap = loadMandatory("sms-sai-cu-phap");
		sms_0_mdt = loadMandatory("sms_0_mdt");
		queueSize = loadUnsignedInteger("queue-size");
		codeResponse = loadMandatory("code-response");
		acceptIsdn = loadMandatory("AcceptIsdn");
		mstrTestMode = loadMandatory("TestMode");
		Object obj = getParameter("Response");
		if (obj != null && obj instanceof Vector) {
			responseParam = (Vector) obj;
		} else {
			responseParam = new Vector();
		}

		if (responseParam == null) {
			responseParam = new Vector();
		}
		Object obj2 = getParameter("Prize");
		if (obj2 != null && obj2 instanceof Vector) {
			prizeParam = (Vector) obj2;
		} else {
			prizeParam = new Vector();
		}

		if (prizeParam == null) {
			prizeParam = new Vector();
		}
		super.fillParameter();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"load-interval", "", ParameterType.PARAM_TEXTBOX_MASK, "999",
				""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"dump-interval", "", ParameterType.PARAM_TEXTBOX_MASK, "99999",
				""));
		vtReturn.addElement(ParameterUtil
				.createParameterDefinition("queue-size", "",
						ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"support-commands", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-sai-cu-phap", "", ParameterType.PARAM_TEXTBOX_MAX, "500",
				""));
				vtReturn.addElement(ParameterUtil.createParameterDefinition(
						"sms_0_mdt", "", ParameterType.PARAM_TEXTBOX_MAX, "500",
						""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"code-response", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "1000", ""));
		
		Vector vtValue2 = new Vector();
		vtValue2.addElement("Y");
		vtValue2.addElement("N");
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_COMBOBOX, vtValue2, ""));
		
		Vector vtValue3 = new Vector();
		vtValue3.addElement(ParameterUtil.createParameterDefinition(
				"command", "", ParameterType.PARAM_TEXTBOX_MAX, "50",
				""));
		vtValue3.addElement(ParameterUtil.createParameterDefinition(
				"sms_response_1", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtValue3.addElement(ParameterUtil.createParameterDefinition(
				"sms_response_n", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));	
		vtValue3.addElement(ParameterUtil.createParameterDefinition(
				"sms_substring_1", "", ParameterType.PARAM_TEXTBOX_MAX, "50",
				""));
		vtValue3.addElement(ParameterUtil.createParameterDefinition(
				"sms_substring_n", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtValue3.addElement(ParameterUtil.createParameterDefinition(
				"mdt_campaign", "", ParameterType.PARAM_TEXTBOX_MAX, "20", ""));		
//		vtValue3.addElement(ParameterUtil.createParameterDefinition(
//				"sms_response_0", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));	

	
		
		vtReturn.addElement(createParameterDefinition("Response", "", ParameterType.PARAM_TABLE, vtValue3, ""));
		
		Vector vtValue5 = new Vector();
		vtValue5.addElement(ParameterUtil.createParameterDefinition(
				"command", "", ParameterType.PARAM_TEXTBOX_MAX, "50",
				""));
		vtValue5.addElement(ParameterUtil.createParameterDefinition(
				"prize-campaign", "", ParameterType.PARAM_TEXTBOX_MAX, "20", ""));
		vtReturn.addElement(createParameterDefinition("Prize", "", ParameterType.PARAM_TABLE, vtValue5, ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
