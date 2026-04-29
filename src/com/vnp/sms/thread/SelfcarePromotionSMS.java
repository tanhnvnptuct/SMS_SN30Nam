package com.vnp.sms.thread;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.fis.database.OracleConnectionFactory;
import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;

public class SelfcarePromotionSMS extends ManageableThreadEx{
	private Connection seftcareConnection;
	private Connection promotionConnection;
	private PreparedStatement psReadQueue;
	private PreparedStatement psUpdateQueue;
	private Statement stmt = null;
	private Statement smsStmt = null;
	private CallableStatement calstmt = null;
	private String dBUrl = "";
	private String dBUserName = "";
	private String dBPassword = "";
	private String mstrSender = "";
	private String startDate = "";
	private String endDate = "";
	private String selectSql = "";
	private String updateSql = "";
	private String dateFormat = "";
	private String prizeDate = "";
	private String sql_mdt = "{ ? = call pkg_web.get_Single_MDT('244',?)}";
	private String promotionDBUrl = "jdbc:oracle:thin:@10.149.248.63/portal";
	private String promotionDBUserName = "promotion";
	private String promotionDBPassword = "promotion";
	private String sms_return = "Tang MDT khi tai ung dung My VinaPhone: VinaPhone kinh tang QK MDT <mdt> de tham du quay thuong: LAPTOP DELL gia tri tren 10 trieu dong. CT quay thuong ngay <lotdate>. Chi tiet: http://khuyenmai.vinaphone.com.vn hoac tren ung dung MyVinaPhone";
			//"Quy khach da nhan duoc 01 ma tham du chuong trinh quay so trung thuong cua VinaPhone va co co hoi trung 1 trong 06 Laptop. Chi tiet xem tai www.vinaphone.com.vn hoac truc tiep tren ung dung MyVinaPhone. Tran trong cam on";
	
	@Override
	public void initPreparedStatement() throws Exception {		
		/*create statement for create code on winner database*/
		
		stmt = mcnMain.createStatement();
		calstmt = mcnMain.prepareCall(sql_mdt);
		calstmt.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);
		
		/*prepare statement for retrieve user data from selfcare database*/
		
		psReadQueue = seftcareConnection.prepareStatement(selectSql);
//		psReadQueue = seftcareConnection.prepareStatement(selectSql,ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE,
//		         ResultSet.HOLD_CURSORS_OVER_COMMIT);
		DateFormat formatter = new SimpleDateFormat(dateFormat);		
		psReadQueue.setDate(1, new java.sql.Date(formatter.parse(startDate).getTime()));
		psReadQueue.setDate(2, new java.sql.Date(formatter.parse(endDate).getTime()));
		logMonitor("prepare read statement : " + selectSql);		
		psUpdateQueue = seftcareConnection.prepareStatement(updateSql);
		
		/*create statement for send notification sms to user*/
		smsStmt = promotionConnection.createStatement();
		
		prizeDate = getPrizeDate();
	}
	
	@Override
	protected void beforeSession() throws Exception {	
		mcnMain = AppServer.getConnection();
		seftcareConnection = Database.getConnection(dBUrl, dBUserName, dBPassword);
		if(seftcareConnection != null){
			logMonitor("creat success seflcare connection : " + dBUrl + " for " + dBUserName);			
		}	
		promotionConnection = Database.getConnection(promotionDBUrl, promotionDBUserName, promotionDBPassword);
		super.beforeSession();
		initPreparedStatement();
	}
	
	@Override
	protected void processSession() throws Exception {
		logMonitor("process sms");
		ResultSet rs = null;	
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			logMonitor("thread is running");
			try {
				
				Database.closeObject(rs);
				rs = psReadQueue.executeQuery();
				
				while (rs.next()) {		
					try{
						logMonitor("current row " + rs.getRow());
						String msisdn = rs.getString("msisdn");
						logMonitor("start sinh ma for " + msisdn);
						String sql_sm = "insert into mdt_common_244 (id, prize_date, msisdn, mdt, createdate, substype) values (seq_mdtcommon_244.nextval, to_date('"
								+ prizeDate
								+ "','dd/mm/yyyy'), '"
								+ msisdn.substring(2)
								+ "', LPAD(seq_mdtcommon_244.nextval,10,'0'), sysdate, 1)";
						logMonitor("execute sm sql : " + sql_sm);
						stmt.execute(sql_sm);
						mcnMain.commit();
						
						logMonitor("re-get mdt for " + msisdn);
						calstmt.setString(2, msisdn.substring(2));
						calstmt.execute();
						String temp = calstmt.getString(1);
						String mdt = temp.substring(0, temp.indexOf("|"));
						String lotdate = temp.substring(temp.indexOf("|") + 1);
						if(mdt != null && mdt.length() > 1){
							logMonitor("update mdt status");
							psUpdateQueue.setString(1, mdt);
							psUpdateQueue.setString(2, msisdn);
							//logMonitor("execute update query for msisdn = " + msisdn + "|" + updateSql);
							psUpdateQueue.executeUpdate();
							seftcareConnection.commit();
							
							//send sms to user
							String sql_mt = "insert into sms_mt_1558(id, msisdn, submit_time, sms_content, status, sent_time) values(seq_sms_mt.nextval, '"
									+ msisdn
									+ "',sysdate,'"
									+ sms_return.replace("<mdt>", mdt).replace("<lotdate>", lotdate) + "',0, sysdate)";
							logMonitor("Chen du lieu MT vao DB|" + sql_mt);
							smsStmt.executeUpdate(sql_mt);
							promotionConnection.commit();
						}						
					}catch(Exception e){
						logMonitor("error when execute sm query : " + e.getMessage());
					}		
				}
				seftcareConnection.commit();
				promotionConnection.commit();
				mcnMain.commit();
			}catch(Exception e){
				logMonitor("error when execute seltcate get query : " + e.getMessage());
		}
			finally {
				Database.closeObject(rs);
			} // TODO Auto-generated method stub
		}
	}
	
	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		logMonitor("close db ofject");
		super.closeDBObject();
		Database.closeObject(mcnMain);
		Database.closeObject(seftcareConnection);
		Database.closeObject(psReadQueue);
		Database.closeObject(stmt);
		Database.closeObject(psUpdateQueue);
		Database.closeObject(calstmt);
	}
	
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("SeflcareDBUrl", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("SeflcareDBUserName", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("SeflcareDBPassword", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("StartDate", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("EndDate", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("DateFormat", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("SelectSql", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("UpdateSql", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
	
	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		dBUrl = loadMandatory("SeflcareDBUrl");
		dBUserName = loadMandatory("SeflcareDBUserName");
		dBPassword = loadMandatory("SeflcareDBPassword");
		mstrSender = loadMandatory("CodeSender");
		startDate = loadMandatory("StartDate");
		endDate = loadMandatory("EndDate");
		dateFormat = loadMandatory("DateFormat");
		selectSql = loadMandatory("SelectSql");
		updateSql = loadMandatory("UpdateSql");
		super.fillParameter();
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
	
	private String getPrizeDate() throws Exception{
		String prizeDate = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date currentDate = new Date();
		Date firtDate = dateFormat.parse("01/06/2017");
		Date secondDate = dateFormat.parse("08/06/2017");
		Date thirdDate = dateFormat.parse("15/06/2017");
		Date fourthDate = dateFormat.parse("22/06/2017");
		Date fifthDate = dateFormat.parse("29/06/2017");
		Date sixthDate = dateFormat.parse("06/07/2017");
		
		if(currentDate.before(firtDate)){
			prizeDate = "07/06/2017";
		}else if(currentDate.before(secondDate)){
			prizeDate = "14/06/2017";
		}else if(currentDate.before(thirdDate)){
			prizeDate = "21/06/2017";
		}else if(currentDate.before(fourthDate)){
			prizeDate = "28/06/2017";
		}else if(currentDate.before(fifthDate)){
			prizeDate = "05/07/2017";
		}else if(currentDate.before(sixthDate)){
			prizeDate = "12/07/2017";
		}else{
			prizeDate = "01/01/2016";
		}
		return prizeDate;
	}
}
