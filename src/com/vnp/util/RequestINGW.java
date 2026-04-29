package com.vnp.util;

import com.fss.util.AppException;
import com.fss.util.StreamUtil;
import com.vnp.util.message.XMLMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RequestINGW {
	int miTransID = 1;
	private String mstrSessionID;
	private String host;
	private int port;
	private String username;
	private String password;
	private int mintKeepAliveInterval = 30000;
	private Socket socket;
	InputStream is;
	public static final String TERMINATED_SYMBOL = "</uinml>";
	public static final String KEEPALIVE_COMMAND = "keepalive";
	public static final String CONNECT_COMMAND = "connect";
	public static final String EXIT_COMMAND = "exit";
	private long mlngNextSendKeepAlive = 0L;
	public static final String QUERY = "query_account_ex";
	public static final String ADD_BALANCE = "modify_list_balance";
	public static final String ADD_BALANCE_2 = "modify_list_balance_2";
	public static final String SET_ALCS = "set_alcs";
	public static final String OFF_ALCS = "remove_alcs";

	public String mstrMessage;
	public String DEL_GROUP_ACC = "delete_group_account";

	public String DEL_PHONEBOOK = "delete_phonebook";

	public String REMOVE_MEMBER_FROM_GROUP = "remove_member_from_group";

	public String MODIFY_SPEND_LIMIT = "modify_spend_limit";

	public String MODIFY_LIMIT_GROUP_ACCOUNT = "modify_spend_limit_group_acc";
	public static final String ADD_MEMBER_TO_GROUP = "add_member_to_group";
	public String ADD_LOCATION = "create_location";

	String REMOVE_LOCATION = "delete_location";
	public static final String ADD_CIRCLE = "add_calling_circle_member";
	public static final String REMOVE_CIRCLE = "remove_calling_circle_member";
	public static final String SET_PC = "set_periodiccharge_2";
	public static final String ADD_BONUS = "create_prom_plan";
	public static final String REMOVE_BONUS = "delete_prom_plan";
	public static final String CREATE_GROUP_ACCOUNT = "create_group_account";
	public static final String CREATE_PHONEBOOK = "create_phonebook";
	boolean isConnected = false;
	//
	public XMLMessage mRequest;
	public XMLMessage mResponse;

	public boolean setAcc(String strIsdn, String accName) throws Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		String strTransID = String.valueOf(this.miTransID);
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.header.command", "set_acc");
		msgRequest.setValue("uinml.data.transid", strTransID);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.accname", accName);
		msgRequest.setValue("uinml.data.accvalue", "0");

		OutputStream out = this.socket.getOutputStream();
		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean reSetAcc(String strIsdn, String accName) throws Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		String strTransID = String.valueOf(this.miTransID);
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.header.command", "set_acc");
		msgRequest.setValue("uinml.data.transid", strTransID);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.accname", accName);
		msgRequest.setValue("uinml.data.accvalue", "0");

		OutputStream out = this.socket.getOutputStream();
		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean regAlcs(String strISDN, String strAlcs, String startDate, String endDate) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "set_alcs");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.name", strAlcs);
		msgRequest.setValue("uinml.data.start_date", startDate);
		msgRequest.setValue("uinml.data.end_date", endDate);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean deleteAlcs(String strISDN, String strAlcsName) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "remove_alcs");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.name", strAlcsName);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean minusMoney(String strISDN, String strMoney, String strComment) throws IOException, Exception {
		boolean bl = true;
		if (strMoney.equals("0.0") || strMoney.equals("-0.0")) {
			strMoney = "0";
		}
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "modify_list_balance");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.list_account", "tkc");
		msgRequest.setValue("uinml.data.list_amount", strMoney);

		msgRequest.setValue("uinml.data.comment", strComment);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean minusMoney(String strISDN, String strMoney, String strDay, String strComment) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "modify_list_balance");
		msgRequest.setValue("uinml.data.msisdn", strISDN);

		msgRequest.setValue("uinml.data.list_account", "tkc:km:km1:km2:data:gprs_student_teenager");
		msgRequest.setValue("uinml.data.list_amount", strMoney);
		msgRequest.setValue("uinml.data.list_days", strDay);
		msgRequest.setValue("uinml.data.comment", strComment);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean minusMoney(String strISDN, String listAccount, String listMoney, String listDate, String strComment) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "modify_list_balance");
		msgRequest.setValue("uinml.data.msisdn", strISDN);

		msgRequest.setValue("uinml.data.list_account", listAccount);
		msgRequest.setValue("uinml.data.list_amount", listMoney);
		msgRequest.setValue("uinml.data.list_days", listDate);
		msgRequest.setValue("uinml.data.comment", strComment);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean changeStatus(String strISDN, String status) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "change_status");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.status", status);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean minusMoney2(String strISDN, String listAccount, String listMoney, String listDate, String strComment) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", String.valueOf(this.miTransID));
		msgRequest.setValue("uinml.header.command", "modify_list_balance_2");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.list_account", listAccount);
		msgRequest.setValue("uinml.data.list_amount", listMoney);
		msgRequest.setValue("uinml.data.list_date", listDate);

		msgRequest.setValue("uinml.data.comment", strComment);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean minusMoney2(String strISDN, String strCommand, String listAccount, String listMoney, String listDate, String strComment) throws IOException,
			Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", String.valueOf(this.miTransID));
		msgRequest.setValue("uinml.header.command", strCommand);
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.list_account", listAccount);
		msgRequest.setValue("uinml.data.list_amount", listMoney);
		if ((listDate != null) || (!listDate.equals(""))) {
			msgRequest.setValue("uinml.data.list_date", listDate);
		}
		msgRequest.setValue("uinml.data.comment", strComment);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean deleteGroupAccount(String strISDN, String groupName) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", this.DEL_GROUP_ACC);
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.group_name", groupName);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean deletePhonebook(String strISDN, String position) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", this.DEL_PHONEBOOK);
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.position", position);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}

	public boolean removeMemberFromGroup(String strISDN) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", this.REMOVE_MEMBER_FROM_GROUP);
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean modifySpendingLimit(String strIsdn, String totalBalance, String totalSpendLimit, String nextSpendLimit, String expDate) throws IOException,
			Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", this.MODIFY_SPEND_LIMIT);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.total_balance", totalBalance);
		msgRequest.setValue("uinml.data.total_spend_balance", totalSpendLimit);
		msgRequest.setValue("uinml.data.next_spend_balance", nextSpendLimit);
		msgRequest.setValue("uinml.data.exp_date", expDate);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean modifyLimitGroupAcc(String groupName, String money, String mstrIsdn) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", this.MODIFY_LIMIT_GROUP_ACCOUNT);
		msgRequest.setValue("uinml.data.msisdn", mstrIsdn);
		msgRequest.setValue("uinml.data.group_name", groupName);
		msgRequest.setValue("uinml.data.money", money);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean addMemberToGroup(String groupName, String strISDN) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "add_member_to_group");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.group_name", groupName);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public XMLMessage query(String strISDN) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "query_account_ex");
		msgRequest.setValue("uinml.data.msisdn", strISDN);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		try {
			XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
			return msgResponse;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public XMLMessage query4(String strISDN) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "query_account_ex_4");
		msgRequest.setValue("uinml.data.msisdn", strISDN);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		try {
			XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
			return msgResponse;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public boolean addLocation(String strIsdn, String strLocation) throws Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		String strTransID = String.valueOf(this.miTransID);
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.header.command", this.ADD_LOCATION);
		msgRequest.setValue("uinml.data.transid", strTransID);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.location", strLocation);
		msgRequest.setValue("uinml.data.position", "1");

		OutputStream out = this.socket.getOutputStream();
		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean removeLocation(String strIsdn) throws Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		String strTransID = String.valueOf(this.miTransID);
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.header.command", this.REMOVE_LOCATION);
		msgRequest.setValue("uinml.data.transid", strTransID);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.position", "1");

		OutputStream out = this.socket.getOutputStream();
		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean addCircleMember(String strCircle, String strIsdn) throws Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		String strTransID = String.valueOf(this.miTransID);
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.header.command", "add_calling_circle_member");
		msgRequest.setValue("uinml.data.transid", strTransID);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);

		msgRequest.setValue("uinml.data.circle_name", strCircle);

		OutputStream out = this.socket.getOutputStream();
		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean removeCircleMember(String strCircle, String strIsdn) throws Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		String strTransID = String.valueOf(this.miTransID);
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.header.command", "remove_calling_circle_member");
		msgRequest.setValue("uinml.data.transid", strTransID);
		msgRequest.setValue("uinml.data.msisdn", strIsdn);

		msgRequest.setValue("uinml.data.circle_name", strCircle);

		OutputStream out = this.socket.getOutputStream();
		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean setPeriodCharge(String strISDN, String chargeId, String startDate, String endDate) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "set_periodiccharge_2");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.charg_id", chargeId);
		msgRequest.setValue("uinml.data.start_date", startDate);
		msgRequest.setValue("uinml.data.end_date", endDate);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}
	public boolean setPeriodCharge3(String strISDN, String chargeId, String startDate, String endDate) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "set_periodiccharge_3");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.charg_id", chargeId);
		msgRequest.setValue("uinml.data.start_date", startDate);
		msgRequest.setValue("uinml.data.end_date", endDate);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}

	public boolean deletePeriodCharge(String strISDN, String chargeId) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "delete_periodiccharge");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.charg_id", chargeId);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}

	public boolean setAccALO(String strIsdn, String accValue) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "set_acc");
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.accname", "ALO");
		msgRequest.setValue("uinml.data.accvalue", accValue);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}

	public boolean addBonusPlan(String strIsdn, String planId) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "create_prom_plan");
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.promid", planId);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}

	public boolean removeBonusPlan(String strIsdn, String planId) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "delete_prom_plan");
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.promid", planId);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");

		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;

	}

	public boolean createGroupAccount(String groupid, String groupName, String mstrIsdn) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "create_group_account");
		msgRequest.setValue("uinml.data.msisdn", mstrIsdn);
		msgRequest.setValue("uinml.data.groupid", groupid);
		msgRequest.setValue("uinml.data.group_name", groupName);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean createPhonebook(String strISDN, String position, String value) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "create_phonebook");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.position", position);
		msgRequest.setValue("uinml.data.value", value);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean changeCos(String strISDN, String newCos) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "change_cos");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.newsubtype", newCos);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean modifyListBalance2(String strISDN, String strListAccount, String strListAmount, String strListDate, String strComment) throws IOException,
			Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "modify_list_balance_2");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.list_account", strListAccount);
		msgRequest.setValue("uinml.data.list_amount", strListAmount);
		msgRequest.setValue("uinml.data.list_date", strListDate);
		msgRequest.setValue("uinml.data.comment", strComment);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean assignBonusPlan(String strISDN, String strBonusPlan) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "assign_bonus_plan");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.bonusplan", strBonusPlan);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean unAssignBonusPlan(String strISDN, String strBonusPlan) throws IOException, Exception {
		boolean bl = true;
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "remove_bonus_plan");
		msgRequest.setValue("uinml.data.msisdn", strISDN);
		msgRequest.setValue("uinml.data.bonusplan", strBonusPlan);
		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}

		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		//
		mRequest = msgRequest;
		mResponse = msgResponse;
		//

		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean regPreferNumber(String strHost, String strMember) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "modify_preferred_number");
		msgRequest.setValue("uinml.data.msisdn", strHost);
		msgRequest.setValue("uinml.data.prefer_number", strMember);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}

	public boolean removePreferNumber(String strIsdn) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "remove_preferred_number");
		msgRequest.setValue("uinml.data.msisdn", strIsdn);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (strStatus.equals("0")) {
			return true;
		}

		this.mstrMessage = msgResponse.getValue("uinml.data.message");
		return false;
	}
	public boolean addEmergencyCreditTest(String processid,String strIsdn,String amount) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "add_emergency_credit_test");
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.processid", processid);
		msgRequest.setValue("uinml.data.amount", amount);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (!strStatus.equals("0")) {
			return false;
		}
		String processStatus = msgResponse.getValue("uinml.data.process_status");
		String processMessage = msgResponse.getValue("uinml.data.process_message");
		this.mstrMessage = processMessage;
		if(processStatus.equals("0")){
			return true;
		}
		return false;
	}
	public boolean minusEmergencyCreditTest(String processid,String strIsdn,String amount,String fee) throws IOException, Exception {
		XMLMessage msgRequest = new XMLMessage();
		this.miTransID += 1;
		msgRequest.setValue("uinml.header.session", this.mstrSessionID);
		msgRequest.setValue("uinml.data.transid", "" + this.miTransID);
		msgRequest.setValue("uinml.header.command", "minus_emergency_credit_test");
		msgRequest.setValue("uinml.data.msisdn", strIsdn);
		msgRequest.setValue("uinml.data.processid", processid);
		msgRequest.setValue("uinml.data.amount", amount);
		msgRequest.setValue("uinml.data.fee", fee);

		OutputStream out = this.socket.getOutputStream();

		synchronized (out) {
			msgRequest.store(out);
			out.flush();
		}
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		byte[] bt;
		synchronized (this.is) {
			bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
		}
		int iIndex = 0;
		while ((bt[iIndex] <= 32) && (iIndex < bt.length)) {
			iIndex++;
		}
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt, iIndex, bt.length - iIndex));
		String strStatus = msgResponse.getValue("uinml.data.status");
		if (!strStatus.equals("0")) {
			return false;
		}
		String processStatus = msgResponse.getValue("uinml.data.process_status");
		String processMessage = msgResponse.getValue("uinml.data.process_message");
		this.mstrMessage = processMessage;
		if(processStatus.equals("0")){
			return true;
		}
		return false;
	}


	public void fillParameter(String strHost, int iPort, String strUserName, String strPassword, int iMintKeepAliveInterval) throws AppException {
		this.host = strHost;
		this.port = iPort;
		this.username = strUserName;
		this.password = strPassword;
		this.mintKeepAliveInterval = iMintKeepAliveInterval;
	}

	public void connect() throws Exception {
		isConnected = false;
		this.socket = new Socket(this.host, this.port);
		XMLMessage msgRequest = new XMLMessage();
		msgRequest.setValue("uinml.header.session", "0");
		msgRequest.setValue("uinml.header.command", "connect");
		msgRequest.setValue("uinml.data.username", this.username);
		msgRequest.setValue("uinml.data.password", this.password);
		msgRequest.store(this.socket.getOutputStream());
		this.socket.getOutputStream().flush();

		byte[] bt = StreamUtil.getDataTerminatedBySymbol(this.socket.getInputStream(), "</uinml>");
		XMLMessage msgResponse = new XMLMessage(new ByteArrayInputStream(bt));
		String strStatus = msgResponse.getValue("uinml.header.status");

		if ((strStatus == null) || (!strStatus.equals("0"))) {
			throw new Exception(msgResponse.getValue("uinml.data.errormessage"));
		}
		this.mstrSessionID = msgResponse.getValue("uinml.header.session");

		if (this.mstrSessionID == null) {
			throw new Exception("Session id not found in response message");
		}
		this.is = this.socket.getInputStream();
		this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
		isConnected = true;
	}

	public void disconnect() {
		isConnected = false;
		if (this.socket != null) {
			try {
				XMLMessage msgRequest = new XMLMessage();
				msgRequest.setValue("uinml.header.session", this.mstrSessionID);
				msgRequest.setValue("uinml.header.command", "exit");
				msgRequest.store(this.socket.getOutputStream());
				this.socket.getOutputStream().flush();
				Thread.sleep(5000L);
				this.socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			this.socket = null;
		}
	}

	public boolean isSendKeepAliveCommand() {
		return this.mlngNextSendKeepAlive < System.currentTimeMillis();
	}

	public void sendKeepAlive() throws Exception {
		try {
			if (!isConnected) {
				connect();
			}
			XMLMessage msgRequest = new XMLMessage();
			msgRequest.setValue("uinml.header.session", this.mstrSessionID);
			msgRequest.setValue("uinml.header.command", "keepalive");
			msgRequest.store(this.socket.getOutputStream());
			this.socket.getOutputStream().flush();
			this.mlngNextSendKeepAlive = (System.currentTimeMillis() + this.mintKeepAliveInterval);
			byte[] bt;
			synchronized (this.is) {
				bt = StreamUtil.getDataTerminatedBySymbol(this.is, "</uinml>");
			}
		} catch (Exception e) {
			isConnected = false;
			throw e;
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	public String getRequestString() throws Exception {
		if (mRequest == null) {
			return "";
		} else {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			mRequest.store(byteOut);
			String str = new String(byteOut.toByteArray());
			return str;
		}
	}

	public String getResponseString() throws Exception {
		if (mResponse == null) {
			return "";
		} else {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			mResponse.store(byteOut);
			String str = new String(byteOut.toByteArray());
			return str;
		}
	}

	public static void main(String[] args) {
		RequestINGW req = new RequestINGW();
		try {
			req.fillParameter("10.149.39.106", 7773, "bigpro", "bigpro.123", 1000);
			req.connect();
			req.disconnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
