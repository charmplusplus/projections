package projections.analysis;

public abstract class ProjectionsActivity {

    public static final int REAL = 0;
    public static final int VIRTUAL = 1;

    private int type;      // whether REAL or VIRTUAL
    private int layerID;   // intended for language layer

    // unique identifier for all projections events
    public int pe;
    public int eventID;

    // required data fields
    public int activityID; // general name for eventType identification
    public long startTime;
    public long endTime;

    // special data field
    public VirtualIdentifier threadId; // general name

    public ProjectionsActivity(int type, int layerID) {
	this.type = type;
	this.layerID = layerID;
    }

}
