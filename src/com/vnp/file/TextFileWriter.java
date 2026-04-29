package com.vnp.file;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import smartlib.util.FileUtil;

public class TextFileWriter
  extends TextFile
{
  public int mintCount = 0;
  public FileOutputStream mFile = null;
  public BufferedOutputStream mBuffer = null;
  
  public void openFile(String strFilePath, int intBuffer)
    throws Exception
  {
    openFile(strFilePath, intBuffer, false);
  }
  
  public void openFile(String strFilePath, int intBuffer, boolean blnAppend)
    throws Exception
  {
    this.mintCount = 0;
    this.mstrFilePath = strFilePath;
    try
    {
      this.mFile = new FileOutputStream(strFilePath, blnAppend);
      this.mBuffer = new BufferedOutputStream(this.mFile, intBuffer);
    }
    catch (Exception e)
    {
      safeCloseFile();
      throw e;
    }
  }
  
  public long getCount()
  {
    return this.mintCount;
  }
  
  public void addText(String strText)
    throws Exception
  {
    this.mintCount += 1;
    strText = strText + '\n';
    try
    {
      this.mBuffer.write(strText.getBytes());
    }
    catch (Exception e)
    {
      throw e;
    }
  }
  
  public void clear()
    throws Exception
  {
    if ((this.mstrFilePath != null) && (!this.mstrFilePath.equals(""))) {
      try
      {
        closeFile();
      }
      catch (Exception e)
      {
        safeCloseFile();
        throw e;
      }
      finally
      {
        FileUtil.deleteFile(this.mstrFilePath);
      }
    }
  }
  
  public void closeFile()
    throws Exception
  {
    this.mintCount = 0;
    try
    {
      this.mBuffer.flush();
      this.mBuffer.close();
      this.mFile.close();
    }
    catch (Exception e)
    {
      throw e;
    }
    finally
    {
      safeCloseFile();
    }
  }
  
  public void safeCloseFile()
    throws Exception
  {
    this.mintCount = 0;
    FileUtil.safeClose(this.mBuffer);
    FileUtil.safeClose(this.mFile);
  }
}

