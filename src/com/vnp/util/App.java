package com.vnp.util;

import javax.swing.*;
import java.awt.Image;
import java.io.*;
import java.util.*;
import java.net.URL;

import com.fss.dictionary.*;
import com.fss.dictionary.Dictionary;

import java.text.DecimalFormat;
import java.sql.*;
import com.fss.sql.Database;
import com.vnp.sms.thread.ThreadManagerEx;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FSS-FPT</p>
 * @author Thai Hoang Hiep
 * @version 1.0
 */

public class App
{
	////////////////////////////////////////////////////////
	// Constant
	////////////////////////////////////////////////////////
	public final static String DATE_FORMAT = "dd/MM/yyyy";
	public final static String DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
	public static String tableAccount;

	public static final String ERROR_DICTIONARY_INFO = "/com/vnp/dial/ERRDictionary.txt";

	////////////////////////////////////////////////////////
	// Member variables
	////////////////////////////////////////////////////////
	public static Properties mprtMenuCaptionScript = new Properties();
	public static Properties mprtDictionary = new Properties();
	public static Dictionary mdic;
	public static String mstrLanguage;
	private static String mstrKey;
	public static ThreadManagerEx manager = null;

	////////////////////////////////////////////////////////
	// Static initialize
	////////////////////////////////////////////////////////
	static
	{
	//	loadDictionary();
		setCurrentLanguage("VN");
	}

	////////////////////////////////////////////////////////
	public static void initialize()
	{
	}

	////////////////////////////////////////////////////////
	/**
	 * Get menu caption script
	 * @param strLanguage language
	 * @return script
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static String getMenuCaptionScript(String strLanguage)
	{
		String strMenuCaptionScript = (String)mprtMenuCaptionScript.get(strLanguage);
		if(strMenuCaptionScript == null)
		{
			strMenuCaptionScript = "";
		}
		return strMenuCaptionScript;
	}

	////////////////////////////////////////////////////////
	/**
	 * Load dictionary form resource
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static void loadDictionary()
	{
		InputStream is = null;
		try
		{
			is = App.class.getResourceAsStream(ERROR_DICTIONARY_INFO);
			Dictionary dic = new Dictionary(is);
			for(int iIndex = 0;iIndex < dic.mndRoot.mvtChild.size();iIndex++)
			{
				// Add new dictionary into list
				String strKey = ((DictionaryNode)dic.mndRoot.mvtChild.elementAt(iIndex)).mstrName;
				String strFileName = dic.getString(strKey + ".File");
				addErrorDictionary(strFileName,strKey);
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				is.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	////////////////////////////////////////////////////////
	/**
	 * Append new error entry
	 * @param strFileName String
	 * @param strKey String
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static void addErrorDictionary(String strFileName,
										  String strKey)
	{
		InputStream is = null;
		try
		{
			// Load dictionary
			is = App.class.getResourceAsStream(strFileName);
			Dictionary dic = new Dictionary(is);

			// Merge with default
			dic.mndRoot.merge(ErrorDictionary.getDictionary(strKey).mndRoot);
			ErrorDictionary.getDictionary(strKey).mndRoot = dic.mndRoot;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				is.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	////////////////////////////////////////////////////////
	/**
	 * Add new dictionary into list
	 * @param strFileName name of file contain dictionary data
	 * @param strKey dictinary unique key
	 * @param strLanguage dictionary language
	 * @param strIconPath path to icon image
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static void addDictionary(String strFileName,
									 String strKey,
									 String strLanguage,
									 String strIconPath)
	{
		InputStream is = null;
		try
		{
			// Load dictionary
			is = App.class.getResourceAsStream(strFileName);
			Dictionary dic = new Dictionary(is);

			// Merge with default
			dic.mndRoot.merge(DefaultDictionary.getDictionary(strKey).mndRoot);

			// Set dictionary properties
			dic.setLanguage(strLanguage);
			dic.setIconPath(strIconPath);
			mprtDictionary.remove(strKey);
			mprtDictionary.put(strKey,dic);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				is.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	////////////////////////////////////////////////////////
	/**
	 * Get instance of dictionary
	 * @param strKey key of dictionary language
	 * @return Dictionary contain dictionary data of language strLanguage
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static Dictionary getDictionary(String strKey)
	{
		Dictionary dic = (Dictionary)mprtDictionary.get(strKey);
		if(dic == null)
		{
			dic = new Dictionary();
		}
		return dic;
	}

	////////////////////////////////////////////////////////
	/**
	 * Get supported language
	 * @return list of language supported by this dictionary
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static String[] getSupportedLanguage()
	{
		Object[] objLang = mprtDictionary.keySet().toArray();
		String[] strReturn = new String[objLang.length];
		for(int iIndex = 0;iIndex < objLang.length;iIndex++)
		{
			strReturn[iIndex] = objLang[iIndex].toString();
		}
		return strReturn;
	}

	////////////////////////////////////////////////////////
	/**
	 * Set active language of this dictionary
	 * @param strKey key of active language
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static void setCurrentLanguage(String strKey)
	{
		mstrKey = strKey;
		mdic = getDictionary(mstrKey);
	}

	////////////////////////////////////////////////////////
	/**
	 * Get active language of this dictionary
	 * @return key of active language
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static String getCurrentLanguage()
	{
		return mstrKey;
	}

	////////////////////////////////////////////////////////
	/**
	 * Get child dictionary
	 * @param strPath child path
	 * @return Dictionary
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static Dictionary getChildDictionary(String strPath)
	{
		DictionaryNode nd = mdic.getNode(strPath);
		if(nd == null)
		{
			return new Dictionary();
		}
		return new Dictionary(nd);
	}

	////////////////////////////////////////////////////////
	/**
	 * @param strKey String
	 * @return JButton
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static javax.swing.JButton createButton(String strKey)
	{
		javax.swing.JButton btn = new javax.swing.JButton();
		applyButton(btn,strKey);
		return btn;
	}

	////////////////////////////////////////////////////////
	/**
	 * Create general menu
	 * @param strKey String
	 * @return JMenu
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static javax.swing.JMenu createMenu(String strKey)
	{
		javax.swing.JMenu mnu = new javax.swing.JMenu();
		applyButton(mnu,strKey);
		return mnu;
	}

	////////////////////////////////////////////////////////
	/**
	 * Create general menu item
	 * @param strKey String
	 * @return JMenuItem
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static javax.swing.JMenuItem createMenuItem(String strKey)
	{
		javax.swing.JMenuItem mnu = new javax.swing.JMenuItem();
		applyButton(mnu,strKey);
		return mnu;
	}

	////////////////////////////////////////////////////////
	/**
	 * Apply general button
	 * @param btn AbstractButton
	 * @param strKey String
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static void applyButton(javax.swing.AbstractButton btn,String strKey)
	{
		btn.setText(getString(strKey));
		String strMnemonic = getString("Mnemonic." + strKey);
		if(!strMnemonic.equals(""))
		{
			btn.setMnemonic(strMnemonic.charAt(0));
		}
	}

	////////////////////////////////////////////////////////
	// Wrap function from Dictionary
	////////////////////////////////////////////////////////
	public static String getString(String strKey)
	{
		return mdic.getString(strKey);
	}

	////////////////////////////////////////////////////////
	public static String getString(String strKey,String strPar)
	{
		return mdic.getString(strKey,strPar);
	}

	////////////////////////////////////////////////////////
	public static String getString(String strKey,String strPar1,String strPar2)
	{
		return mdic.getString(strKey,strPar1,strPar2);
	}

	////////////////////////////////////////////////////////
	public static String getString(String strKey,String strPar[])
	{
		return mdic.getString(strKey,strPar);
	}

	////////////////////////////////////////////////////////
	/**
	 *
	 * @param nd DictionaryNode
	 * @return Vector
	 */
	////////////////////////////////////////////////////////
	public static Vector getComboData(com.fss.dictionary.DictionaryNode nd)
	{
		if(nd == null || nd.mvtChild == null)
		{
			return new Vector();
		}
		Vector vtComboData = new Vector();
		Vector vt = nd.mvtChild;
		for(int iIndex = 0;iIndex < vt.size();iIndex++)
		{
			nd = (com.fss.dictionary.DictionaryNode)vt.elementAt(iIndex);
			Vector vtComboRow = new Vector();
			vtComboRow.addElement(nd.mstrName);
			vtComboRow.addElement(nd.mstrValue);
			vtComboData.addElement(vtComboRow);
		}
		return vtComboData;
	}
	/**
	 *
	 * @param con Connection
	 * @param strParam String
	 * @param strParamType String
	 * @return String
	 * @since 11/03/2014
	 */
	public static String getApParam(Connection con, String strParam, String strParamType) {
		PreparedStatement prmStmt = null;
		ResultSet rsData = null;
		try{
			prmStmt = con.prepareStatement("Select par_value From Ap_Param Where par_name = ? and par_type = ?");
			prmStmt.setString(1,strParam);
			prmStmt.setString(2,strParamType);
			rsData = prmStmt.executeQuery();
			if(rsData.next()) {
				return rsData.getString("par_value");
			}
		}
		catch(Exception ex){
			return "";
		}
		finally {
			Database.closeObject(prmStmt);
		}
		return "";
	}
}
