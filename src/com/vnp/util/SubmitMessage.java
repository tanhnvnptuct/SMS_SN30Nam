package com.vnp.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.fss.queue.Message;
import com.logica.smpp.pdu.SubmitSM;

public class SubmitMessage implements Message
{
	private String content;
	private Hashtable mprtAttribute;
	private SubmitSM submitSM;
	private long requestID;
	private long responseID;
	private String extendedMessage;
	private boolean usingSequence = true;
	private String mstrContent;

	public SubmitMessage(SubmitSM submitSM) throws Exception
	{
		this(submitSM,true);
	}

	////////////////////////////////////////////////////////
	/**
	 * Load message from input stream
	 * @param is InputStream
	 * @throws Exception
	 */
	////////////////////////////////////////////////////////
	public void load(InputStream is) throws Exception
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		while(true)
		{
			int iValue = is.read();
			if(iValue < 0)
				throw new Exception("No data to read");
			if(iValue == '\r' || iValue == '\n')
				break;
			os.write(iValue);
		}
		mstrContent = new String(os.toByteArray());
	}
	////////////////////////////////////////////////////////
	/**
	 * Set message attribute
	 * @param prt Hashtable
	 */
	////////////////////////////////////////////////////////
	public void setAttributes(Hashtable prt)
	{
		mprtAttribute = prt;
	}
	////////////////////////////////////////////////////////
	/**
	 *
	 * @return Hashtable
	 */
	////////////////////////////////////////////////////////
	public Hashtable getAttributes()
	{
		return mprtAttribute;
	}
	////////////////////////////////////////////////////////
	/**
	 * Get message attribute
	 * @param strKey String
	 * @return String
	 */
	////////////////////////////////////////////////////////
	public Object getAttribute(String strKey)
	{
		return mprtAttribute.get(strKey);
	}

	public SubmitMessage(SubmitSM submitSM, boolean usingSequence) throws Exception
	{
		extendedMessage = "";
		this.usingSequence = usingSequence;
		//responseID = (int)SequenceManager.getSequence("RESPONSE_SEQ");
		mprtAttribute = new Properties();
		this.submitSM = submitSM;
		if(usingSequence)
			this.submitSM.setSequenceNumber((int)responseID);
		else
			this.submitSM.setSequenceNumber(0);
	}

	public boolean isUsingSequence()
	{
		return usingSequence;
	}

	////////////////////////////////////////////////////////
	/**
	 * Store message to output stream
	 * @param os OutputStream
	 * @throws Exception
	 */
	////////////////////////////////////////////////////////
	public void store(OutputStream os) throws Exception
	{
	}

	public void setAttribute(String strKey, Object objValue)
	{
		mprtAttribute.put(strKey, objValue);
	}

	public void setAttributes(Properties prt)
	{
		mprtAttribute = prt;
	}

	public String getContent()
	{
		return content;
	}

	public SubmitSM getSubmitSM()
	{
		return submitSM;
	}

	public long getRequestID()
	{
		return requestID;
	}

	public void setRequestID(long requestID)
	{
		this.requestID = requestID;
	}

	public long getResponseID()
	{
		return responseID;
	}

	private void setResponseID(long responseID)
	{
		this.responseID = responseID;
	}

	public String getExtendedMessage()
	{
		return extendedMessage;
	}

	public void setExtendedMessage(String extendedMessage)
	{
		this.extendedMessage = extendedMessage;
	}

	public void setAttributes(Map map)
	{
		this.mprtAttribute = ((Hashtable)map);
	}
}
