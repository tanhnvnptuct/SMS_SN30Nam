package com.vnp.util.message;

import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FSS-FPT</p>
 * @author Thai Hoang Hiep
 * @version 1.0
 */

public class XMLNode
{
	public static final String SEPARATOR_SYMBOL = ".";
	public String mstrName;
	public String mstrValue;
	public Properties mprt;
	public XMLNode mndFirstChild,mndLastChild;
	public XMLNode mndNext;
	////////////////////////////////////////////////////////
	// Constructor
	////////////////////////////////////////////////////////
	public XMLNode()
	{
	}
	////////////////////////////////////////////////////////
	public XMLNode(String strName,String strValue,Properties prt)
	{
		mstrName = strName;
		mstrValue = strValue;
		mprt = prt;
	}
	////////////////////////////////////////////////////////
	/**
	 * Add new child
	 * @param ndChild XMLNode
	 */
	////////////////////////////////////////////////////////
	public void addChild(XMLNode ndChild)
	{
		if(mndFirstChild == null)
			mndFirstChild = ndChild;
		else
			mndLastChild.mndNext = ndChild;
		mndLastChild = ndChild;
		mndLastChild.mndNext = null;
	}
	////////////////////////////////////////////////////////
	/**
	 * Remove child
	 * @param ndChild XMLNode
	 */
	////////////////////////////////////////////////////////
	public void removeChild(XMLNode ndChild)
	{
		if(ndChild == null)
			return;
		XMLNode ndPrevious = null;
		XMLNode ndRemove = mndFirstChild;
		while(ndRemove != null)
		{
			if(ndRemove == ndChild)
			{
				if(ndPrevious == null)
				{
					mndFirstChild = ndRemove.mndNext;
					if(mndFirstChild == null)
						mndLastChild = null;
					else if(mndFirstChild.mndNext == null)
						mndLastChild = mndFirstChild;
				}
				else
				{
					ndPrevious.mndNext = ndRemove.mndNext;
					if(ndPrevious.mndNext == null)
						mndLastChild = ndPrevious;
				}
				return;
			}
			else
			{
				ndPrevious = ndRemove;
				ndRemove = ndRemove.mndNext;
			}
		}
	}
	////////////////////////////////////////////////////////
	/**
	 * Remove child
	 * @param strPath String
	 */
	////////////////////////////////////////////////////////
	public void removeChild(String strPath)
	{
		int iIndex = strPath.indexOf(SEPARATOR_SYMBOL);
		if(iIndex < 0)
		{
			iIndex = strPath.indexOf('[');
			int iCount = 0;
			if(iIndex >= 0)
			{
				int iCloseIndex = strPath.indexOf(']',iIndex + 1);
				if(iCloseIndex >= 0)
				{
					String strIndex = strPath.substring(iIndex + 1,iCloseIndex);
					strPath = strPath.substring(0,iIndex);
					try
					{
						iCount = Integer.parseInt(strIndex);
					}
					catch(Exception e)
					{
					}
				}
			}

			XMLNode ndPrevious = null;
			XMLNode ndRemove = mndFirstChild;
			while(ndRemove != null)
			{
				if(ndRemove.mstrName.equalsIgnoreCase(strPath))
				{
					if(iCount > 0)
						iCount--;
					else
					{
						if(ndPrevious == null)
						{
							mndFirstChild = ndRemove.mndNext;
							if(mndFirstChild == null)
								mndLastChild = null;
							else if(mndFirstChild.mndNext == null)
								mndLastChild = mndFirstChild;
						}
						else
						{
							ndPrevious.mndNext = ndRemove.mndNext;
							if(ndPrevious.mndNext == null)
								mndLastChild = ndPrevious;
						}
						return;
					}
				}
				else
				{
					ndPrevious = ndRemove;
					ndRemove = ndRemove.mndNext;
				}
			}
		}
		else
		{
			String strNodeName = strPath.substring(0,iIndex).trim();
			String strSubPath = strPath.substring(iIndex + 1,strPath.length()).trim();
			XMLNode ndReturn = getChild(strNodeName);
			if(ndReturn != null && strSubPath.length() > 0)
				ndReturn.removeChild(strSubPath);
		}
	}
	////////////////////////////////////////////////////////
	/**
	 * Get child by path
	 * @param strPath String
	 * @return XMLNode
	 */
	////////////////////////////////////////////////////////
	public XMLNode getChild(String strPath)
	{
		return getChild(strPath,false);
	}
	////////////////////////////////////////////////////////
	/**
	 * Get child by path
	 * @param strPath String
	 * @param bAutoCreate boolean
	 * @return XMLNode
	 */
	////////////////////////////////////////////////////////
	public XMLNode getChild(String strPath,boolean bAutoCreate)
	{
		int iIndex = strPath.indexOf(SEPARATOR_SYMBOL);
		if(iIndex < 0)
		{
			iIndex = strPath.indexOf('[');
			int iCount = 0;
			if(iIndex >= 0)
			{
				int iCloseIndex = strPath.indexOf(']',iIndex + 1);
				if(iCloseIndex >= 0)
				{
					String strIndex = strPath.substring(iIndex + 1,iCloseIndex);
					strPath = strPath.substring(0,iIndex);
					try
					{
						iCount = Integer.parseInt(strIndex);
					}
					catch(Exception e)
					{
					}
				}
			}

			XMLNode ndReturn = mndFirstChild;
			while(ndReturn != null)
			{
				if(ndReturn.mstrName.equalsIgnoreCase(strPath))
				{
					if(iCount > 0)
						iCount--;
					else
						return ndReturn;
				}
				ndReturn = ndReturn.mndNext;
			}
			if(!bAutoCreate)
				return null;
			XMLNode nd = null;
			while(iCount >= 0)
			{
				nd = new XMLNode();
				nd.mstrName = strPath;
				addChild(nd);
				iCount--;
			}
			return nd;
		}
		else
		{
			String strNodeName = strPath.substring(0,iIndex).trim();
			String strSubPath = strPath.substring(iIndex + 1,strPath.length()).trim();
			XMLNode ndReturn = getChild(strNodeName,bAutoCreate);
			if(ndReturn != null && strSubPath.length() > 0)
				ndReturn = ndReturn.getChild(strSubPath,bAutoCreate);
			return ndReturn;
		}
	}
	////////////////////////////////////////////////////////
	/**
	 * Set child
	 * @param strPath String
	 * @param ndChild XMLNode
	 */
	////////////////////////////////////////////////////////
	public void setChild(String strPath,XMLNode ndChild)
	{
		int iIndex = strPath.indexOf(SEPARATOR_SYMBOL);
		if(iIndex < 0)
		{
			iIndex = strPath.indexOf('[');
			int iCount = 0;
			if(iIndex >= 0)
			{
				int iCloseIndex = strPath.indexOf(']',iIndex + 1);
				if(iCloseIndex >= 0)
				{
					String strIndex = strPath.substring(iIndex + 1,iCloseIndex);
					strPath = strPath.substring(0,iIndex);
					try
					{
						iCount = Integer.parseInt(strIndex);
					}
					catch(Exception e)
					{
					}
				}
			}

			XMLNode ndPrevious = null;
			XMLNode ndReplace = mndFirstChild;
			while(ndReplace != null)
			{
				if(ndReplace.mstrName.equalsIgnoreCase(strPath))
				{
					if(iCount > 0)
						iCount--;
					else
					{
						ndChild.mndNext = ndReplace.mndNext;
						if(ndPrevious == null)
							mndFirstChild = ndChild;
						else
							ndPrevious.mndNext = ndChild;
						if(ndReplace == mndLastChild)
							mndLastChild = ndChild;
						return;
					}
				}
				else
				{
					ndPrevious = ndReplace;
					ndReplace = ndReplace.mndNext;
				}
			}
			while(iCount > 0)
			{
				XMLNode nd = new XMLNode(ndChild.mstrName,null,null);
				addChild(nd);
				iCount--;
			}
			addChild(ndChild);
		}
		else
		{
			String strNodeName = strPath.substring(0,iIndex).trim();
			String strSubPath = strPath.substring(iIndex + 1,strPath.length()).trim();
			XMLNode ndReturn = getChild(strNodeName,true);
			if(ndReturn != null && strSubPath.length() > 0)
				ndReturn.setChild(strSubPath,ndChild);
		}
	}
}
