package com.vnp.file;

public abstract class TextFile
{
  public String mstrFilePath = new String();
  
  public abstract void openFile(String paramString, int paramInt)
    throws Exception;
  
  public abstract void closeFile()
    throws Exception;
  
  public abstract void safeCloseFile()
    throws Exception;
}
