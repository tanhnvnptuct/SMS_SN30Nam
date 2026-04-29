package com.vnp.util.message;

import com.fss.queue.Message;
import com.fss.util.StringEscapeUtil;
import com.fss.util.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLMessage extends DefaultHandler implements Message
{
	private Hashtable mprtAttribute = new Hashtable();
	private XMLNode mnd;
	private XMLNode mndRoot;
	private StringBuffer mstrValue;
	private Vector mvtNameStack;
	private Vector mvtDataStack;

	public XMLMessage()
	{
		this.mndRoot = new XMLNode();
	}

	public XMLMessage(InputStream is) throws Exception
	{
		load(is);
	}

	public void load(InputStream is) throws Exception
	{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setValidating(false);
		SAXParser parser = spf.newSAXParser();
		this.mvtNameStack = new Vector();
		this.mvtDataStack = new Vector();
		this.mndRoot = new XMLNode();
		parser.parse(is,this);
	}

	public void store(OutputStream os) throws Exception
	{
		store(os,true);
	}

	public void store(OutputStream os,boolean bIncludeHeader) throws Exception
	{
		if(bIncludeHeader){
			os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n".getBytes());
		}
		store(os,this.mndRoot,"");
	}

	private void store(OutputStream os,XMLNode nd,String strTab) throws Exception
	{
		XMLNode ndTemp = nd.mndFirstChild;
		while(ndTemp != null){
			os.write(strTab.getBytes());
			os.write(60);
			os.write(ndTemp.mstrName.getBytes());
			if(ndTemp.mprt != null){
				Enumeration enm = ndTemp.mprt.keys();
				while(enm.hasMoreElements()){
					Object obj = enm.nextElement();
					os.write(32);
					os.write(obj.toString().getBytes());
					os.write(61);
					os.write(34);
					os.write(ndTemp.mprt.get(obj).toString().getBytes());
					os.write(34);
				}
			}
			os.write(62);

			if(ndTemp.mndFirstChild != null){
				os.write(13);
				os.write(10);
				if((ndTemp.mstrValue != null) && (ndTemp.mstrValue.length() > 0)){
					os.write(strTab.getBytes());
					os.write(9);
					os.write(ndTemp.mstrValue.getBytes());
					os.write(13);
					os.write(10);
				}

				store(os,ndTemp,strTab + "\t");
				os.write(strTab.getBytes());
			}
			else if((ndTemp.mstrValue != null) && (ndTemp.mstrValue.length() > 0)){
				os.write(StringEscapeUtil.escapeXml(ndTemp.mstrValue).getBytes());
			}

			os.write(60);
			os.write(47);
			os.write(ndTemp.mstrName.getBytes());
			os.write(62);
			os.write(13);
			os.write(10);

			ndTemp = ndTemp.mndNext;
		}
	}

	public Object getAttribute(String strKey)
	{
		return this.mprtAttribute.get(strKey);
	}

	public void setAttribute(String strKey,Object objValue)
	{
		this.mprtAttribute.put(strKey,objValue);
	}

	public String getValue(String strKey)
	{
		return getValue(strKey,"");
	}

	public String getValue(String strKey,String strNullValue)
	{
		XMLNode nd = getChild(strKey);
		if(nd != null){
			return StringUtil.nvl(nd.mstrValue,strNullValue);
		}
		return strNullValue;
	}

	public void setValue(String strKey,String strValue)
	{
		XMLNode nd = getChild(strKey,true);
		nd.mstrValue = strValue;
	}

	public String getContent() throws Exception
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		store(os);
		return new String(os.toByteArray());
	}

	public void setAttributes(Hashtable prt)
	{
		this.mprtAttribute = prt;
	}

	public Hashtable getAttributes()
	{
		return this.mprtAttribute;
	}

	public void startElement(String uri,String name,String qName,Attributes atts) throws SAXException
	{
		this.mvtNameStack.addElement(qName);
		this.mstrValue = null;
		this.mnd = new XMLNode();
		this.mnd.mstrName = qName;
		if(atts.getLength() > 0){
			this.mnd.mprt = new Properties();
			for(int iIndex = 0;iIndex < atts.getLength();iIndex++){
				this.mnd.mprt.setProperty(atts.getQName(iIndex),atts.getValue(iIndex));
			}
		}
		this.mvtDataStack.addElement(this.mnd);
	}

	public void endElement(String uri,String name,String qName) throws SAXException
	{
		if(!qName.equals(this.mvtNameStack.elementAt(this.mvtNameStack.size() - 1))){
			throw new SAXException("Found end mark of " + qName + " without start mark");
		}
		XMLNode nd = (XMLNode)this.mvtDataStack.elementAt(this.mvtDataStack.size() - 1);
		this.mvtDataStack.removeElementAt(this.mvtDataStack.size() - 1);
		if(this.mstrValue != null){
			nd.mstrValue = StringEscapeUtil.unescapeXml(this.mstrValue.toString().trim());
		}
		else{
			nd.mstrValue = "";
		}
		this.mvtNameStack.removeElementAt(this.mvtNameStack.size() - 1);
		this.mstrValue = null;

		if(this.mvtNameStack.size() == 0){
			this.mndRoot.addChild(nd);
		}
		else{
			((XMLNode)this.mvtDataStack.elementAt(this.mvtDataStack.size() - 1)).addChild(nd);
		}
	}

	public void characters(char[] ch,int start,int length)
	{
		if(this.mstrValue == null){
			this.mstrValue = new StringBuffer();
		}
		this.mstrValue.append(new String(ch,start,length));
	}

	public void addChild(XMLNode ndChild)
	{
		this.mndRoot.addChild(ndChild);
	}

	public void removeChild(XMLNode ndChild)
	{
		this.mndRoot.removeChild(ndChild);
	}

	public void removeChild(String strPath)
	{
		this.mndRoot.removeChild(strPath);
	}

	public XMLNode getChild(String strPath)
	{
		return this.mndRoot.getChild(strPath);
	}

	public XMLNode getChild(String strPath,boolean bAutoCreate)
	{
		return this.mndRoot.getChild(strPath,bAutoCreate);
	}

	public void setChild(String strPath,XMLNode ndChild)
	{
		this.mndRoot.setChild(strPath,ndChild);
	}

	public void setAttributes(Map map)
	{
		this.mprtAttribute = ((Hashtable)map);
	}
}
