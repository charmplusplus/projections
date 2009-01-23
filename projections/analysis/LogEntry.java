package projections.analysis;

import projections.misc.*;

/** A class representing an entry in a log */
class LogEntry
{
	public int TransactionType, MsgType, Entry;
	long Time;
	int EventID, Pe;
	int MsgLen;
	ObjectId id;
	long recvTime;
	long sendTime;
	int numPEs;
	int destPEs[];
	long cpuBegin, cpuEnd;
	// PAPI entries/information
	int numPapiCounts;
	long papiCounts[];
	Integer userSupplied;
	Integer memoryUsage;

	// AMPI function tracing. The duplication is unfortunate but required.
	int FunctionID;
	AmpiFunctionData ampiData;

	String note;

	public void setAmpiData(int functionID, int lineNo, 
			String sourceFileName) {
		ampiData = new AmpiFunctionData();
		ampiData.FunctionID = functionID;
		ampiData.LineNo = lineNo;
		ampiData.sourceFileName = sourceFileName;
	}


	public LogEntry(LogEntryData data) {

		TransactionType = data.type;
		MsgType = data.mtype;
		Time = data.time;
		Entry = data.entry;
		EventID = data.event;
		Pe = data.pe;
		MsgLen = data.msglen;
		sendTime = data.sendTime;
		recvTime = data.recvTime;
		id = new ObjectId(data.id[0],data.id[1],data.id[2],data.id[3]);
		userSupplied = data.userSupplied;
		memoryUsage = data.memoryUsage;

		note = data.note;

		numPEs = data.numPEs;

		if (data.destPEs != null) {
			destPEs = new int[data.destPEs.length];
			for (int i=0;i<destPEs.length;i++) {
				destPEs[i] = data.destPEs[i];
			}
		}

		cpuBegin = data.cpuStartTime;
		cpuEnd = data.cpuEndTime;
		numPapiCounts = data.numPerfCounts;
		papiCounts = new long[numPapiCounts];
		for (int i=0;i<numPapiCounts;i++) {
			papiCounts[i] = data.perfCounts[i];
		}

		FunctionID = data.entry; // Kinda wierd alternative to entry
		if (data.funcName != null) {
			setAmpiData(data.entry, data.lineNo, 
					new String(data.funcName));
		}

	}

	public Integer userSuppliedValue() {
		return userSupplied;
	}

	public Integer memoryUsage() {
		return memoryUsage;
	}

//	public void setUserSupplied(int userSuppliedValue) {
//		userSupplied = new Integer(userSuppliedValue);
//	}

}
