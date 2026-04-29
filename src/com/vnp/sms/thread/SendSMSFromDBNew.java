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

public class SendSMSFromDBNew extends ManageableThreadEx{
	private PreparedStatement psReadQueue;
	private PreparedStatement psUpdateQueue;
	private String mstrSender = "";
	private String mstrTableSMSQueue;
	private String mstrSqlSelect = "";
	//String mstrSqlGet = "select * from "+mstrTableSMSQueue+"partition(Part_{0}) where status in (0, 2) order by submit_time";
	private String mstrSqlGet = "";
	private String mstrSqlUpdate = "";
	private String acceptIsdn = "";
	private String test_mode = "";
	private int icount = 0;

	@Override
	public void initPreparedStatement() throws Exception {
		Date currentDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		//logMonitor(dateFormat.format(currentDate));
		//logMonitor(String.format(mstrSqlGet, dateFormat.format(currentDate)));
		mstrSqlGet = "select * from "+mstrTableSMSQueue+" partition(Part_"+dateFormat.format(currentDate)+") where status in (0, 2) and rownum < 2000 order by submit_time";
//		mstrSqlGet = "select * from "+mstrTableSMSQueue+" where status = 0 and msisdn not in (select msisdn from blacklist) and rownum < 2000 order by submit_time";
		//logMonitor(mstrSqlGet);
		logMonitor("mstrSqlGet " + mstrSqlGet);
		logMonitor("psDeleteQueue " + mstrSqlUpdate);
		psReadQueue = mcnMain.prepareStatement(mstrSqlGet);
		//psReadQueue = mcnMain.prepareStatement(mstrSqlSelect);
		psUpdateQueue = mcnMain.prepareStatement(mstrSqlUpdate);
	}

	@Override
	protected void beforeSession() throws Exception {
		mcnMain = AppServer.getConnection();
		super.beforeSession();
		initPreparedStatement();
	}

	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(mcnMain);
		Database.closeObject(psReadQueue);
		Database.closeObject(psUpdateQueue);
	}

	@Override
	protected void processSession() throws Exception {
		//logMonitor("process sms");
		ResultSet rs = null;
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			//logMonitor("execute from db success");
			try {
				Database.closeObject(rs);
				rs = psReadQueue.executeQuery();				
				while (rs.next()) {		
//					logMonitor("current row " + rs.getRow());
					String rowid = rs.getString("id");
					String msisdn = rs.getString("msisdn");
					String content = rs.getString("sms_content");
//					logMonitor("send sms from " + msisdn + "content " + content);
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
		try{
			if(test_mode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + strIsdn + "|") < 0){
				return;
			}
//			while (getThreadManagerEx().queueSubmit.size() > 100000) {
//				Thread.sleep(100);
//				logMonitor("Queue full...");
//			}
//			logMonitor(strIsdn + " : " + strMessage);
			SubmitSM submitSM = new SubmitSM();
			submitSM.setShortMessage(strMessage);
//			submitSM.set
			submitSM.getDestAddr().setAddress(strIsdn);
			submitSM.getSourceAddr().setAddress(mstrSender);
			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
//			logMonitor("[SEND_SMS]" + strIsdn + " with content: " + strMessage);
			getThreadManagerEx().queueSubmit.enqueue(submitMessage);
		}catch(Exception e){
			logMonitor("error when send sms for " + strIsdn + " with content " + strMessage);
		}
		
	}

	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		mstrSqlSelect = loadMandatory("SqlSelect");
		mstrSqlUpdate = loadMandatory("SqlUpdate");
		mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		mstrSender = loadMandatory("CodeSender");
		acceptIsdn = loadMandatory("AcceptIsdn");
		test_mode = loadMandatory("TestMode");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("SqlSelect", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("SqlUpdate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
