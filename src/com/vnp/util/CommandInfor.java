package com.vnp.util;

import java.util.*;

import com.fss.util.*;
import com.fss.queue.Message;
import java.io.InputStream;
import java.io.OutputStream;

public class CommandInfor implements Message
{
	private long transID;
	private boolean sended = false;
	private boolean success = false;
	private String errorMessage;
	private boolean processed = false;
	private int timeout;
	private String isdn;

	private int retryCount = 0;
	private String inRetnCode = "0";
	private boolean isCancel = false;
	private Date transDate;
	private String listBalance;
	private String errorCode;
	private String state;
	private String active;

	private String accValue;

	private String status;
	private String message;
	private String mtrAddAmount = "";
	private String cos;
	private String serviceType;
	Hashtable hashtable = new Hashtable();

	public CommandInfor()
	{
		timeout = 30;

	}

	public String getServiceType()
	{
		return serviceType;
	}

	public void setServiceType(String serviceType)
	{
		this.serviceType = serviceType;
	}

	public long getTransID()
	{
		return transID;
	}

	public boolean isSended()
	{
		return sended;
	}

	public boolean isSuccess()
	{
		return success;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public String getAddAmount()
	{
		return this.mtrAddAmount;
	}

	public void setAddAmount(String strAddAmount)
	{
		this.mtrAddAmount = strAddAmount;
	}

	public String getCos()
	{
		return this.cos;
	}

	public void setCos(String cos)
	{
		this.cos = cos;
	}

	public boolean isProcessed()
	{
		return processed;
	}

	public int getTimeout()
	{
		return timeout;
	}

	public Date getTransDate()
	{
		return transDate;
	}

	public String getIsdn()
	{
		return isdn;
	}

	public int getRetryCount()
	{
		return retryCount;
	}

	public String getInRetnCode()
	{
		return com.fss.util.StringUtil.lpad(inRetnCode,3,'0');
	}

	public boolean isIsCancel()
	{
		return isCancel;
	}

	public void setTransID(long transID)
	{
		this.transID = transID;
	}

	public void setSended(boolean sended)
	{
		this.sended = sended;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public void setProcessed(boolean processed)
	{
		this.processed = processed;
	}

	public void setTransDate(Date transDate)
	{
		this.transDate = transDate;
	}

	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	public void setIsdn(String isdn)
	{
		this.isdn = isdn;
	}

	public void setRetryCount(int retryCount)
	{
		this.retryCount = retryCount;
	}

	public void setInRetnCode(String inRetnCode)
	{
		this.inRetnCode = inRetnCode;
	}

	public void setIsCancel(boolean isCancel)
	{
		this.isCancel = isCancel;
	}

	public boolean isTimeout()
	{
		if(System.currentTimeMillis() > transDate.getTime() + timeout * 1000){
			return true;
		}
		return false;
	}

	public void setBalance(String listBalance)
	{
		this.listBalance = listBalance;
	}

	public String getBalance()
	{
		return this.listBalance;
	}

	public void setErrorCode(String errorCode)
	{
		this.errorCode = errorCode;
	}

	public String getErrorCode()
	{
		return this.errorCode;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public String getState()
	{
		return this.state;
	}

	public void setActive(String active)
	{
		this.active = active;
	}

	public String getActive()
	{
		return this.active;
	}

	public void setAcc(String accValue)
	{
		this.accValue = accValue;
	}

	public String getAcc()
	{
		return this.accValue;
	}

	public String getStatus()
	{
		return this.status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getMessage()
	{
		return this.message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public void load(InputStream inputStream) throws Exception
	{
	}

	public void store(OutputStream outputStream) throws Exception
	{
	}

	public String getContent() throws Exception
	{
		return "";
	}

	public Object getAttribute(String string)
	{
		return hashtable.get(string);
	}

	public void setAttribute(String string,Object object)
	{
		hashtable.put(string,object);
	}

	public Hashtable getAttributes()
	{
		return null;
	}

	public void setAttributes(Hashtable hashtable)
	{
	}

	public void setAttributes(Map map)
	{
		this.hashtable = ((Hashtable)map);
	}
}
