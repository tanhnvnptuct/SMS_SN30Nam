package com.vnp.util;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MDate
{

	//Contructor
	public MDate()
	{

	}

	public static String GetDate()
	{
		String DATE = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
		return DATE;
	}

	public static String GetDate(String sFormat)
	{
		String DATE = new SimpleDateFormat(sFormat).format(new Date());
		return DATE;
	}

	public static String GetDateFormat(Date sDate,String sFormat)
	{
		String DATE = new SimpleDateFormat(sFormat).format(new Date());
		return DATE;
	}

	public Date StringToDate(String sDate)
	{
		Date ti = new Date();
		ti.setYear(Integer.parseInt(sDate.substring(0,4)) - 1900);
		ti.setMonth(Integer.parseInt(sDate.substring(4,6)) - 1);
		ti.setDate(Integer.parseInt(sDate.substring(6,8)));
		ti.setHours(Integer.parseInt(sDate.substring(8,10)));
		ti.setMinutes(Integer.parseInt(sDate.substring(10,12)));
		ti.setSeconds(Integer.parseInt(sDate.substring(12,14)));
		return ti;
	}

	public static Date GetDateTime(Date ti,String sDate,int iDate)
	{
		//if(iSet == 1)
		//{
		ti.setYear(Integer.parseInt(sDate.substring(0,4)) - 1900);
		ti.setMonth(Integer.parseInt(sDate.substring(4,6)) - 1);
		ti.setDate(Integer.parseInt(sDate.substring(6,8)));
		//}
		ti.setDate(ti.getDate() + iDate);
		return ti;
	}

	public static String IncreaseDate(String sDate,String sDateFormat,int iDate) //throws IOException,ParseException
	{
		Date ti = new Date();
		ti = GetDateTime(ti,sDate,iDate);
		sDate = new SimpleDateFormat(sDateFormat).format(ti);
		return sDate;
	}

	public Date GetDateTime2(Date ti,String sDate,int iDate)
	{

		ti.setDate(Integer.parseInt(sDate.substring(0,2)));
		ti.setMonth(Integer.parseInt(sDate.substring(3,5)) - 1);

		ti.setYear(Integer.parseInt(sDate.substring(6,10)) - 1900);

		//}
		ti.setDate(ti.getDate() + iDate);
		ti.setHours(Integer.parseInt(sDate.substring(11,13)));
		ti.setMinutes(Integer.parseInt(sDate.substring(14,16)));
		ti.setSeconds(Integer.parseInt(sDate.substring(17,19)));
		return ti;
	}

	public String IncreaseDate2(String sDate,String sDateFormat,int iDate) //throws IOException,ParseException
	{
		Date ti = new Date();
		ti = GetDateTime2(ti,sDate,iDate);
		sDate = new SimpleDateFormat(sDateFormat).format(ti);
		return sDate;
	}

	public Date ToDate(Date ti,String sDate)
	{
		ti.setYear(Integer.parseInt(sDate.substring(0,4)) - 1900);
		ti.setMonth(Integer.parseInt(sDate.substring(4,6)) - 1);
		ti.setDate(Integer.parseInt(sDate.substring(6,8)));
		return ti;
	}

	public String CreateDateFormat(String sDate,String sDateFormat) //throws IOException,ParseException
	{
		Date ti = new Date();
		ti = ToDate(ti,sDate);
		sDate = new SimpleDateFormat(sDateFormat).format(ti);
		return sDate;
	}

	public String ToFormat(String sDate,String sfrom,String dfrom) throws ParseException
	{
		String DATE = new SimpleDateFormat(dfrom).format((new SimpleDateFormat(sfrom)).parse(sDate));
		return DATE;
	}

	public static long GetHourDistance(String[] argt)
	{
		Date d1 = new GregorianCalendar(Integer.parseInt(argt[0]),
										Integer.parseInt(argt[1]) - 1,
										Integer.parseInt(argt[2]),
										Integer.parseInt(argt[3]),
										Integer.parseInt(argt[4]),
										Integer.parseInt(argt[5])).getTime();
		// Today's date
		Date d2 = new Date();
		// Get msec from each, and subtract.
		long diff = d2.getTime() - d1.getTime();
		//System.out.println("Difference between " + d2 + "\n" + "and " + d1 + " is " +
		//                                       (diff / ONE_HOUR) + " hours.");
		return diff / ONE_HOUR;
	}

	public static long GetHourDistance(String[] argt,String[] argt2)
	{
		Date d1 = new GregorianCalendar(Integer.parseInt(argt[0]),
										Integer.parseInt(argt[1]) - 1,
										Integer.parseInt(argt[2]),
										Integer.parseInt(argt[3]),
										Integer.parseInt(argt[4]),
										Integer.parseInt(argt[5])).getTime();
		// Today's date
		//Date d2 = new Date( );
		Date d2 = new GregorianCalendar(Integer.parseInt(argt2[0]),
										Integer.parseInt(argt2[1]) - 1,
										Integer.parseInt(argt2[2]),
										Integer.parseInt(argt2[3]),
										Integer.parseInt(argt2[4]),
										Integer.parseInt(argt2[5])).getTime();
		// Get msec from each, and subtract.
		long diff = d2.getTime() - d1.getTime();
		//System.out.println("Difference between " + d2 + "\n" + "and " + d1 + " is " +
		//                                       (diff / ONE_HOUR) + " hours.");
		return diff / ONE_HOUR;
	}

	public static long GetDayDistance(String[] argt)
	{
		Date d1 = new GregorianCalendar(Integer.parseInt(argt[0]),
										Integer.parseInt(argt[1]) - 1,
										Integer.parseInt(argt[2]),
										Integer.parseInt(argt[3]),
										Integer.parseInt(argt[4]),
										Integer.parseInt(argt[5])).getTime();
		// Today's date
		Date d2 = new Date();
		// Get msec from each, and subtract.
		long diff = d2.getTime() - d1.getTime();
		System.out.println("Difference between " + d2 + "\n" + "and " + d1 + " is " +
						   (diff / ONE_DAY) + " hours.");
		return diff / ONE_DAY;
	}

	public long GetDayDistance(String sDay1,String sDay2)
	{
		return daysBetween(StringToDate(sDay1),StringToDate(sDay2));
	}

	void TestDate(String args[])
	{
		Calendar c1 = new GregorianCalendar();
		Calendar c2 = new GregorianCalendar();
		c1.set(Integer.parseInt(args[0]),
			   Integer.parseInt(args[1]) - 1,
			   Integer.parseInt(args[2]),0,0,0);
		c2.set(Integer.parseInt(args[3]),
			   Integer.parseInt(args[4]) - 1,
			   Integer.parseInt(args[5]),0,0,0);
		System.out.println(daysBetween(c1.getTime(),c2.getTime()) +
						   " day(s) between " + args[0] + "-" + args[1] + "-" +
						   args[2] + " and " + args[3] + "-" + args[4] + "-" + args[5]);
	}

	static final long ONE_HOUR = 60 * 60 * 1000L;

	static final long ONE_DAY = 60 * 60 * 24 * 1000L;

	public long daysBetween(Date d1,Date d2)
	{
		return((d2.getTime() - d1.getTime()) / (ONE_HOUR * 24));
		//return ((d2.getTime() - d1.getTime() + ONE_HOUR) / (ONE_HOUR * 24));
		//return (long)(d2.getDate() - d1.getDate());
	}

	public long getWorkDays(Date startDt,Date endDt)
	{
		Calendar startCal,endCal;
		startCal = Calendar.getInstance();
		startCal.setTime(startDt);
		endCal = Calendar.getInstance();
		endCal.setTime(endDt);
		int workDays = 0;

		//Return 0 if start and end are the same
		if(startCal.getTimeInMillis() == endCal.getTimeInMillis()){
			return 0;
		}
		//Just in case the dates were transposed this prevents infinite loop
		if(startCal.getTimeInMillis() > endCal.getTimeInMillis()){
			startCal.setTime(endDt);
			endCal.setTime(startDt);
		}

		do{
			startCal.add(Calendar.DAY_OF_MONTH,1);
			if(startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY){
				++workDays;
			}
		}
		while(startCal.getTimeInMillis() < endCal.getTimeInMillis());

		return(long)workDays;
	}

}

/*
 public static void main(String args[])
 {

//  V� du. g� tu+` command line:

//  java TestDate 1999 0 1 1999 8 1

//  ?e^? la^'y so^' ng�y tu+` 1999-01-01 ?e^'n 1999-09-01

//  (nho+' l� th�ng ?u+o+.c ?�nh tu+` 0...)

//
 String[] ListDate = {"2007","06","29","2007","07","01"};
 TestDate a = new TestDate(ListDate);

 String[] ListDate2 = {"2007","05","31","16","51","01"};
 long iHour = GetTimeDistance(ListDate2);




 }
 */

/*
  Date ti = new GregorianCalendar(Integer.parseInt(sDate.substring(0,4)),
  Integer.parseInt(sDate.substring(4,6)),
  Integer.parseInt(sDate.substring(6,8)),
  Integer.parseInt(sDate.substring(8,10)),
  Integer.parseInt(sDate.substring(10,12)),
  Integer.parseInt(sDate.substring(12,14))).getTime( );
 */
