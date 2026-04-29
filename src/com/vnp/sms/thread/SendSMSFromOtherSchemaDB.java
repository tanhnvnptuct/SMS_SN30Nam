package com.vnp.sms.thread;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;

public class SendSMSFromOtherSchemaDB extends ManageableThreadEx{
	private PreparedStatement psReadQueue;
	private PreparedStatement psUpdateQueue;
	private String mstrSender = "";
	private String mstrTableSMSQueue;
	private String mstrSqlSelect = "";
	private String dBUrl = "";
	private String dBUserName = "";
	private String dBPassword = "";
	private String mstrSqlGet = "";
	private String mstrSqlUpdate = "";
	int icount = 0;
	
	@Override
	public void initPreparedStatement() throws Exception {
		Date currentDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		//logMonitor(dateFormat.format(currentDate));
		//logMonitor(String.format(mstrSqlGet, dateFormat.format(currentDate)));
		mstrSqlGet = "select * from "+mstrTableSMSQueue+" partition(Part_"+dateFormat.format(currentDate)+") where status in (0, 2) order by submit_time";
		//logMonitor(mstrSqlGet);
		logMonitor("mstrSqlGet " + mstrSqlGet);
		logMonitor("psDeleteQueue " + mstrSqlUpdate);
		psReadQueue = mcnMain.prepareStatement(mstrSqlGet);
		//psReadQueue = mcnMain.prepareStatement(mstrSqlSelect);
		psUpdateQueue = mcnMain.prepareStatement(mstrSqlUpdate);
	}

	@Override
	protected void beforeSession() throws Exception {
		mcnMain = Database.getConnection(dBUrl, dBUserName, dBPassword);//AppServer.getConnection();
		super.beforeSession();
		initPreparedStatement();
	}
	
	@Override
	protected void processSession() throws Exception {
		logMonitor("process sms");
		ResultSet rs = null;
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			logMonitor("execute from db success");
			try {
				Database.closeObject(rs);
				rs = psReadQueue.executeQuery();				
				while (rs.next()) {					
					logMonitor("current row " + rs.getRow());
					String rowid = rs.getString("id");
					String msisdn = rs.getString("msisdn");
					String content = rs.getString("sms_content");
					logMonitor("send sms from " + msisdn + "content " + content);
					submitMessage(msisdn, content);
					psUpdateQueue.setString(1, rowid);
					psUpdateQueue.executeUpdate();
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
		submitSM.getSourceAddr().setAddress(mstrSender);
		SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
		getThreadManagerEx().queueSubmit.enqueue(submitMessage);
	}

	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		mstrSqlSelect = loadMandatory("SqlSelect");
		mstrSqlUpdate = loadMandatory("SqlUpdate");
		mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		mstrSender = loadMandatory("CodeSender");
		dBUrl = loadMandatory("DBUrl");
		dBUserName = loadMandatory("DBUserName");
		dBPassword = loadMandatory("DBPassword");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("SqlSelect", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("SqlUpdate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("DBUrl", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("DBUserName", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("DBPassword", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}

}
