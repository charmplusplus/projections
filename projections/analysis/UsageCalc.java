package projections.analysis;

import java.lang.*;
import java.io.*;
import projections.misc.*;
import java.util.*;

public class UsageCalc extends ProjDefs
{
    private String fileName;
    private int beginTime;
    private int endTime;
    private int startTime;
    private int pnum;
    private float data[];
    private int dataLen;
    private int packtime;
    private int packstarttime;
    private int unpacktime;
    private int unpackstarttime;
    private int curEntry = -1;
    private int numUserEntries;

    public UsageCalc(String ArgV, int numUserEnt)
    {
	fileName = ArgV;
	dataLen = numUserEnt + 4;
	numUserEntries = numUserEnt;
	data = new float[dataLen];
    }
    
    public void ReadInLogFiles(int procnum, int begintime, int endtime) 
    {
	int time;
	int type;
	int entry;
	int progStartTime;
	int len;
	float total;
	beginTime = begintime;
	endTime = endtime;
	pnum = procnum;

	for(int i=0; i<dataLen; i++)
	    data[i] = 0;

	try {
		FileReader file = new FileReader(fileName + "."+pnum+".log");
		AsciiIntegerReader log=new AsciiIntegerReader(new BufferedReader(file));
		log.nextLine(); // The first line contains junk
		//The second line gives the program start time
		log.nextInt();
		progStartTime = log.nextInt();
	
		startTime = 0;
		time=0;
		try { while (time<endTime) { //EOF exception terminates loop
			log.nextLine();//Skip old junk at end of line
			type=log.nextInt();
			switch(type) {
			case BEGIN_IDLE: case END_IDLE:
			case BEGIN_PACK: case END_PACK:
			case BEGIN_UNPACK: case END_UNPACK:
				time = log.nextInt();
				intervalCalc(type, 0, (time-progStartTime));
				break;
			case BEGIN_PROCESSING: case END_PROCESSING:
				log.nextInt(); //skip message type
				entry = log.nextInt();
				time = log.nextInt();
				intervalCalc(type, entry, (time-progStartTime));
				break;
			default:
				/*Ignore it.*/
			}
		}} catch (EOFException e) {
			log.close();
		}
	}
	catch (IOException e)
	    {System.out.println("Exception while reading log files"); }
	total = 0;
	for(int j=0; j<(dataLen-1); j++) //Scale times to percent
		data[j] = 100*data[j]/(endTime-beginTime);
    }

    private void intervalCalc(int type, int entry, int time)
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
	    data[curEntry] += (time - startTime) - packtime - unpacktime;
		break;

	case BEGIN_IDLE:
	    startTime = time;
		break;
	case END_IDLE:
	    data[numUserEntries] += (time - startTime);
		break;

	case BEGIN_PACK:
		packstarttime = time;
		break;
	case END_PACK:
		packtime += time - packstarttime;
		data[numUserEntries+1] += (time - packstarttime);
		break;

	case BEGIN_UNPACK:
	    unpackstarttime = time;
		break;
	case END_UNPACK:
		unpacktime += time - unpackstarttime;
		data[numUserEntries+2] += (time - unpackstarttime);
		break;
	default:
		/*ignore it*/
	};
    }
    
    public float[] getData()
    { return data; }
}















