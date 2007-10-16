package projections.analysis;

import java.io.*;

public class ProjectionsFormatException 
    extends IOException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String expectedVersion;
    private String reason;

    public ProjectionsFormatException(String expectedVersion, String reason) {
	this.expectedVersion = expectedVersion;
	this.reason = reason;
    }

    public String getExpectedVersion() {
	return expectedVersion;
    }

    public String getReason() {
	return reason;
    }

    public String toString() {
	return "[ver:" + expectedVersion + "] - " + reason;
    }
}
