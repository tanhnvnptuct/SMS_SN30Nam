package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.google.common.collect.EvictingQueue;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.CISApi;
import com.vnp.util.ReceiveMessage;
import com.vnp.util.SubmitMessage;
import com.vnp.util.IParkingApi;

public class ProcessMessageIParking extends ManageableThreadEx{
	int queueSize;	
	private Connection conn = null;
	private Statement stmt = null;
	private IParkingApi api = null;
	private CISApi cisApi = null;
	
	private String codeResponse = "";
	private String sms_sai_cu_phap = "";
	private String supportCommands = "";
	private String sms_loi_he_thong = "";
	private String sms_thanh_toan_vuot_nguong = "";
	private String sms_ma_tien_khong_dung = "";
	private String sms_ma_diem_do_khong_dung = "";
	private String sms_bien_xe_khong_dung = "";
	private String sms_het_gio_trong_xe = "";
	private String sms_khong_du_tien_tk = "";
	private String sms_thanh_cong = "";
	private String test_mode = "";
	private String acceptIsdn = "";
	private String iparkingApiUser = "";
	private String iparkingApiPass = "";
	private String cisApiUser = "";
	private String cisApiPass = "";
	final static Logger logger = Logger.getLogger("process");
	final static Logger cdrLogger = Logger.getLogger("cdr");
	public static EvictingQueue<String[]> requests = EvictingQueue.create(40000);
	
	public void fillParameter() throws AppException {
		queueSize = loadUnsignedInteger("queue-size");
		codeResponse = loadMandatory("code-response");
		sms_sai_cu_phap = loadMandatory("sms_sai_cu_phap");
		sms_loi_he_thong = loadMandatory("sms_loi_he_thong");
		sms_thanh_toan_vuot_nguong = loadMandatory("sms_thanh_toan_vuot_nguong");
		sms_ma_tien_khong_dung = loadMandatory("sms_ma_tien_khong_dung");
		sms_ma_diem_do_khong_dung = loadMandatory("sms_ma_diem_do_khong_dung");
		sms_bien_xe_khong_dung = loadMandatory("sms_bien_xe_khong_dung");
		sms_het_gio_trong_xe = loadMandatory("sms_het_gio_trong_xe");
		sms_khong_du_tien_tk = loadMandatory("sms_khong_du_tien_tk");
		sms_thanh_cong = loadMandatory("sms_thanh_cong");
		supportCommands = loadMandatory("supportCommands");
		iparkingApiUser = loadMandatory("iparkingApiUser");
		iparkingApiPass = loadMandatory("iparkingApiPass");
		cisApiUser = loadMandatory("cisApiUser");
		cisApiPass = loadMandatory("cisApiPass");
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
				"iparkingApiUser", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"iparkingApiPass", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"cisApiUser", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"cisApiPass", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"supportCommands", "", ParameterType.PARAM_TEXTBOX_MAX, "300",""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"code-response", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_loi_he_thong", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_thanh_toan_vuot_nguong", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_ma_tien_khong_dung", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_sai_cu_phap", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_ma_diem_do_khong_dung", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_bien_xe_khong_dung", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_het_gio_trong_xe", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_khong_du_tien_tk", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition(
				"sms_thanh_cong", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
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
		try {
			api = new IParkingApi();
			cisApi = new CISApi();
			initPreparedStatement();
		} catch (Exception ex) {
			logMonitor("Before Session error: " + ex.getMessage());
		}
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
					String cisApiRes = "";
					int forwardmoRes = 1000;
					int rechargingRes = 1000;
					int submitpaymentRes = 1000;
					String transaction_id = "0";
					String transactionAmount = "0";
					String serie = "";
					//tung note: param for write cdr
					String id = String.valueOf(System.nanoTime());
			    	long start = System.currentTimeMillis();
					String messageContent = deliverSM.getShortMessage()
							.toUpperCase().trim();
					String callingNumber = deliverSM.getSourceAddr().getAddress();
					String calledNumber = deliverSM.getDestAddr().getAddress();
					logMonitor("Decrypting --> " + callingNumber + "|"
							+ messageContent + "|" + calledNumber);
					try{
						if(test_mode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + callingNumber + "|") < 0){
							
						}else if(messageContent.startsWith("IP")){
							logToFile("Decrypting --> " + callingNumber + "|" + messageContent + "|" + calledNumber, logger);
							String[] smsContentArr = messageContent.split(" ");
							if(smsContentArr.length == 3 || smsContentArr.length == 4){
								String command = smsContentArr[0];
								String parkingCode = smsContentArr[1];
								String carNumberPlate = smsContentArr[2];
								if ((supportCommands.indexOf("|" + command + "|")) >= 0){
									transactionAmount = command.substring(2) + "000";
									transaction_id = String.valueOf(System.currentTimeMillis());
									cisApiRes = cisApi.forwardmo(callingNumber,messageContent,cisApiUser,cisApiPass,transaction_id, transactionAmount);
									String[] cisApiResArr = cisApiRes.split("\\|");
									forwardmoRes = Integer.parseInt(cisApiResArr[0]);
									logToFile("TransactionID: " + transaction_id + "|callingNumber: " + callingNumber + "|messageContent: " + messageContent + "|forwardmoRes: " + forwardmoRes, logger);
									logMonitor("forwardmoRes:" + cisApiRes);
									switch(forwardmoRes){
									case 0:
										rechargingRes = api.rechargingIparkingService(callingNumber, command, transactionAmount, parkingCode, transaction_id, carNumberPlate);
										if(rechargingRes == 99){
											int loginRes = api.login(iparkingApiUser, iparkingApiPass);
											if(loginRes == 0){
												rechargingRes = api.rechargingIparkingService(callingNumber, command, transactionAmount, parkingCode, transaction_id, carNumberPlate);
											}
										}
										if(rechargingRes == 9999){
											rechargingRes = api.rechargingIparkingPost(callingNumber,transactionAmount);
										}
										logMonitor("rechargingRes:" + String.valueOf(rechargingRes));
										logToFile("TransactionID: " + transaction_id + "|callingNumber: " + callingNumber + "|messageContent: " + messageContent + "|rechargingRes: " + rechargingRes, logger);
										if(rechargingRes == 0){
											String cdr = transaction_id + "," + callingNumber + "," + messageContent + "," + transactionAmount + "," + parkingCode + "," + carNumberPlate;
											logToFile(cdr, cdrLogger);											
											cisApiRes = cisApi.submitpayment(callingNumber, messageContent, transactionAmount, cisApiUser, cisApiPass, transaction_id);
											logMonitor("submitpaymentRes:" + cisApiRes);
											cisApiResArr = cisApiRes.split("\\|");
											submitpaymentRes = Integer.parseInt(cisApiResArr[0]);
											serie = cisApiResArr[1];
											logToFile("TransactionID: " + transaction_id + "|callingNumber: " + callingNumber + "|messageContent: " + messageContent + "|submitpaymentRes: " + submitpaymentRes, logger);											
											if(submitpaymentRes == 0 && serie != null && !serie.equalsIgnoreCase("Null")){
												//send sms thanh cong
												Send_sms_resp(callingNumber, sms_thanh_cong.replaceAll("<amount>", transactionAmount), codeResponse);
											}else{
												//tru cuoc thanh cong nhung khong tao duoc phieu do xe
												//retry again
												cisApiRes = cisApi.submitpayment(callingNumber, messageContent, transactionAmount, cisApiUser, cisApiPass, transaction_id);
												cisApiResArr = cisApiRes.split("\\|");
												submitpaymentRes = Integer.parseInt(cisApiResArr[0]);
												serie = cisApiResArr[1];
												logToFile("Retry TransactionID: " + transaction_id + "|callingNumber: " + callingNumber + "|messageContent: " + messageContent + "|submitpaymentRes: " + submitpaymentRes, logger);
												if(submitpaymentRes == 0 && serie != null && !serie.equalsIgnoreCase("Null")){
													//send sms thanh cong
													Send_sms_resp(callingNumber, sms_thanh_cong.replaceAll("<amount>", transactionAmount), codeResponse);
												}else{
													//todo: xu ly
												}
											}
										}else if(rechargingRes == 2){
											Send_sms_resp(callingNumber, sms_khong_du_tien_tk, codeResponse);
										}else if(rechargingRes == 4001){
											Send_sms_resp(callingNumber, sms_thanh_toan_vuot_nguong, codeResponse);
										}else{
											Send_sms_resp(callingNumber, sms_loi_he_thong, codeResponse);
										}
										break;
									case 1:									
										Send_sms_resp(callingNumber, sms_loi_he_thong, codeResponse);
										break;
									case 2:
										Send_sms_resp(callingNumber, sms_sai_cu_phap, codeResponse);
										break;
									case 4:
										Send_sms_resp(callingNumber, sms_ma_diem_do_khong_dung, codeResponse);
										break;
//									case 5:									
									case 6:
										Send_sms_resp(callingNumber, sms_het_gio_trong_xe, codeResponse);
										break;
//									case 7:
//										Send_sms_resp(callingNumber, sms_ma_tien_khong_dung, codeResponse);
//										break;
									case -9999:
									case -1:
										Send_sms_resp(callingNumber, sms_loi_he_thong, codeResponse);
										break;
									default:
										String sms_content = cisApiResArr[1];
										Send_sms_resp(callingNumber, sms_content, codeResponse);
										break;	
									}

								}else{
									Send_sms_resp(callingNumber, sms_ma_tien_khong_dung, codeResponse);
								}
							}else{
								Send_sms_resp(callingNumber, sms_sai_cu_phap, codeResponse);
							}
						}else{
							Send_sms_resp(callingNumber, sms_sai_cu_phap, codeResponse);
						}
						
						//tung note : record request to write cdr
						requests.add(new String[]{id, callingNumber, messageContent, transaction_id, String.valueOf(forwardmoRes), String.valueOf(rechargingRes), String.valueOf(submitpaymentRes), transactionAmount, String.valueOf(System.currentTimeMillis()-start)});
					}catch(Exception e){
						Send_sms_resp(callingNumber, sms_loi_he_thong, codeResponse);
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
	
	public void logToFile(String content, Logger logger){
		try{
			Date currentDate = new Date();
			DateFormat datetimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			String nowString = datetimeFormat.format(currentDate);
			logger.info(nowString + "|" + content);
		}catch(Exception e){
		}		
	}
}
