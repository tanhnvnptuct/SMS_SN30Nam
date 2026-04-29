package com.vnp.util;

public class VasProApi {
	private String appName = "CRS";
	private String userName = "BIGPRO";
	private String userNote = "BIGII2017";
	private String clientIp = "10.149.248.67";
	private String clientApp = "IOS";

	private final String vasapi_url = "http://10.1.10.173/vascmd/vasprovisioning/api";
	private final int timeout = 10000;

	private ResponseUtil resUtil;

	public VasProApi() {
		super();
		resUtil = new ResponseUtil();
	}

	public String subscriber(String reqid, String msisdn, String service, String s_package, String promotion,
			String trial, String usernote) throws Exception {
		String logcotent = "VasProvisioning.subscriber:" + reqid + ";" + msisdn + ";" + service + ";" + s_package + ";"
				+ promotion + ";" + trial + ";" + userName + ";" + clientIp + ";" + usernote + ";";
		String return_ = "";

		String request = "<RQST>\n" + "   <name>subscribe</name>\n" + "   <requestid>" + reqid + "</requestid>\n"
				+ "   <msisdn>" + msisdn + "</msisdn>\n" + "   <service>" + service + "</service>\n" + "   <package>"
				+ s_package + "</package>\n" + "   <promotion>" + promotion + "</promotion>\n" + "   <trial>" + trial
				+ "</trial>\n" + "   <note>" + usernote + "</note>\n" + "   <username>" + userName + "</username>\n"
				+ "   <userip>" + clientIp + "</userip>\n" + "   <application>" + clientApp + "</application>\n"
				+ "   <channel>API</channel>\n" + "</RQST>";
		return_ = resUtil.getHttpPost(request, vasapi_url, timeout);
		String resultCode = return_.substring(return_.indexOf("<error>") + "<error>".length(),
				return_.indexOf("</error>"));

		return resultCode;

	}

	public String unsubscriber(String app, String reqid, String msisdn, String service, String s_package,
			String username, String userip, String usernote, String application) throws Exception {
		String logcotent = "VasProvisioning.unsubscriber:" + reqid + ";" + msisdn + ";" + service + ";" + s_package
				+ ";" + username + ";" + userip + ";" + usernote + ";";
		String return_ = "";

		String request = "<RQST>\n" + "   <name>unsubscribe</name>\n" + "   <requestid>" + reqid + "</requestid>\n"
				+ "   <msisdn>" + msisdn + "</msisdn>\n" + "   <service>" + service + "</service>\n" + "   <package>"
				+ s_package + "</package>\n" + "   <username>" + username + "</username>\n" + "   <userip>" + userip
				+ "</userip>\n" + "   <application>" + application + "</application>\n" + "   <channel>API</channel>\n"
				+ "</RQST>";
		return_ = resUtil.getHttpPost(request, vasapi_url, timeout);
		String resultCode = return_.substring(return_.indexOf("<error>") + "<error>".length(),
				return_.indexOf("</error>"));

		return resultCode;

	}

	public String getstatus(String reqid, String msisdn, String service, String s_package) throws Exception {
		String return_ = "";
		String ret;

		String request = "<RQST>\n" + "   <name>getstatus</name>\n" + "   <requestid>" + reqid + "</requestid>\n"
				+ "   <msisdn>" + msisdn + "</msisdn>\n" + "   <service>" + service + "</service>\n" + "   <package>"
				+ s_package + "</package>\n" + "   <sourcetype>1</sourcetype> " + "   <username>" + userName
				+ "</username>\n" + "   <userip>" + clientIp + "</userip>\n" + "   <application>" + clientApp
				+ "</application>\n" + "   <channel>API</channel>\n" + "</RQST>";
		return_ = resUtil.getHttpPost(request, vasapi_url, timeout);
		if (return_ == null || return_.equals("")) {
			return "-2|null|null|null|null|null"; 
		}
		String error = return_.substring(return_.indexOf("<error>") + "<error>".length(), return_.indexOf("</error>"));
		if (error.equals("0")) { 
			// status
			String status = return_.substring(return_.indexOf("<status>") + "<status>".length(),
					return_.indexOf("</status>"));
			// last_time_subscribe
			String last_time_subscribe = "";
			try {
				last_time_subscribe = return_.substring(
						return_.indexOf("<last_time_subscribe>") + "<last_time_subscribe>".length(),
						return_.indexOf("</last_time_subscribe>"));
			} catch (Exception ex) {
			}
			// last_time_unsubscribe
			String last_time_unsubscribe = "";
			try {
				last_time_unsubscribe = return_.substring(
						return_.indexOf("<last_time_unsubscribe>") + "<last_time_unsubscribe>".length(),
						return_.indexOf("</last_time_unsubscribe>"));
			} catch (Exception ex) {
			}
			// last_time_renew
			String last_time_renew = "";
			try {
				last_time_renew = return_.substring(return_.indexOf("<last_time_renew>") + "<last_time_renew>".length(),
						return_.indexOf("</last_time_renew>"));
			} catch (Exception ex) {
			}
			// last_time_retry
			String last_time_retry = "";
			try {
				last_time_retry = return_.substring(return_.indexOf("<last_time_retry>") + "<last_time_retry>".length(),
						return_.indexOf("</last_time_retry>"));
			} catch (Exception ex) {
			}
			// expiredtime
			String expiredtime = "";
			try {
				expiredtime = return_.substring(return_.indexOf("<expiredtime>") + "<expiredtime>".length(),
						return_.indexOf("</expiredtime>"));
			} catch (Exception ex) {
			}

			ret = status + "|" + last_time_subscribe + "|" + last_time_unsubscribe + "|" + last_time_renew + "|"
					+ last_time_retry + "|" + expiredtime;

			return ret;
		} else {
			ret = error + "|null|null|null|null|null"; 

			return ret;
		}

	}

	public boolean isActiveMI(String app, String reqid, String msisdn, String username, String userip)
			throws Exception {
		String logcotent = "VasProvisioning.getstatusservice:" + reqid + ";" + msisdn + ";MI;" + username + ";" + userip
				+ ";";
		String return_ = "";

		String request = "<RQST>\n" + "<name>getstatusservice</name>\n" + "<requestid>" + reqid + "</requestid>\n"
				+ "<msisdn>" + msisdn + "</msisdn>\n" + "<service>MI</service>\n" + "<application>IOS</application>\n"
				+ "<username>" + username + "</username>\n" + "<userip>" + userip + "</userip>\n"
				+ "<channel>API</channel>\n" + "</RQST>";
		return_ = resUtil.getHttpPost(request, vasapi_url, timeout);
		try {
			String extra_information = return_.substring(
					return_.indexOf("<extra_information>") + "<extra_information>".length(),
					return_.indexOf("</extra_information>"));

			String active_pkg = extra_information.split("\\|")[2];
			return true;
		} catch (Exception ex) {
			return false;
		}

	}

}
