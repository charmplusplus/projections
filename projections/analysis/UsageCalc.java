package projections.analysis;

import java.lang.*;
import java.io.*;
import projections.misc.*;
import java.util.*;

public class UsageCalc extends ProjDefs
{
	private long beginTime,endTime;
	private long startTime;
	private int pnum;
	private int dataLen;
	private long packtime,packstarttime;
	private long unpacktime,unpackstarttime;
	private int numUserEntries;
    
    // curEntry is global because it has to deal with the relationship
    // between BEGIN_PROCESSING and END_PROCESSING events within the
    // same log file.
    //
    // it needs, however, to be reset between the reading of two log files.
    private int curEntry = -1;
	
	private void intervalCalc(float[] data,int type, int entry, long time)
	{

	if (time<beginTime) time=beginTime;
	if (time>endTime) time=endTime;

	switch(type) {
	case BEGIN_PROCESSING:
		packtime = 0;
		unpacktime = 0;
		curEntry = entry;
		startTime = time;
		break;
	case END_PROCESSING:
	// curEntry == -1 means that there was no corresponding BEGIN_PROCESSING event, if so ignore the entrypoint
	    if(curEntry != -1)		
	    	data[curEntry] += (int)((time - startTime) - packtime - unpacktime);
	    break;

	case BEGIN_IDLE:
	    startTime = time;
		break;
	case END_IDLE:
	    data[numUserEntries] += (int)(time - startTime);
		break;

	case BEGIN_PACK:
		packstarttime = time;
		break;
	case END_PACK:
		packtime += time - packstarttime;
		data[numUserEntries+1] += (int)(time - packstarttime);
		break;

	case BEGIN_UNPACK:
	    unpackstarttime = time;
		break;
	case END_UNPACK:
		unpacktime += time - unpackstarttime;
		data[numUserEntries+2] += (int)(time - unpackstarttime);
		break;
	default:
		/*ignore it*/
	};
	}
	public float[] usage(StsReader sts,int procnum, long begintime, long endtime) 
	{
	long time;
	int type;
	int entry;
	int len;
	float total;
	beginTime = begintime;
	endTime = endtime;
	pnum = procnum;
	dataLen = sts.getEntryCount() + 4;
	numUserEntries = sts.getEntryCount();

	float[] data = new float[dataLen];

	try {
		FileReader file = new FileReader(sts.getLogName(pnum));
		AsciiIntegerReader log=new AsciiIntegerReader(new BufferedReader(file));
		curEntry = -1;
		log.nextLine(); // The first line contains junk
		//The second line gives the program start time
		log.nextInt();
	
		startTime = 0;
		time=0;
		try { while (time<endTime) { //EOF exception terminates loop
			log.nextLine();//Skip old junk at end of line
			type=log.nextInt();
			switch(type) {
			case BEGIN_IDLE: case END_IDLE:
			case BEGIN_PACK: case END_PACK:
			case BEGIN_UNPACK: case END_UNPACK:
				time = log.nextLong();
				intervalCalc(data,type, 0, (time));
				break;
			case BEGIN_PROCESSING: case END_PROCESSING:
				log.nextInt(); //skip message type
				entry = log.nextInt();
				time = log.nextLong();
				intervalCalc(data,type, entry, (time));
				break;
			default:
				/*Ignore it.*/
			}
		}} catch (EOFException e) {
			log.close();
		}
		catch (IOException e) {
			log.close();
		}
	}
	catch (IOException e)
	    {System.out.println("Exception while reading log file "+pnum); }
	total = 0;
	for(int j=0; j<(dataLen-1); j++) //Scale times to percent
		data[j] = 100*data[j]/(endTime-beginTime);
	return data;
	}
}
