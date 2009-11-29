package projections.analysis;

import java.io.IOException;

public class ProjectionsFormatException 
    extends IOException
{

	private String expectedVersion;
    private String reason;

    public ProjectionsFormatException(String expectedVersion, String reason) {
	this.expectedVersion = expectedVersion;
	this.reason = reason;
    }



    public String toString() {
	return "[ver:" + expectedVersion + "] - " + reason;
    }
}
