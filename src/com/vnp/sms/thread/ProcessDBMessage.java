// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 23/05/2006 11:41:49 AM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   ProcessDBMessage.java

package com.vnp.sms.thread;

import com.fss.queue.Queue;
import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.util.AppException;
import com.fss.util.StringUtil;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;

//import com.fss.mwallet.util.SubmitMessage;
import java.sql.*;
import java.util.Vector;
import oracle.jdbc.*;
import oracle.jdbc.pool.OracleDataSource;

//import com.fss.mwallet.util.AppServer;
//import com.fss.mwallet.thread.EvoucherThread;

// Referenced classes of package com.fss.mwallet.thread:
//            EvoucherThread, ThreadManagerEx


public class ProcessDBMessage extends ManageableThreadEx
{
	protected String defaultSourceNumber;
	private int icount = 0 ;
	
	private String soureAdress = "";
	
	private String sqlSelect = "";
	

	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("Source-Address", "", ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(createParameterDefinition("Sql-Select", "", ParameterType.PARAM_TEXTBOX_MAX, "500"));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}

	public void fillParameter() throws AppException
	{
//		defaultSourceNumber = loadMandatory("default-source-address");
		soureAdress = loadMandatory("Source-Address");
		sqlSelect = loadMandatory("Sql-Select");
		super.fillParameter();
	}
 public String correctMsisdn(String msisdn)
    {
        if(msisdn.startsWith("0"))
            msisdn = msisdn.substring(1);
        if(!msisdn.startsWith("84"))
            msisdn = "84"+msisdn;
        return msisdn;
    }
	public void processSession() throws AppException
	{
	
		PreparedStatement pstmt = null;
		PreparedStatement pstmtDelete = null;
		ResultSet rsMessage = null;
		Connection cn=null;
		try
		{
			//init conn 2 DB
			cn = AppServer.getConnection();
		    
			fillLogFile();
			cn.setAutoCommit(false);
			logMonitor("Process sending message from Database.....");
			//String sqlSelect = "select id, msisdn, sms_content From sms_mt where status = 0";			
			String sqlDelete = "update sms_mt set status = 1, sent_time = sysdate where id = ?";
			pstmt = cn.prepareStatement(this.sqlSelect);
			pstmtDelete = cn.prepareStatement(sqlDelete);
				
			while (miThreadCommand != 2)
			{				
				rsMessage = pstmt.executeQuery();
								
				while (rsMessage.next()) {
					
					String rowid = rsMessage.getString("id");					
					submitMessage(rsMessage.getString("msisdn"), rsMessage.getString("sms_content"));
					pstmtDelete.setString(1, rowid);
					pstmtDelete.executeUpdate();					
					cn.commit();				
				}			
			}
			
			
		}
		catch(Exception e)
		{
			logMonitor("Error occurs in processSession " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			Database.closeObject(cn);
			Database.closeObject(pstmt);
			Database.closeObject(pstmtDelete);
			Database.closeObject(rsMessage);
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException ex)
			{
			}
		}
	}
	
	
	// Send Message
	
	protected void submitMessage(String strIsdn, String strMessage) throws Exception {
	while (getThreadManagerEx().queueSubmit.size() > 1000) {
		Thread.sleep(100);
		logMonitor("Queue full...");
	}
	logMonitor("Send SMS to "+strIsdn + " with content " + strMessage);
	SubmitSM submitSM = new SubmitSM();
	submitSM.setShortMessage(strMessage);
	submitSM.getDestAddr().setAddress(strIsdn);
	submitSM.getSourceAddr().setAddress(this.soureAdress);
	SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
	getThreadManagerEx().queueSubmit.enqueue(submitMessage);
}
}
