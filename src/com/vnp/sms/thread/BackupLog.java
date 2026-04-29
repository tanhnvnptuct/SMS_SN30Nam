package com.vnp.sms.thread;

import java.io.File;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;

import com.fss.thread.ParameterType;
import com.fss.thread.ParameterUtil;
import com.fss.util.AppException;

public class BackupLog extends ManageableThreadEx
{
    private String oldDir = "";
    private String newDir = "";
    
	protected void processSession()
		    throws Exception
		  {
		    try
		    {
		      moveFile();		      		 
		    }
		    catch (Exception e)
		    {
		      logMonitor(e.getMessage());
		      e.printStackTrace();
		    }
		  }
	
	public void fillParameter()
		    throws AppException
		  {
		    this.oldDir = loadDirectory("Old Directory", true, true);
		    this.newDir = loadMandatory("New Directory");		   
		    super.fillParameter();
		  }
		  
		  public Vector getParameterDefinition()
		  {
		    Vector vtReturn = new Vector();
		    vtReturn.addElement(createParameterDefinition("Old Directory", "", ParameterType.PARAM_TEXTBOX_MAX, "300"));
		    vtReturn.addElement(createParameterDefinition("New Directory", "", ParameterType.PARAM_TEXTBOX_MAX, "300"));
		    
		    vtReturn.addAll(super.getParameterDefinition());
		    return vtReturn;
		  }
		  
	public void moveFile()
    {	
    	try{
    		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    		Date date = new Date(); 
    		//String filename = Integer.toString(date.getYear()) + Integer.toString(date.getMonth()) + Integer.toString(date.getDate());
    		String filename = dateFormat.format(date);
    		     		
    		
    	   
    	   File afile =new File(oldDir+filename+".log");
    	   logMonitor("Bat dau backup file: " + oldDir+filename+".log");
    	   
    	   if(afile.renameTo(new File(newDir + afile.getName()))){
    	//System.out.println("File is moved successful!");
    		 logMonitor("Backup file log: " + oldDir+filename+".log Successfully!");
    	   }else{
    		//System.out.println("File is failed to move!");
    		   logMonitor("Backup file log: " + oldDir+filename+".log fail!");
    	   }
    	    
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
	
}