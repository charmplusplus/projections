package projections.analysis;

import java.io.IOException;

class ProjectionsFormatException 
    extends IOException
{

	private String expectedVersion;
    private String reason;

    protected ProjectionsFormatException(String expectedVersion, String reason) {
	this.expectedVersion = expectedVersion;
	this.reason = reason;
    }

    public String toString() {
	return "[ver:" + expectedVersion + "] - " + reason;
    }
}
