package com.vnp.sms.thread;

import java.sql.Connection;
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

public class SendSMSReadQueue extends ManageableThreadEx {
	PreparedStatement psReadQueue;
	PreparedStatement psDeleteQueue;
	String mstrSender = "";
	String mstrTableSMSQueue ="";
	
	String mstrSqlSelect = "";
	int icount = 0;

	@Override
	public void initPreparedStatement() throws Exception {
		//psReadQueue = mcnMain.prepareStatement("select rowid,a.* from " + mstrTableSMSQueue + " a   order by request_time");
		//psReadQueue = mcnMain.prepareStatement("select id, msisdn, sms_content From sms_mt where status in (0,2) order by submit_time");	       
		psReadQueue = mcnMain.prepareStatement(mstrSqlSelect);
		psDeleteQueue = mcnMain.prepareStatement("update sms_mt set status = 1, sent_time = sysdate where id = ?");

	}

	@Override
	protected void beforeSession() throws Exception {
		// TODO Auto-generated method stub
		super.beforeSession();
		initPreparedStatement();
	}

	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(psReadQueue);
		Database.closeObject(psDeleteQueue);
	}

	@Override
	
	protected void processSession() throws Exception {
		ResultSet rs = null;
		while (miThreadCommand != ThreadConstant.THREAD_STOP) {
			try {
				Database.closeObject(rs);
				logMonitor("Process sending message from Database.....");
				rs = psReadQueue.executeQuery();
				while (rs.next()) {
					logMonitor("Doc duoc du lieu tu DB....");
					String rowid = rs.getString("id");
					logMonitor("id = "+rowid+" msisdn : "+rs.getString("msisdn")+" content: "+rs.getString("sms_content"));
					submitMessage(rs.getString("msisdn"), rs.getString("sms_content"));
					psDeleteQueue.setString(1, rowid);
					psDeleteQueue.executeUpdate();					
					
					if (icount % 1000 == 0) {
						mcnMain.commit();
						icount = 0;
					}
				}
				mcnMain.commit();
			}
			finally {
				Database.closeObject(rs);
			} // TODO Auto-generated method stub
		}
	}
		
	

	protected void submitMessage(String strIsdn, String strMessage) throws Exception {
		while (getThreadManagerEx().queueSubmit.size() > 1000) {
			Thread.sleep(100);
			logMonitor("Queue full...");
		}
		logMonitor("Send SMS to "+strIsdn + " with content " + strMessage);
		SubmitSM submitSM = new SubmitSM();
		submitSM.setShortMessage(strMessage);
		submitSM.getDestAddr().setAddress(strIsdn);
		submitSM.getSourceAddr().setAddress(mstrSender);
		SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
		getThreadManagerEx().queueSubmit.enqueue(submitMessage);
	}

	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		//mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		mstrSqlSelect = loadMandatory("SqlSelect");
		mstrSender = loadMandatory("CodeSender");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		// //////////////////////////////////////////////////////
		//vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "300", ""));
		vtReturn.addElement(createParameterDefinition("SqlSelect", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
