package com.vnp.util;

import java.sql.*;
import java.util.*;

import com.fss.sql.*;
import com.fss.util.*;

import oracle.jdbc.pool.OracleDataSource;

import com.fss.dictionary.*;
import com.fss.dictionary.Dictionary;
import java.io.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FSS-FPT</p>
 * @author Thai Hoang Hiep
 * @version 1.0
 */

@SuppressWarnings("deprecation")
public class AppServer
{
	////////////////////////////////////////////////////////
	// Constant
	////////////////////////////////////////////////////////
	public static final String SYSTEM_PASSWORD_FILE = "system.pwd";
	////////////////////////////////////////////////////////
	// Variables
	////////////////////////////////////////////////////////
	private static com.fss.dictionary.Dictionary dic = new com.fss.dictionary.Dictionary();
	private static OracleConnectionFactory pool = null;
//	private  static OracleDataSource ods = null;
	private final  static String CACHE_NAME = "MYCACHE";
	////////////////////////////////////////////////////////
	// Static initialize
	////////////////////////////////////////////////////////
	static
	{
		try{
			loadConfig("/com/vnp/sms/ServerConfig.txt");
			App.initialize();
			String strNode = getParam("DefaultDatabase");
			int iConnectPoolSize = 20;
			try{
				iConnectPoolSize = Integer.parseInt(getParam("ConnectionPoolSize"));
			}
			catch(Exception e){
			}
			Dictionary dicConnection = new Dictionary(getDictionaryNode("Connection." + strNode));
			pool = new OracleConnectionFactory(dicConnection.getString("Url"),
											   dicConnection.getString("UserName"),
											   dicConnection.getString("Password"),
											   iConnectPoolSize);
//			ods = new OracleDataSource();
//            ods.setURL(dicConnection.getString("Url"));
//            //ods.setServerName(arg0);
//            ods.setUser(dicConnection.getString("UserName"));
//            ods.setPassword(dicConnection.getString("Password"));
//            // caching parms
//            ods.setConnectionCachingEnabled(true);
//            ods.setConnectionCacheName(CACHE_NAME);
//            Properties cacheProps = new Properties();
//            cacheProps.setProperty("MinLimit", "1");
//            cacheProps.setProperty("MaxLimit", String.valueOf(iConnectPoolSize));
//            cacheProps.setProperty("InitialLimit", "1");
//            cacheProps.setProperty("ConnectionWaitTimeout", "20");
//            cacheProps.setProperty("ValidateConnection", "true");
//            ods.setConnectionCacheProperties(cacheProps);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	////////////////////////////////////////////////////////
	/**
	 * Load parameter from resource
	 * @param strFileName path to config file
	 * @throws Exception
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static void loadConfig0(String strFileName) throws Exception
	{
		InputStream in = null;
		try{
			in = AppServer.class.getResourceAsStream(strFileName);
			dic.load(in);
		}
		finally{
			FileUtil.safeClose(in);
		}
	}

	////////////////////////////////////////////////////////
	/**
	 * Load parameter from resource
	 * @param strFileName path to config file
	 * @throws Exception
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
//	public static void loadConfig(String strFileName) throws Exception
//	{
//		InputStream in = null;
//		try{
//			in = AppServer.class.getResourceAsStream(strFileName);
//			dic.load(in);
//		}
//		finally{
//			FileUtil.safeClose(in);
//		}
//	}

	public static void loadConfig(String strFileName) throws Exception
	{
		try {
			dic.load(AppServer.class.getResourceAsStream(strFileName));
		}
		catch(Exception ex) {
			String strWorkingDir = System.getProperty("user.dir");
			if (!strWorkingDir.endsWith("/") || !strWorkingDir.endsWith("\\")) {
				strWorkingDir += "/";
			}
			dic.load(new FileInputStream(new File(strWorkingDir + strFileName)));
		}
	}


	////////////////////////////////////////////////////////
	/**
	 *
	 * @param strKey String
	 * @return Vector
	 */
	////////////////////////////////////////////////////////
	public static Vector getParamList(DictionaryNode nd,String str)
	{
		Vector vtReturn = new Vector();
		if(nd != null && nd.getChildList() != null){
			Vector vt = nd.getChildList();
			for(int iIndex = 0;iIndex < vt.size();iIndex++){
				DictionaryNode ndChild = (DictionaryNode)vt.elementAt(iIndex);
				String strChildName = StringUtil.nvl(ndChild.mstrName,"");
				Vector vtRow = new Vector();
				if(str.equals("")){
					vtRow.addElement(strChildName);
					vtRow.addElement(StringUtil.nvl(ndChild.mstrValue,""));
					vtReturn.addElement(vtRow);
					vtReturn.addAll(getParamList(ndChild,strChildName));
				}
				else{
					vtRow.addElement(str + "." + strChildName);
					vtRow.addElement(StringUtil.nvl(ndChild.mstrValue,""));
					vtReturn.addElement(vtRow);
					vtReturn.addAll(getParamList(ndChild,
												 str + "." + strChildName));
				}
			}
		}
		return vtReturn;
	}

	////////////////////////////////////////////////////////
	public static Vector getParamList()
	{
		return getParamList(dic.mndRoot,"");
	}

	////////////////////////////////////////////////////////
	/**
	 * Get parameter from server config
	 * @param strKey String
	 * @return String
	 */
	////////////////////////////////////////////////////////
	public static String getParam(String strKey)
	{
		return dic.getString(strKey);
	}

	////////////////////////////////////////////////////////
	/**
	 *
	 * @param strKey String
	 * @param strValue String
	 * @throws Exception
	 */
	////////////////////////////////////////////////////////
	public static void setParam(String strKey,String strValue) throws
		Exception
	{
		if(strKey.equals("DefaultDatabase")){
			DictionaryNode nd = getDictionaryNode("Connection." + strValue);
			if(nd == null){
				nd = getDictionaryNode("Connection");
				Vector vtConnection = nd.getChildList();
				String strChildList = "";
				for(int iIndex = 0;iIndex < vtConnection.size();iIndex++){
					strChildList +=
						((DictionaryNode)vtConnection.elementAt(iIndex)).
						mstrName + ",";
				}
				if(!strChildList.equals("")){
					strChildList = strChildList.substring(0,
														  strChildList.length() - 1);
				}
				throw new Exception("Database '" + strValue +
									"' was not declared. Available database is: " +
									strChildList);
			}
		}
		else{
			DictionaryNode nd = getDictionaryNode(strKey);
			if(nd == null){
				throw new Exception("Parameter '" + strKey + "' not found");
			}
			nd.mstrValue = StringUtil.nvl(strValue,"");
		}
	}

	////////////////////////////////////////////////////////
	/**
	 * Get a dictionary node from server config
	 * @param node String
	 * @return DictionaryNode
	 */
	////////////////////////////////////////////////////////
	public static DictionaryNode getDictionaryNode(String node)
	{
		return dic.getNode(node);
	}

	////////////////////////////////////////////////////////
	/**
	 * Open conection to database
	 * @return connection created
	 * @throws java.lang.Exception
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static Connection getConnection() throws Exception
	{
		return pool.getConnection();
//		if (ods == null) {
//	          throw new SQLException("OracleDataSource is null.");
//	      }
//	    return ods.getConnection();
	}

	public static boolean isDate(String value)
	{
		if(com.fss.util.DateUtil.isDate(value,getParam("DateFormat")) ||
		   com.fss.util.DateUtil.isDate(value,getParam("DateTimeFormat"))){
			return true;
		}
		else{
			return false;
		}

	}

	public static void main(String[] args)
	{
		System.out.println("");
	}
}
