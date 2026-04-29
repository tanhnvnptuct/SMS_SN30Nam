package com.vnp.sms.thread;

import java.sql.Connection;
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
import com.google.common.collect.EvictingQueue;
import com.vnp.util.AppServer;
//
public class ProcessDedicatedAccount extends ManageableThreadEx{
	protected Connection conn = null;
	private PreparedStatement psReadQueue;
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private String dBUrl = "jdbc:oracle:thin:@10.156.3.200:1521/rptdata";
	private String dBUserName = "ncpt_vas";
	private String dBPassword = "abc#123";
	private String mstrProcessDate = "";
	public static EvictingQueue<String> success = EvictingQueue.create(40000);
	
	@Override
	protected void beforeSession() throws Exception {
		conn = Database.getConnection(dBUrl, dBUserName, dBPassword);
		super.beforeSession();
		initPreparedStatement();
	}
	
	@Override
	public void initPreparedStatement() throws Exception {
		Date currentDate = new Date();
		String mstrSqlGet = "SELECT /*+parallel(a 8)*/ subscriber_id FROM ocs_e.dedicated_account a"
							+ " WHERE log_date = to_date('" + dateFormat.format(currentDate) + "','yyyymmdd') - 1 "
							+ "AND expiry_date = TO_DATE ('09/15/2018', 'mm/dd/yyyy') AND dedicated_account_id = 3 AND dedicated_account_balance >= 100000";
		logMonitor(mstrSqlGet);
		psReadQueue = conn.prepareStatement(mstrSqlGet);
	}

	@Override
	protected void processSession() throws Exception {		
		logMonitor("process thread");
		ResultSet rs = null;
		Date currentDate = new Date();
		if (miThreadCommand != ThreadConstant.THREAD_STOP && dateFormat.format(currentDate).equals(mstrProcessDate)) {
			logMonitor("execute from db success");
			try {
				rs = psReadQueue.executeQuery();				
				while (rs.next()) {
					String subscriber_id = rs.getString("subscriber_id");
					success.add(subscriber_id);
				}				
			}
			finally {
				Database.closeObject(rs);
			}
		}
		logMonitor("process complete. queue size: " + success.size());
	}

	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(mcnMain);
		Database.closeObject(conn);
		Database.closeObject(psReadQueue);
	}
	
	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		mstrProcessDate = loadMandatory("ProcessDate");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("ProcessDate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
