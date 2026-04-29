package com.vnp.sms.thread;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.SubmitMessage;

public class SendSMSFromDB extends ManageableThreadEx {
	PreparedStatement psReadQueue;
	PreparedStatement psDeleteQueue;
	String mstrSender = "900";
	String mstrTableSMSQueue;
	int icount = 0;

	@Override
	public void initPreparedStatement() throws Exception {
		psReadQueue = mcnMain.prepareStatement("select * from " + mstrTableSMSQueue + " where status in (0, 2) order by submit_time");
		psDeleteQueue = mcnMain.prepareStatement("update " + mstrTableSMSQueue + " set status =1, sent_time = sysdate where rowid = ?");

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
				rs = psReadQueue.executeQuery();
				while (rs.next()) {
					String rowid = rs.getString("id");
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
		logMonitor(strIsdn + " : " + strMessage);
		SubmitSM submitSM = new SubmitSM();
		submitSM.setShortMessage(strMessage);
		submitSM.getDestAddr().setAddress(strIsdn);
		submitSM.getSourceAddr().setAddress("1714");
		SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
		getThreadManagerEx().queueSubmit.enqueue(submitMessage);
	}

	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		// //////////////////////////////////////////////////////
		vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "300", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
