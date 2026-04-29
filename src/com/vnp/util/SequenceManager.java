package com.vnp.util;

import java.util.*;
import java.io.*;
import com.fss.util.*;

public class SequenceManager
{
	private Properties prtSequence = new Properties();
	private int cachSize;

	public SequenceManager(String sequencePath) throws Exception
	{
		this(sequencePath, 100);
	}

	public SequenceManager(String sequencePath, int cachSize) throws IOException
	{
		this.cachSize = cachSize;
		if (sequencePath == null || sequencePath.equals(""))
		{
			throw new IOException("No sequence path found");
		}
		File flList = new File(sequencePath);
		if (!flList.exists())
		{
			throw new IOException("Sequence path not found");
		}
		String[] fileList = flList.list();
		String absolutePath = flList.getAbsolutePath();
		absolutePath = (!absolutePath.endsWith("\\") || !absolutePath.endsWith("/")) ? absolutePath + "/" : absolutePath;
		boolean havingSequence = false;
		for (int i = 0; i < fileList.length; i++)
		{
			String fileName = fileList[i];

			if (fileName.endsWith(".seq"))
			{
				Vector vtRow = new Vector();
				vtRow.add(fileName);
				File fl = new File(absolutePath + fileName);
				RandomAccessFile raf = new RandomAccessFile(fl, "rw");
				raf.getChannel().lock();
				vtRow.add(raf);
				vtRow.add(new Vector());
				prtSequence.put(fileName, vtRow);
				havingSequence = true;
			}
		}
		if (!havingSequence)
		{
			throw new IOException("No sequence found");
		}
	}

	/********************************************************/
	public long getSequence(String sequenceName) throws IOException
	{
		long rtValue = 0;
		Vector vtRowData = (Vector) prtSequence.get(sequenceName + ".seq");
		if (vtRowData == null)
		{
			throw new IOException("Cound not found the sequence with name " + sequenceName);
		}
		synchronized (vtRowData)
		{
			Vector vtValue = (Vector) vtRowData.elementAt(2);
			if (vtValue.size() == 0)
			{
				readSequenceFromFile(sequenceName);
			}
			if (vtValue.size() == 0)
			{
				throw new IOException("Could not extend value, the value reached to max");
			}
			rtValue = Long.parseLong(vtValue.elementAt(0).toString());
			vtValue.remove(0);
			return rtValue;
		}
	}

	/********************************************************/
	private void readSequenceFromFile(String sequenceName) throws IOException
	{
		RandomAccessFile raf = null;
		try
		{
			Vector vtRow = (Vector) prtSequence.get(sequenceName + ".seq");
			Vector vtValue = (Vector) vtRow.elementAt(2);
			raf = (RandomAccessFile) vtRow.elementAt(1);
			byte[] buff = new byte[(int) raf.length()];
			raf.seek(0);
			int iByteRead = raf.read(buff);
			String strValue = new String(buff, 0, iByteRead);
			String[] elements = strValue.split(":");
			if (elements.length != 4)
			{
				throw new IOException("Invalid data in sequence " + sequenceName + " length: " + elements.length + ", length: " + raf.length() + ", byteread: " + iByteRead);
			}
			long currentVal = Long.parseLong(elements[0]);
			long maxValue = Long.parseLong(elements[1]);
			int step = Integer.parseInt(elements[2]);
			String recursive = elements[3];

			if (currentVal >= maxValue)
			{
				if (!recursive.equals("Y"))
				{
					return;
				}
				else
				{
					currentVal = currentVal % step;
				}
			}

			for (int j = 0; j < cachSize; j++)
			{
				vtValue.add(String.valueOf(currentVal));
				currentVal += step;
				if (currentVal > maxValue)
				{
					break;
				}
			}
			String writeBack = currentVal + ":" + maxValue + ":" + step + ":" + recursive;
			raf.setLength(writeBack.length());
			raf.seek(0);
			raf.write(writeBack.getBytes());
		}
		catch (IOException ex)
		{
			raf.close();
			throw ex;
		}
	}

	public void close()
	{
		Enumeration enumSequence = prtSequence.keys();
		while (enumSequence.hasMoreElements())
		{
			Vector vtRow = (Vector) prtSequence.get(enumSequence.nextElement());
			RandomAccessFile raf = (RandomAccessFile) vtRow.elementAt(1);
			try
			{
				raf.close();
				raf = null;
			}
			catch (Exception e)
			{}
		}
	}

	public void finalize()
	{
		close();
	}
}
