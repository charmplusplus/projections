package projections.analysis;

import java.io.*;

/**
 *  The base class for all future projections readers. It provides basic
 *  status flags and information that will facilitate future implementations 
 *  of caching of projections data reading.
 */
public abstract class ProjectionsReader
{
    // static status constants
    protected static final int UNAVAILABLE = 0;
    protected static final int UNREAD = 1;
    protected static final int READ = 2;
    
    protected int status;
    protected long bytesRead;
    
    /**
     *  INHERITANCE NOTE:
     *  The constructor should set the initial status of the reader.
     *  1) Locate the file. If the file is not found, set the status
     *     flag to UNAVAILABLE.
     *  2) otherwise, set the flag to UNREAD.
     *
     *  Any subclass *must* call this base class constructor for
     *  correctness.
     *
     *  any decision to read data at construction time must be made
     *  by the subclass' constructor. The default constructor will
     *  only perform the necessary checks.
     */
    public ProjectionsReader() {
	bytesRead = 0;
	if (isAvailable()) {
	    status = UNREAD;
	} else {
	    status = UNAVAILABLE;
	}
    }

    /**
     *  Subclasses should implement the ability to recognize files of its own
     *  type (eg. summary detail, logs)
     */
    protected abstract boolean isAvailable();

    /**
     *  This is the public interface for getting the reader to load
     *  data into its structures.
     */
    public void readData()
	throws IOException
    {
	switch (status) {
	case READ:
	    // data is already in the reader object. No need to re-read
	    // the data. Should not happen, hence throw exception as 
	    // warning.
	    throw new IOException("Warning: Data already read into memory!");
	case UNAVAILABLE:
	    throw new IOException("Data unavailable, cannot be read!");
	case UNREAD:
	    try {
		bytesRead = read();
		status = READ;
	    } catch (IOException e) {
		// data was found to be corrupt, hence unavailable.
		// the calling tool can choose to ignore this exception
		// and continue to work in the absense of useful data.
		bytesRead = -1;
		status = UNAVAILABLE;
		throw new IOException(e.toString());
	    }
	    break;
	default:
	    System.err.println("Unrecognized status flag. Catastrophic " +
			       "error. Exiting.");
	    System.exit(-1);
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

    /**
     *  Every subclass of ProjectionsReader should be able to drop
     *  data when no longer in use for reclamation by the garbage
     *  collector. This allows caching schemes to reclaim memory
     *  for other data required by the user.
     */
    public void evictData() 
	throws IOException
    {
	nullifyData();
	if (status != READ) {
	    throw new IOException("Warning: Evicting non-existent data!");
	}
	status = UNREAD;
    }

    /**
     *  The subclass must implement the details of how its data should
     *  be nullified for garbage collection.
     */
    protected abstract void nullifyData();
}
