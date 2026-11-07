package com.vnp.sms.thread;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import org.smpp.*;
import org.smpp.pdu.*;
import org.smpp.util.ByteBuffer;

import com.fss.sql.Database;
import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.util.AppServer;
import com.vnp.util.SubmitMessage;

public class SendSMSFromDBNew3 extends ManageableThreadEx {
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
	private static int seq = 1;
	private static int max_seq = 2147483647;

	@Override
	public void initPreparedStatement() throws Exception {
		currentDate = new Date();
		mstrCampaignId = "";
		setupAcceptCampaign();
		if (mstrCampaignId.trim().equals("")) {
			mstrCampaignId = "0";
		}
		// mstrSqlGet = "select * from "+mstrTableSMSQueue+"
		// partition(Part_"+dateFormat.format(currentDate)+") where status in (0, 2) and
		// campaign_id in (" + mstrCampaignId + ") and msisdn not in (select msisdn from
		// blacklist) and rownum < 2000 order by submit_time";
		mstrSqlGet = "select * from " + mstrTableSMSQueue
				+ " where submit_time >= trunc(sysdate-1) and submit_time < trunc(sysdate+1) and status in (0, 2) and campaign_id in ("
				+ mstrCampaignId
				+ ") and msisdn not in (select msisdn from blacklist) and rownum < 2000 order by submit_time";
		logMonitor("mstrSqlGet " + mstrSqlGet);
		logMonitor("psUpdateQueue " + mstrSqlUpdate);
		psReadQueue = mcnMain.prepareStatement(mstrSqlGet);
		psUpdateQueue = mcnMain.prepareStatement(mstrSqlUpdate);
	}

	private void setupAcceptCampaign() throws ParseException {
		Calendar calendar = Calendar.getInstance();
		for (Object obj : campaignParam) {
			String campaignId = (String) ((Vector) obj).get(0);
			String expireDate = (String) ((Vector) obj).get(1);
			if (!campaignId.trim().equals("") && !expireDate.trim().equals("")) {
				Date exp = dateFormat.parse(expireDate);
				calendar.setTime(exp);
				calendar.add(Calendar.DAY_OF_YEAR, 1);
				if (currentDate.before(calendar.getTime())) {
					mstrCampaignId = mstrCampaignId + campaignId + ",";
				}
			}
		}
		if (!mstrCampaignId.trim().equals(""))
			mstrCampaignId = mstrCampaignId.substring(0, mstrCampaignId.length() - 1);
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
			} finally {
				Database.closeObject(rs);
			}
		}
	}

	protected void submitMessage(String strIsdn, String strMessage) throws Exception {
		try {
			if (test_mode.equalsIgnoreCase("Y") && acceptIsdn.indexOf("|" + strIsdn + "|") < 0) {
				return;
			}
			while (getThreadManagerEx().queueSubmit.size() > 100000) {
				Thread.sleep(100);
				logMonitor("Queue full...");
			}
//			Charset encode = Charset.forName(Data.ENC_UTF16_BE);
			Charset encode = onlyGSM7BitCharacter(strMessage);

//			byte coding = ((byte) 0x08);
			byte coding = autoDataCoding(encode);

//			byte[] messageData = autoMessageData(strMessage, encode);
//			List<ByteBuffer> messages = new ArrayList<ByteBuffer>();
			List<String> messages = new ArrayList<String>();


			int seqSize = 134;
			if (encode == Charset.forName(Data.ENC_UTF16_BE))
				seqSize = 67;
			else
				seqSize = 150;

			int seqCount = strMessage.length() / seqSize + (strMessage.length() % seqSize == 0 ? 0 : 1);
			for (int iSeq = 0; iSeq < seqCount; iSeq++) {
				int start = iSeq * seqSize;
				int end = Math.min((iSeq + 1) * seqSize, strMessage.length());
				String strbuff = strMessage.substring(start, end);
				messages.add(strbuff);
//				ByteBuffer bb = new ByteBuffer();
//				bb.setBuffer(Arrays.copyOfRange(messageData, start, end));
//				messages.add(bb);
			}

			short sarRef = (short) new Random().nextInt(255);
			int messageSize = messages.size();

			for (int i = 0; i < messageSize; i++) {
//				byte[] rawBytes = messages.get(i).getBuffer();
//				String strbuff = new String(rawBytes, encode);
				String strbuff = messages.get(i);
				SubmitSM request = new SubmitSM();
				request.getDestAddr().setAddress(strIsdn);

//				Address addressSrc = new Address();
//				addressSrc.setAddress(mstrSender);
//				addressSrc.setNpi((byte) 0x01);
//				request.setSourceAddr(addressSrc);
		        request.getSourceAddr().setAddress(mstrSender);

				request.setShortMessage(strbuff,encode.name());
				request.setDataCoding(coding);
//				request.setEsmClass((byte) 0x00); 
													
				int newSeq = (seq++ % max_seq) + 1;
				request.setSequenceNumber(newSeq);

				if (messageSize > 1) {
					request.setSarMsgRefNum(sarRef);
					request.setSarTotalSegments((short) messageSize);
					request.setSarSegmentSeqnum((short) (i + 1));
				}

				SubmitMessage submitMessage = new SubmitMessage(request, false);
				getThreadManagerEx().queueSubmit.enqueue(submitMessage);
			}

//			
		} catch (Exception e) {
			logMonitor("error when send sms for " + strIsdn + " with content " + strMessage + " Exception "
					+ e.getMessage());
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
		vtValue2.addElement(ParameterUtil.createParameterDefinition("IDChuongTrinh", "",
				ParameterType.PARAM_TEXTBOX_MAX, "50", ""));
		vtValue2.addElement(
				ParameterUtil.createParameterDefinition("ExpireDate", "", ParameterType.PARAM_TEXTBOX_MAX, "500", ""));
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

	public static Charset onlyGSM7BitCharacter(String strContent) {
		CharsetEncoder encoder;
		encoder = Charset.forName("ISO-8859-1").newEncoder();

		if (encoder.canEncode(strContent)) {
			return Charset.forName(Data.ENC_ISO8859_1);
		} else {
			return Charset.forName(Data.ENC_UTF16_BE);
		}
	}

	private byte autoDataCoding(Charset encode) {
		if (encode.equals(Charset.forName(Data.ENC_UTF16_BE)))
			return ((byte) 0x08);
		else
			return ((byte) 0x00);// return ((byte) 0x00);
	}

	private byte[] autoMessageData(String content, Charset encode) {
		if (encode == Charset.forName(Data.ENC_UTF16_BE)) {
			return content.getBytes(encode);
		} else {
			return content.getBytes(Charset.forName("ASCII"));
		}
	}

}
