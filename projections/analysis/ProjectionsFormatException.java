package projections.analysis;

import java.io.*;

public class ProjectionsFormatException 
    extends IOException
{
    private String expectedVersion;

    public ProjectionsFormatException(String expectedVersion, String message) {
	super("[ver:" + expectedVersion + "] " + message);
	this.expectedVersion = expectedVersion;
    }

    public String getExpectedVersion() {
	return expectedVersion;
    }
}
