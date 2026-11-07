package com.vnp.sms.thread;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;

public class SendSMSFromDBNew2_bak extends ManageableThreadEx{
	private PreparedStatement psReadQueue;
	private PreparedStatement psUpdateQueue;
	private String mstrSender = "";
	private String mstrTableSMSQueue;
	private String mstrCampaignId = "";
	protected Vector campaignParam = new Vector();
	private String mstrSqlGet = "";
	private String mstrSqlUpdate = "";
	private String acceptIsdn = "";
	private String test_mode = "";
	private int icount = 0;
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private Date currentDate = new Date();		

	@Override
	public void initPreparedStatement() throws Exception {
		currentDate = new Date();
		mstrCampaignId = "";
		setupAcceptCampaign();		
		if(mstrCampaignId.trim().equals("")){
			mstrCampaignId = "0";
		}	
		//mstrSqlGet = "select * from "+mstrTableSMSQueue+" partition(Part_"+dateFormat.format(currentDate)+") where status in (0, 2) and campaign_id in (" + mstrCampaignId + ") and msisdn not in (select msisdn from blacklist) and rownum < 2000 order by submit_time";
		mstrSqlGet = "select * from "+mstrTableSMSQueue+" where submit_time >= trunc(sysdate-1) and submit_time < trunc(sysdate+1) and status in (0, 2) and campaign_id in (" + mstrCampaignId + ") and msisdn not in (select msisdn from blacklist) and rownum < 2000 order by submit_time";
		logMonitor("mstrSqlGet " + mstrSqlGet);
		logMonitor("psUpdateQueue " + mstrSqlUpdate);
		psReadQueue = mcnMain.prepareStatement(mstrSqlGet);
		psUpdateQueue = mcnMain.prepareStatement(mstrSqlUpdate);
	}
	
	private void setupAcceptCampaign() throws ParseException {
		Calendar calendar = Calendar.getInstance();
		for(Object obj : campaignParam){
			String campaignId = (String) ((Vector) obj).get(0);
			String expireDate = (String) ((Vector) obj).get(1);
			if(!campaignId.trim().equals("") && !expireDate.trim().equals("")){
				Date exp = dateFormat.parse(expireDate);
				calendar.setTime(exp);
				calendar.add(Calendar.DAY_OF_YEAR, 1);
				if(currentDate.before(calendar.getTime())){
					mstrCampaignId = mstrCampaignId + campaignId + ",";
				}
			}
		}
		if(!mstrCampaignId.trim().equals("")) mstrCampaignId = mstrCampaignId.substring(0, mstrCampaignId.length() - 1);
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
		ResultSet rs = null;
		if (miThreadCommand != ThreadConstant.THREAD_STOP) {
			try {
				Database.closeObject(rs);
				rs = psReadQueue.executeQuery();				
				while (rs.next()) {		
					String rowid = rs.getString("id");
					String msisdn = rs.getString("msisdn");
					String content = rs.getString("sms_content");
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
			} 
		}		
	}

	protected void submitMessage(String strIsdn, String strMessage) throws Exception {
		try{
			if(test_mode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + strIsdn + "|") < 0){
				return;
			}
			while (getThreadManagerEx().queueSubmit.size() > 100000) {
				Thread.sleep(100);
				logMonitor("Queue full...");
			}
			SubmitSM submitSM = new SubmitSM();
			submitSM.setShortMessage(strMessage);
			submitSM.getDestAddr().setAddress(strIsdn);
			submitSM.getSourceAddr().setAddress(mstrSender);
			SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
			getThreadManagerEx().queueSubmit.enqueue(submitMessage);
		}catch(Exception e){
			logMonitor("error when send sms for " + strIsdn + " with content " + strMessage);
		}
		
	}

	public void fillParameter() throws AppException {
		mintKeepAliveInterval = mintKeepAliveInterval * 1000;
		Object obj = getParameter("CampaignId");
		if (obj != null && obj instanceof Vector) {
			campaignParam = (Vector) obj;
		} else {
			campaignParam = new Vector();
		}
		if (campaignParam == null) {
			campaignParam = new Vector();
		}
		mstrSqlUpdate = loadMandatory("SqlUpdate");
		mstrTableSMSQueue = loadMandatory("TableSMSQueue");
		mstrSender = loadMandatory("CodeSender");
		acceptIsdn = loadMandatory("AcceptIsdn");
		test_mode = loadMandatory("TestMode");
		super.fillParameter();
	}

	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		
		Vector vtValue2 = new Vector();
		vtValue2.addElement(ParameterUtil.createParameterDefinition(
				"IDChuongTrinh", "", ParameterType.PARAM_TEXTBOX_MAX, "50",
				""));
		vtValue2.addElement(ParameterUtil.createParameterDefinition(
				"ExpireDate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));		
		vtReturn.addElement(createParameterDefinition("CampaignId", "", ParameterType.PARAM_TABLE, vtValue2, ""));
		vtReturn.addElement(createParameterDefinition("SqlUpdate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("TableSMSQueue", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
		vtReturn.addElement(createParameterDefinition("CodeSender", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		vtReturn.addElement(createParameterDefinition("AcceptIsdn", "", ParameterType.PARAM_TEXTBOX_MAX, "100", ""));
		Vector vtValue1 = new Vector();
		vtValue1.addElement("N");
		vtValue1.addElement("Y");
		vtReturn.addElement(createParameterDefinition("TestMode", "", ParameterType.PARAM_COMBOBOX, vtValue1, ""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}
}
