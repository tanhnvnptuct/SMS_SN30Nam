package com.vnp.sms.thread;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;
import com.vnp.util.Util;

public class SendSMSFromDBExt extends ManageableThreadEx{
	private PreparedStatement psReadQueue;
	private PreparedStatement psUpdateQueue;
	private String mstrTableSMSQueue;
	private String mstrSqlGet = "";
	private String mstrSqlUpdate = "";
	private String acceptIsdn = "";
	private String test_mode = "";
	private int icount = 0;
	
	@Override
	protected void beforeSession() throws Exception {
		mcnMain = AppServer.getConnection();
		super.beforeSession();
		initPreparedStatement();
	}
	
	@Override
	public void initPreparedStatement() throws Exception{
		icount = 0;
		mstrSqlGet = "select * from " + mstrTableSMSQueue + " where submit_time >= trunc(sysdate) and status in (0,2) and rownum < 2000 order by submit_time";
		mstrSqlUpdate = "update " + mstrTableSMSQueue + " set status =1, sent_time = sysdate where id = ?";
		logMonitor("mstrSqlGet " + mstrSqlGet);
		logMonitor("psUpdateQueue " + mstrSqlUpdate);
		psReadQueue = mcnMain.prepareStatement(mstrSqlGet);
		psUpdateQueue = mcnMain.prepareStatement(mstrSqlUpdate);
	}
	
	@Override
	protected void processSession() throws Exception {
		ResultSet rs = null;
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			try {
				Database.closeObject(rs);
				rs = psReadQueue.executeQuery();				
				while (rs.next()) {		
					icount++;
					String rowid = rs.getString("id");
					String msisdn = rs.getString("msisdn");
					String content = rs.getString("sms_content");
					String longcode = rs.getString("longcode");
					String is_vn = rs.getString("is_vn");
					submitMessage(longcode, msisdn, content, is_vn);
					psUpdateQueue.setString(1, rowid);
					psUpdateQueue.executeUpdate();
					if (icount % 1000 == 0) {
						mcnMain.commit();
					}
				}
				mcnMain.commit();
				logMonitor("Number of SMS: " + icount);
			}
			finally {
				mcnMain.commit();
				Database.closeObject(rs);
			} 
		}		
	}

	@Override
	public void closeDBObject() {
		super.closeDBObject();
		Database.closeObject(mcnMain);
		Database.closeObject(psReadQueue);
		Database.closeObject(psUpdateQueue);
	}
	
	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		acceptIsdn = loadMandatory("AcceptIsdn");
		test_mode = loadMandatory("TestMode");
		super.fillParameter();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		Vector vtValue1 = new Vector();
		vtValue1.addElement("N");
		vtValue1.addElement("Y");
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_COMBOBOX, vtValue1, ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
	
	protected void submitMessage(String sender, String strIsdn, String strMessage, String is_vn) throws Exception {
		try{
			String content = "";
			if(test_mode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + strIsdn + "|") < 0){
				return;
			}
			while (getThreadManagerEx().queueSubmit.size() > 100000) {
				Thread.sleep(100);
				logMonitor("Queue full...");
			}
			SubmitSM submitSM = new SubmitSM();
			if(is_vn != null && is_vn.equals(("1"))){
				submitSM.setDataCoding((byte)(0x08));
				submitSM.setShortMessage( strMessage, "UTF-16" );
			}else{
				submitSM.setShortMessage(strMessage);
			}
			submitSM.getDestAddr().setAddress(strIsdn);
			submitSM.getSourceAddr().setAddress(sender);
			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
			getThreadManagerEx().queueSubmit.enqueue(submitMessage);
		}catch(Exception e){
			logMonitor("error when send sms for " + strIsdn + " with content " + strMessage);
		}
		
	}
}
