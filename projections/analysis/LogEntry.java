package projections.analysis;

class LogEntry
{
    public int Replay, TransactionType, MsgType, Entry;
    long Time;
    int EventID, Dest, Pe;
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
}
