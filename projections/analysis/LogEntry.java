package projections.analysis;

import projections.misc.*;

class LogEntry
{
    public int Replay, TransactionType, MsgType, Entry;
    long Time;
    int EventID, Pe;
    int MsgLen;
    ObjectId id;
    long recvTime;
    long sendTime;
    int destPEs[];
    long cpuBegin, cpuEnd;
    // PAPI entries/information
    int numPapiCounts;
    long papiCounts[];

    // AMPI function tracing. The duplication is unfortunate but required.
    int FunctionID;
    AmpiFunctionData ampiData;

    public void setAmpiData(int functionID, int lineNo, 
			    String sourceFileName) {
	ampiData = new AmpiFunctionData();
	ampiData.FunctionID = functionID;
	ampiData.LineNo = lineNo;
	ampiData.sourceFileName = sourceFileName;
    }

    /**
     *   A temporary measure to adapt LogEntryData objects to this class
     *   for use with timelines.
     */
    public static LogEntry adapt(LogEntryData data) {
	LogEntry log = new LogEntry();

	log.TransactionType = data.type;
	log.MsgType = data.mtype;
	log.Time = data.time;
	log.Entry = data.entry;
	log.EventID = data.event;
	log.Pe = data.pe;
	log.MsgLen = data.msglen;
	log.sendTime = data.sendTime;
	log.recvTime = data.recvTime;
	log.id = new ObjectId(data.id[0],data.id[1],data.id[2]);
	
	if (data.destPEs != null) {
	    log.destPEs = new int[data.destPEs.length];
	    for (int i=0;i<log.destPEs.length;i++) {
		log.destPEs[i] = data.destPEs[i];
	    }
	}
	
	log.cpuBegin = data.cpuStartTime;
	log.cpuEnd = data.cpuEndTime;
	log.numPapiCounts = data.numPerfCounts;
	log.papiCounts = new long[log.numPapiCounts];
	for (int i=0;i<log.numPapiCounts;i++) {
	    log.papiCounts[i] = data.perfCounts[i];
	}

	log.FunctionID = data.entry; // Kinda wierd alternative to entry
	if (data.funcName != null) {
	    log.setAmpiData(data.entry, data.lineNo, 
			    new String(data.funcName));
	}

	return log;
    }
}
