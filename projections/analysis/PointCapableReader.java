package projections.analysis;

import projections.misc.*;

import java.io.*;

public interface PointCapableReader {
    
    public LogEntryData nextEvent()
        throws IOException, EOFException;

    public LogEntryData nextEventOnOrAfter(long timestamp)
        throws IOException, EOFException;    

    public LogEntryData nextEventOfType(int eventType)
        throws IOException, EOFException;    

    public LogEntryData nextEventOfTypeOnOrAfter(int eventType, long timestamp)
        throws IOException, EOFException; 

}
