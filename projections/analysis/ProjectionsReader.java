package projections.analysis;

import java.io.*;

/**
 *  The base class for all future projections readers. It provides basic
 *  status flags and information that will facilitate future implementations 
 *  of caching of projections data reading.
 *
 *  UNAVAILABLE indicates that the source either does not exist or is not
 *  readable.
 *
 *  Each Reader is meant to be used for each file, typically a processor.
 *
 */
public abstract class ProjectionsReader
{
    private boolean available;

    protected long bytesRead;
    // this can be any identifying string - full path name (default),
    // network address, URL etc ...
    protected String sourceString;

    /**
     *  INHERITANCE NOTE:
     *
     *  Any subclass *must* call this base class constructor for
     *  correctness.
     *
     *  any decision to read data at construction time must be made
     *  by the subclass' constructor. The default constructor will
     *  only perform the necessary checks.
     */
    public ProjectionsReader(String sourceString) {
	bytesRead = 0;
	this.sourceString = sourceString;
	available = checkAvailable();
    }

    /**
     *  Implementing Subclasses should implement the code for determining
     *  if a specified source is available.
     *
     *  Here's an example using a File source:
     *
     *     protected boolean checkAvailable() {
     *        File sourceFile = new File(sourceString);
     *        // canRead() checks for existence by default.
     *        return sourceFile.canRead();
     *     }
     */
    protected abstract boolean checkAvailable();

    /**
     *  This accessor can be used by external tools to determine if a
     *  particular file (typically a processor) is available for use.
     */
    public final boolean isAvailable() {
	return available;
    }

    /**
     *  This is the public interface for getting the reader to load
     *  data into its structures.
     *  The calling tool can choose to ignore IO exceptions
     *  and continue to work in the absense of useful data.
     */
    public final void readData()
	throws IOException
    {
	if (!isAvailable()) {
	    throw new IOException("Data from source [" + sourceString + 
				  "] unavailable. Read Attempt failed!");
	} else {
	    try {
		bytesRead = read();
	    } catch (ProjectionsFormatException e) {
		// data was found to be corrupt, hence unavailable.
		bytesRead = -1;
		available = false;
		throw new IOException("[" + sourceString + "] : " + 
				      e.toString());
	    }
	}
    }

    /**
     *  Every subclass of ProjectionsReader must have a read
     *  method to load data into the reader on a JIT manner.
     *
     *  The read method is expected to return a byte count on
     *  the amount of data read. A -1 is to be returned if 
     *  the attempt to read resulted in an exception.
     */
    protected abstract long read() throws IOException;
}
