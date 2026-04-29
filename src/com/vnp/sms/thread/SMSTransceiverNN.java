package com.vnp.sms.thread;

import com.fss.thread.ParameterType;
import com.fss.util.AppException;
import com.fss.util.StringUtil;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.Session;
import com.logica.smpp.SmppObject;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.debug.Debug;
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
import com.logica.smpp.pdu.UnbindResp;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.vnp.util.AppServer;
import com.vnp.util.ReceiveMessage;
import com.vnp.util.SubmitMessage;
import java.util.Vector;

public class SMSTransceiverNN
  extends ManageableThreadEx
{
  String syncMode;
  Session session;
  boolean bound;
  String ipAddress;
  int port;
  String systemId;
  String password;
  String bindMode;
  boolean asynchronous;
  SMPPTestPDUEventListener pduListener;
  AddressRange addressRange;
  Address sourceAddress;
  Address destAddress;
  byte smDefaultMsgId;
  long receiveTimeout;
  int enquireInterval;
  long nextEnquireLink;
  long lastReceiveEnquireLinkResp;
  boolean hasError;
  String ignoreMessage;
  String[] paramIgnore;
  String mstrSystemType;
  int numOfDispatcher;
  
  public SMSTransceiverNN()
  {
    this.session = null;
    
    this.bound = false;
    
    this.ipAddress = null;
    
    this.port = 0;
    
    this.systemId = null;
    
    this.password = null;
    
    this.bindMode = "t";
    
    this.asynchronous = false;
    
    this.pduListener = null;
    
    this.addressRange = new AddressRange();
    
    this.sourceAddress = new Address();
    this.destAddress = new Address();
    this.smDefaultMsgId = 0;
    
    this.receiveTimeout = -1L;
    this.enquireInterval = 60;
    this.nextEnquireLink = 0L;
    this.lastReceiveEnquireLinkResp = 0L;
    this.hasError = false;
    this.ignoreMessage = "";
    
    this.mstrSystemType = "";
    this.numOfDispatcher = 1;
  }
  
  public static int mintSequenceNumber = 0;
  static final String dbgDir = "./";
  public static final int DSIM = 16;
  public static final int DSIMD = 17;
  public static final int DSIMD2 = 18;
  static Debug debug = null;
  static Event event = null;
  
  public void processSession()
    throws Exception
  {
    initTCPIPMonitor();
    SMSReceiver[] smsReceiver = new SMSReceiver[this.numOfDispatcher];
    SMSTransmitter[] smsTransmitter = new SMSTransmitter[this.numOfDispatcher];
    try
    {
      this.hasError = false;
      logMonitor("Start binding to SMSC server....");
      bind();
      if (!this.bound)
      {
        int i;
        return;
      }
      for (int i = 0; i < this.numOfDispatcher; i++)
      {
        logMonitor("Start receiver process " + i);
        smsReceiver[i] = new SMSReceiver(this);
        smsReceiver[i].start();
        logMonitor("Start transmitter process " + i);
        smsTransmitter[i] = new SMSTransmitter(this);
        smsTransmitter[i].start();
      }
      this.lastReceiveEnquireLinkResp = System.currentTimeMillis();
      while ((this.miThreadCommand != 2) && (!this.hasError))
      {
        if (shouldSendEnquireLink()) {
          enquireLink();
        }
        if (shouldReConnect())
        {
          logMonitor("Wait message last more than usual, need to reconnect...");
          this.hasError = true;
        }
        fillLogFile();
        Thread.sleep(100L);
      }
    }
    finally
    {
      
      for (int i = 0; i < this.numOfDispatcher; i++)
      {
        if (smsReceiver[i] != null) {
          smsReceiver[i].stopNow();
        }
        if (smsTransmitter[i] != null) {
          smsTransmitter[i].stopNow();
        }
        Thread.sleep(100L);
      }
      unbind();
    }
  }
  
  public void updateLastReceiveEnquireLinkResp()
  {
    this.lastReceiveEnquireLinkResp = System.currentTimeMillis();
  }
  
  public boolean shouldReConnect()
  {
    return this.nextEnquireLink - 4 * this.enquireInterval * 1000 > this.lastReceiveEnquireLinkResp;
  }
  
  public class SMSReceiver
    extends Thread
  {
    boolean bRunning = true;
    
    public void stopNow()
    {
      this.bRunning = false;
    }
    
    public SMSReceiver(SMSTransceiverNN smsTransceiverNN) {}
    
    public boolean isRunning()
    {
      return this.bRunning;
    }
    
    public void run()
    {
      while (this.bRunning) {
        try
        {
          SMSTransceiverNN.this.receive();
        }
        catch (Exception e)
        {
          SMSTransceiverNN.this.hasError = true;
          this.bRunning = false;
          SMSTransceiverNN.this.logMonitor("Error occurs when getting message: " + e);
        }
      }
    }
  }
  
  public boolean shouldSendEnquireLink()
  {
    return System.currentTimeMillis() >= this.nextEnquireLink;
  }
  
  public void updateTimeNextSendEnquireLink()
  {
    this.nextEnquireLink = (System.currentTimeMillis() + this.enquireInterval * 1000);
  }
  
  public class SMSTransmitter
    extends Thread
  {
    boolean bRunning = true;
    
    public void stopNow()
    {
      this.bRunning = false;
    }
    
    public SMSTransmitter(SMSTransceiverNN smsTransceiverNN) {}
    
    public boolean isRunning()
    {
      return this.bRunning;
    }
    
    public synchronized SubmitMessage detactMsg(ThreadManagerEx mgr)
    {
      SubmitMessage submitMessage = null;
      synchronized (mgr.queueSubmit)
      {
        try
        {
          submitMessage = (SubmitMessage)mgr.queueSubmit.dequeue();
        }
        catch (Exception ex) {}
      }
      return submitMessage;
    }
    
    public String formatSendingMobileNo(String mobileNumber)
    {
      String msisdnPrefix = AppServer.getParam("MSISDNPrefix");
      int sendMSISDNPrefix = Integer.parseInt(AppServer.getParam("SendMSISDNPrefix"));
      if (mobileNumber.startsWith(msisdnPrefix)) {
        return sendMSISDNPrefix + mobileNumber.substring(msisdnPrefix.length());
      }
      return mobileNumber;
    }
    
    public void run()
    {
      while (this.bRunning) {
        try
        {
          SubmitSM submitRequest = null;
          SubmitMessage submitMessage = detactMsg(SMSTransceiverNN.this.getThreadManagerEx());
          if (submitMessage != null)
          {
            submitRequest = submitMessage.getSubmitSM();
            submitRequest.getSourceAddr().setNpi(SMSTransceiverNN.this.sourceAddress.getNpi());
            submitRequest.getSourceAddr().setTon(SMSTransceiverNN.this.sourceAddress.getTon());
            submitRequest.getDestAddr().setNpi(SMSTransceiverNN.this.destAddress.getNpi());
            submitRequest.getDestAddr().setTon(SMSTransceiverNN.this.destAddress.getTon());
            String strCalledNumber = submitRequest.getDestAddr().getAddress();
            submitRequest.getDestAddr().setAddress(strCalledNumber);
            int iSeqNum = SMSTransceiver.getSequenceNumber();
            submitRequest.setSequenceNumber(iSeqNum);
            
            logMonitor("Submit data " + submitRequest.debugString());
            if (SMSTransceiverNN.this.asynchronous)
            {
              SMSTransceiverNN.this.session.submit(submitRequest);
            }
            else
            {
              SubmitSMResp submitResponse = null;
              submitResponse = SMSTransceiverNN.this.session.submit(submitRequest);
              if (submitResponse != null)
              {
                SMSTransceiverNN.this.logMonitor(submitResponse.debugString());
              }
              else
              {
                SMSTransceiverNN.this.logMonitor("No response from smsc.");
                SMSTransceiverNN.this.hasError = true;
              }
            }
            Thread.sleep(20L);
          }
          else
          {
            Thread.sleep(20L);
          }
        }
        catch (Exception e)
        {
          SMSTransceiverNN.this.hasError = true;
          this.bRunning = false;
          SMSTransceiverNN.this.logMonitor("Error occurs when getting message: " + e);
        }
      }
    }
  }
  
  public void bind()
    throws Exception
  {
    logMonitor("Binding with bind-mode: " + this.bindMode + " and sync-mode: " + this.syncMode);
    try
    {
      if (this.bound) {
        unbind();
      }
      BindRequest request = null;
      BindResponse response = null;
      if (this.bindMode.compareToIgnoreCase("t") == 0) {
        request = new BindTransmitter();
      } else if (this.bindMode.compareToIgnoreCase("r") == 0) {
        request = new BindReceiver();
      } else if (this.bindMode.compareToIgnoreCase("tr") == 0) {
        request = new BindTransciever();
      } else {
        throw new Exception("Invalid bind mode, expected t, r or tr, got " + this.bindMode + ". Operation canceled.");
      }
      TCPIPConnection connection = new TCPIPConnection(this.ipAddress, this.port);
      connection.setReceiveTimeout(10000L);
      this.session = new Session(connection);
      
      request.setSystemId(this.systemId);
      request.setPassword(this.password);
      request.setSystemType(this.mstrSystemType);
      request.setInterfaceVersion((byte)52);
      request.setAddressRange(this.addressRange);
      
      logMonitor("Bind request " + getBindRequestInfor(request));
      if (this.asynchronous)
      {
        this.pduListener = new SMPPTestPDUEventListener(this.session);
        response = this.session.bind(request, this.pduListener);
      }
      else
      {
        response = this.session.bind(request);
      }
      if (response != null)
      {
        logMonitor("Bind response " + response.debugString());
        if (response.getCommandStatus() == 0) {
          this.bound = true;
        } else {
          logMonitor("Bind failure with error code:" + response.getCommandStatus());
        }
      }
      else
      {
        logMonitor("No response from smsc,please check the parammeters.");
      }
    }
    catch (Exception e)
    {
      logMonitor("Bind operation failed. ");
      throw e;
    }
  }
  
  private String getBindRequestInfor(BindRequest request)
  {
    String dbgs = "(bindreq: ";
    dbgs = dbgs + request.getSystemId();
    dbgs = dbgs + " ";
    dbgs = dbgs + request.getSystemType();
    dbgs = dbgs + " ";
    dbgs = dbgs + Integer.toString(request.getInterfaceVersion());
    dbgs = dbgs + " ";
    dbgs = dbgs + request.getAddressRange().debugString();
    dbgs = dbgs + ") ";
    return dbgs;
  }
  
  private void unbind()
  {
    logMonitor("Going to unbind");
    if (this.session == null) {
      return;
    }
    try
    {
      if (!this.bound)
      {
        logMonitor("Not bound, cannot unbind.");
      }
      else
      {
        UnbindResp response = this.session.unbind();
        logMonitor("Unbind response " + response == null ? "is null" : response.debugString());
        this.bound = false;
      }
    }
    catch (Exception e)
    {
      logMonitor("Unbind operation failed. " + e);
    }
    finally
    {
      this.bound = false;
    }
  }
  
  private synchronized void enquireLink()
    throws Exception
  {
    EnquireLink request = new EnquireLink();
    
    logMonitor("Enquire Link request " + request.debugString());
    if (this.asynchronous)
    {
      this.session.enquireLink(request);
    }
    else
    {
      EnquireLinkResp response = this.session.enquireLink(request);
      logMonitor("Enquire Link response " + response == null ? "No response." : response.debugString());
      if ((response == null) || (response.getCommandStatus() != 0)) {
        this.hasError = true;
      } else {
        updateLastReceiveEnquireLinkResp();
      }
    }
    updateTimeNextSendEnquireLink();
  }
  
  private boolean receive()
    throws Exception
  {
    PDU pdu = null;
    if (this.asynchronous)
    {
      ServerPDUEvent pduEvent = this.pduListener.getRequestEvent(this.receiveTimeout);
      if (pduEvent != null) {
        pdu = pduEvent.getPDU();
      }
    }
    else
    {
      pdu = this.session.receive(this.receiveTimeout);
    }
    if (pdu == null) {
      return false;
    }
    if (pdu.isRequest())
    {
      Response response = ((Request)pdu).getResponse();
      logMonitor("ACK to SMSC: " + response.debugString());
      synchronized (this.session)
      {
        this.session.respond(response);
      }
      
      //logMonitor("NOI DUNG PDU "+pdu.debugString());
                  
      if ((pdu instanceof DeliverSM))
      {
        DeliverSM deliverSM = (DeliverSM)pdu;
        
        String vstrSourceAddress = deliverSM.getSourceAddr().getAddress();
        
        logMonitor("NHAN TIN NHAN TU SO "+vstrSourceAddress);
        
        if (!vstrSourceAddress.equals(""))
        {
          logMonitor("Received data: " + deliverSM.debugString());
          ReceiveMessage receiveMessage = new ReceiveMessage(pdu);
          receiveMessage.setAttribute("DataType", "DeliverSM");
          getThreadManagerEx().queueReceiver.attach(receiveMessage);
        }
        else
        {
          logMonitor("Received unknown data1: " + deliverSM.debugString());
        }
      }
      else
      {
        logMonitor("Received unknown data2: " + pdu.debugString());
      }
    }
    else if (pdu.isResponse())
    {
      logMonitor(pdu.debugString());
      if ((pdu instanceof EnquireLinkResp)) {
        updateLastReceiveEnquireLinkResp();
      }
    }
    return true;
  }
  
  private class SMPPTestPDUEventListener
    extends SmppObject
    implements ServerPDUEventListener
  {
    Session session;
    com.logica.smpp.util.Queue requestEvents = new com.logica.smpp.util.Queue();
    
    public SMPPTestPDUEventListener(Session session)
    {
      this.session = session;
    }
    
    public void handleEvent(ServerPDUEvent event)
    {
      PDU pdu = event.getPDU();
      if ((pdu.isRequest()) || (pdu.isResponse())) {
        synchronized (this.requestEvents)
        {
          this.requestEvents.enqueue(event);
          this.requestEvents.notify();
        }
      } else {
        SMSTransceiverNN.this.logMonitor("pdu of unknown class (not request nor response) received, discarding " + pdu.debugString());
      }
    }
    
    public ServerPDUEvent getRequestEvent(long timeout)
    {
      ServerPDUEvent pduEvent = null;
      synchronized (this.requestEvents)
      {
        if (this.requestEvents.isEmpty()) {
          try
          {
            this.requestEvents.wait(timeout);
          }
          catch (InterruptedException e) {}
        }
        if (!this.requestEvents.isEmpty()) {
          try
          {
            pduEvent = (ServerPDUEvent)this.requestEvents.dequeue();
          }
          catch (Exception ex) {}
        }
      }
      return pduEvent;
    }
  }
  
  public Vector getParameterDefinition()
  {
    Vector vtReturn = new Vector();
    
    vtReturn.addElement(createParameterDefinition("NumOfDispatcher", "", ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
    vtReturn.addElement(createParameterDefinition("ip-address", "", ParameterType.PARAM_TEXTBOX_MAX, "15", ""));
    vtReturn.addElement(createParameterDefinition("port", "", ParameterType.PARAM_TEXTBOX_MASK, "99999", ""));
    vtReturn.addElement(createParameterDefinition("system-id", "", ParameterType.PARAM_TEXTBOX_MAX, "30", ""));
    vtReturn.addElement(createParameterDefinition("password", "", ParameterType.PARAM_PASSWORD, "30", ""));
    vtReturn.addElement(createParameterDefinition("SystemType", "", ParameterType.PARAM_TEXTBOX_MAX, "30", ""));
    vtReturn.addElement(createParameterDefinition("addr-ton", "", ParameterType.PARAM_TEXTBOX_MASK, "9", ""));
    vtReturn.addElement(createParameterDefinition("addr-npi", "", ParameterType.PARAM_TEXTBOX_MASK, "9", ""));
    vtReturn.addElement(createParameterDefinition("address-range", "", ParameterType.PARAM_TEXTBOX_MAX, "15", ""));
    vtReturn.addElement(createParameterDefinition("source-ton", "", ParameterType.PARAM_TEXTBOX_MASK, "9", ""));
    vtReturn.addElement(createParameterDefinition("source-npi", "", ParameterType.PARAM_TEXTBOX_MASK, "9", ""));
    vtReturn.addElement(createParameterDefinition("source-address", "", ParameterType.PARAM_TEXTBOX_MAX, "15", ""));
    vtReturn.addElement(createParameterDefinition("destination-ton", "", ParameterType.PARAM_TEXTBOX_MASK, "9", ""));
    vtReturn.addElement(createParameterDefinition("destination-npi", "", ParameterType.PARAM_TEXTBOX_MASK, "9", ""));
    vtReturn.addElement(createParameterDefinition("bind-mode", "", ParameterType.PARAM_TEXTBOX_MAX, "2", ""));
    vtReturn.addElement(createParameterDefinition("receive-timeout", "", ParameterType.PARAM_TEXTBOX_MASK, "999", ""));
    vtReturn.addElement(createParameterDefinition("sync-mode", "", ParameterType.PARAM_TEXTBOX_MAX, "1", ""));
    vtReturn.addElement(createParameterDefinition("enquire-interval", "", ParameterType.PARAM_TEXTBOX_MASK, "999", ""));
    vtReturn.addElement(createParameterDefinition("ignore-message", "", ParameterType.PARAM_TEXTBOX_MAX, "30", ""));
    
    vtReturn.addAll(super.getParameterDefinition());
    return vtReturn;
  }
  
  public void fillParameter()
    throws AppException
  {
    this.numOfDispatcher = loadUnsignedInteger("NumOfDispatcher");
    this.ipAddress = loadMandatory("ip-address");
    this.port = loadUnsignedInteger("port");
    
    this.systemId = loadMandatory("system-id");
    this.password = loadMandatory("password");
    this.mstrSystemType = loadMandatory("SystemType");
    byte ton = (byte)loadInteger("addr-ton");
    byte npi = (byte)loadInteger("addr-npi");
    String addr = loadMandatory("address-range");
    this.enquireInterval = loadInteger("enquire-interval");
    this.addressRange.setTon(ton);
    this.addressRange.setNpi(npi);
    try
    {
      this.addressRange.setAddressRange(addr);
    }
    catch (WrongLengthOfStringException e)
    {
      throw new AppException("The length of address-range parameter is wrong.");
    }
    ton = (byte)loadInteger("source-ton");
    npi = (byte)loadInteger("source-npi");
    addr = loadMandatory("source-address");
    setAddressParameter("source-address", this.sourceAddress, ton, npi, addr);
    
    ton = (byte)loadInteger("destination-ton");
    npi = (byte)loadInteger("destination-npi");
    setAddressParameter("source-address", this.destAddress, ton, npi, addr);
    
    this.bindMode = loadMandatory("bind-mode");
    if ((!this.bindMode.equalsIgnoreCase("t")) && (!this.bindMode.equalsIgnoreCase("r")) && (!this.bindMode.equalsIgnoreCase("tr"))) {
      throw new AppException("The bind-mode parameter is wrong. ");
    }
    int rcvTimeout;
    if (this.receiveTimeout == -1) {
      rcvTimeout = -1;
    } else {
      rcvTimeout = (int)this.receiveTimeout / 1000;
    }
    
    rcvTimeout = loadInteger("receive-timeout");
    
    if (rcvTimeout == -1) {
      this.receiveTimeout = -1;
    } else {
      this.receiveTimeout = (rcvTimeout * 1000);
    }
    this.syncMode = loadMandatory("sync-mode");
    if (this.syncMode.equalsIgnoreCase("s")) {
      this.asynchronous = false;
    } else if (this.syncMode.equalsIgnoreCase("a")) {
      this.asynchronous = true;
    } else {
      this.asynchronous = false;
    }
    this.ignoreMessage = StringUtil.nvl(getParameter("ignore-message"), "");
    this.paramIgnore = this.ignoreMessage.split(",");
    super.fillParameter();
  }
  
  private void setAddressParameter(String descr, Address address, byte ton, byte npi, String addr)
  {
    address.setTon(ton);
    address.setNpi(npi);
    try
    {
      address.setAddress(addr);
    }
    catch (WrongLengthOfStringException e)
    {
      logMonitor("The length of " + descr + " parameter is wrong.");
    }
  }
  
  public static synchronized int getSequenceNumber()
  {
    if (mintSequenceNumber >= Integer.MAX_VALUE) {
      mintSequenceNumber = 0;
    }
    return mintSequenceNumber++;
  }
  
  public void initTCPIPMonitor()
  {
    debug = new FileDebug("./", "sim.dbg");
    event = new FileEvent("./", "sim.evt");
    SmppObject.setDebug(debug);
    SmppObject.setEvent(event);
    debug.activate();
    event.activate();
    debug.deactivate(18);
  }
}
