package projections.analysis;

import java.lang.*;
import java.io.*;
import java.util.*;

import projections.gui.*;
import projections.misc.*;

public class UsageCalc extends ProjDefs
{
    private long beginTime,endTime;
    private long startTime;
    private int pnum;
    private int dataLen;
    private long packtime,packstarttime;
    private long unpacktime,unpackstarttime;
    private int numUserEntries;
    private double version;
    private int countflag=0;
    
    // curEntry is global because it has to deal with the relationship
    // between BEGIN_PROCESSING and END_PROCESSING events within the
    // same log file.
    //
    // it needs, however, to be reset between the reading of two log files.
    private int curEntry = -1;
    
    private void intervalCalc(float[][] data,int type, int entry, long time) {

	if (type != CREATION) {
	    if (!(time>beginTime+1)) {
		time=beginTime;
		countflag = 0;
	    } else {
		if (countflag != 2)
		    countflag = 1;
	    }
	    if (time>endTime) {
		time=endTime;
		countflag = 2;
	    } else {
		if(countflag != 0) {
		    countflag =1;
		}
	    }
	} else {
	    if (countflag != 1) {
		time = 0;
	    }
	}

	switch(type) {
	case BEGIN_PROCESSING:
	    packtime = 0;
	    unpacktime = 0;
	    curEntry = entry;
	    startTime = time;
	    break;
	case END_PROCESSING:
	    // curEntry == -1 means that there was no corresponding 
	    // BEGIN_PROCESSING event, if so ignore the entrypoint
	    if (curEntry != -1)		
	    	data[0][curEntry] += 
		    (float )((time - startTime) - packtime - unpacktime);
	    break;
	case CREATION:
	    if(curEntry != -1){
		data[1][curEntry] = data[1][curEntry] + (float )time;
	    }
	    break;
	case BEGIN_IDLE:
	    startTime = time;
	    break;
	case END_IDLE:
	    // +2 places Idle time at the top of the usage profile display
	    data[0][numUserEntries+2] += (float )(time - startTime);
	    break;
	case BEGIN_PACK:
	    packstarttime = time;
	    break;
	case END_PACK:
	    // Packing is the first non-entry data item to be displayed
	    // in the profile window.
	    packtime += time - packstarttime;
	    data[0][numUserEntries] += (float )(time - packstarttime);
	    break;
	case BEGIN_UNPACK:
	    unpackstarttime = time;
	    break;
	case END_UNPACK:
	    // Unpacking is the second non-entry data item to be displayed
	    // in the profile window.
	    unpacktime += time - unpackstarttime;
	    data[0][numUserEntries+1] += (float)(time - unpackstarttime);
	    break;
	default:
	    /*ignore it*/
	};
    }

    public float[][] usage(int procnum, long begintime, 
			   long endtime, double v) {
	long time;
	long sendTime;
	int type;
	int entry;
	int len;
	float total;
	version = v;
	beginTime = begintime;
	endTime = endtime;
	pnum = procnum;
	numUserEntries = Analysis.getNumUserEntries();
	dataLen = numUserEntries + 4;

	float[][] data = new float[2][dataLen];
	// initialization
	for(int i=0;i<dataLen;i++){
	    data[0][i] = (float )0.0;
	    data[1][i] = (float )0.0;
	}

	try {
	    FileReader file = new FileReader(Analysis.getLogName(pnum));
	    AsciiIntegerReader log =
		new AsciiIntegerReader(new BufferedReader(file));
	    curEntry = -1;
	    log.nextLine(); // The first line contains junk
	    //The second line gives the program start time
	    log.nextInt();
	
	    startTime = 0;
	    time=0;
	    try { 
		while (time<endTime) { //EOF exception terminates loop
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
		    case CREATION:
			log.nextInt();  // mtype
			log.nextInt();  // ep idx
			log.nextLong(); // time stamp
			log.nextInt();  // event id
			log.nextInt();  // pe id
			if (version > 1.0)
			    log.nextInt();  // msg length
			if(version > 4.9) {
			    sendTime = log.nextLong();  // send time
			    // System.out.println("SendTime "+sendTime);
			} else {
			    sendTime = 0;
			}
			intervalCalc(data,type,0,sendTime);
			break;
		    default:
				/*Ignore it.*/
		    }
		}
	    } catch (EOFException e) {
		log.close();
	    } catch (IOException e) {
		log.close();
	    }
	} catch (IOException e) {
	    System.out.println("Exception while reading log file "+pnum); 
	}
	total = 0;
	for (int j=0; j<(dataLen-1); j++) { //Scale times to percent
	    // System.out.println("Data " + data[0][j] + " Send Time " + 
	    // data[1][j]);
	    data[0][j] = data[0][j] - data[1][j];
	    data[0][j] = 
		(float )(100.0*data[0][j])/(float )(endTime-beginTime);
	    data[1][j] = 
		(float )(100.0*data[1][j])/(float )(endTime-beginTime);
	}
	return data;
    }
}
