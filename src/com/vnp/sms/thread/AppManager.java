package com.vnp.sms.thread;

import java.io.*;
import java.sql.*;
import java.util.*;

import org.apache.log4j.PropertyConfigurator;

import com.fss.util.*;
import com.fss.thread.*;
import com.fss.dictionary.*;
import com.vnp.util.*;
import java.text.SimpleDateFormat;

public class AppManager implements ProcessorListener
{
	////////////////////////////////////////////////////////
	// Purpose: Get parameter from memory
	// Date: 29/04/2003
	////////////////////////////////////////////////////////
	public Connection getConnection() throws Exception
	{
		return AppServer.getConnection();

	}

	////////////////////////////////////////////////////////sel
	// Purpose: Get parameter from memory
	// Date: 29/04/2003
	////////////////////////////////////////////////////////
	public String getParameter(String strKey)
	{
		try{
			return AppServer.getDictionaryNode(strKey).getValue();

		}
		catch(Exception e){
			return "";
		}
	}

	////////////////////////////////////////////////////////
	// Purpose: Event handler
	// Date: 29/04/2003
	////////////////////////////////////////////////////////
	public void onCreate(ThreadProcessor processor) throws Exception
	{
		processor.log = null;
		processor.authenticator = new AppAuthenticator();
	}

	////////////////////////////////////////////////////////
	public void onOpen(ThreadProcessor processor) throws Exception
	{
		processor.mcnMain = getConnection();
		((AppAuthenticator)processor.authenticator).setConnection(processor.mcnMain);
	}

	///////////////////////////////////////////////////////////
	// Main entry
	///////////////////////////////////////////////////////////


	public static void main(String argvs[]) throws Exception
	{
		try{

			try{

//				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//				Calendar nextNotifTime = Calendar.getInstance();
//				//nextNotifTime.setTime(new java.util.Date());
//
//				int i = nextNotifTime.get(Calendar.MONTH);
//				System.out.print(i);
////				nextNotifTime.setTime(new java.util.Date());
//				nextNotifTime.add(Calendar.MONTH,1);
//				nextNotifTime.set(Calendar.DATE,1);
//				nextNotifTime.set(Calendar.HOUR,23);
//				nextNotifTime.set(Calendar.MINUTE,59);
//				nextNotifTime.set(Calendar.SECOND,59);
//				nextNotifTime.add(Calendar.DATE, -1);
//				java.util.Date dueDate = new java.util.Date(nextNotifTime.getTimeInMillis());
//				String str = sdf.format(dueDate);
				//tunglv: log4j config if need
//				PropertyConfigurator.configure("log4j.properties");

			}
			catch(Exception ex){
				System.out.print(ex.toString());
				ex.printStackTrace();
			}
			// Change system output to file
			AppManager lsn = new AppManager();

			//log file
			//DictionaryNode n = AppServer.getDictionaryNode("Thread.LogFile");
			String strLogFile = "";//n.getValue();
			if(strLogFile == null || strLogFile.equals("")){
				strLogFile = "error.log";
			}
			File fl = new File(strLogFile);
			if(fl.getParentFile() != null){
				fl.getParentFile().mkdirs();
			}
			PrintStream ps = new PrintStream(new LogOutputStream(strLogFile));
			System.setOut(ps);
			System.setErr(ps);

			// Change system timezone
			App.tableAccount = lsn.getParameter("AccountTable");
			int iTimeZoneOffset = 25200000;
			try{
				iTimeZoneOffset = Integer.parseInt(AppServer.getDictionaryNode("TimeZone.Offset").getValue());
			}
			catch(Exception e){
			}
			DictionaryNode nd = AppServer.getDictionaryNode("TimeZone.Description");
			String strTimeZoneDescription = null;
			if(nd != null){
				strTimeZoneDescription = nd.getValue();
			}
			if(strTimeZoneDescription == null || strTimeZoneDescription.length() == 0){
				strTimeZoneDescription = "Viet Nam/Ha Noi";
			}
			TimeZone.setDefault(new SimpleTimeZone(iTimeZoneOffset,strTimeZoneDescription));

			// Set port
			int iPort = 1309;
			try{
				iPort = Integer.parseInt(lsn.getParameter("Thread.PortID"));
			}
			catch(Exception e){
			}

			ThreadManagerEx threadManager = new ThreadManagerEx(iPort,lsn);
			// Fix loading method
			threadManager.setLoadingMethod(ThreadManager.LOAD_FROM_FILE);
			threadManager.setProcessorListener(lsn);

			// Set max connection
			try{
				int iMaxConnectionAllowed = Integer.parseInt(lsn.getParameter("MaxConnectionAllowed"));
				if(iMaxConnectionAllowed > 0){
					threadManager.setMaxConnectionAllowed(iMaxConnectionAllowed);
				}
			}
			catch(Exception e){
			}

			// Set actionFile
			String strActionFile = AppServer.getDictionaryNode("Thread.ActionLog").getValue();
			if(strActionFile == null || strActionFile.equals("")){
				strActionFile = "action.log";
			}
			fl = new File(strActionFile);
			if(fl.getParentFile() != null){
				fl.getParentFile().mkdirs();
			}
			threadManager.setActionLogFile(strActionFile);

			// Start manager
			Global.APP_NAME = "VinaPhone Eload System v2.0";
			threadManager.start();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public ThreadManagerEx loadThreadServer()
	{
		try{
			// Change system output to file
			AppManager lsn = new AppManager();

			//log file
			String strLogFile = AppServer.getDictionaryNode("Thread.LogFile").getValue();
			if(strLogFile == null || strLogFile.equals("")){
				strLogFile = "error.log";
			}
			File fl = new File(strLogFile);
			if(fl.getParentFile() != null){
				fl.getParentFile().mkdirs();
			}
			PrintStream ps = new PrintStream(new LogOutputStream(strLogFile));
			System.setOut(ps);
			System.setErr(ps);

			// Change system timezone
			int iTimeZoneOffset = 25200000;
			try{
				iTimeZoneOffset = Integer.parseInt(AppServer.getDictionaryNode("TimeZone.Offset").getValue());
			}
			catch(Exception e){
			}
			DictionaryNode nd = AppServer.getDictionaryNode("TimeZone.Description");
			String strTimeZoneDescription = null;
			if(nd != null){
				strTimeZoneDescription = nd.getValue();
			}
			if(strTimeZoneDescription == null || strTimeZoneDescription.length() == 0){
				strTimeZoneDescription = "Viet Nam/Ha Noi";
			}
			TimeZone.setDefault(new SimpleTimeZone(iTimeZoneOffset,strTimeZoneDescription));

			// Set port
			int iPort = 1309;
			try{
				iPort = Integer.parseInt(lsn.getParameter("Thread.PortID"));
			}
			catch(Exception e){
			}
			int iTimeInterval = 300;
			try{
				iTimeInterval = Integer.parseInt(lsn.getParameter("DBSybcInterval"));
			}
			catch(Exception e){
			}
			ThreadManagerEx threadManager = new ThreadManagerEx(iPort,lsn);
			threadManager.setStatus(ThreadManagerEx.STARTING_STATUS);

			// Fix loading method
			threadManager.setLoadingMethod(ThreadManager.LOAD_FROM_FILE);
			threadManager.setProcessorListener(lsn);

			// Set max connection
			try{
				int iMaxConnectionAllowed = Integer.parseInt(lsn.getParameter("MaxConnectionAllowed"));
				if(iMaxConnectionAllowed > 0){
					threadManager.setMaxConnectionAllowed(iMaxConnectionAllowed);
				}
			}
			catch(Exception e){
			}

			// Set actionFile
			String strActionFile = AppServer.getDictionaryNode("Thread.ActionLog").getValue();
			if(strActionFile == null || strActionFile.equals("")){
				strActionFile = "action.log";
			}
			fl = new File(strActionFile);
			if(fl.getParentFile() != null){
				fl.getParentFile().mkdirs();
			}
			threadManager.setActionLogFile(strActionFile);

			// Start manager
			Global.APP_NAME = "Eload Interface System v1.0";
			threadManager.start();
			threadManager.setStatus(ThreadManagerEx.STARTED_STATUS);

			return threadManager;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

}
