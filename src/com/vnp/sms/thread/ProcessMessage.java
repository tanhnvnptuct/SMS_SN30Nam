/**
 * 
 *
 * process message for xu ly tin nhan spam
 */

package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class ProcessMessage extends ManageableThreadEx{
	int queueSize;
	String supportCommands = "";
	
	private Connection conn = null;
	private Statement stmt = null;
	private PreparedStatement getSpamItemStatement = null;
	
	private String sql_get_spam_item = "select * from spam_list where report_sub = ? and spam_sub is null and create_date >= trunc(sysdate)";
	
	private String codeResponse = "";
	
	private String sms_sai_cu_phap = "Cu phap khong dung. QK vui long nhan <Thue bao spam> <dau cach> <noi dung spam> de thong bao dau so gui tin spam, chi tiet lien he 9191!";
	private String sms_thanh_cong = "Cam on ban da cung cap thong tin va tin nhan rac.";
	private String sms_loi_he_thong = "Xin loi quy khach. Dich vu tam thoi bi gian doan. Quy khach vui long thao tac lai sau";
	private String sms_loi_luu_tin_nhan = "Xin loi quy khach. Da xay ra loi trong qua trinh xu ly. Quy khach vui long thao tac lai.";
	private String sms_moi_gui_spam_sub = "De xu ly nguon gui tin nhan rac, de nghi ban soan tin theo cu phap  NGUON_GUI va gui 9409 (trong do NGUON_GUI la Thue bao, Dau so hoặc Brandname gửi tin rac)";
	private String sms_qua_thoi_han_xac_nhan = "Thoi gian xac nhan thong tin spam cua QK da het. Vui long thao tac lai de thong bao dau so spam. Chi tiet lien he 9191!";
	
	private CacheManager cm = CacheManager.newInstance("C:/TinNhanSpam 9409/ehcache.xml");//CacheManager.newInstance("src/config/ehcache.xml");
	
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
	protected void beforeSession() throws Exception {
		super.beforeSession();
	}

	@Override
	protected void processSession() throws Exception {
		conn = AppServer.getConnection();
		conn.setAutoCommit(true);
		stmt = conn.createStatement();
		getSpamItemStatement = conn.prepareStatement(sql_get_spam_item);
		getThreadManagerEx().queueReceiver.setMaxQueueSize(queueSize);
		while (miThreadCommand != ThreadConstant.THREAD_STOP) {
			if (!loadFromQueue()) {
				fillLogFile();
				Thread.sleep(30);
			}
		}
	}
	
	private boolean loadFromQueue() throws AppException, Exception {
		// logMonitor("Process msg from queue");
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
//		String spamMsisdn = "";
//		String content = "";
		logMonitor("process queue");

		// while(queueSize >= getThreadManagerEx().queueRequest.getQueueSize())
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
//						String sql_mt = "insert into sms_mt(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
//								+ callingNumber
//								+ "',sysdate,'"
//								+ sms_thanh_cong + "',0, sysdate)";
//						logMonitor("Chen du lieu MT vao DB|" + sql_mt);
//						stmt.executeUpdate(sql_mt);
//						Send_sms_resp(callingNumber.substring(2),
//								sms_thanh_cong, codeResponse);
//					}else{
//						String sql_mt = "insert into sms_mt(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
//								+ callingNumber
//								+ "',sysdate,'"
//								+ sms_sai_cu_phap + "',0, sysdate)";
//						logMonitor("Chen du lieu MT vao DB|" + sql_mt);
//						stmt.executeUpdate(sql_mt);
//						Send_sms_resp(callingNumber.substring(2),
//								sms_sai_cu_phap, codeResponse);							
//					}
					
					//cacheonix test
//					boolean keyExists = cache.containsKey(callingNumber);
//					if(keyExists){
//						String value = cache.get(callingNumber);
//						if(!value.isEmpty()){
//							
//						}
//					}else{
//						String replacedValue1 = cache.put(callingNumber, messageContent);
//						if(!replacedValue1.isEmpty()){
//							
//						}
//					}
					
					//ehcache test
					try{
						Cache cache = cm.getCache("spamcache");
						Element ele = cache.get(callingNumber);
						if(ele == null){
							logMonitor("do not have cache item");
							getSpamItemStatement.setString(1, callingNumber);
							ResultSet chk = getSpamItemStatement.executeQuery();
							if(chk.next()){
								int id = chk.getInt("id");
								String sql_move_spam_item = "insert into spam_list_tmp select * from spam_list where id = " + id;
								stmt.executeUpdate(sql_move_spam_item);
								String sql_remove_spam_item = "delete from spam_list where id = " + id;
								logMonitor("execute remove spam list " + sql_remove_spam_item);
								stmt.executeUpdate(sql_remove_spam_item);
								Send_sms_resp(callingNumber.substring(2),
										sms_qua_thoi_han_xac_nhan, codeResponse);
							}else{																
								String sql_insert_spam_sub = "insert into spam_list(id, content, report_sub)"
										+ " values(spam_list_id_seq.nextval, '"
										+ messageContent
										+ "','"
										+ callingNumber + "')";
								stmt.executeUpdate(sql_insert_spam_sub);
								chk = getSpamItemStatement.executeQuery();
								if(chk.next()){
									cache.put(new Element(callingNumber,chk.getInt("id")));
									Send_sms_resp(callingNumber.substring(2),
											sms_moi_gui_spam_sub, codeResponse);
								}else{
									Send_sms_resp(callingNumber.substring(2),
											sms_loi_luu_tin_nhan, codeResponse);
								}								
							}
							
						}else{
							String id = ele.getObjectValue().toString();
							//update spam sub for report
							String sql_update_spam_sub = "update spam_list set spam_sub = '"
									+ messageContent
									+ "' where id = "
									+ id;
							logMonitor("execute update spam list " + sql_update_spam_sub);
							stmt.executeUpdate(sql_update_spam_sub);
							cache.remove(callingNumber);
							Send_sms_resp(callingNumber.substring(2),
									sms_thanh_cong, codeResponse);
						}
					}catch(Exception e){
						logMonitor("error when process report" + e.getMessage());
						Send_sms_resp(callingNumber.substring(2),
								sms_loi_he_thong, codeResponse);
					}					
				}
			}
		}
		return exists;
	}
	
//	private boolean validSpamIsdn(String isdn){
//		try{
//			if(!isdn.matches("[0-9]+")){
//				logMonitor("isdn not only content number");
//				return false;
//			}
//			String normalizeIsdn = Util.normalizeIsdn(isdn);
//			if(normalizeIsdn.startsWith("9") || normalizeIsdn.startsWith("8")){
//				return normalizeIsdn.length() == 9;
//			}else if(normalizeIsdn.startsWith("1")){
//				return normalizeIsdn.length() == 10;
//			}else{
//				return false;
//			}
//			
//		}catch(Exception e){
//			return false;
//		}		
//	}

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
