package projections.misc;

/**
 *
 *  Written by Chee Wai Lee
 *  (with sniplets of code from StsReader.java in projections.analysis package)
 *  3/28/2002
 *
 *  EntryTypeData stores information about an EP from a .sts file.
 *
 */

public class EntryTypeData {
    public String name;  // full function header of the entry point.
    public int chareID;  // the chare in which this entry point resides.
    public int msgID;    // source message id.
}
