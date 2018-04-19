package projections.analysis;

import projections.misc.LogEntryData;

/** A class representing an entry in a log */
class LogEntry
{
	protected int TransactionType;
	protected int Entry;
	long Time;
	long endTime;
	int EventID, Pe;
	int MsgLen;
	ObjectId id;
	long recvTime;
	int numPEs;
	int destPEs[];
	long cpuBegin, cpuEnd;
	// PAPI entries/information
	int numPapiCounts;
	long papiCounts[];
	private Integer userSupplied;
	private long memoryUsage;
	//UserStat variables
	double stat;
	double userTime;
	// AMPI function tracing. The duplication is unfortunate but required.
	int FunctionID;
	AmpiFunctionData ampiData;

	int nestedID; // Nested thread ID, e.g. virtual AMPI ranks

	String note;

	private void setAmpiData(int functionID, int lineNo, 
			String sourceFileName) {
		ampiData = new AmpiFunctionData();
		ampiData.FunctionID = functionID;
		ampiData.LineNo = lineNo;
		ampiData.sourceFileName = sourceFileName;
	}


	protected LogEntry(LogEntryData data) {
		endTime = data.endTime;
		TransactionType = data.type;
		Time = data.time;
		Entry = data.entry;
		EventID = data.event;
		Pe = data.pe;
		MsgLen = data.msglen;
		recvTime = data.recvTime;
		id = new ObjectId(data.id);
		userSupplied = data.userSupplied;
		memoryUsage = data.memoryUsage;
		stat = data.stat;
		note = data.note;
		userTime = data.userTime;
		numPEs = data.numPEs;
		nestedID = data.nestedID;

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

	protected Integer userSuppliedValue() {
		return userSupplied;
	}

	protected long memoryUsage() {
		return memoryUsage;
	}

//	public void setUserSupplied(int userSuppliedValue) {
//		userSupplied = new Integer(userSuppliedValue);
//	}

}
