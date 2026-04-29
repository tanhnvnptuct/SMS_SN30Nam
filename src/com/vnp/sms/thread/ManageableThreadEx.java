package com.vnp.sms.thread;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ManageableThread;
import com.fss.util.StringUtil;
import com.vnp.util.AppServer;
import com.vnp.util.RequestINGW;
import com.vnp.util.RequestMessage;

public abstract class ManageableThreadEx extends ManageableThread {
	public RequestINGW mreqIngw = null;
	public String mstrHost;
	public int miPort;
	public String mstrUsername;
	public String mstrPassword;
	public int mintKeepAliveInterval = 30000; // seconds
	private static int iINGWConnectRetryTime = 100;
	private static int iDBConnectRetryTime = 100;
	static {
		String strRetryTime = StringUtil.nvl(AppServer.getParam("INGWRetrytime"), "100");
		if (strRetryTime.equals("")) {
			strRetryTime = "100";
		}
		iINGWConnectRetryTime = Integer.parseInt(strRetryTime);
		String strDBRetrytime = StringUtil.nvl(AppServer.getParam("DBConnectRetryTime"), "100");
		if (strDBRetrytime.equals("")) {
			strDBRetrytime = "100";
		}
		iDBConnectRetryTime = Integer.parseInt(strDBRetrytime);
	}

	protected ThreadManagerEx getThreadManagerEx() {
		return ((ThreadManagerEx) mmgrMain);
	}

	public ManageableThreadEx getThreadByName(String pstrThreadName, String pstrThreadClassName) {
		Vector vvecThreadLists = mmgrMain.getThreadList();
		ManageableThreadEx mCaller = null;
		for (int i = 0; i < vvecThreadLists.size(); i++) {
			if (vvecThreadLists.elementAt(i) instanceof ManageableThreadEx) {
				ManageableThreadEx vThread = (ManageableThreadEx) vvecThreadLists.elementAt(i);
				String vstrClassName = vThread.getClassName();
				String vstrThreadName = vThread.getThreadName();
				if (vstrClassName.indexOf(pstrThreadClassName) >= 0 && vstrThreadName.equalsIgnoreCase(pstrThreadName)) {
					mCaller = vThread;
					break;
				}
			}
		}
		return mCaller;
	}

	public boolean retryConnectInGateWay() throws Exception {

		int waitMinute = 1;
		long waittime = 1000 * 60 * waitMinute;
		int retry = 0;
		while (retry <= iINGWConnectRetryTime) {
			try {
				if (mreqIngw == null) {
					mreqIngw = new RequestINGW();
					mreqIngw.fillParameter(mstrHost, miPort, mstrUsername, mstrPassword, mintKeepAliveInterval);
				}
				if (!mreqIngw.isConnected()) {
					mreqIngw.disconnect();
					mreqIngw.connect();
				}
				if (mreqIngw.isSendKeepAliveCommand()) {
					mreqIngw.sendKeepAlive();
					logMonitor("Send keepalive INGW");
				}
				// OK
				return true;
			}
			catch (Exception e) {
				logMonitor(" Ket noi reset do qua thoi gian xu ly");

			}
			catch (Throwable t) {

			}
			// not connected met
			retry++;
			Thread.sleep(waittime);
		}

		return false;

	}

	public void validateParameter() throws Exception {
	}

	public void initPreparedStatement() throws Exception {
		
	}

	public void closeDBObject() {
		
	}

	public boolean checkDBConnection() {

		int waitMinute = 1;
		long waittime = 1000 * 60 * waitMinute;
		int retry = 0;
		try {

			while (retry <= iDBConnectRetryTime && (mcnMain == null || mcnMain.isClosed())) {

				if (mcnMain == null) {
					mcnMain = mmgrMain.getConnection();
				}

				if (mcnMain.isClosed()) {
					closeDBObject();
					Database.closeObject(mcnMain);
					mcnMain = null;
					mcnMain = mmgrMain.getConnection();
				}
				if (mcnMain == null || mcnMain.isClosed()) {
					logMonitor("Connection DB lost , system wait in " + waitMinute + " minute");
					Thread.sleep(waittime);
					retry++;
				} else {
					initPreparedStatement();
					return true;
				}
			}
			//
			if (retry == 0) {
				return true;
			} else {
				return false;
			}

		}
		catch (Exception r) {
			return false;
		}
	}

	@Override
	protected void afterSession() throws Exception {
		// TODO Auto-generated method stub
		try {
			mreqIngw.disconnect();
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		closeDBObject();
		super.afterSession();

	}

}
