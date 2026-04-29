package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.SubmitMessage;

public class SendSMSFromSQLService extends ManageableThreadEx{
	private Connection dbConnection;
	private Statement stmt = null;
	private PreparedStatement psUpdateQueue;
	private String connectionString = "";
	private String mstrTableSMSQueue = "";
	private String mstrSender = "";
	private String sql_select_mt = "select * from <table_name> where status = 0";
	private String sql_update = "update <table_name> set status = 1, sent_time = GETDATE() where id = ?";

	@Override
	protected void beforeSession() throws Exception {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		dbConnection =DriverManager.getConnection(connectionString);		
		initPreparedStatement();
	}
	
	@Override
	public void initPreparedStatement() throws Exception {
		if(dbConnection != null){
			logMonitor("connect success");
			sql_select_mt = sql_select_mt.replace("<table_name>", mstrTableSMSQueue);
			sql_update = sql_update.replace("<table_name>", mstrTableSMSQueue);
			stmt = dbConnection.createStatement();	
			psUpdateQueue = dbConnection.prepareStatement(sql_update);
		}
	}
	
	@Override
	protected void processSession() throws Exception {
		// TODO Auto-generated method stub	
		ResultSet rs = stmt.executeQuery(sql_select_mt);
		while(rs.next()){
			logMonitor("msisdn : " + rs.getString("msisdn"));
			String msisdn = rs.getString("msisdn");
			String content = rs.getString("sms_content");
			submitMessage(msisdn, content);
			int id = rs.getInt("id");
			psUpdateQueue.setInt(1, id);
			psUpdateQueue.executeUpdate();
			dbConnection.commit();
		}
	}
	
	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
//		mstrSqlSelect = loadMandatory("SqlSelect");
//		mstrSqlUpdate = loadMandatory("SqlUpdate");
		mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		mstrSender = loadMandatory("CodeSender");
		connectionString = loadMandatory("ConnectionString");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
//		vtReturn.addElement(createParameterDefinition("SqlSelect", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
//		vtReturn.addElement(createParameterDefinition("SqlUpdate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("ConnectionString", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
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
}
