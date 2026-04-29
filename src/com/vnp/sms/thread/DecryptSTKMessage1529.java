package com.vnp.sms.thread;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Hashtable;
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

public class DecryptSTKMessage1529 extends ManageableThreadEx {
	int loadInterval;
	int queueSize;
	int dumpInterval;
	String defaultCommandID = "";
	String commandCode = "";
	String serviceCode = "";
	String commandName = "";
	String supportCommands = "";

	String smsResponse = "";
	String smsResponsecmu = "";
	String smsResponsed2 = "";
	String smsResponsekq = "";

	String codeResponse = "";

	private String arr[] = null;
	public static Hashtable ht;

	// now date = new now();
	// private String code="1714";
	// private String code="1529";

	private String sms_sys_err = "Xin loi QK, dich vu tam thoi gian doan, Quy khach vui long tra cuu lai sau!";
	private String sms_sai_cu_phap = "Cu phap khong dung. QK vui long nhan MDTHW de tra cuu so luong ma du thuong, chi tiet lien he 9191!";
	private String sms_sai_cu_phap_1529 = "Cu phap khong dung. QK vui long nhan MDT gui 1529 de tra cuu so luong ma du thuong, chi tiet lien he 9191!";

	private Connection conn = null;
	private Statement stmt = null;
	private Statement stmtC = null;

	private CallableStatement calstmt = null;
	private CallableStatement calstmtcmu = null;
	private CallableStatement calstmtcmu2 = null;
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

	public DecryptSTKMessage1529() {
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
		smsResponsecmu = loadMandatory("sms-response-cmu");
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
				"sms-response-cmu", "", ParameterType.PARAM_TEXTBOX_MAX, "300",
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
		Database.closeObject(calstmtcmu);
		Database.closeObject(calstmtcmu2);
		Database.closeObject(calstmd2);
		Database.closeObject(calstmtFull);
	}

	public void processSession() throws AppException {

		try {
			conn = AppServer.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			// stmtC=conn.createStatement();

			String sql_mdt = "{ ? = call pkg_inquiry2.fi_MDT_Count_Iphone_HW(?)}";
			calstmt = conn.prepareCall(sql_mdt);

			calstmt.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

//			String sql_mdtcmu = "{ ? = call pkg_inquiry2.fi_MDT_Count_cmu(?)}";
			String sql_mdtcmu = "{ ? = call pkg_inquiry2.fi_MDT_Count_Iphone(?)}";
			calstmtcmu = conn.prepareCall(sql_mdtcmu);

			calstmtcmu.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

			String sql_mdtcmu2 = "{ ? = call pkg_inquiry2.fi_MDT_kq_cmu(?)}";
			calstmtcmu2 = conn.prepareCall(sql_mdtcmu2);

			calstmtcmu2
					.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);

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
		// logMonitor("Process msg from queue");
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;

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

					if ((supportCommands.indexOf("|" + messageContent + "|")) >= 0) {

						// * process REQ insert 2 DB
						String msisdn = callingNumber.substring(2);
						String cmd = messageContent;
						String act = "";

						if (cmd.equalsIgnoreCase("MDT")) {
							String sms_return = smsResponse;
							// Send_sms_resp(msisdn,sms_return,codeResponse);

							// Comment do chuong trinh 20 nam ket thuc

							try {

								// Chen thong tin SMS_MO luu Log

								String sql_mo = "insert into sms_mo(id, msisdn, receive_time, sms_content, status) values(seq_sms_mo.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ messageContent + "',1)";
								logMonitor("Chen du lieu MO vao DB|" + sql_mo);
								stmt.executeUpdate(sql_mo);

								calstmtcmu.setString(2, callingNumber);
								calstmtcmu.execute();

								String temp = calstmtcmu.getString(1);

								sms_return = sms_return.replace("<mdt>",
										temp.substring(0, temp.indexOf("|")));
								sms_return = sms_return.replace("<date>",
										temp.substring(temp.indexOf("|") + 1));

								logMonitor("Sending message to "
										+ callingNumber + " with content: "
										+ sms_return);

								//Send_sms_resp(msisdn, sms_return, codeResponse);

								// Chen du lieu vao SMS_MT
								String sql_mt = "insert into sms_mt_1529(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ sms_return + "',0, sysdate)";
								logMonitor("Chen du lieu MT vao DB|" + sql_mt);
								stmt.executeUpdate(sql_mt);

							} catch (Exception e) {
								Send_sms_resp(msisdn, sms_sys_err, codeResponse);
								e.printStackTrace();
							}
						}

						else if (cmd.equalsIgnoreCase("MDT KQ")
								|| cmd.equalsIgnoreCase("MDTKQ")) {
							String sms_return = smsResponsekq;
							String sms_fail = "CTKM XAI VINA, TRUNG VANG 9999: Ma du thuong cua QK khong nam trong danh sach trung thuong. Chuc QK may man lan sau. Cam on QK da su dung dich vu VinaPhone. Chi tiet: http://khuyenmai.vinaphone.com.vn";

							try {

								// Chen thong tin SMS_MO luu Log

								String sql_mo = "insert into sms_mo(id, msisdn, receive_time, sms_content, status) values(seq_sms_mo.nextval, '"
										+ callingNumber
										+ "',sysdate,'"
										+ messageContent + "',1)";
								logMonitor("Chen du lieu MO vao DB|" + sql_mo);
								stmt.executeUpdate(sql_mo);

								calstmtcmu2.setString(2, callingNumber);
								calstmtcmu2.execute();

								String temp = calstmtcmu2.getString(1);

								if (temp.substring(0, temp.indexOf("|"))
										.equals("0")) {
									sms_return = sms_fail;
								} else {
									sms_return = sms_return
											.replace(
													"<kq>",
													temp.substring(0,
															temp.indexOf("|")));
									sms_return = sms_return
											.replace("<date>", temp
													.substring(temp
															.indexOf("|") + 1));
								}

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

					} // END COMMAND MESSAGE CONTENT
					else {
//						Send_sms_resp(callingNumber.substring(2),
//								sms_sai_cu_phap_1529, codeResponse);
						String sql_mt = "insert into sms_mt_1529(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
								+ callingNumber
								+ "',sysdate,'"
								+ sms_sai_cu_phap_1529 + "',0, sysdate)";
						logMonitor("Chen du lieu MT vao DB|" + sql_mt);
						stmt.executeUpdate(sql_mt);
						break;
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
