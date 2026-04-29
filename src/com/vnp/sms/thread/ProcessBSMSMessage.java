package com.vnp.sms.thread;

import java.util.Properties;
import java.util.Vector;

import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.DeliverSM;
import com.vnp.util.MyAPIResponseUtil;
import com.vnp.util.ReceiveMessage;
import com.vnp.util.Util;

public class ProcessBSMSMessage extends ManageableThreadEx{
	private int queueSize;
	private String codeResponse = "";
	private String apiUrl = "";
	private String mstrTestMode = "";
	private String acceptIsdn = "";
	private MyAPIResponseUtil resUtil;
	private String jsonStringFormat = "{\"msisdn\":\"%s\",\"content\":\"%s\"}";
	
	@Override
	protected void beforeSession() throws Exception {
		super.beforeSession();
		resUtil = new MyAPIResponseUtil();
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
	
	private boolean loadFromQueue() throws AppException, Exception{
		Properties prtDeliverSM = new Properties();
		prtDeliverSM.put("DataType", "DeliverSM");
		boolean exists = false;
		logMonitor("queue size: " + getThreadManagerEx().queueReceiver.getQueueSize());
		while (getThreadManagerEx().queueReceiver.getQueueSize() > 0){
			ReceiveMessage receiveMessage = (ReceiveMessage) getThreadManagerEx().queueReceiver.detach(prtDeliverSM);
			if (receiveMessage != null){
				DeliverSM deliverSM = (DeliverSM) receiveMessage.getReceiveSM();
				if (deliverSM != null && deliverSM.getShortMessage() != null){
					exists = true;
//					String test = new String(deliverSM.getShortMessage().getBytes(), "UTF-16BE");
//					String test2 = new String(deliverSM.getShortMessage().getBytes(), "UTF-16LE");
					String messageContent = deliverSM.getShortMessage().toUpperCase().trim();
					String callingNumber = deliverSM.getSourceAddr().getAddress();
					String calledNumber = deliverSM.getDestAddr().getAddress();
					logMonitor("Decrypting --> " + callingNumber + "|" + messageContent + "|" + calledNumber);
					if(mstrTestMode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + callingNumber + "|") < 0){
						logMonitor("Test Mode: msisdn " + callingNumber + " not allowed");
					}else{
						String req = String.format(jsonStringFormat, callingNumber, messageContent);
						logMonitor("API Req: " + req);
						String res = resUtil.getHttpPost(req, apiUrl, 30);
						logMonitor("API Res: " + res);
					}
				}
			}
		}
		return exists;
	}
	
	public void fillParameter() throws AppException {
		queueSize = loadUnsignedInteger("queue-size");
		codeResponse = loadMandatory("code-response");
		apiUrl = loadMandatory("api-url");
		acceptIsdn = loadMandatory("AcceptIsdn");
		mstrTestMode = loadMandatory("TestMode");
		super.fillParameter();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(ParameterUtil.createParameterDefinition("queue-size", "", ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("code-response", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("api-url", "", ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "99999", ""));		
		Vector vtValue2 = new Vector();
		vtValue2.addElement("Y");
		vtValue2.addElement("N");
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_COMBOBOX, vtValue2, ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
