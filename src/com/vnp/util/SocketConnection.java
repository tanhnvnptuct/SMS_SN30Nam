package com.vnp.util;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.DataInputStream;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2014</p>
 *
 * <p>Company: </p>
 *
 * @author Hungdv
 * @version 1.0
 */
public class SocketConnection
{
	private Socket mskClient = null;
	private String mstrSocketHostIp = "";
	private int mintSocketPort = -1;
	private DataInputStream mbufClient = null;

	private PrintWriter mSocketOutput = null;
	public SocketConnection(String strIP,int iPort) throws Exception
	{
		mstrSocketHostIp = strIP;
		mintSocketPort = iPort;
		initSocketConnection();
	}
	public void sendCommand(String strCmd) {
		mSocketOutput.println(strCmd);
	}
	public void initSocketConnection() throws Exception {
		try{
			close();
		}
		catch(Exception ex){
		}
		mskClient = new Socket(mstrSocketHostIp,mintSocketPort);
		mSocketOutput = new PrintWriter(mskClient.getOutputStream(),true);
		mbufClient = new DataInputStream(mskClient.getInputStream());
	}
	public String getResponse() throws Exception{
		return mbufClient.readUTF();
//		int ch = mbufClient.read();
//		while(ch == 0){
//			ch = mbufClient.read();
//		}
//		if(ch == -1){
//			initSocketConnection();
//		}
//		while(ch == 0){
//			ch = mbufClient.read();
//		}
//
//		StringBuffer line = new StringBuffer();
//		while(ch != 0){
//			if(ch == -1){
//				mbufClient.close();
//				break;
//			}
//			if((ch != 10) && (ch != 13)){
//				line.append((char)ch);
//			}
//			ch = mbufClient.read();
//		}
//		return line.toString();
	}
	public Socket getSocket() {
		return mskClient;
	}
	public DataInputStream getDataInputStream() {
		return mbufClient;
	}
	public void close() throws Exception
	{
		if(mbufClient != null)
			mbufClient.close();
		if(mSocketOutput != null){
			mSocketOutput.close();
		}
		if(mskClient != null){
			mskClient.close();
		}

	}

	public static void main(String args[]) {
		try{
			SocketConnection socket = new SocketConnection("190.10.10.5",1990);
			socket.sendCommand("REG|912579968|QT99");
			boolean bln = socket.getDataInputStream().readBoolean();
			System.out.print(bln);
			socket.close();
		}
		catch(Exception ex){
		}
	}

}
