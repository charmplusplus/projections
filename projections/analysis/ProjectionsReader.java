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
 *  The use of ProjDefs as a superclass allows the reader to deal with 
 *  event types.
 *
 *  Base Assumption
 *  ---------------
 *  Each Reader is meant to be used for each file, typically a processor.
 *
 */
public abstract class ProjectionsReader
    extends ProjDefs
{ 
    private boolean available;
    protected String expectedVersion = null;

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
    public ProjectionsReader(String sourceString, String versionOverride) {
	expectedVersion = versionOverride;
	this.sourceString = sourceString;
	available = checkAvailable();
	if (available) {
	    try {
		readStaticData();
	    } catch (ProjectionsFormatException e) {
		System.err.println("Format Exception when reading from " +
				   "source [" + sourceString + "]");
		System.err.println(e.toString());
		System.err.println("Data is now marked as unavailable.");
		available = false;
	    } catch (IOException e) {
		System.err.println("Unexpected IO error when reading from " +
				   "source [" + sourceString + "]");
		System.err.println("Data is now marked as unavailable.");
		available = false;
	    }
	}
    }

    /**
     *  Wrapper constructor that sets the expectedVersion variable for data
     *  files that have no self-identifying version data (as with many of
     *  the older versions of projections logs).
     *
     *  If the implementing reader encounters a file that identifies itself
     *  otherwise, a ProjectionsFormatException is thrown.
     */
    public ProjectionsReader(String sourceString) {
	this(sourceString, null);
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

//    /**
//     *  This accessor can be used by external tools to determine if a
//     *  particular file (typically a processor) is available for use.
//     */
//    public final boolean isAvailable() {
//	return available;
//    }

    
//    /**
//     *  This accessor can be used by the inheriting class to mark
//     *  the file unavailable in response to more specialized conditions.
//     */
//    protected final void markUnavailable() {
//	available = false;
//    }

    
    /**
     *  INHERITANCE NOTE: Implementing classes should override this 
     *  method for the reading of static data stored in the target
     *  file. This method MUST return the number of bytes read. 
     */
    protected abstract void readStaticData() throws IOException;

//    /**
//     *  INHERITANCE NOTE: This is a public method that implementing
//     *  classes should use to bring the reader to a state where all
//     *  NON-STATIC data is unread.
//     */
//    public abstract void reset() throws IOException;

}
