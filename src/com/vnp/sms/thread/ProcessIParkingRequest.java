package com.vnp.sms.thread;

import java.sql.Connection;
import java.sql.Statement;

import com.fss.sql.Database;
import com.vnp.util.AppServer;
import com.vnp.util.CISApi;
import com.vnp.util.IParkingApi;

public class ProcessIParkingRequest extends ManageableThreadEx{
	private Connection conn = null;
	private Statement stmt = null;
	
	@Override
	public void beforeSession() throws Exception {
		super.beforeSession();
		initPreparedStatement();
	}
	
	public void initPreparedStatement() throws Exception {
		//get connection
		conn = AppServer.getConnection();
		conn.setAutoCommit(true);
		
		//create/prepare statement
		stmt = conn.createStatement();
	}
	
	@Override
	public void closeDBObject() {
		// TODO Auto-generated method stub
		super.closeDBObject();
		Database.closeObject(conn);
		Database.closeObject(stmt);
	}
	
	@Override
	protected void processSession() throws Exception {		
		//run process
		while (ProcessMessageIParking.requests.size()>0){
			String[] request = ProcessMessageIParking.requests.poll();
			if (request!=null){
				String id = request[0];
				String callingNumber = request[1];
				String messageContent = request[2];
				String transaction_id = request[3];
				String forwardmoRes = request[4];
				String rechargingRes = request[5];
				String submitpaymentRes = request[6];
				String transactionAmount = request[7];
				String cost = request[8];
				String sql = "insert into iparking_detail(id, date_time, callingNumber, messagecontent, transactionid, forwardmores, rechargingres, submitpaymentres, transactionamout, cost) values ("
						+ id
						+ ", sysdate, '"
						+ callingNumber
						+ "' ,'"
						+ messageContent
						+ "', "
						+ transaction_id
						+ ", "
						+ forwardmoRes
						+ ", "
						+ rechargingRes
						+ ", "
						+ submitpaymentRes
						+ ", "
						+ transactionAmount
						+ ", "
						+ cost
						+ ")";
				try{
					logMonitor("execute insert: " + sql);
					stmt.execute(sql);
				}catch(Exception e){
					logMonitor("insert cdr error:" + e.getMessage() + "|" + sql);
				}
				conn.commit();
			}
		}
	}
}
