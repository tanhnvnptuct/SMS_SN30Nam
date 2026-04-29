package com.vnp.util;

import java.util.Date;
import java.util.Calendar;

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
 * @author not attributable
 * @version 1.0
 */
public class MyDateUtil {
	public static Date firstDayOfThisMonth() {
		Calendar nowCal = Calendar.getInstance();
		int month = nowCal.get(Calendar.MONTH); // nowCal.get(Calendar.MONTH) +
												// 1;
		int year = nowCal.get(Calendar.YEAR);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		Date dueDate = new Date(cal.getTimeInMillis());
		return dueDate;
	}

	public static Date firstDayOfMonth(java.util.Date dtMonth) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dtMonth);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		Calendar cale = Calendar.getInstance();
		cale.clear();
		cale.set(Calendar.YEAR, year);
		cale.set(Calendar.MONTH, month);
		cale.set(Calendar.DAY_OF_MONTH, 1);
		Date dueDate = new Date(cale.getTimeInMillis());
		return dueDate;
	}

	public static Date lastDayOfThisMonth() {
		Calendar nowCal = Calendar.getInstance();
		int month = nowCal.get(Calendar.MONTH); // nowCal.get(Calendar.MONTH) +
												// 1;
		int year = nowCal.get(Calendar.YEAR);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.add(Calendar.MONTH, 1);
		cal.set(Calendar.DATE, 1);
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		Date dueDate = new Date(cal.getTimeInMillis());
		return dueDate;
	}

	public static Date lastDayOfThisMonth(Date date) {
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(date);
		int month = nowCal.get(Calendar.MONTH); // nowCal.get(Calendar.MONTH) +
												// 1;
		int year = nowCal.get(Calendar.YEAR);

		Calendar nextNotifTime = Calendar.getInstance();
		nextNotifTime.clear();
		nextNotifTime.set(Calendar.YEAR, year);
		nextNotifTime.set(Calendar.MONTH, month);
		nextNotifTime.add(Calendar.MONTH, 1);
		nextNotifTime.set(Calendar.DATE, 1);
		nextNotifTime.add(Calendar.DATE, -1);
		nextNotifTime.set(Calendar.HOUR, 23);
		nextNotifTime.set(Calendar.MINUTE, 59);
		nextNotifTime.set(Calendar.SECOND, 59);
		Date dueDate = new Date(nextNotifTime.getTimeInMillis());
		return dueDate;
	}

	public static int compare2Date(String strDate1, String strDate2) {
		String strTemp1 = strDate1.substring(6, 10) + strDate1.substring(3, 5) + strDate1.substring(0, 2);
		String strTemp2 = strDate2.substring(6, 10) + strDate2.substring(3, 5) + strDate2.substring(0, 2);
		if (Integer.parseInt(strTemp1) > Integer.parseInt(strTemp2)) {
			return 1;
		} else if (Integer.parseInt(strTemp1) == Integer.parseInt(strTemp2)) {
			return 0;
		} else {
			return -1;
		}
	}

	public static Date addDay(Date date, int numDay) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, numDay);
		return calendar.getTime();
	}

	public static Date addMonth(Date date, int iMonth) {
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(date);
		nowCal.add(Calendar.MONTH, iMonth);
		Date dueDate = new Date(nowCal.getTimeInMillis());
		return dueDate;
	}

	// ngay dau tien cua cach day iMonth Month
	public static Date firstDayOfNextMonth(int iMonth) {
		Calendar nowCal = Calendar.getInstance();
		int month = nowCal.get(Calendar.MONTH) + iMonth; // nowCal.get(Calendar.MONTH)
															// + 1;
		int year = nowCal.get(Calendar.YEAR);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		Date dueDate = new Date(cal.getTimeInMillis());
		return dueDate;
	}

	public static Date firstDayOfNextMonth(Date date) {
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(date);
		int month = nowCal.get(Calendar.MONTH) + 1; // nowCal.get(Calendar.MONTH)
													// + 1;
		int year = nowCal.get(Calendar.YEAR);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		Date dueDate = new Date(cal.getTimeInMillis());
		return dueDate;
	}

	public static Date trunc(Date dtDate) {
		Calendar nowCal = Calendar.getInstance();
		nowCal.setTime(dtDate);
		int month = nowCal.get(Calendar.MONTH);
		int year = nowCal.get(Calendar.YEAR);
		int day = nowCal.get(Calendar.DATE);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		Date dueDate = new Date(cal.getTimeInMillis());
		return dueDate;
	}

	public static java.util.Date endDayOf(java.util.Date dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();

	}

}
