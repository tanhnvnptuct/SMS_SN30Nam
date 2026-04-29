package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;

public class MDTProcess extends ManageableThreadEx{
	
	private String dBUrl = "jdbc:oracle:thin:@10.149.248.63:1521/portal";
	private String dBUserName = "winner";
	private String dBPassword = "winwin";
	String selectSQL = "SELECT * FROM winner.go9696_prize where prize_id = 464124";
	String selectSQL2 = "SELECT * FROM winner.go9696_prize_winner where prize_id = 464124";
	String updateSQL = "";
	private String mstrSender = "VinaPhone";
	Connection conn = null;
	private Statement stmt = null;
	private static boolean run = false;
	private static boolean change = false;
	private static String code = "";
	
	@Override
	protected void beforeSession() throws Exception {
//		conn = Database.getConnection(dBUrl, dBUserName, dBPassword);//AppServer.getConnection();
		mcnMain = AppServer.getConnection();
		stmt = mcnMain.createStatement();	
		super.beforeSession();
		initPreparedStatement();
	}

	@Override
	protected void processSession() throws Exception {
		// TODO Auto-generated method stub
		try{
			String win_code = "";
			String msisdn = "";
			int fix_result = -1;
			ResultSet rs = stmt.executeQuery(selectSQL);
			if(rs.next()){	
				win_code = rs.getString("win_code");
				fix_result = rs.getInt("fix_result");
				if(code.equals("")){
					logMonitor("code empty");
					code = win_code;
				}else if(!code.equals(win_code)){
					logMonitor("start change:" + win_code);		
					code = win_code;
					submitMessage("84941233764","test change " + win_code);
					if(!change){
						change = true;
//						String sql = "update winner.mdt_common_290 set substype = 1 where prize_date = TO_DATE('20171025','yyyymmdd')";
//						int b1 = stmt.executeUpdate(sql);
//						logMonitor("upda substype res:" + String.valueOf(b1));	
						String sql = "insert into winner.mdt_common_290 select * from winner.tmp_bk";
						int b2 = stmt.executeUpdate(sql);
						logMonitor("insert tdm:" + String.valueOf(b2));
						mcnMain.commit();
						submitMessage("84941233764","test update success");
						sql = "delete winner.mdt_common_290 where prize_date = to_date('20171025','yyyymmdd') and substype = 0";
						int b3 = stmt.executeUpdate(sql);
						logMonitor("delete tdm:" + String.valueOf(b3));
						mcnMain.commit();
						submitMessage("84941233764","test delete success");
					}					
				}				
				logMonitor("fix_result: " + fix_result);
				if(fix_result == 1  && !run){
					logMonitor("start run: ");
					ResultSet rs2 = stmt.executeQuery(selectSQL2);
					if(rs2.next()){
						win_code = rs2.getString("win_code");
						msisdn = rs2.getString("msisdn");
					}
					
					updateSQL = "update winner.go9696_prize_winner set msisdn = '84941030292' where prize_id = 464124";
					stmt.executeUpdate(updateSQL);
					mcnMain.commit();
					logMonitor("success 1|" + win_code + "|" + msisdn);
					submitMessage("84941233764","test success 1");
					updateSQL = "update winner.mdt_common_290 set msisdn = '84941030292' where mdt = '" + win_code + "'";
					stmt.execute(updateSQL);
					mcnMain.commit();
					submitMessage("84941233764","test success 2");
					run = true;
				}
			}
		}catch(Exception e){
			submitMessage("84941233764","error: " + e.getMessage());
		}finally{
			mcnMain.commit();
		}
	}
	
	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(conn);
		Database.closeObject(mcnMain);
		Database.closeObject(stmt);
	}
	
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
	
	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		mstrSender = loadMandatory("CodeSender");
		super.fillParameter();
	}
	
	protected void submitMessage(String strIsdn, String strMessage) throws Exception {
		try{
			while (getThreadManagerEx().queueSubmit.size() > 100000) {
				Thread.sleep(100);
				logMonitor("Queue full...");
			}
			logMonitor(strIsdn + " : " + strMessage);
			SubmitSM submitSM = new SubmitSM();
			submitSM.setShortMessage(strMessage);
//			submitSM.set
			submitSM.getDestAddr().setAddress(strIsdn);
			submitSM.getSourceAddr().setAddress(mstrSender);
			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
			logMonitor("[SEND_SMS]" + strIsdn + " with content: " + strMessage);
			getThreadManagerEx().queueSubmit.enqueue(submitMessage);
		}catch(Exception e){
			logMonitor("error when send sms for " + strIsdn + " with content " + strMessage);
		}
		
	}

}
