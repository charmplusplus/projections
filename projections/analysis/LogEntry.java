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
    // AMPI function tracing
    int FunctionID, LineNo;
    String sourceFileName;
}
