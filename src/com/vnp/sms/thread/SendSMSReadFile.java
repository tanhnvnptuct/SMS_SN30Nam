package com.vnp.sms.thread;

import java.io.*;
import java.util.*;

import com.fss.thread.*;
import com.fss.util.*;
import com.logica.smpp.pdu.SubmitSM;
import com.vnp.sms.thread.*;
import com.vnp.util.SubmitMessage;
import com.fss.sql.OracleConnectionFactory;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
public class SendSMSReadFile extends ManageableThreadEx {
	protected String mstrImportDir;
	protected String mstrBackupDir;
	protected String mstrLogDir;
	protected String mstrWildcard = ".txt";
	boolean mblDeleteFile = false;
	int miRecordCount;
	int miRecordFail;
	int miImportRecord;
	int miMaxQueueSize = 100;
	

	public void fillParameter() throws AppException {
		////////////////////////////////////////////////////////
		// Fill parameter
		////////////////////////////////////////////////////////
		mstrImportDir = loadDirectory("ImportDir", true, true);
		mstrBackupDir = loadDirectory("BackupDir", true, false);
		mstrWildcard = this.loadMandatory("WildCard");
		String strIsDeleteFile = this.loadYesNo("DeleteFile");
		mblDeleteFile = strIsDeleteFile.equals("Y");
		miMaxQueueSize = loadInteger("MaxQueueSize");
		mstrLogDir = loadDirectory("LogDir", true, true);

		super.fillParameter();
	}

	////////////////////////////////////////////////////////
	// Override
	////////////////////////////////////////////////////////
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("MaxQueueSize", "100", ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(createParameterDefinition("ImportDir", "", ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(createParameterDefinition("BackupDir", "", ParameterType.PARAM_TEXTBOX_MAX, "100"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("WildCard", "", ParameterType.PARAM_TEXTBOX_MAX, "99"));
		vtReturn.addElement(ParameterUtil.createParameterDefinition("DeleteFile", "", ParameterType.PARAM_TEXTBOX_MAX, "99"));

		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}

	public void processSession() throws Exception {

		while (miThreadCommand != ThreadConstant.THREAD_STOP) {
								
			// List file and convert
			File fl = new File(mstrImportDir);
			String strFileList[] = fl.list(new WildcardFilter(mstrWildcard));
			if (strFileList != null && strFileList.length > 0) {
				Arrays.sort(strFileList);
				int iFileCount = strFileList.length;
				for (int iFileIndex = 0; iFileIndex < iFileCount && miThreadCommand != ThreadConstant.THREAD_STOP; iFileIndex++) {
					if (validateFile(strFileList[iFileIndex])) {
						try {
							importFile(strFileList[iFileIndex]);
						}
						catch (Exception e) {
							logMonitor("Error occured: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
			try {
				Thread.sleep(10000);
				logMonitor("Khong co file day ra");
				fillLogFile();
			}
			catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}

	}

	protected boolean validateFile(String strFileName) throws Exception {
		File flTemp = new File(mstrImportDir, strFileName);
		if (flTemp.isDirectory()) { return false; }
		return true;
	}

	public void importFile(String strFileName) throws Exception {
		logMonitor("Start Import File " + strFileName);
		//tien hanh xu ly file
		String strLine = null;
		///////////////////init Value/////////////////////
		miRecordCount = 0;
		miRecordFail = 0;
		miImportRecord = 0;

		PreparedStatement stmtDelete = null;		
		
		BufferedReader in = new BufferedReader(new FileReader(mstrImportDir + strFileName));
		// Read file
		Vector vtTemp = new Vector();
		while ((strLine = in.readLine()) != null) {
			miRecordCount++;
			String[] arr = StringUtil.toStringArray(strLine, "|");
			String id = arr[0];
			String strIsdn = arr[1];
			String strSender = arr[2];
			String strMessage = arr[3];
			//day vao queue de cong khuyen mai
			submitMessage(strIsdn, strSender, strMessage);
			
			try{
				stmtDelete = this.mcnMain.prepareStatement("update sms_mt set sent_time = sysdate where id = ?");
				stmtDelete.setString(1,id);
				stmtDelete.executeUpdate();
				this.mcnMain.commit();
			}
			catch (Exception e)
		    {
		      e.printStackTrace();
		      logMonitor("Loi ket noi DB cap nhat trang thai da nhan tin!");
		    }
		}
		in.close();
		String msg = "Total of record          : " + miRecordCount;

		logMonitor(msg);
		logMonitor("End process file: " + strFileName);

		afterImport(mstrImportDir, strFileName);

	}

	protected void submitMessage(String strIsdn, String mstrSender, String strMessage) throws Exception {
		while (getThreadManagerEx().queueSubmit.size() > 500) {
			Thread.sleep(100);
			logMonitor("Queue full : size=" + getThreadManagerEx().queueSubmit.size());
		}
		logMonitor(strIsdn + " : " + strMessage);
		SubmitSM submitSM = new SubmitSM();
		submitSM.setShortMessage(strMessage);
		submitSM.getDestAddr().setAddress(strIsdn);
		submitSM.getSourceAddr().setAddress(mstrSender);
		SubmitMessage submitMessage = new SubmitMessage(submitSM, false);
		getThreadManagerEx().queueSubmit.enqueue(submitMessage);
	}

	public void afterImport(String strPath, String strFileName) throws Exception {
		fileConvertedHandler(strPath, strFileName);
		//updateDatabase();
	}

	////////////////////////////////////////////////////////////////
	// Muc dich: xu ly voi file da convert
	// Date: 20/10/2003
	////////////////////////////////////////////////////////////////
	private void fileConvertedHandler(String strPath, String strFileName) throws AppException {
		if (mblDeleteFile) {
			FileUtil.deleteFile(strPath + strFileName);
		} else {
			if (!mstrBackupDir.equals("")) {
				if (!FileUtil.renameFile(strPath + strFileName, mstrBackupDir
						+ strFileName)) { throw new AppException("Cannot rename file " + strPath + strFileName + " to " + mstrBackupDir + strFileName); }
			} else {
				FileUtil.deleteFile(strPath + strFileName);
			}
		}

	}

}
