package projections.analysis;

import projections.misc.*;

import java.io.*;

public interface PointCapableReader {
    
    public void nextEvent(LogEntryData data)
        throws IOException, EOFException;

    public void nextEventOnOrAfter(long timestamp, LogEntryData data)
        throws IOException, EOFException;    

    public void nextEventOfType(int eventType, LogEntryData data)
        throws IOException, EOFException;    

    public void nextEventOfTypeOnOrAfter(int eventType, long timestamp,
					 LogEntryData data)
        throws IOException, EOFException;    

}
