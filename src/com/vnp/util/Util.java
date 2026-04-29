package com.vnp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.fss.util.StringUtil;

public class Util {
	private static String numberic = "[0-9]+";
	
	public static Vector toStringVector(String strSource, String strSeparator) {
		Vector vtReturn = new Vector();
		int iIndex = 0;
		int iLastIndex = 0;
		while ((iIndex = strSource.indexOf(strSeparator, iLastIndex)) >= 0) {
			vtReturn.addElement(strSource.substring(iLastIndex, iIndex).trim());
			iLastIndex = iIndex + strSeparator.length();
		}

		if (iLastIndex >= 0 && iLastIndex <= strSource.length()) {
			String element = strSource.substring(iLastIndex, strSource.length()).trim();
			if (element != null && !element.equals("")) {
				vtReturn.addElement(element);
			}
		}
		return vtReturn;
	}

	public static String normalizeIsdn(String strIsdn) throws Exception {
		String strpIsdn = StringUtil.nvl(strIsdn, "").trim();
		String strNormalIsdn = strpIsdn;
		if (strpIsdn.startsWith("84")) {
			strNormalIsdn = strpIsdn.substring(2, strpIsdn.length());
		} else if (strpIsdn.startsWith("0")) {
			strNormalIsdn = strpIsdn.substring(1, strpIsdn.length());
		} else {
			strNormalIsdn = strpIsdn;
		}
		return strNormalIsdn;
	}
	
	public static String formatMsisdn(String strIsdn) throws Exception {
		String strpIsdn = StringUtil.nvl(strIsdn, "").trim();
		String strNormalIsdn = strpIsdn;
		if (strpIsdn.startsWith("84")) {
			strNormalIsdn = strpIsdn;
		} else if (strpIsdn.startsWith("0")) {
			strNormalIsdn = "84" + strpIsdn.substring(1, strpIsdn.length());
		} else {
			strNormalIsdn = "84" + strpIsdn;
		}
		return strNormalIsdn;
	}
	
	public static boolean validIsdn(String strIsdn) {
		try{
			String msisdn = formatMsisdn(strIsdn);
			if(!msisdn.matches(numberic)) return false;
			if(msisdn.startsWith("8491") || msisdn.startsWith("8494") || msisdn.startsWith("8488")
					|| msisdn.startsWith("8483") || msisdn.startsWith("8484") || msisdn.startsWith("8485") || msisdn.startsWith("8481") || msisdn.startsWith("8482")) return msisdn.length() == 11;
			if(msisdn.startsWith("84123") || msisdn.startsWith("84124") || msisdn.startsWith("84125") || msisdn.startsWith("84127") || msisdn.startsWith("84129")) return msisdn.length() == 12;
			return false;
		}catch(Exception ex){
			return false;
		}	
	}
	
	public static boolean validVNTxt(String content){
		try{
			String punyCode = java.net.IDN.toASCII(content);
			return punyCode.startsWith("xn--");
		}catch(Exception ex){
			return false;
		}	
	}
	
	
	public static boolean match(String strWild, String strVal, boolean bIgnoreCase) {
		if (bIgnoreCase) {
			strVal = strVal.toUpperCase();
			strWild = strWild.toUpperCase();
		}

		int iWild = 0;
		int iVal = 0;
		int iWildCount = strWild.length();
		int iValCount = strVal.length();

		while ((iVal < iValCount) && (iWild < iWildCount)) {
			if ((strWild.charAt(iWild) == strVal.charAt(iVal)) || (strWild.charAt(iWild) == '?')) {
				iWild++;
				iVal++;
			} else {
				if (strWild.charAt(iWild) == '*') {
					iWild++;
					if (iWild >= iWildCount) {
						return true;
					}
					for (;;) {
						if ((iVal < iValCount) && (strWild.charAt(iWild) != strVal.charAt(iVal))) {
							iVal++;
						} else {
							if (iVal >= iValCount) {
								return false;
							}
							String strNewWild = strWild.substring(iWild, iWildCount);
							String strNewVal = strVal.substring(iVal, iValCount);
							if (match(strNewWild, strNewVal, bIgnoreCase)) {
								return true;
							}

							iVal++;
						}
					}
				}

				return false;
			}
		}

		if ((iVal >= iValCount)
				&& ((iWild >= iWildCount) || (isOptional(strWild.substring(iWild, strWild.length()))))) {
			return true;
		}

		return false;
	}
	
	public static boolean isOptional(String str) {
		for (int iIndex = 0; iIndex < str.length(); iIndex++) {
			if (str.charAt(iIndex) != '*') {
				return false;
			}
		}
		return true;
	}
	
	public static String post(Map<String,String> params) throws IOException {
		URL u = new URL(params.get("url"));
		
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setReadTimeout(20000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty( "Content-Type", "text/xml; charset=utf-8" );
        
        if (params.containsKey("SOAPAction")){
        	conn.setRequestProperty( "SOAPAction", params.get("SOAPAction") );
        }
        
        String data = FileUtils.readFileToString(new File(System.getProperty("user.dir")+"/conf/req/"+params.get("service")),"UTF-8");
        data = eval(data,params);
        
        conn.setRequestProperty( "Content-Length", String.valueOf(data.length()));
        OutputStream os = conn.getOutputStream();
        os.write(data.getBytes("UTF-8"));
        os.flush();
        os.close();

        String inputLine;
        StringBuffer out = new StringBuffer();
        BufferedReader in = null;
        BufferedReader in1 = null;
        try{
        	if(conn.getInputStream() != null){
            	in = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));            
                while ((inputLine = in.readLine()) != null) {
                	out.append(inputLine+"\n");
                }
                in.close();
            }
        }catch(Exception ioex){        	
        }
        
        try{
        	if(conn.getErrorStream() != null){
            	in1 = new BufferedReader(new InputStreamReader(conn.getErrorStream(),"UTF-8"));
                while ((inputLine = in1.readLine()) != null) {
                	out.append(inputLine+"\n");
                }
                in1.close();
            }
            
        }catch(Exception ioex){
        	
        }
        if(in != null){
        	in.close();
        }
        if(in1 != null){
        	in1.close();
        }
        return out.toString();
	}
	
	public static String eval(String input, Map<String,String> parameters){		
		VelocityContext vcontext = new VelocityContext(parameters);
		StringWriter sw = new StringWriter();
		Velocity.evaluate(vcontext, sw, "velocity",input);
		return sw.toString();
	}
	
	public static String getXmlText(String xml, String path) throws Exception {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(xml));
//		Element e = (Element) document.selectSingleNode(path);
//		String res = e.getText();
//		return res;
		return document.selectSingleNode(path).getText();
	}
	
	public static String getXmlAttr(String xml, String tagPath, String attrName) throws DocumentException {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(new StringReader(xml));
			Element e = (Element) document.selectSingleNode(tagPath);
			return e.attributeValue(attrName);
		} catch (Exception e) {
			return null;
		}

	}
	
	@SuppressWarnings("unchecked")
	public static List<Node> getNodes(String xml, String path) throws DocumentException {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(new StringReader(xml));
			return document.selectNodes(path);
		} catch (Exception e) {
			return null;
		}
	}

}
