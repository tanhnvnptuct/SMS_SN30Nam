package com.vnp.util;

import com.fss.util.StringUtil;

/**
 * <p>
 * Title:
 * </p>
 *
 * <p>
 * Description:
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 *
 * <p>
 * Company:
 * </p>
 *
 * @author hungdv
 * @version 1.0
 * @since 03/04/2014
 */
public class ServiceUtil {
	public ServiceUtil() {
	}

	public static long getMoneyMinus(String strBalance, String strMoneyAdd, String remainBalance) {
		long lngMoneyAdd = Long.parseLong(strMoneyAdd);
		long lngCredit = Long.parseLong(strBalance);
		long lRemain = Long.parseLong(remainBalance);
		if (lngCredit >= lngMoneyAdd + lRemain) {
			lngMoneyAdd = -lngMoneyAdd;
		} else if (lngCredit < lngMoneyAdd + lRemain && lngCredit >= lngMoneyAdd) {
			lngMoneyAdd = -(lngCredit - lRemain);
		}
		return lngMoneyAdd;
	}

	public static long getMoneyMinus(String strBalance, String strMoneyAdd) {
		long lngMoneyAdd = Long.parseLong(strMoneyAdd);
		long lngCredit = Long.parseLong(strBalance);
		if (lngCredit >= lngMoneyAdd + 100) {
			lngMoneyAdd = -lngMoneyAdd;
		} else if (lngCredit < lngMoneyAdd + 100 && lngCredit >= lngMoneyAdd) {
			lngMoneyAdd = -(lngCredit - 100);
		}
		return lngMoneyAdd;
	}

	public static String getMoneyMinus(String strBalance, double plngMoneyAdd) {
		double lngMoneyAdd = plngMoneyAdd;
		double lngCredit = Double.parseDouble(strBalance);
		if (lngCredit >= lngMoneyAdd + 100) {
			lngMoneyAdd = -lngMoneyAdd;
		} else if (lngCredit < lngMoneyAdd + 100 && lngCredit >= lngMoneyAdd) {
			lngMoneyAdd = -(lngCredit - 100);
		}
		if (lngMoneyAdd == -0) {
			lngMoneyAdd = 0;
		}
		return StringUtil.format(lngMoneyAdd,"#######");
	}

	public static void main(String args[]) {		
		
		System.out.println( getMoneyMinus("109988880000", 10000.23));
		
	}
}
