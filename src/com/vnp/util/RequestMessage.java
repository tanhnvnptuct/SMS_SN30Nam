package com.vnp.util;


import com.fss.queue.Message;
import com.fss.util.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

public class RequestMessage
  implements Message
{
  private String mstrContent;
  private Date callDate;
  private String calledNumber;
  private String callingNumber;
  private String commandCode;
  private String content;
  private long messageID;
  private byte messageSource;
  private Hashtable mprtAttribute;
  private String serviceCode;
  private String commandID;
  private String extendedParam;
  private String CommandName;

  private String targetNumber;

  private String processStatus = "-1";
  private String messageReturn = "";

  public RequestMessage()
  {
	this.mprtAttribute = new Properties();
  }

  public Object getAttribute(String strKey)
  {
	return this.mprtAttribute.get(strKey);
  }

  public void store(OutputStream os)
	throws Exception
  {
  }

  public Hashtable getAttributes()
  {
	return this.mprtAttribute;
  }

  public Date getCallDate()
  {
	return this.callDate;
  }

  public void load(InputStream is)
	throws Exception
  {
	ByteArrayOutputStream os = new ByteArrayOutputStream();
	while (true) {
	  int iValue = is.read();
	  if (iValue < 0) {
		throw new Exception("No data to read");
	  }
	  if ((iValue == 13) || (iValue == 10)) {
		break;
	  }
	  os.write(iValue);
	}
	this.mstrContent = new String(os.toByteArray());
  }

  public String getCalledNumber()
  {
	return this.calledNumber;
  }

  public String getCallingNumber()
  {
	return this.callingNumber;
  }

  public String getCommandCode()
  {
	return this.commandCode;
  }

  public String getContent()
  {
	return this.content;
  }

  public long getMessageID()
  {
	return this.messageID;
  }

  public byte getMessageSource()
  {
	return this.messageSource;
  }


  public String getServiceCode()
  {
	return this.serviceCode;
  }

  public String getCommandName()
  {
	return this.CommandName;
  }

  public void setAttributes(Hashtable prt)
  {
	this.mprtAttribute = prt;
  }

  public void setAttribute(String strKey, Object objValue)
  {
	this.mprtAttribute.put(strKey, objValue);
  }

  public void setCommandName(String CommandName)
  {
	this.CommandName = CommandName;
  }

  public void setAttributes(Properties prt)
  {
	this.mprtAttribute = prt;
  }

  public void setCallDate(Date callDate)
  {
	this.callDate = callDate;
  }

  public void setCalledNumber(String calledNumber)
  {
	this.calledNumber = calledNumber;
  }

  public void setCallingNumber(String callingNumber)
  {
	this.callingNumber = callingNumber;
  }

  public void setCommandCode(String commandCode)
  {
	this.commandCode = commandCode;
  }

  public void setContent(String content)
  {
	this.content = content;
  }

  public void setMessageID(long messageID)
  {
	this.messageID = messageID;
  }

  public void setMessageSource(byte messageSource)
  {
	this.messageSource = messageSource;
  }



  public void setServiceCode(String serviceCode)
  {
	this.serviceCode = serviceCode;
  }

  public String getCommandID()
  {
	return this.commandID;
  }

  public void setCommandID(String commandID)
  {
	this.commandID = commandID;
  }

  public String getExtendedParam()
  {
	return this.extendedParam;
  }

  public void setExtendedParam(String extendedParam)
  {
	this.extendedParam = StringUtil.nvl(extendedParam, "");
  }


  public String getTargetNumber()
  {
	return this.targetNumber;
  }

  public String getProcessStatus()
  {
	return this.processStatus;
  }

  public String getMessageReturn()
  {
	return this.messageReturn;
  }



  public void setTargetNumber(String targetNumber)
  {
	this.targetNumber = targetNumber;
  }

  public void setProcessStatus(String processStatus)
  {
	this.processStatus = processStatus;
  }

  public void setMessageReturn(String messageReturn)
  {
	this.messageReturn = messageReturn;
  }

  public void setAttributes(Map map)
  {
	this.mprtAttribute = ((Hashtable)map);
  }

}
