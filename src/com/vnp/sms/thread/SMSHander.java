package com.vnp.sms.thread;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.fss.thread.*;
import com.fss.util.*;
import com.logica.smpp.pdu.*;
import com.vnp.util.*;
import java.text.SimpleDateFormat;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class SMSHander extends ManageableThreadEx
{

	private ServerSocket msck;
	int miPort = 1111;
	int bufferSize = 50000;
	private Socket clientSocket;
	private static Executor exec = Executors.newCachedThreadPool();
	Vector mvtBlacklist = new Vector();
	private Blacklist blst;
	protected void processSession()
	{
		try{
			msck = new ServerSocket(miPort);
			logMonitor("Start listening on " + miPort);
			blst = new Blacklist();
			blst.start();
			while(miThreadCommand != ThreadConstant.THREAD_STOP && true){ // ((inputLine = in.readLine()) != null) {
				clientSocket = msck.accept();
				logMonitor("Start receving from " + clientSocket.getInetAddress().getHostAddress());
				exec.execute(new SocketReceiver(clientSocket)); //Create new Thread to handler this client's connection
			}
			blst.requireStop();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public class Blacklist extends Thread
	{
		boolean blStop = false;
		Date lastDate = new Date() ;
		boolean blFist = true;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		public void requireStop()
		{
			blStop = true;
		}

		public void run()
		{
			blStop = false;
			while(!blStop){
				//10 phut load lai file txt 1 lan
				Date date = new Date();
				long lTime = date.getTime() - lastDate.getTime();
				if(lTime / 1000 / 60 >= 5 || blFist){
					// doc file
					String strFileName = AppServer.getDictionaryNode("Thread.Blackfile").getValue();
					try{
						BufferedReader in = new BufferedReader(new FileReader(strFileName));
						String strLine = null;
						mvtBlacklist = new Vector();
						while((strLine = in.readLine()) != null){
							mvtBlacklist.add(strLine);
						}
						in.close();
						blFist = false;
						lastDate = new Date();
					}
					catch(Exception ex1){
						ex1.printStackTrace();
					}
				}
				else{
					try{
						Thread.sleep(10000);
					}
					catch(InterruptedException ex){
					}
				}

			}
		}
	}

	public boolean inBlacklist(String strIsdn)
	{
		boolean bl = false;
		for(int i = 0;i < mvtBlacklist.size();i++){
			if(mvtBlacklist.elementAt(i).equals(strIsdn)){
				return true;
			}
		}
		return bl;
	}

	public class SocketReceiver implements Runnable
	{
		private final Socket clientSocket;

		public SocketReceiver(Socket clsk)
		{
			clientSocket = clsk;
		}

		public void run()
		{
			byte[] inputbytes = new byte[bufferSize];
			InputStream in = null;
			OutputStream outsk;
			try{
				in = clientSocket.getInputStream();
				while(true){
					if(in.available() != 0){
						int i_number = in.read(inputbytes); //Read client's data
						byte[] data = new byte[i_number];
						System.arraycopy(inputbytes,0,data,0,i_number);
						String strData = new String(data);
						logMonitor("Received.:" + strData);
						int isize = getThreadManagerEx().queueSubmit.size();
						logMonitor("Queue size: " + isize);
						process(strData);
						// Detect terminated signal by Input
						if(strData.toUpperCase().endsWith("QUIT")){
							logMonitor("exit program");
							break;
						}
					}
					else{
						try{
							fillLogFile();
						}
						catch(AppException ex2){
						}
						Thread.sleep(20L);
					}
				}
			}
			catch(IOException ex){ // Neu client tu close thi se vao day, ==> finally luc nao cung se giai phong socket
			}
			catch(InterruptedException ex){
				/** @todo Handle this exception */
			}
			finally{
				try{
					clientSocket.close();
					logMonitor("exit program");
				}
				catch(IOException ex1){
				}
			}

		}
	}

	public void process(String strData)
	{
		String arrData[] = strData.split(";");
		String isdn;
		String message;
		String temp2;
		String temp[];
		for(int i = 0;i < arrData.length;i++){
			temp2 = arrData[i].trim();
			temp = temp2.split("#");
			if(temp.length >= 2){
				isdn = temp[0];
				message = temp[1];
				try{
					if(!inBlacklist(isdn)){
						submitMessage(isdn,message);
					}
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}

	protected void submitMessage(String strIsdn,String strMessage) throws Exception
	{
		SubmitSM submitSM = new SubmitSM();
		submitSM.setShortMessage(strMessage);
		submitSM.getDestAddr().setAddress(strIsdn);
		submitSM.getSourceAddr().setAddress("900");
		SubmitMessage submitMessage = new SubmitMessage(submitSM,false);
		getThreadManagerEx().queueSubmit.enqueue(submitMessage);
	}

	public Vector getParameterDefinition()
	{
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("port-listener","",ParameterType.PARAM_TEXTBOX_MASK,"99999",""));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}

	public void fillParameter() throws AppException
	{
		miPort = loadUnsignedInteger("port-listener");

		super.fillParameter();
	}


}
