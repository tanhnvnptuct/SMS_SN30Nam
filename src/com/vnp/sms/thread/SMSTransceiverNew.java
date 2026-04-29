package com.vnp.sms.thread;

/*
 * Copyright (c) 1996-2001
 * Logica Mobile Networks Limited
 * All rights reserved.
 *
 * This software is distributed under Logica Open Source License Version 1.0
 * ("Licence Agreement"). You shall use it and distribute only in accordance
 * with the terms of the License Agreement.
 *
 */

import java.util.Vector;

import com.vnp.util.AppServer;
import com.vnp.util.ReceiveMessage;
import com.vnp.util.SubmitMessage;
import com.fss.thread.ParameterType;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;
import com.fss.util.StringUtil;
import com.logica.smpp.Data;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.Session;
import com.logica.smpp.SmppObject;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.debug.Debug;
import com.logica.smpp.debug.DefaultDebug;
import com.logica.smpp.debug.DefaultEvent;
import com.logica.smpp.debug.Event;
import com.logica.smpp.debug.FileDebug;
import com.logica.smpp.debug.FileEvent;
import com.logica.smpp.pdu.Address;
import com.logica.smpp.pdu.AddressRange;
import com.logica.smpp.pdu.BindReceiver;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.BindResponse;
import com.logica.smpp.pdu.BindTransciever;
import com.logica.smpp.pdu.BindTransmitter;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.EnquireLink;
import com.logica.smpp.pdu.EnquireLinkResp;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.SubmitSMResp;
import com.logica.smpp.pdu.Unbind;
import com.logica.smpp.pdu.UnbindResp;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.logica.smpp.util.Queue;
import java.net.ServerSocket;

public class SMSTransceiverNew extends ManageableThreadEx
{
	String syncMode;
	/**
	 * This is the SMPP session used for communication with SMSC.
	 */
	Session session = null;

	/**
	 * If the application is bound to the SMSC.
	 */
	boolean bound = false;

	/**
	 * Address of the SMSC.
	 */
	String ipAddress = null;

	/**
	 * The port number to bind to on the SMSC server.
	 */
	int port = 0;
	/**
	 * The name which identifies you to SMSC.
	 */
	String systemId = null;
	/**
	 * The password for authentication to SMSC.
	 */
	String password = null;

	/**
	 * How you want to bind to the SMSC: transmitter (t), receiver (r) or
	 * transciever (tr). Transciever can both send messages and receive
	 * messages. Note, that if you bind as receiver you can still receive
	 * responses to you requests (submissions).
	 */
	String bindMode = "t";

	/**
	 * Indicates that the Session has to be asynchronous.
	 * Asynchronous Session means that when submitting a Request to the SMSC
	 * the Session does not wait for a response. Instead the Session is provided
	 * with an instance of implementation of ServerPDUListener from the smpp
	 * library which receives all PDUs received from the SMSC. It's
	 * application responsibility to match the received Response with sended Requests.
	 */
	boolean asynchronous = false;

	/**
	 * This is an instance of listener which obtains all PDUs received from the SMSC.
	 * Application doesn't have explicitly call Session's receive() function,
	 * all PDUs are passed to this application callback object.
	 * See documentation in Session, Receiver and ServerPDUEventListener classes
	 * form the SMPP library.
	 */
	SMPPTestPDUEventListener pduListener = null;

	/**
	 * The range of addresses the smpp session will serve.
	 */
	AddressRange addressRange = new AddressRange();

	/*
	 * for information about these variables have a look in SMPP 3.4
	 * specification
	 */
	Address sourceAddress = new Address();
	Address destAddress = new Address();
	byte smDefaultMsgId = 0;
	/**
	 * If you attemt to receive message, how long will the application
	 * wait for data.
	 */
	long receiveTimeout = Data.RECEIVE_BLOCKING;
	int enquireInterval = 60;
	long nextEnquireLink = 0;
	long lastReceiveEnquireLinkResp = 0;
	boolean hasError = false;
	String ignoreMessage = "";
	String[] paramIgnore;
	String mstrSystemType = "";
	int numOfDispatcher = 1;
	public static int mintSequenceNumber = 0;
	static final String dbgDir = "./";
	public static final int DSIM = 16;
	public static final int DSIMD = 17;
	public static final int DSIMD2 = 18;
	/**
	 * The debug object.
	 */
	static Debug debug = null;

	/**
	 * The event object.
	 */
	static Event event = null;
	/**
	 *
	 * @throws Exception
	 */
	public void processSession() throws Exception
	{
		initTCPIPMonitor();
		SMSReceiver[] smsReceiver = new SMSReceiver[numOfDispatcher];
		SMSTransmitter[] smsTransmitter = new SMSTransmitter[numOfDispatcher];



		try{
			hasError = false;
			logMonitor("Start binding to SMSC server....");
			bind();
			if(!bound){
				return;
			}
			for(int i = 0;i < numOfDispatcher;i++){
				logMonitor("Start receiver process " + i);
				smsReceiver[i] = new SMSReceiver(this);
				smsReceiver[i].start();
				logMonitor("Start transmitter process " + i);
				smsTransmitter[i] = new SMSTransmitter(this);
				smsTransmitter[i].start();
			}
			lastReceiveEnquireLinkResp = System.currentTimeMillis();
			while(miThreadCommand != ThreadConstant.THREAD_STOP && !hasError){
				if(shouldSendEnquireLink()){
					enquireLink();
				}
				if(shouldReConnect()){
					logMonitor("Wait message last more than usual, need to reconnect...");
					hasError = true;
				}
				fillLogFile();
				Thread.sleep(100);
			}
		}
		finally{
			for(int i = 0;i < numOfDispatcher;i++){
				if(smsReceiver[i] != null){
					smsReceiver[i].stopNow();
				}
				if(smsTransmitter[i] != null){
					smsTransmitter[i].stopNow();
				}
				Thread.sleep(100);
			}
			unbind();
		}
	}

	public void updateLastReceiveEnquireLinkResp()
	{
		lastReceiveEnquireLinkResp = System.currentTimeMillis();
	}

	public boolean shouldReConnect()
	{
		// Khi gui bang tin enquireLink xong ma 10s sau van chua co response thi reconnect
		return nextEnquireLink - 4 * enquireInterval * 1000 > lastReceiveEnquireLinkResp;
	}

	public class SMSReceiver extends Thread
	{
		boolean bRunning = true;
		public void stopNow()
		{
			bRunning = false;
		}

		public SMSReceiver(SMSTransceiverNew smsTransceiver)
		{
		}

		public boolean isRunning()
		{
			return bRunning;
		}

		public void run()
		{
			while(bRunning){
				try{
					receive();
				}
				catch(Exception e){
					hasError = true;
					bRunning = false;
					logMonitor("Error occurs when getting message: " + e);
				}
			}
		}
	}

	public boolean shouldSendEnquireLink()
	{
		return System.currentTimeMillis() >= nextEnquireLink;
	}

	public void updateTimeNextSendEnquireLink()
	{
		nextEnquireLink = System.currentTimeMillis() + enquireInterval * 1000;
	}

	public class SMSTransmitter extends Thread
	{
		boolean bRunning = true;
		public void stopNow()
		{
			bRunning = false;
		}

		public SMSTransmitter(SMSTransceiverNew smsTransceiver)
		{
		}

		public boolean isRunning()
		{
			return bRunning;
		}

		public synchronized SubmitMessage detactMsg(ThreadManagerEx mgr)
		{
			SubmitMessage submitMessage = null;
			synchronized(mgr.queueSubmit){
				try{
					submitMessage = (SubmitMessage)mgr.queueSubmit.dequeue();
				}
				catch(Exception ex){
				}
			}
			return submitMessage;
		}

		public String formatSendingMobileNo(String mobileNumber)
		{
			String msisdnPrefix = AppServer.getParam("MSISDNPrefix");
			int sendMSISDNPrefix = Integer.parseInt(AppServer.getParam("SendMSISDNPrefix"));
			if(mobileNumber.startsWith(msisdnPrefix)){
				return sendMSISDNPrefix +
					mobileNumber.substring(msisdnPrefix.length());
			}
			return mobileNumber;
		}

		public void run()
		{
			while(bRunning){
				try{
					SubmitSM submitRequest = null;
					SubmitMessage submitMessage = detactMsg(getThreadManagerEx());
					if(submitMessage != null){
						submitRequest = submitMessage.getSubmitSM();
						submitRequest.getSourceAddr().setNpi((byte)sourceAddress.getNpi());
						submitRequest.getSourceAddr().setTon((byte)sourceAddress.getTon());
						submitRequest.getDestAddr().setNpi((byte)destAddress.getNpi());
						submitRequest.getDestAddr().setTon((byte)destAddress.getTon());
						String strCalledNumber =submitRequest.getDestAddr().getAddress();
						submitRequest.getDestAddr().setAddress(strCalledNumber);
						int iSeqNum = getSequenceNumber();
						submitRequest.setSequenceNumber(iSeqNum);
						//additional paprameters
						logMonitor("Submit data " + submitRequest.debugString());
						if(asynchronous){
							session.submit(submitRequest);
						}
						else{
							SubmitSMResp submitResponse = null;
							submitResponse = session.submit(submitRequest);
							if(submitResponse != null){
								logMonitor(submitResponse.debugString());
							}
							else{
								logMonitor("No response from smsc.");
								hasError = true;
							}
						}
						Thread.sleep(20);
					}
					else{
						Thread.sleep(20);
					}
				}
				catch(Exception e){
					hasError = true;
					bRunning = false;
					logMonitor("Error occurs when getting message: " + e);
				}
			}
		}
	}

	/**
	 * The first method called to start communication betwen an ESME and a SMSC. A
	 * new instance of <code>TCPIPConnection</code> is created and the IP address
	 * and port obtained from user are passed to this instance. New
	 * <code>Session</code> is created which uses the created
	 * <code>TCPIPConnection</code>. All the parameters required for a bind are set
	 * to the <code>BindRequest</code> and this request is passed to the
	 * <code>Session</code>'s <code>bind</code> method. If the call is successful,
	 * the application should be bound to the SMSC. See "SMPP Protocol
	 * Specification 3.4, 4.1 BIND Operation."
	 *
	 * @see BindRequest
	 * @see BindResponse
	 * @see TCPIPConnection
	 * @see Session#bind(BindRequest)
	 * @see Session#bind(BindRequest,ServerPDUEventListener)
	 * @throws Exception
	 */
	public void bind() throws Exception
	{
		logMonitor("Binding with bind-mode: " + bindMode + " and sync-mode: " + syncMode);
		try{
			if(bound){
				this.unbind();
			}

			BindRequest request = null;
			BindResponse response = null;

			if(bindMode.compareToIgnoreCase("t") == 0){
				request = new BindTransmitter();
			}
			else if(bindMode.compareToIgnoreCase("r") == 0){
				request = new BindReceiver();
			}
			else if(bindMode.compareToIgnoreCase("tr") == 0){
				request = new BindTransciever();
			}
			else{
				throw new Exception("Invalid bind mode, expected t, r or tr, got " +
									bindMode + ". Operation canceled.");
			}

			TCPIPConnection connection = new TCPIPConnection(ipAddress,port);
			connection.setReceiveTimeout(10 * 1000);
			session = new Session(connection);
			// set values
			request.setSystemId(systemId);
			request.setPassword(password);
			request.setSystemType(mstrSystemType);
			request.setInterfaceVersion((byte)0x34);
			request.setAddressRange(addressRange);

			// send the request
			logMonitor("Bind request " + getBindRequestInfor(request));
			if(asynchronous){
				pduListener = new SMPPTestPDUEventListener(session);
				response = session.bind(request,pduListener);
			}
			else{
				response = session.bind(request);
			}
			if(response != null){
				logMonitor("Bind response " + response.debugString());
				if(response.getCommandStatus() == Data.ESME_ROK){
					bound = true;
				}
				else{
					logMonitor("Bind failure with error code:" + response.getCommandStatus());
				}
			}
			else{
				logMonitor("No response from smsc,please check the parammeters.");
			}
		}
		catch(Exception e){
			logMonitor("Bind operation failed. ");
			throw e;
		}
	}

	private String getBindRequestInfor(BindRequest request)
	{
		String dbgs = "(bindreq: ";
		dbgs += request.getSystemId();
		dbgs += " ";
		dbgs += request.getSystemType();
		dbgs += " ";
		dbgs += Integer.toString(request.getInterfaceVersion());
		dbgs += " ";
		dbgs += request.getAddressRange().debugString();
		dbgs += ") ";
		return dbgs;
	}

	/**
	 * Ubinds (logs out) from the SMSC and closes the connection.
	 *
	 * See "SMPP Protocol Specification 3.4, 4.2 UNBIND Operation."
	 * @see Session#unbind()
	 * @see Unbind
	 * @see UnbindResp
	 */
	private void unbind()
	{
		logMonitor("Going to unbind");
		if(session == null){
			return;
		}
		try{
			if(!bound){
				logMonitor("Not bound, cannot unbind.");
				return;
			}
			// send the request
			UnbindResp response = session.unbind();
			logMonitor("Unbind response " + response == null ? "is null" : response.debugString());
			bound = false;
		}
		catch(Exception e){
			logMonitor("Unbind operation failed. " + e);
		}
		finally{
			bound = false;
		}
	}

	/**
	 * Creates a new instance of <code>EnquireSM</code> class. This PDU is used to
	 * check that application level of the other party is alive. It can be sent
	 * both by SMSC and ESME. See "SMPP Protocol Specification 3.4, 4.11
	 * ENQUIRE_LINK Operation."
	 *
	 * @see Session#enquireLink(EnquireLink)
	 * @see EnquireLink
	 * @see EnquireLinkResp
	 * @throws Exception
	 */
	private synchronized void enquireLink() throws Exception
	{
		EnquireLink request = new EnquireLink();
		EnquireLinkResp response;
		logMonitor("Enquire Link request " + request.debugString());
		if(asynchronous){
			session.enquireLink(request);
		}
		else{
			response = session.enquireLink(request);
			logMonitor("Enquire Link response " + response == null ? "No response." : response.debugString());
			if(response == null || response.getCommandStatus() != Data.ESME_ROK){
				hasError = true;

			}
			else{
				updateLastReceiveEnquireLinkResp();
			}
		}
		updateTimeNextSendEnquireLink();
	}

	/**
	 * Receives one PDU of any type from SMSC and prints it on the screen.
	 *
	 * @see Session#receive()
	 * @see Response
	 * @see ServerPDUEvent
	 * @throws Exception
	 * @return boolean
	 */
	private boolean receive() throws Exception
	{
		PDU pdu = null;
		if(asynchronous){
			ServerPDUEvent pduEvent = pduListener.getRequestEvent(receiveTimeout);
			if(pduEvent != null){
				pdu = pduEvent.getPDU();
			}
		}
		else{
			pdu = session.receive(receiveTimeout);
		}
		if(pdu == null){
			return false;
		}
		if(pdu.isRequest()){
			//sending response to SMSC
			Response response = ((Request)pdu).getResponse();
			logMonitor("ACK to SMSC: " + response.debugString());
			//send default response
			synchronized(session){
				session.respond(response);
			}
			//enqueue data
			if(pdu instanceof DeliverSM){
				DeliverSM deliverSM = (DeliverSM)pdu;
				//Check Address Range
				String vstrSourceAddress = deliverSM.getSourceAddr().getAddress();
				if(!vstrSourceAddress.equals("")){
					logMonitor("Received data: " + deliverSM.debugString());
					ReceiveMessage receiveMessage = new ReceiveMessage(pdu);
					receiveMessage.setAttribute("DataType","DeliverSM");
					getThreadManagerEx().queueReceiver.attach(receiveMessage);
				}
				else{
					logMonitor("Received unknown data: " + deliverSM.debugString());
				}
			}
			else{
				logMonitor("Received unknown data: " + pdu.debugString());
			}
		}
		else if(pdu.isResponse()){
			logMonitor(pdu.debugString());
			if(pdu instanceof EnquireLinkResp){
				updateLastReceiveEnquireLinkResp();
			}
		}
		return true;
	}

	/**
	 * Implements simple PDU listener which handles PDUs received from SMSC.
	 * It puts the received requests into a queue and discards all received
	 * responses. Requests then can be fetched (should be) from the queue by
	 * calling to the method <code>getRequestEvent</code>.
	 * @see Queue
	 * @see ServerPDUEvent
	 * @see ServerPDUEventListener
	 * @see SmppObject
	 */
	private class SMPPTestPDUEventListener extends SmppObject implements
		ServerPDUEventListener
	{
		Session session;
		Queue requestEvents = new Queue();

		public SMPPTestPDUEventListener(Session session)
		{
			this.session = session;
		}

		public void handleEvent(ServerPDUEvent event)
		{
			PDU pdu = event.getPDU();
			if(pdu.isRequest() || pdu.isResponse()){
				synchronized(requestEvents){
					requestEvents.enqueue(event);
					requestEvents.notify();
				}
			}
			else{
				logMonitor("pdu of unknown class (not request nor " +
						   "response) received, discarding " +
						   pdu.debugString());
			}
		}

		/**
		 * Returns received pdu from the queue. If the queue is empty, the method
		 * blocks for the specified timeout.
		 *
		 * @param timeout long
		 * @return ServerPDUEvent
		 */
		public ServerPDUEvent getRequestEvent(long timeout)
		{
			ServerPDUEvent pduEvent = null;
			synchronized(requestEvents){
				if(requestEvents.isEmpty()){
					try{
						requestEvents.wait(timeout);
					}
					catch(InterruptedException e){
						// ignoring, actually this is what we're waiting for
					}
				}
				if(!requestEvents.isEmpty()){
					try{
						pduEvent = (ServerPDUEvent)requestEvents.dequeue();
					}
					catch(Exception ex){
					}
				}
			}
			return pduEvent;
		}
	}

	@SuppressWarnings("unchecked")
	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();
		////////////////////////////////////////////////////////
		vtReturn.addElement(createParameterDefinition("NumOfDispatcher","",ParameterType.PARAM_TEXTBOX_MASK,"99999",""));
		vtReturn.addElement(createParameterDefinition("ip-address","",ParameterType.PARAM_TEXTBOX_MAX,"15",""));
		vtReturn.addElement(createParameterDefinition("port","",ParameterType.PARAM_TEXTBOX_MASK,"99999",""));
		vtReturn.addElement(createParameterDefinition("system-id","",ParameterType.PARAM_TEXTBOX_MAX,"30",""));
		vtReturn.addElement(createParameterDefinition("password","",ParameterType.PARAM_PASSWORD,"30",""));
		vtReturn.addElement(createParameterDefinition("SystemType","",ParameterType.PARAM_TEXTBOX_MAX,"30",""));
		vtReturn.addElement(createParameterDefinition("addr-ton","",ParameterType.PARAM_TEXTBOX_MASK,"9",""));
		vtReturn.addElement(createParameterDefinition("addr-npi","",ParameterType.PARAM_TEXTBOX_MASK,"9",""));
		vtReturn.addElement(createParameterDefinition("address-range","",ParameterType.PARAM_TEXTBOX_MAX,"15",""));
		vtReturn.addElement(createParameterDefinition("source-ton","",ParameterType.PARAM_TEXTBOX_MASK,"9",""));
		vtReturn.addElement(createParameterDefinition("source-npi","",ParameterType.PARAM_TEXTBOX_MASK,"9",""));
		vtReturn.addElement(createParameterDefinition("source-address","",ParameterType.PARAM_TEXTBOX_MAX,"15",""));
		vtReturn.addElement(createParameterDefinition("destination-ton","",ParameterType.PARAM_TEXTBOX_MASK,"9",""));
		vtReturn.addElement(createParameterDefinition("destination-npi","",ParameterType.PARAM_TEXTBOX_MASK,"9",""));
		vtReturn.addElement(createParameterDefinition("bind-mode","",ParameterType.PARAM_TEXTBOX_MAX,"2",""));
		vtReturn.addElement(createParameterDefinition("receive-timeout","",ParameterType.PARAM_TEXTBOX_MASK,"999",""));
		vtReturn.addElement(createParameterDefinition("sync-mode","",ParameterType.PARAM_TEXTBOX_MAX,"1",""));
		vtReturn.addElement(createParameterDefinition("enquire-interval","",ParameterType.PARAM_TEXTBOX_MASK,"999",""));
		vtReturn.addElement(createParameterDefinition("ignore-message","",ParameterType.PARAM_TEXTBOX_MAX,"30",""));

		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}

	public void fillParameter() throws AppException
	{
		byte ton;
		byte npi;
		String addr;
		int rcvTimeout;
		numOfDispatcher = loadUnsignedInteger("NumOfDispatcher");
		ipAddress = loadMandatory("ip-address");
		port = loadUnsignedInteger("port");

		systemId = loadMandatory("system-id");
		password = loadMandatory("password");
		mstrSystemType = loadMandatory("SystemType");
		ton = (byte)loadInteger("addr-ton");
		npi = (byte)loadInteger("addr-npi");
		addr = loadMandatory("address-range");
		enquireInterval = loadInteger("enquire-interval");
		addressRange.setTon(ton);
		addressRange.setNpi(npi);
		try{
			addressRange.setAddressRange(addr);
		}
		catch(WrongLengthOfStringException e){
			throw new AppException(
				"The length of address-range parameter is wrong.");
		}

		ton = (byte)loadInteger("source-ton");
		npi = (byte)loadInteger("source-npi");
		addr = loadMandatory("source-address");
		setAddressParameter("source-address",sourceAddress,ton,npi,addr);

		ton = (byte)loadInteger("destination-ton");
		npi = (byte)loadInteger("destination-npi");
		setAddressParameter("source-address",destAddress,ton,npi,addr);

		bindMode = loadMandatory("bind-mode");
		if(!bindMode.equalsIgnoreCase("t") &&
		   !bindMode.equalsIgnoreCase("r") &&
		   !bindMode.equalsIgnoreCase("tr")){
			throw new AppException("The bind-mode parameter is wrong. ");
		}

		// receive timeout in the cfg file is in seconds, we need milliseconds
		// also conversion from -1 which indicates infinite blocking
		// in the cfg file to Data.RECEIVE_BLOCKING which indicates infinite
		// blocking in the library is needed.
		if(receiveTimeout == Data.RECEIVE_BLOCKING){
			rcvTimeout = -1;
		}
		else{
			rcvTimeout = ((int)receiveTimeout) / 1000;
		}
		rcvTimeout = loadInteger("receive-timeout");
		if(rcvTimeout == -1){
			receiveTimeout = Data.RECEIVE_BLOCKING;
		}
		else{
			receiveTimeout = rcvTimeout * 1000;
		}

		syncMode = loadMandatory("sync-mode");
		if(syncMode.equalsIgnoreCase("s")){
			asynchronous = false;
		}
		else if(syncMode.equalsIgnoreCase("a")){
			asynchronous = true;
		}
		else{
			asynchronous = false;
		}
		ignoreMessage = StringUtil.nvl(getParameter("ignore-message"),"");
		paramIgnore = ignoreMessage.split(",");
		super.fillParameter();
	}

	/**
	 * Sets attributes of <code>Address</code> to the provided values.
	 *
	 * @param descr String
	 * @param address Address
	 * @param ton byte
	 * @param npi byte
	 * @param addr String
	 */
	private void setAddressParameter(String descr,Address address,byte ton,
									 byte npi,String addr)
	{
		address.setTon(ton);
		address.setNpi(npi);
		try{
			address.setAddress(addr);
		}
		catch(WrongLengthOfStringException e){
			logMonitor("The length of " + descr + " parameter is wrong.");
		}
	}

	public static synchronized int getSequenceNumber()
	{
		if(mintSequenceNumber >= Integer.MAX_VALUE){
			mintSequenceNumber = 0;
		}
		return mintSequenceNumber++;
	}

	public void initTCPIPMonitor()
	{

		debug = new FileDebug(dbgDir,"sim.dbg");
		event = new FileEvent(dbgDir,"sim.evt");
		SmppObject.setDebug(debug);
		SmppObject.setEvent(event);
		debug.activate();
		event.activate();
		debug.deactivate(DSIMD2);

	}
}
