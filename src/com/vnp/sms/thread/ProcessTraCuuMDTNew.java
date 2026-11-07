package com.vnp.sms.thread;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import org.smpp.*;
import org.smpp.pdu.*;
import org.smpp.util.ByteBuffer;
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
	private static int seq = 1;
	private static int max_seq = 2147483647;
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
		
		int batchSize = 0;
		
//		CallableStatement ps = conn.prepareCall("insert into sms_quay_thuong "
//				+ "(id,msisdn,log_date,smo,status) values (seq_sms_quay_thuong.NEXTVAL,?,sysdate,?,0)");

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
								}
								else if(Integer.parseInt(mdtCount) <= 10){
									String tmp = temp.substring(temp.indexOf("|") + 1);
									sms_return = sms_return.replaceAll("<mdt>", tmp.substring(tmp.indexOf("|") + 1).replaceAll("\\|", ","));
								}else{
									String tmp = temp.substring(temp.indexOf("|") + 1);
									sms_return = sms_return.replaceAll("<mdt>", tmp.substring(tmp.indexOf("|") + 1).replaceAll("\\|", ",") + "..");
								}
								
								
								//Send_sms_resp(msisdn, sms_return, codeResponse);
								Send_sms_resp_UTF16(msisdn, sms_return);
							}	
							
							//bo sung tra cuu cho cho CTKM moi
//							else if(cmd.equalsIgnoreCase("30NAM")){
//								
//								ps.setString(1, callingNumber);
//								ps.setString(2, cmd);
////								ps.addBatch();
//								ps.execute();
////								batchSize++;
//								logMonitor("30 Nam:" + callingNumber);
//							}
							
							
						}catch(Exception e){
							logMonitor("Ex: " + e.getMessage() + "|" + e.getStackTrace());
							//Send_sms_resp(msisdn, sms_sys_err, codeResponse);
							Send_sms_resp_UTF16(msisdn, sms_sys_err);
						}
					} // END COMMAND MESSAGE CONTENT
					else {
//						Send_sms_resp(callingNumber.substring(2),
//								sms_sai_cu_phap, codeResponse);		
						Send_sms_resp_UTF16(callingNumber.substring(2),
								sms_sai_cu_phap);		
					}
				}
			}
		}
//		if (batchSize>0)
//			ps.executeBatch();
		
		return exists;
	}

	public void Send_sms_resp(String msisdn, String Strtxt, String owner) {
		try {
			
			int seqSize = 150;
			int seqCount = Strtxt.length() / seqSize + (Strtxt.length() % seqSize == 0 ? 0 : 1);
			List<String> messages = new ArrayList<String>();
			for (int iSeq = 0; iSeq < seqCount; iSeq++) {
				int start = iSeq * seqSize;
				int end = Math.min((iSeq + 1) * seqSize, Strtxt.length());

				String strbuff = Strtxt.substring(start, end);
				messages.add(strbuff);
			}

			short sarRef = (short) new Random().nextInt(255);
			int messageSize = messages.size();
			int orginSeq = 0;
			
			for (int i = 0; i < messageSize; i++) {
				String strMsgBuff = messages.get(i);
				
				SubmitSM submitSM = new SubmitSM();
				submitSM.setShortMessage(strMsgBuff);
				submitSM.getSourceAddr().setAddress(owner);
				submitSM.getDestAddr().setAddress("84" + msisdn);
				if (messageSize > 1) {
					submitSM.setSarMsgRefNum(sarRef);
					submitSM.setSarTotalSegments((short) messageSize);
					submitSM.setSarSegmentSeqnum((short) (i + 1));
	            }
				submitSM.setDataCoding((byte) 0x08);
				SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
				getThreadManagerEx().queueSubmit.enqueue(submitMessage);

			}

			
			
//			SubmitSM submitSM = new SubmitSM();
//			submitSM.setShortMessage(Strtxt);
//			submitSM.getSourceAddr().setAddress(owner);
//			submitSM.getDestAddr().setAddress("84" + msisdn);
//			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
			logMonitor("[SEND_SMS]" + msisdn + " with content: " + Strtxt);

//			getThreadManagerEx().queueSubmit.enqueue(submitMessage);
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	protected void Send_sms_resp_UTF16(String strIsdn, String strMessage) throws Exception {
		try {
			
			while (getThreadManagerEx().queueSubmit.size() > 100000) {
				Thread.sleep(100);
				logMonitor("Queue full...");
			}
			Charset encode = onlyGSM7BitCharacter(strMessage);

			byte coding = autoDataCoding(encode);

			List<String> messages = new ArrayList<String>();


			int seqSize = 134;
			if (encode == Charset.forName(Data.ENC_UTF16_BE))
				seqSize = 67;
			else
				seqSize = 150;

			int seqCount = strMessage.length() / seqSize + (strMessage.length() % seqSize == 0 ? 0 : 1);
			for (int iSeq = 0; iSeq < seqCount; iSeq++) {
				int start = iSeq * seqSize;
				int end = Math.min((iSeq + 1) * seqSize, strMessage.length());
				String strbuff = strMessage.substring(start, end);
				messages.add(strbuff);
			}

			short sarRef = (short) new Random().nextInt(255);
			int messageSize = messages.size();

			for (int i = 0; i < messageSize; i++) {
				String strbuff = messages.get(i);
				SubmitSM request = new SubmitSM();
				request.getDestAddr().setAddress("84"+strIsdn);

		        request.getSourceAddr().setAddress(codeResponse);

				request.setShortMessage(strbuff,encode.name());
				request.setDataCoding(coding);
													
				int newSeq = (seq++ % max_seq) + 1;
				request.setSequenceNumber(newSeq);

				if (messageSize > 1) {
					request.setSarMsgRefNum(sarRef);
					request.setSarTotalSegments((short) messageSize);
					request.setSarSegmentSeqnum((short) (i + 1));
				}

				SubmitMessage submitMessage = new SubmitMessage(request, false);
				getThreadManagerEx().queueSubmit.enqueue(submitMessage);
			}
			logMonitor("[SEND_SMS UTF16]" + strIsdn + " with content: " + strMessage);
//			
		} catch (Exception e) {
			logMonitor("error when send sms for " + strIsdn + " with content " + strMessage + " Exception "
					+ e.getMessage());
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
	
	public static Charset onlyGSM7BitCharacter(String strContent) {
		CharsetEncoder encoder;
		encoder = Charset.forName("ISO-8859-1").newEncoder();

		if (encoder.canEncode(strContent)) {
			return Charset.forName(Data.ENC_ISO8859_1);
		} else {
			return Charset.forName(Data.ENC_UTF16_BE);
		}
	}

	private byte autoDataCoding(Charset encode) {
		if (encode.equals(Charset.forName(Data.ENC_UTF16_BE)))
			return ((byte) 0x08);
		else
			return ((byte) 0x00);// return ((byte) 0x00);
	}
}
