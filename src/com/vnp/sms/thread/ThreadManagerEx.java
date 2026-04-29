package com.vnp.sms.thread;

import java.util.Vector;

import com.fss.queue.MessageQueue;
/*************************************************************
 * <p>Copyright: Copyright (c) 2004 FTL-FSS-FPT</p>
 * <p>Description: Class extends from ThreadManager </p>
 * @author VinhBT
 * @since 09/05/2006
 ************************************************************/
import com.fss.thread.ManageableThread;
import com.fss.thread.ProcessorListener;
import com.fss.thread.ThreadManager;
import com.logica.smpp.util.Queue;

public class ThreadManagerEx extends ThreadManager {

	public Queue queueSubmit; //submit message
	public com.fss.queue.Queue queueReceiver;	
	public Queue queueAssignAlacart;
	public Queue queueVD10Assign;
	public Queue queueVD10AssignLog;
	public Queue queueKMChan1C;
	public Queue queueKMChan2C;
	public Queue queueKMChan1CLog;
	public Queue queueKMChan2CLog;

	public static final int STATUS_NONE = -1;
	public static final int STARTING_STATUS = 0;
	public static final int STARTED_STATUS = 1;
	public static final int STOPPING_STATUS = 2;
	public static final int STOPPED_STATUS = 3;
	private int mintStatus = STATUS_NONE;

	public int getStatus() {
		return mintStatus;
	}

	public void setStatus(int pintStatus) {
		mintStatus = pintStatus;
	}

	/*************************************************************
	 * Description: Override function
	 * @param
	 * @return
	 * @throws
	 * @modifier HoaiPTT
	 *************************************************************/

	public ThreadManagerEx(int iPort, ProcessorListener lsn) throws Exception {
		super(iPort, lsn);
		String[] strMessage3 = new String[1];
		strMessage3[0] = "DataType";
		queueReceiver = new com.fss.queue.Queue(strMessage3);
		queueSubmit = new Queue();
		queueAssignAlacart = new Queue();
		queueVD10Assign = new Queue();
		queueVD10AssignLog = new Queue();
		queueKMChan1C = new Queue();
		queueKMChan1CLog = new Queue();
		queueKMChan2C = new Queue();
		queueKMChan2CLog = new Queue();
		
	}

	public void stopThread(ManageableThread thread) {
		super.stopThread(thread);
	}

}
