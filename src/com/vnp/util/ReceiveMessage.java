package com.vnp.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.fss.queue.Message;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.SubmitSMResp;

public class ReceiveMessage implements Message
{
	private String content;
	private Hashtable mprtAttribute;
	private Object receiveSM;
	private String mstrContent;

	public ReceiveMessage(Object receiveSM)
	{
		mprtAttribute = new Properties();
		this.receiveSM = receiveSM;
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


	public Object getAttribute(String strKey)
	{
		return mprtAttribute.get(strKey);
	}

	public Hashtable getAttributes()
	{
		return mprtAttribute;
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

	public Object getReceiveSM()
	{
		return receiveSM;
	}

	public SubmitSMResp getSubmitSMResp()
	{
		return (SubmitSMResp)getReceiveSM();
	}

	public DeliverSM getDeliverSM()
	{
		return (DeliverSM)getReceiveSM();
	}

	public void setAttributes(Map map)
	{
		mprtAttribute = (Hashtable) map;
	}
}
