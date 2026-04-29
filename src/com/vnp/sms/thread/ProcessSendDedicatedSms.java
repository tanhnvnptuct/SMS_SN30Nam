package com.vnp.sms.thread;

import java.sql.CallableStatement;

import com.fss.sql.Database;
import com.vnp.util.AppServer;
import com.vnp.util.RechargingApi;
import com.vnp.util.Util;


public class ProcessSendDedicatedSms extends ManageableThreadEx{
	private CallableStatement stmt;
	private String sms_content = "TB: Tai khoan khuyen mai co han su dung den 15/9/2018 cua Quy khach la <balance> d. QK vui long kiem tra va tiep tuc su dung truoc thoi han. Tran trong cam on!";
	private RechargingApi api;
	
	@Override
	protected void beforeSession() throws Exception {
		mcnMain = AppServer.getConnection();
		super.beforeSession();
		try {
			api = new RechargingApi();
			initPreparedStatement();
		} catch (Exception ex) {
			logMonitor("Before Session error: " + ex.getMessage());
		}
	}
	
	@Override
	public void initPreparedStatement() throws Exception{
		stmt = mcnMain.prepareCall("insert into sms_mt_1558(id,msisdn,sms_content,campaign_id) values (seq_sms_mt.nextval,?,?,5076)");
	}

	@Override
	protected void processSession() throws Exception {
		logMonitor("process thread queue size: " + ProcessDedicatedAccount.success.size());
		int iCount = 0;
		while (ProcessDedicatedAccount.success.size()>0){
			String msisdn = ProcessDedicatedAccount.success.poll();
			try{
				String transaction_id = String.valueOf(System.currentTimeMillis());
				double balance = api.getAccountKM3Balance(Util.formatMsisdn(msisdn), transaction_id);
				if(balance > 0){
					stmt.setString(1, Util.formatMsisdn(msisdn));
					stmt.setString(2, sms_content.replace("<balance>", String.valueOf(balance)));
					stmt.addBatch();
					iCount++;
					if (iCount % 1000 == 0) {
						stmt.executeBatch();
						logMonitor("SMS Sent : " + iCount);
					}
				}else{
					logMonitor("CheckAccountKM3Balance|" + msisdn + "|" + transaction_id + "|" + balance);
				}
			}catch(Exception e){
				logMonitor("Process send sms msisdn: " + msisdn + "|Err:" + e.getMessage());
			}			
		}
		stmt.executeBatch();
		mcnMain.commit();
		logMonitor("Total send : " + iCount);
	}
	
	@Override
	public void closeDBObject() {
		super.closeDBObject();
		Database.closeObject(stmt);
		Database.closeObject(mcnMain);		
	}

}
