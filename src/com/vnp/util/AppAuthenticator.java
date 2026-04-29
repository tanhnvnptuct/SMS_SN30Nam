package com.vnp.util;

import com.fss.util.*;
import java.net.InetAddress;
import java.util.Hashtable;

/******************************************************************************
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: FSS-FPT</p>
 * @Author VinhBT
 * @version 1.0
 ******************************************************************************/


public class AppAuthenticator extends DBTableAuthenticator
{
	Hashtable prtCatche = new Hashtable();
	public String verifyPassword(String strUserName, String strPassword) throws Exception
	{
		if (!strUserName.toUpperCase().equals("ADMIN"))
		{
//			setAuthenticateHost(AppServer.getDictionaryNode("Authen.Url").getValue());
		}
		return
			super.verifyPassword(strUserName, strPassword);
	}

	public void checkIP(InetAddress ipaddress, String strUserID) throws Exception
	{
	}

	public boolean checkAccessTime(String strUserID)
	{
		return true;
	}

	public Hashtable getPermissionCache()
	{
		return prtCatche;
	}
}
