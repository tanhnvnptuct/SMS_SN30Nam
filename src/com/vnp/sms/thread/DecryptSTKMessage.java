package com.vnp.sms.thread;

import java.io.*;
import com.logica.smpp.pdu.*;
import com.vnp.util.AppServer;
import com.vnp.util.ReceiveMessage;
import com.vnp.util.SubmitMessage;

import java.util.*;
//import com.fss.mwallet.util.*;
import java.util.Vector;
import com.fss.thread.*;
import com.fss.dictionary.*;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.util.AppException;
import com.logica.smpp.Data;
//import com.fss.mwallet.stk.*;
import com.fss.sql.*;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import oracle.jdbc.*;
import oracle.jdbc.pool.OracleDataSource;
//import vnp_services_pkg.*;
//import com.vnp.ws.UCIP.*;
import com.comverse_in.prepaid.ccws.*;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;
//import com.vnp.ws.PPSViNa.*;
import org.apache.log4j.MDC;

public class DecryptSTKMessage extends ManageableThreadEx {
	int loadInterval;
	int queueSize;
	int dumpInterval;
	String defaultCommandID = "";
	String commandCode = "";
	String serviceCode = "";
	String commandName = "";
	String supportCommands = "";

	String smsResponse = "";
	String smsResponse2 = "";
	String smsResponse_mvnp_no_mdt = "";
	String smsResponsed2 = "";
	String smsResponsekq = "";

	String codeResponse = "";

	private String arr[] = null;
	public static Hashtable ht;

	private String sms_sys_err = "Xin loi QK, dich vu tam thoi gian doan, Quy khach vui long tra cuu lai sau!";
	private String sms_sai_cu_phap = "Cu phap khong dung. QK vui long soan lai cu phap de tra cuu so luong ma du thuong, chi tiet lien he 9191!";
//	private String sms_sai_cu_phap_1529 = "Cu phap khong dung. QK vui long nhan MDT gui 1529 de tra cuu so luong ma du thuong, chi tiet lien he 9191!";

	private Connection conn = null;
	private Statement stmt = null;
	private Statement stmtC = null;

	private CallableStatement calstmt = null;
	private CallableStatement calstmtqnvang = null;
	private CallableStatement calstmtvinhlong = null;
	private CallableStatement calstmd2 = null;
	private CallableStatement calstmtFull = null;

	// private CallableStatement calstmtd2;

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

	public DecryptSTKMessage() {
		loadInterval = 1;
		queueSize = 10000;
		dumpInterval = 300;

		ht = new Hashtable();
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					"CONF\\addrMAPTalkEZ.conf"));
			String str;
			while ((str = in.readLine()) != null) {
				String arr[] = str.split(",");
				ht.put(arr[0].trim(), arr[1].trim());

			}
			logMonitor("[INFO]>>load [addr.conf] complete:" + ht.size()
					+ " record");
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// /////////////////////////
	}

	public void fillParameter() throws AppException {
		loadInterval = loadUnsignedInteger("load-interval");
		queueSize = loadUnsignedInteger("queue-size");
		dumpInterval = loadUnsignedInteger("dump-interval");
		defaultCommandID = loadMandatory("default-commandid");
		supportCommands = loadMandatory("support-commands");
		
		smsResponse = loadMandatory("sms-response");
		smsResponse2 = loadMandatory("sms-response2");
		sms_sai_cu_phap = loadMandatory("sms-sai-cu-phap");
		smsResponse_mvnp_no_mdt = loadMandatory("sms-response-mvnp-0");
		smsResponsed2 = loadMandatory("sms-response-d2");
		smsResponsekq = loadMandatory("sms-response-kq");
		codeResponse = loadMandatory("code-response");

		super.fillParameter();
	}

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
				"default-commandid", "", ParameterType.PARAM_TEXTBOX_MASK,
				"999", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"support-commands", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));

		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-response", "", ParameterType.PARAM_TEXTBOX_MAX, "300", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-response2", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-sai-cu-phap", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-response-mvnp-0", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-response-kq", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms-response-d2", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
				""));
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
		Database.closeObject(calstmt);
		Database.closeObject(calstmtqnvang);
		Database.closeObject(calstmtvinhlong);
		Database.closeObject(calstmd2);
		Database.closeObject(calstmtFull);
	}

	public void processSession() throws AppException {

		try {
			conn = AppServer.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			// stmtC=conn.createStatement();

			String sql_mdt = "{ ? = call pkg_inquiry2.fi_MDT_Count_Iphone_Ezpay(?)}";
			calstmt = conn.prepareCall(sql_mdt);

			calstmt.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

//			String sql_mdtcmu = "{ ? = call pkg_inquiry2.fi_MDT_Count_cmu(?)}";
			String sql_mdtmyvnp = "{ ? = call pkg_inquiry2.fi_MDT_Count_Iphone_MYVNP(?)}";
			calstmtqnvang = conn.prepareCall(sql_mdtmyvnp);

			calstmtqnvang.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

			String sql_mdtvinhlong = "{ ? = call pkg_inquiry2.fi_MDT_Count_Iphone_VINHLONG(?)}";
			calstmtvinhlong = conn.prepareCall(sql_mdtvinhlong);

			calstmtvinhlong.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

			String sql_mdtd2 = "{ ? = call pkg_inquiry2.fi_MDT_kq_d2(?)}";
			calstmd2 = conn.prepareCall(sql_mdtd2);

			calstmd2.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

			String sql_full = "{ ? = call pkg_mdt.process_mdt(?,?)}";
			calstmtFull = conn.prepareCall(sql_full);
			calstmtFull
					.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

			// calstmt = conn.prepareCall(arg0);

			getThreadManagerEx().queueReceiver.setMaxQueueSize(queueSize);
			while (miThreadCommand != ThreadConstant.THREAD_STOP) {
				if (!loadFromQueue()) {
					fillLogFile();
					Thread.sleep(30);
				}
			}
		} catch (Exception ex) {
			logMonitor("Error occurs: " + ex.getMessage());
		}
	}

	/**
	 * Synschronize request message directly from SMS receiver queue
	 * 
	 * @throws AppException
	 * @throws Exception
	 */
	private boolean loadFromQueue() throws AppException, Exception {
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
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

					if ((supportCommands.indexOf("|" + messageContent + "|")) >= 0) {

						// * process REQ insert 2 DB
						String msisdn = callingNumber.substring(2);
						String cmd = messageContent;
						String act = "";

						if (cmd.equalsIgnoreCase("MDT")) {
							String sms_return = smsResponse;
							try {
								String sql_mo = "insert into sms_mo(id, msisdn, receive_time, sms_content, status) values(seq_sms_mo.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ messageContent + "',1)";
								logMonitor("Chen du lieu MO vao DB|" + sql_mo);
								stmt.executeUpdate(sql_mo);

								calstmt.setString(2, callingNumber);
								calstmt.execute();

								String temp = calstmt.getString(1);

								sms_return = sms_return.replace("<mdt>",
										temp.substring(0, temp.indexOf("|")));
								sms_return = sms_return.replace("<date>",
										temp.substring(temp.indexOf("|") + 1));

								logMonitor("Sending message to "
										+ callingNumber + " with content: "
										+ sms_return);

								Send_sms_resp(msisdn, sms_return, codeResponse);

								// Chen du lieu vao SMS_MT
								String sql_mt = "insert into sms_mt(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ sms_return + "',1, sysdate)";
								logMonitor("Chen du lieu MT vao DB|" + sql_mt);
								stmt.executeUpdate(sql_mt);

							} catch (Exception e) {
								Send_sms_resp(msisdn, sms_sys_err, codeResponse);
								e.printStackTrace();
							}
						}
						
						else if (cmd.equalsIgnoreCase("QNVANG")) {
							String sms_return = smsResponse2;
							try {
								String sql_mo = "insert into sms_mo(id, msisdn, receive_time, sms_content, status) values(seq_sms_mo.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ messageContent + "',1)";
								logMonitor("Chen du lieu MO vao DB|" + sql_mo);
								stmt.executeUpdate(sql_mo);

								calstmtqnvang.setString(2, callingNumber.substring(2));
								calstmtqnvang.execute();

								String temp = calstmtqnvang.getString(1);
								String mdt = temp.substring(0, temp.indexOf("|"));
								logMonitor("ket qua tra cuu :" + temp);
								sms_return = sms_return.replace("<mdt>",mdt);
								sms_return = sms_return.replace("<date>",
										temp.substring(temp.indexOf("|") + 1));
								logMonitor("Sending message to "
										+ callingNumber + " with content: "
										+ sms_return);

								Send_sms_resp(msisdn, sms_return, codeResponse);

								// Chen du lieu vao SMS_MT
								String sql_mt = "insert into sms_mt(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ sms_return + "',1, sysdate)";
								logMonitor("Chen du lieu MT vao DB|" + sql_mt);
								stmt.executeUpdate(sql_mt);

							} catch (Exception e) {
								Send_sms_resp(msisdn, sms_sys_err, codeResponse);
								e.printStackTrace();
							}
						} else if (cmd.equalsIgnoreCase("VL9999")) {
							String sms_return = smsResponsed2;
							try {
								String sql_mo = "insert into sms_mo(id, msisdn, receive_time, sms_content, status) values(seq_sms_mo.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ messageContent + "',1)";
								logMonitor("Chen du lieu MO vao DB|" + sql_mo);
								stmt.executeUpdate(sql_mo);

								calstmtvinhlong.setString(2, callingNumber.substring(2));
								calstmtvinhlong.execute();

								String temp = calstmtvinhlong.getString(1);
								String mdt = temp.substring(0, temp.indexOf("|"));
								logMonitor("ket qua tra cuu :" + temp);
								sms_return = sms_return.replace("<mdt>",mdt);
								sms_return = sms_return.replace("<date>",
										temp.substring(temp.indexOf("|") + 1));
								logMonitor("Sending message to "
										+ callingNumber + " with content: "
										+ sms_return);

								Send_sms_resp(msisdn, sms_return, codeResponse);

								// Chen du lieu vao SMS_MT
								String sql_mt = "insert into sms_mt(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ sms_return + "',1, sysdate)";
								logMonitor("Chen du lieu MT vao DB|" + sql_mt);
								stmt.executeUpdate(sql_mt);

							} catch (Exception e) {
								Send_sms_resp(msisdn, sms_sys_err, codeResponse);
								e.printStackTrace();
							}
						} else if(cmd.equalsIgnoreCase("CAMAU")){
							
						}

					} // END COMMAND MESSAGE CONTENT
					else {
						Send_sms_resp(callingNumber.substring(2),
								sms_sai_cu_phap, codeResponse);					
					}
				}
			}
		}
		return exists;
	}

	public static void main(String bkv[]) {
		DecryptSTKMessage t = new DecryptSTKMessage();

	}
}